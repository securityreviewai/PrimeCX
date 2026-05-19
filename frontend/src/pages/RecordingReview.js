import React, { useState, useEffect, useRef, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  getRecordingPlayback,
  saveRecordingRedactions,
  getRecordingAudit,
  createQaReview,
  getRetentionPolicy,
} from '../services/api';

const colors = {
  primary: '#4F46E5', success: '#10B981', warning: '#F59E0B',
  danger: '#EF4444', gray100: '#F3F4F6', gray200: '#E5E7EB',
  gray500: '#6B7280', gray700: '#374151', gray900: '#111827',
};

const s = {
  heading: { fontSize: 24, fontWeight: 700, color: colors.gray900, marginBottom: 8 },
  sub: { fontSize: 14, color: colors.gray500, marginBottom: 24 },
  grid: { display: 'grid', gridTemplateColumns: '1fr 360px', gap: 24, alignItems: 'start' },
  card: { background: '#fff', borderRadius: 12, padding: 24, boxShadow: '0 1px 4px rgba(0,0,0,0.06)', marginBottom: 24 },
  cardTitle: { fontSize: 16, fontWeight: 700, color: colors.gray900, marginBottom: 16 },
  videoWrap: { position: 'relative', background: '#000', borderRadius: 8, overflow: 'hidden' },
  video: { width: '100%', display: 'block', maxHeight: 480 },
  redaction: {
    position: 'absolute', backdropFilter: 'blur(12px)', WebkitBackdropFilter: 'blur(12px)',
    background: 'rgba(0,0,0,0.15)', border: '2px dashed rgba(255,255,255,0.4)', cursor: 'pointer',
  },
  label: { fontSize: 13, fontWeight: 600, color: colors.gray700, marginBottom: 6, display: 'block' },
  input: {
    width: '100%', padding: '8px 12px', borderRadius: 8, border: `1px solid ${colors.gray200}`,
    fontSize: 14, marginBottom: 12,
  },
  btn: {
    padding: '10px 18px', borderRadius: 8, border: 'none', fontSize: 14,
    fontWeight: 600, cursor: 'pointer', color: '#fff', background: colors.primary,
  },
  btnSecondary: {
    padding: '8px 14px', borderRadius: 8, border: `1px solid ${colors.gray200}`,
    fontSize: 13, fontWeight: 600, cursor: 'pointer', background: '#fff', color: colors.gray700,
    marginRight: 8,
  },
  scoreRow: { display: 'flex', gap: 12, marginBottom: 12 },
  scoreInput: { width: 60, textAlign: 'center' },
  reviewCard: {
    padding: 12, borderRadius: 8, border: `1px solid ${colors.gray200}`, marginBottom: 8, fontSize: 13,
  },
  auditRow: { fontSize: 12, color: colors.gray500, padding: '6px 0', borderBottom: `1px solid ${colors.gray100}` },
  empty: { color: colors.gray500, fontSize: 14, textAlign: 'center', padding: 32 },
};

function ScoreInput({ label, value, onChange }) {
  return (
    <div style={{ flex: 1 }}>
      <label style={s.label}>{label} (1-5)</label>
      <input
        type="number" min={1} max={5} style={{ ...s.input, ...s.scoreInput }}
        value={value} onChange={(e) => onChange(Number(e.target.value))}
      />
    </div>
  );
}

