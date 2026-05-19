/**
 * Browser screen capture with chunked S3 multipart upload.
 */

const CHUNK_INTERVAL_MS = 5000;
const MIN_PART_SIZE = 5 * 1024 * 1024; // S3 multipart minimum except last part

export class ScreenRecorder {
  constructor(api) {
    this.api = api;
    this.mediaRecorder = null;
    this.stream = null;
    this.chunks = [];
    this.sessionId = null;
    this.uploadState = null;
    this.partNumber = 1;
    this.pendingParts = [];
    this.totalBytes = 0;
    this.startTime = null;
    this.onProgress = null;
    this.onError = null;
    this._uploading = false;
  }

  async start(sessionId) {
    this.sessionId = sessionId;
    this.chunks = [];
    this.partNumber = 1;
    this.pendingParts = [];
    this.totalBytes = 0;
    this.startTime = Date.now();

    this.stream = await navigator.mediaDevices.getDisplayMedia({
      video: { cursor: 'always' },
      audio: true,
    });

    const mimeType = MediaRecorder.isTypeSupported('video/webm;codecs=vp9,opus')
      ? 'video/webm;codecs=vp9,opus'
      : 'video/webm';

    const initRes = await this.api.initMultipartUpload({
      sessionId,
      fileName: `session-${sessionId}-${Date.now()}.webm`,
      contentType: mimeType,
    });
    this.uploadState = initRes.data;

    this.mediaRecorder = new MediaRecorder(this.stream, { mimeType });
    this.mediaRecorder.ondataavailable = (e) => {
      if (e.data && e.data.size > 0) {
        this.chunks.push(e.data);
        this.totalBytes += e.data.size;
        this._maybeUploadPart();
      }
    };

    this.mediaRecorder.onstop = () => {
      this.stream.getTracks().forEach((t) => t.stop());
    };

    streamOnEnd(this.stream, () => this.stop());

    this.mediaRecorder.start(CHUNK_INTERVAL_MS);
    return this.uploadState;
  }

  async _maybeUploadPart() {
    if (this._uploading || this.chunks.length === 0) return;
    const blob = new Blob(this.chunks, { type: this.mediaRecorder?.mimeType || 'video/webm' });
    if (blob.size < MIN_PART_SIZE && this.mediaRecorder?.state === 'recording') return;

    this._uploading = true;
    const partBlob = blob;
    this.chunks = [];

    try {
      const partRes = await this.api.getMultipartPartUrl({
        s3Key: this.uploadState.s3Key,
        uploadId: this.uploadState.uploadId,
        partNumber: this.partNumber,
      });

      const uploadRes = await fetch(partRes.data.uploadUrl, {
        method: 'PUT',
        body: partBlob,
      });
      if (!uploadRes.ok) throw new Error(`Part upload failed: ${uploadRes.status}`);
      const etag = uploadRes.headers.get('ETag')?.replace(/"/g, '');
      this.pendingParts.push({ partNumber: this.partNumber, etag });
      this.partNumber += 1;

      if (this.onProgress) {
        this.onProgress({ uploadedParts: this.pendingParts.length, totalBytes: this.totalBytes });
      }
    } catch (err) {
      if (this.onError) this.onError(err);
    } finally {
      this._uploading = false;
      if (this.chunks.length > 0) this._maybeUploadPart();
    }
  }

  async stop() {
    if (!this.mediaRecorder || this.mediaRecorder.state === 'inactive') {
      return null;
    }

    return new Promise((resolve, reject) => {
      this.mediaRecorder.onstop = async () => {
        try {
          this.stream?.getTracks().forEach((t) => t.stop());

          if (this.chunks.length > 0) {
            const finalBlob = new Blob(this.chunks, { type: this.mediaRecorder.mimeType });
            const partRes = await this.api.getMultipartPartUrl({
              s3Key: this.uploadState.s3Key,
              uploadId: this.uploadState.uploadId,
              partNumber: this.partNumber,
            });
            const uploadRes = await fetch(partRes.data.uploadUrl, {
              method: 'PUT',
              body: finalBlob,
            });
            if (!uploadRes.ok) throw new Error(`Final part upload failed: ${uploadRes.status}`);
            const etag = uploadRes.headers.get('ETag')?.replace(/"/g, '');
            this.pendingParts.push({ partNumber: this.partNumber, etag });
            this.totalBytes += finalBlob.size;
          }

          const duration = Math.round((Date.now() - this.startTime) / 1000);
          const completeRes = await this.api.completeMultipartUpload({
            sessionId: this.sessionId,
            s3Key: this.uploadState.s3Key,
            uploadId: this.uploadState.uploadId,
            fileName: `session-${this.sessionId}.webm`,
            contentType: this.mediaRecorder.mimeType,
            fileSize: this.totalBytes,
            duration,
            parts: this.pendingParts,
          });

          if (this.onProgress) this.onProgress({ uploadedParts: this.pendingParts.length, totalBytes: this.totalBytes, done: true });
          resolve(completeRes.data);
        } catch (err) {
          if (this.onError) this.onError(err);
          reject(err);
        }
      };

      this.mediaRecorder.stop();
    });
  }

  isRecording() {
    return this.mediaRecorder?.state === 'recording';
  }
}

function streamOnEnd(stream, callback) {
  stream.getVideoTracks().forEach((track) => {
    track.onended = callback;
  });
}

export default ScreenRecorder;
