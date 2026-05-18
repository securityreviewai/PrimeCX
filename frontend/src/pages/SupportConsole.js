import React, { useState, useEffect, useRef, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { getTickets, getTicketPool, claimTicket, startSession, endSession, getUploadUrl } from '../services/api';

const colors = {
  primary: '#4F46E5', success: '#10B981', warning: '#F59E0B',
  danger: '#EF4444', gray100: '#F3F4F6', gray200: '#E5E7EB',
  gray500: '#6B7280', gray700: '#374151', gray900: '#111827',
};

const statusColors = { OPEN: colors.primary, IN_PROGRESS: colors.warning, RESOLVED: colors.success, CLOSED: colors.gray500 };

const POOL_ROLES = ['support_executive', 'support_admin', 'support_manager'];

const styles = {
  heading: { fontSize: 24, fontWeight: 700, color: colors.gray900, marginBottom: 24 },
  grid: { display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 24, alignItems: 'start' },
  leftCol: { display: 'flex', flexDirection: 'column', gap: 24 },
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
  uploadArea: {
    border: `2px dashed ${colors.gray200}`, borderRadius: 8, padding: 20,
    textAlign: 'center', marginTop: 16, color: colors.gray500, fontSize: 14, cursor: 'pointer',
  },
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
  const [poolTickets, setPoolTickets] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [activeSession, setActiveSession] = useState(null);
  const [elapsed, setElapsed] = useState(0);
  const [endNotes, setEndNotes] = useState('');
  const [ending, setEnding] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(null);
  const [claimingId, setClaimingId] = useState(null);
  const timerRef = useRef(null);

  const showPool = POOL_ROLES.includes(user?.role);
  const canClaim = user?.role === 'support_executive';

  const refreshLists = useCallback(async () => {
    const assignedRes = await getTickets();
    setTickets(assignedRes.data);
    if (showPool) {
      try {
        const poolRes = await getTicketPool();
        setPoolTickets(poolRes.data.content || []);
      } catch {
        setPoolTickets([]);
      }
    }
  }, [showPool]);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        setLoading(true);
        setError(null);
        await refreshLists();
      } catch {
        if (!cancelled) setError('Failed to load tickets');
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => { cancelled = true; };
  }, [refreshLists, user?.role]);

  useEffect(() => {
    if (activeSession) {
      timerRef.current = setInterval(() => setElapsed((e) => e + 1), 1000);
    }
    return () => clearInterval(timerRef.current);
  }, [activeSession]);

  const handleClaim = async (ticketId) => {
    try {
      setClaimingId(ticketId);
      setError(null);
      await claimTicket(ticketId);
      await refreshLists();
    } catch {
      setError('Could not claim ticket (it may have been taken or is no longer open).');
    } finally {
      setClaimingId(null);
    }
  };

  const handleStartSession = async (ticketId) => {
    try {
      const res = await startSession({ ticketId });
      setActiveSession(res.data);
      setElapsed(0);
    } catch {
      setError('Failed to start session');
    }
  };

  const handleEndSession = async () => {
    if (!activeSession) return;
    try {
      setEnding(true);
      await endSession(activeSession.id, endNotes);
      setActiveSession(null);
      setElapsed(0);
      setEndNotes('');
      clearInterval(timerRef.current);
    } catch {
      setError('Failed to end session');
    } finally {
      setEnding(false);
    }
  };

  const handleUpload = async () => {
    if (!activeSession) return;
    try {
      setUploadProgress(0);
      await getUploadUrl(activeSession.id, 'recording.webm', 'video/webm');
      const interval = setInterval(() => {
        setUploadProgress((p) => {
          if (p >= 100) { clearInterval(interval); return 100; }
          return p + 10;
        });
      }, 300);
    } catch {
      setError('Failed to get upload URL');
      setUploadProgress(null);
    }
  };

  return (
    <div>
      <h1 style={styles.heading}>Support Console</h1>
      <div style={styles.grid}>
        <div style={styles.leftCol}>
          {showPool && (
            <div style={styles.card}>
              <h3 style={styles.cardTitle}>Unassigned queue</h3>
              <p style={{ fontSize: 13, color: colors.gray500, marginTop: -8, marginBottom: 16 }}>
                Open tickets with no assignee (oldest first). Executives can claim to work them.
              </p>
              {loading ? (
                <div style={styles.empty}>Loading...</div>
              ) : poolTickets.length === 0 ? (
                <div style={styles.empty}>No tickets in the queue</div>
              ) : (
                poolTickets.map((t) => (
                  <div key={t.id} style={styles.ticketRow}>
                    <div
                      style={{ flex: 1, cursor: canClaim ? 'default' : 'pointer' }}
                      onClick={() => {
                        if (!canClaim) navigate(`/tickets/${t.id}`);
                      }}
                    >
                      <div style={{ fontWeight: 600, fontSize: 14 }}>{t.title}</div>
                      <div style={{ fontSize: 12, color: colors.gray500, marginTop: 2 }}>
                        #{t.id} &middot; {t.userName || 'Customer'}
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
                      {canClaim && (
                        <button
                          type="button"
                          style={{ ...styles.btn, background: colors.primary }}
                          disabled={claimingId === t.id}
                          onClick={() => handleClaim(t.id)}
                        >
                          {claimingId === t.id ? 'Claiming…' : 'Claim'}
                        </button>
                      )}
                    </div>
                  </div>
                ))
              )}
            </div>
          )}

          <div style={styles.card}>
            <h3 style={styles.cardTitle}>Assigned to me</h3>
            {loading ? (
              <div style={styles.empty}>Loading...</div>
            ) : error ? (
              <div style={{ ...styles.empty, color: colors.danger }}>{error}</div>
            ) : tickets.length === 0 ? (
              <div style={styles.empty}>No assigned tickets yet — grab one from the queue</div>
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
                        onClick={() => handleStartSession(t.id)}
                      >
                        Start Session
                      </button>
                    )}
                  </div>
                </div>
              ))
            )}
          </div>
        </div>

        <div>
          {activeSession ? (
            <div style={styles.sessionPanel}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
                <h3 style={{ ...styles.cardTitle, margin: 0, color: colors.primary }}>Active Session</h3>
                <div style={{ display: 'flex', alignItems: 'center', fontSize: 13, fontWeight: 600, color: colors.danger }}>
                  <span style={styles.recordingDot} />
                  Recording
                </div>
              </div>

              <div style={styles.timer}>{formatDuration(elapsed)}</div>

              <div style={{ fontSize: 13, color: colors.gray700, marginTop: 12 }}>
                Session #{activeSession.id} &middot; Ticket #{activeSession.ticketId}
              </div>

              <textarea
                style={styles.textarea}
                placeholder="Session notes..."
                value={endNotes}
                onChange={(e) => setEndNotes(e.target.value)}
              />

              <div style={{ display: 'flex', gap: 12 }}>
                <button
                  style={{ ...styles.btn, background: colors.danger, flex: 1, padding: '12px 16px' }}
                  onClick={handleEndSession}
                  disabled={ending}
                >
                  {ending ? 'Ending...' : 'End Session'}
                </button>
                <button
                  style={{ ...styles.btn, background: colors.primary, flex: 1, padding: '12px 16px' }}
                  onClick={handleUpload}
                >
                  Upload Recording
                </button>
              </div>

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
                Start a session from an assigned ticket
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