export default function RecordingReview({ user }) {
  const { id } = useParams();
  const navigate = useNavigate();
  const videoRef = useRef(null);
  const wrapRef = useRef(null);

  const [playback, setPlayback] = useState(null);
  const [audit, setAudit] = useState([]);
  const [retention, setRetention] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [redactions, setRedactions] = useState([]);
  const [drawing, setDrawing] = useState(false);
  const [drawStart, setDrawStart] = useState(null);
  const [draftRegion, setDraftRegion] = useState(null);
  const [savingRedactions, setSavingRedactions] = useState(false);
  const [qaForm, setQaForm] = useState({ empathyScore: 3, accuracyScore: 3, complianceScore: 3, notes: '' });
  const [submittingQa, setSubmittingQa] = useState(false);

  const load = useCallback(async () => {
    try {
      setLoading(true);
      const [pbRes, auditRes, polRes] = await Promise.all([
        getRecordingPlayback(id),
        getRecordingAudit(id),
        getRetentionPolicy(),
      ]);
      setPlayback(pbRes.data);
      setRedactions(pbRes.data.redactionRegions || []);
      setAudit(auditRes.data);
      setRetention(polRes.data);
    } catch {
      setError('Failed to load recording or access denied');
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => { load(); }, [load]);

  const normalizedToPixel = (region) => {
    const wrap = wrapRef.current;
    if (!wrap) return region;
    return {
      ...region,
      px: region.x * wrap.clientWidth,
      py: region.y * wrap.clientHeight,
      pw: region.width * wrap.clientWidth,
      ph: region.height * wrap.clientHeight,
    };
  };

  const handleMouseDown = (e) => {
    if (!wrapRef.current) return;
    const rect = wrapRef.current.getBoundingClientRect();
    setDrawing(true);
    setDrawStart({ x: e.clientX - rect.left, y: e.clientY - rect.top });
    setDraftRegion(null);
  };

  const handleMouseMove = (e) => {
    if (!drawing || !drawStart || !wrapRef.current) return;
    const rect = wrapRef.current.getBoundingClientRect();
    const x = e.clientX - rect.left;
    const y = e.clientY - rect.top;
    setDraftRegion({
      x: Math.min(drawStart.x, x),
      y: Math.min(drawStart.y, y),
      width: Math.abs(x - drawStart.x),
      height: Math.abs(y - drawStart.y),
    });
  };

  const handleMouseUp = () => {
    if (!drawing || !draftRegion || !wrapRef.current) {
      setDrawing(false);
      return;
    }
    const w = wrapRef.current.clientWidth;
    const h = wrapRef.current.clientHeight;
    if (draftRegion.width > 20 && draftRegion.height > 20) {
      setRedactions((prev) => [...prev, {
        x: draftRegion.x / w,
        y: draftRegion.y / h,
        width: draftRegion.width / w,
        height: draftRegion.height / h,
        label: 'Sensitive',
      }]);
    }
    setDrawing(false);
    setDrawStart(null);
    setDraftRegion(null);
  };

  const handleSaveRedactions = async () => {
    try {
      setSavingRedactions(true);
      await saveRecordingRedactions(id, redactions);
      await load();
    } catch {
      setError('Failed to save redactions');
    } finally {
      setSavingRedactions(false);
    }
  };

  const handleSubmitQa = async (e) => {
    e.preventDefault();
    try {
      setSubmittingQa(true);
      await createQaReview(id, qaForm);
      await load();
      setQaForm({ empathyScore: 3, accuracyScore: 3, complianceScore: 3, notes: '' });
    } catch {
      setError('Failed to submit QA review');
    } finally {
      setSubmittingQa(false);
    }
  };

  if (loading) return <div style={s.empty}>Loading recording...</div>;
  if (error) return <div style={{ ...s.empty, color: colors.danger }}>{error}</div>;
  if (!playback) return null;

  const role = user?.role;
  const canReview = role === 'support_manager' || role === 'support_admin';

  return (
    <div>
      <button style={{ ...s.btnSecondary, marginBottom: 16 }} onClick={() => navigate(-1)}>
        &larr; Back
      </button>
      <h1 style={s.heading}>Recording Review #{playback.id}</h1>
      <p style={s.sub}>
        Session #{playback.sessionId} &middot; {playback.durationSeconds}s &middot;
        {playback.retentionExpiresAt
          ? ` Retention until ${new Date(playback.retentionExpiresAt).toLocaleDateString()}`
          : ''}
      </p>

      <div style={s.grid}>
        <div>
          <div style={s.card}>
            <h3 style={s.cardTitle}>Playback</h3>
            <div
              ref={wrapRef}
              style={s.videoWrap}
              onMouseDown={canReview ? handleMouseDown : undefined}
              onMouseMove={canReview ? handleMouseMove : undefined}
              onMouseUp={canReview ? handleMouseUp : undefined}
              onMouseLeave={canReview ? handleMouseUp : undefined}
            >
              <video
                ref={videoRef}
                style={s.video}
                controls
                src={playback.presignedUrl}
                crossOrigin="anonymous"
              />
              {redactions.map((r, i) => {
                const p = normalizedToPixel(r);
                return (
                  <div
                    key={i}
                    style={{
                      ...s.redaction,
                      left: p.px, top: p.py, width: p.pw, height: p.ph,
                    }}
                    title={r.label}
                    onClick={(e) => { e.stopPropagation(); setRedactions((prev) => prev.filter((_, j) => j !== i)); }}
                  />
                );
              })}
              {draftRegion && (
                <div style={{
                  ...s.redaction,
                  left: draftRegion.x, top: draftRegion.y,
                  width: draftRegion.width, height: draftRegion.height,
                  borderColor: colors.primary,
                }} />
              )}
            </div>
            {canReview && (
              <div style={{ marginTop: 12, display: 'flex', gap: 8, alignItems: 'center' }}>
                <span style={{ fontSize: 13, color: colors.gray500 }}>Drag on video to add blur regions. Click region to remove.</span>
                <button style={s.btn} onClick={handleSaveRedactions} disabled={savingRedactions}>
                  {savingRedactions ? 'Saving...' : 'Save Redactions'}
                </button>
              </div>
            )}
          </div>

          {playback.transcript && (
            <div style={s.card}>
              <h3 style={s.cardTitle}>Auto-Transcript</h3>
              <p style={{ fontSize: 14, color: colors.gray700, lineHeight: 1.6, whiteSpace: 'pre-wrap' }}>
                {playback.transcript}
              </p>
            </div>
          )}
        </div>

        <div>
          {canReview && (
            <>
              <div style={s.card}>
                <h3 style={s.cardTitle}>QA Scorecard</h3>
                <form onSubmit={handleSubmitQa}>
                  <div style={s.scoreRow}>
                    <ScoreInput label="Empathy" value={qaForm.empathyScore}
                      onChange={(v) => setQaForm((f) => ({ ...f, empathyScore: v }))} />
                    <ScoreInput label="Accuracy" value={qaForm.accuracyScore}
                      onChange={(v) => setQaForm((f) => ({ ...f, accuracyScore: v }))} />
                    <ScoreInput label="Compliance" value={qaForm.complianceScore}
                      onChange={(v) => setQaForm((f) => ({ ...f, complianceScore: v }))} />
                  </div>
                  <label style={s.label}>Notes</label>
                  <textarea
                    style={{ ...s.input, minHeight: 80, fontFamily: 'inherit' }}
                    value={qaForm.notes}
                    onChange={(e) => setQaForm((f) => ({ ...f, notes: e.target.value }))}
                    placeholder="Coaching notes..."
                  />
                  <button type="submit" style={s.btn} disabled={submittingQa}>
                    {submittingQa ? 'Submitting...' : 'Submit Review'}
                  </button>
                </form>
              </div>

              <div style={s.card}>
                <h3 style={s.cardTitle}>Previous QA Reviews</h3>
                {(playback.qaReviews || []).length === 0 ? (
                  <div style={s.empty}>No reviews yet</div>
                ) : (
                  playback.qaReviews.map((r) => (
                    <div key={r.id} style={s.reviewCard}>
                      <div style={{ fontWeight: 600 }}>{r.reviewerName}</div>
                      <div style={{ marginTop: 4 }}>
                        Empathy {r.empathyScore} &middot; Accuracy {r.accuracyScore} &middot;
                        Compliance {r.complianceScore} &middot; Overall {r.overallScore}/5
                      </div>
                      {r.notes && <div style={{ marginTop: 6, color: colors.gray500 }}>{r.notes}</div>}
                      <div style={{ fontSize: 11, color: colors.gray500, marginTop: 4 }}>
                        {new Date(r.createdAt).toLocaleString()}
                      </div>
                    </div>
                  ))
                )}
              </div>
            </>
          )}

          <div style={s.card}>
            <h3 style={s.cardTitle}>Retention Policy</h3>
            {retention && (
              <div style={{ fontSize: 13, color: colors.gray700, lineHeight: 1.8 }}>
                <div>Retention: {retention.retentionDays} days</div>
                <div>Move to IA: {retention.transitionToIaDays} days</div>
                <div>Move to Glacier: {retention.transitionToGlacierDays} days</div>
                <div>Auto-transcription: {retention.autoTranscriptionEnabled ? 'On' : 'Off'}</div>
                <div>Auto-analysis on session end: {retention.autoAnalysisOnSessionEnd ? 'On' : 'Off'}</div>
              </div>
            )}
          </div>

          {canReview && (
            <div style={s.card}>
              <h3 style={s.cardTitle}>Access Audit Log</h3>
              {audit.length === 0 ? (
                <div style={s.empty}>No access events</div>
              ) : (
                audit.map((a) => (
                  <div key={a.id} style={s.auditRow}>
                    {new Date(a.accessedAt).toLocaleString()} — {a.userName} ({a.accessType})
                    {a.ipAddress && ` from ${a.ipAddress}`}
                  </div>
                ))
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
