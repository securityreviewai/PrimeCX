import React, { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { getTickets, startSession, endSession, recordingApi } from '../services/api';
import ScreenRecorder from '../utils/screenRecorder';

const colors = {
  primary: '#4F46E5', success: '#10B981', warning: '#F59E0B',
  danger: '#EF4444', gray100: '#F3F4F6', gray200: '#E5E7EB',
  gray500: '#6B7280', gray700: '#374151', gray900: '#111827',
};

const statusColors = { OPEN: colors.primary, IN_PROGRESS: colors.warning, RESOLVED: colors.success, CLOSED: colors.gray500 };

const styles = {
  heading: { fontSize: 24, fontWeight: 700, color: colors.gray900, marginBottom: 24 },
  grid: { display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 24, alignItems: 'start' },
  card: { background: '#fff', borderRadius: 12, padding: 24, boxShadow: '0 1px 4px rgba(0,0,0,0.06)' },
  cardTitle: { fontSize: 16, fontWeight: 700, color: colors.gray900, marginBottom: 16 },
  ticketRow: {
    display: 'flex', alignItems: 'center', justifyContent: 'space-between',
    padding: '12px 14px', borderRadius: 8, marginBottom: 8,
    border: `1px solid ${colors.gray200}`,
  },
  badge: {
    display: 'inline-block', padding: '3px 10px', borderRadius: 20, fontSize: 12,
    fontWeight: 600, textTransform: 'uppercase',
  },
  btn: {
    padding: '8px 16px', borderRadius: 8, border: 'none', fontSize: 13,
    fontWeight: 600, cursor: 'pointer', color: '#fff',
  },
  sessionPanel: {
    background: 'linear-gradient(135deg, #EEF2FF, #E0E7FF)', borderRadius: 12,
    padding: 24, border: `2px solid ${colors.primary}`,
  },
  timer: { fontSize: 36, fontWeight: 700, color: colors.primary, fontVariantNumeric: 'tabular-nums' },
  recordingDot: {
    width: 10, height: 10, borderRadius: '50%', background: colors.danger,
    display: 'inline-block', marginRight: 8, animation: 'pulse 1.5s infinite',
  },
  textarea: {
    width: '100%', padding: '10px 14px', borderRadius: 8, border: `1px solid ${colors.gray200}`,
    fontSize: 14, outline: 'none', minHeight: 80, resize: 'vertical', fontFamily: 'inherit',
    marginTop: 12, marginBottom: 12,
  },
  empty: { color: colors.gray500, fontSize: 14, textAlign: 'center', padding: 40 },
};

function formatDuration(seconds) {
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  const s = seconds % 60;
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
}

export default function SupportConsole({ user }) {
  const navigate = useNavigate();
  const [tickets, setTickets] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [activeSession, setActiveSession] = useState(null);
  const [elapsed, setElapsed] = useState(0);
  const [endNotes, setEndNotes] = useState('');
  const [ending, setEnding] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(null);
  const [isRecording, setIsRecording] = useState(false);
  const [recordingError, setRecordingError] = useState(null);
  const timerRef = useRef(null);
  const recorderRef = useRef(null);

  useEffect(() => {
    const fetch = async () => {
      try {
        const res = await getTickets();
        setTickets(res.data);
      } catch {
        setError('Failed to load tickets');
      } finally {
        setLoading(false);
      }
    };
    fetch();
  }, []);

  useEffect(() => {
    if (activeSession) {
      timerRef.current = setInterval(() => setElapsed((e) => e + 1), 1000);
    }
    return () => clearInterval(timerRef.current);
  }, [activeSession]);

  useEffect(() => () => {
    if (recorderRef.current?.isRecording()) {
      recorderRef.current.stop().catch(() => {});
    }
  }, []);

  const handleStartSession = async (ticket) => {
    try {
      setRecordingError(null);
      const res = await startSession({ ticketId: ticket.id, userId: ticket.userId });
      setActiveSession(res.data);
      setElapsed(0);

      const recorder = new ScreenRecorder(recordingApi);
      recorder.onProgress = (p) => {
        setUploadProgress(p.done ? 100 : Math.min(90, p.uploadedParts * 15));
      };
      recorder.onError = (err) => {
        setRecordingError(err.message || 'Recording upload failed');
      };
      recorderRef.current = recorder;

      try {
        await recorder.start(res.data.id);
        setIsRecording(true);
      } catch (recErr) {
        if (recErr.name === 'NotAllowedError') {
          setRecordingError('Screen capture permission denied. Session active without recording.');
        } else {
          setRecordingError(recErr.message || 'Failed to start screen capture');
        }
      }
    } catch {
      setError('Failed to start session');
    }
  };

  const handleEndSession = async () => {
    if (!activeSession) return;
    try {
      setEnding(true);
      setUploadProgress(0);

      if (recorderRef.current?.isRecording()) {
        setUploadProgress(50);
        await recorderRef.current.stop();
        setUploadProgress(100);
      }

      await endSession(activeSession.id, endNotes);
      setActiveSession(null);
      setElapsed(0);
      setEndNotes('');
      setIsRecording(false);
      recorderRef.current = null;
      clearInterval(timerRef.current);
      setTimeout(() => setUploadProgress(null), 2000);
    } catch {
      setError('Failed to end session');
    } finally {
      setEnding(false);
    }
  };

  return (
    <div>
      <style>{`@keyframes pulse { 0%,100%{opacity:1} 50%{opacity:0.4} }`}</style>
      <h1 style={styles.heading}>Support Console</h1>
      <div style={styles.grid}>
        <div style={styles.card}>
          <h3 style={styles.cardTitle}>Assigned Tickets</h3>
          {loading ? (
            <div style={styles.empty}>Loading...</div>
          ) : error ? (
            <div style={{ ...styles.empty, color: colors.danger }}>{error}</div>
          ) : tickets.length === 0 ? (
            <div style={styles.empty}>No assigned tickets</div>
          ) : (
            tickets.map((t) => (
              <div key={t.id} style={styles.ticketRow}>
                <div style={{ flex: 1, cursor: 'pointer' }} onClick={() => navigate(`/tickets/${t.id}`)}>
                  <div style={{ fontWeight: 600, fontSize: 14 }}>{t.title}</div>
                  <div style={{ fontSize: 12, color: colors.gray500, marginTop: 2 }}>
                    #{t.id}
                  </div>
                </div>
                <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
                  <span style={{
                    ...styles.badge,
                    background: `${statusColors[t.status] || colors.gray500}18`,
                    color: statusColors[t.status] || colors.gray500,
                  }}>
                    {t.status?.replace('_', ' ')}
                  </span>
                  {!activeSession && (
                    <button
                      style={{ ...styles.btn, background: colors.success }}
                      onClick={() => handleStartSession(t)}
                    >
                      Start Session
                    </button>
                  )}
                </div>
              </div>
            ))
          )}
        </div>

        <div>
          {activeSession ? (
            <div style={styles.sessionPanel}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
                <h3 style={{ ...styles.cardTitle, margin: 0, color: colors.primary }}>Active Session</h3>
                <div style={{ display: 'flex', alignItems: 'center', fontSize: 13, fontWeight: 600, color: colors.danger }}>
                  {isRecording && <span style={styles.recordingDot} />}
                  {isRecording ? 'Recording' : 'No capture'}
                </div>
              </div>

              <div style={styles.timer}>{formatDuration(elapsed)}</div>

              <div style={{ fontSize: 13, color: colors.gray700, marginTop: 12 }}>
                Session #{activeSession.id} &middot; Ticket #{activeSession.ticketId}
              </div>

              {recordingError && (
                <div style={{ fontSize: 12, color: colors.danger, marginTop: 8 }}>{recordingError}</div>
              )}

              <textarea
                style={styles.textarea}
                placeholder="Session notes (used for auto AI analysis on end)..."
                value={endNotes}
                onChange={(e) => setEndNotes(e.target.value)}
              />

              <button
                style={{ ...styles.btn, background: colors.danger, width: '100%', padding: '12px 16px' }}
                onClick={handleEndSession}
                disabled={ending}
              >
                {ending ? 'Ending & uploading...' : 'End Session'}
              </button>

              {uploadProgress !== null && (
                <div style={{ marginTop: 16 }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 13, marginBottom: 6 }}>
                    <span style={{ color: colors.gray700 }}>Upload progress</span>
                    <span style={{ fontWeight: 600, color: colors.primary }}>{uploadProgress}%</span>
                  </div>
                  <div style={{ background: colors.gray200, borderRadius: 4, height: 6, overflow: 'hidden' }}>
                    <div style={{
                      width: `${uploadProgress}%`, height: '100%', background: colors.primary,
                      borderRadius: 4, transition: 'width 0.3s',
                    }} />
                  </div>
                </div>
              )}
            </div>
          ) : (
            <div style={{ ...styles.card, textAlign: 'center', color: colors.gray500, padding: 48 }}>
              <div style={{ fontSize: 40, marginBottom: 12 }}>&#x1F3A7;</div>
              <div style={{ fontSize: 15, fontWeight: 500 }}>No active session</div>
              <div style={{ fontSize: 13, marginTop: 8 }}>
                Start a session to begin screen capture and chunked upload
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
