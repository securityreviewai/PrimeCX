import React, { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { getTickets, startSession, endSession, getUploadUrl, exportTicketsCsv, getCannedResponses } from '../services/api';
import { formatTicketLastTouch } from '../utils/formatTicketLastTouch';
import CopySessionLinkButton from '../components/CopySessionLinkButton';
import KnowledgeBasePanel from '../components/KnowledgeBasePanel';

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
  uploadArea: {
    border: `2px dashed ${colors.gray200}`, borderRadius: 8, padding: 20,
    textAlign: 'center', marginTop: 16, color: colors.gray500, fontSize: 14, cursor: 'pointer',
  },
};

function canAccessKnowledgeBase(user) {
  const r = (user?.role || '').replace(/^ROLE_/i, '').toLowerCase();
  return r === 'support_executive' || r === 'support_admin' || r === 'support_manager';
}

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
  const [overdueOnly, setOverdueOnly] = useState(false);
  const [cannedResponses, setCannedResponses] = useState([]);
  const [crPickerVisible, setCrPickerVisible] = useState(false);
  const [crSearch, setCrSearch] = useState('');
  const timerRef = useRef(null);

  useEffect(() => {
    const fetch = async () => {
      try {
        const res = await getTickets(overdueOnly ? { overdueOnly: true } : undefined);
        setTickets(res.data);
      } catch {
        setError('Failed to load tickets');
      } finally {
        setLoading(false);
      }
    };
    fetch();
  }, [overdueOnly]);

  useEffect(() => {
    if (activeSession) {
      timerRef.current = setInterval(() => setElapsed((e) => e + 1), 1000);
    }
    return () => clearInterval(timerRef.current);
  }, [activeSession]);

  useEffect(() => {
    if (canAccessKnowledgeBase(user)) {
      getCannedResponses().then((res) => setCannedResponses(res.data || [])).catch(() => {});
    }
  }, [user]);

  const filteredCanned = crSearch
    ? cannedResponses.filter((cr) =>
        cr.shortcode.includes(crSearch.toLowerCase()) ||
        cr.title.toLowerCase().includes(crSearch.toLowerCase()) ||
        (cr.category || '').toLowerCase().includes(crSearch.toLowerCase())
      )
    : cannedResponses;

  const insertCannedResponse = (cr) => {
    setEndNotes((prev) => {
      const prefix = prev && !prev.endsWith('\n') ? prev + '\n' : prev || '';
      return prefix + cr.content;
    });
    setCrPickerVisible(false);
    setCrSearch('');
  };

  const handleNotesChange = (e) => {
    const val = e.target.value;
    setEndNotes(val);
    const lastLine = val.split('\n').pop();
    if (lastLine.startsWith('/')) {
      setCrSearch(lastLine.slice(1));
      setCrPickerVisible(true);
    } else {
      setCrPickerVisible(false);
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

  const handleExportCsv = async () => {
    try {
      const res = await exportTicketsCsv();
      const url = window.URL.createObjectURL(new Blob([res.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', 'tickets.csv');
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch {
      setError('Failed to export tickets');
    }
  };

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24, flexWrap: 'wrap', gap: 12 }}>
        <h1 style={{ ...styles.heading, margin: 0 }}>Support Console</h1>
        <label style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 13, color: colors.gray700 }}>
          <input
            type="checkbox"
            checked={overdueOnly}
            onChange={(e) => setOverdueOnly(e.target.checked)}
          />
          Overdue only
        </label>
        <button
          style={{ ...styles.btn, background: colors.primary }}
          onClick={handleExportCsv}
        >
          Export CSV
        </button>
      </div>
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
                    {t.dueAt && (
                      <span style={{ marginLeft: 8 }}>
                        · Due {new Date(t.dueAt).toLocaleString()}
                        {['RESOLVED', 'CLOSED'].includes(t.status) ? '' : (new Date(t.dueAt) < new Date() ? (
                          <span style={{ color: colors.danger, fontWeight: 600, marginLeft: 4 }}>(overdue)</span>
                        ) : null)}
                      </span>
                    )}
                    <span style={{ marginLeft: 8 }}>
                      · Last touch {formatTicketLastTouch(t)}
                    </span>
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

        <div>
          {activeSession ? (
            <div style={styles.sessionPanel}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
                <h3 style={{ ...styles.cardTitle, margin: 0, color: colors.primary }}>Active Session</h3>
                <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                  <CopySessionLinkButton sessionId={activeSession.id} label="Copy handoff link" />
                  <div style={{ display: 'flex', alignItems: 'center', fontSize: 13, fontWeight: 600, color: colors.danger }}>
                    <span style={styles.recordingDot} />
                    Recording
                  </div>
                </div>
              </div>

              <div style={styles.timer}>{formatDuration(elapsed)}</div>

              <div style={{ fontSize: 13, color: colors.gray700, marginTop: 12 }}>
                Session #{activeSession.id} &middot; Ticket #{activeSession.ticketId}
              </div>

              <textarea
                style={styles.textarea}
                placeholder="Session notes... (type / to insert a canned response)"
                value={endNotes}
                onChange={handleNotesChange}
              />

              {crPickerVisible && filteredCanned.length > 0 && (
                <div style={{
                  border: `1px solid ${colors.gray200}`, borderRadius: 8, maxHeight: 200,
                  overflowY: 'auto', marginBottom: 12, background: '#fff',
                }}>
                  {filteredCanned.slice(0, 8).map((cr) => (
                    <button
                      key={cr.id}
                      type="button"
                      onClick={() => insertCannedResponse(cr)}
                      style={{
                        display: 'block', width: '100%', textAlign: 'left', padding: '8px 12px',
                        border: 'none', borderBottom: `1px solid ${colors.gray100}`,
                        background: 'none', cursor: 'pointer', fontSize: 13,
                      }}
                    >
                      <span style={{ fontWeight: 600, color: colors.primary }}>/ {cr.shortcode}</span>
                      <span style={{ color: colors.gray500, marginLeft: 8 }}>{cr.title}</span>
                      {cr.category && (
                        <span style={{ fontSize: 11, color: colors.gray500, marginLeft: 8 }}>
                          [{cr.category}]
                        </span>
                      )}
                    </button>
                  ))}
                </div>
              )}

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

              {canAccessKnowledgeBase(user) && (
                <KnowledgeBasePanel user={user} />
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
