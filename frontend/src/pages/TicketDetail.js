import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getTicket, updateTicket, getSessions, getRecordingsBySession, getTicketMessages, postTicketMessage } from '../services/api';

const colors = {
  primary: '#4F46E5', success: '#10B981', warning: '#F59E0B',
  danger: '#EF4444', gray100: '#F3F4F6', gray200: '#E5E7EB',
  gray500: '#6B7280', gray700: '#374151', gray900: '#111827',
};

const priorityColors = { LOW: colors.success, MEDIUM: colors.warning, HIGH: '#F97316', CRITICAL: colors.danger };
const statusColors = { OPEN: colors.primary, IN_PROGRESS: colors.warning, RESOLVED: colors.success, CLOSED: colors.gray500 };

const styles = {
  backBtn: {
    background: 'none', border: 'none', color: colors.primary, fontSize: 14,
    fontWeight: 600, cursor: 'pointer', marginBottom: 16, padding: 0,
  },
  card: { background: '#fff', borderRadius: 12, padding: 24, boxShadow: '0 1px 4px rgba(0,0,0,0.06)', marginBottom: 20 },
  title: { fontSize: 22, fontWeight: 700, color: colors.gray900, marginBottom: 12 },
  meta: { display: 'flex', gap: 12, flexWrap: 'wrap', marginBottom: 16 },
  badge: {
    display: 'inline-block', padding: '4px 12px', borderRadius: 20, fontSize: 12,
    fontWeight: 600, textTransform: 'uppercase',
  },
  description: { fontSize: 15, color: colors.gray700, lineHeight: 1.6, marginBottom: 20 },
  sectionTitle: { fontSize: 16, fontWeight: 700, color: colors.gray900, marginBottom: 12 },
  select: {
    padding: '8px 14px', borderRadius: 8, border: `1px solid ${colors.gray200}`,
    fontSize: 14, outline: 'none', background: '#fff', marginRight: 12,
  },
  btn: {
    padding: '8px 20px', borderRadius: 8, border: 'none', fontSize: 14,
    fontWeight: 600, cursor: 'pointer', color: '#fff', background: colors.primary,
  },
  sessionRow: {
    padding: '12px 16px', borderRadius: 8, border: `1px solid ${colors.gray200}`,
    marginBottom: 8, display: 'flex', justifyContent: 'space-between', alignItems: 'center',
  },
  empty: { color: colors.gray500, fontSize: 14, textAlign: 'center', padding: 32 },
  msgBox: {
    padding: '12px 14px', borderRadius: 10, background: colors.gray100,
    marginBottom: 10, border: `1px solid ${colors.gray200}`,
  },
  textarea: {
    width: '100%', padding: '10px 14px', borderRadius: 8, border: `1px solid ${colors.gray200}`,
    fontSize: 14, outline: 'none', minHeight: 88, resize: 'vertical', fontFamily: 'inherit',
    marginBottom: 10,
  },
};

export default function TicketDetail({ user }) {
  const { id } = useParams();
  const navigate = useNavigate();
  const [ticket, setTicket] = useState(null);
  const [sessions, setSessions] = useState([]);
  const [recordings, setRecordings] = useState({});
  const [messages, setMessages] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [statusUpdate, setStatusUpdate] = useState('');
  const [updating, setUpdating] = useState(false);
  const [msgBody, setMsgBody] = useState('');
  const [postingMsg, setPostingMsg] = useState(false);

  const canChangeStatus =
    user?.role === 'support_executive'
    || user?.role === 'support_admin'
    || user?.role === 'support_manager';

  useEffect(() => {
    const fetchData = async () => {
      try {
        setLoading(true);
        setError(null);
        const [ticketRes, sessionsRes, msgRes] = await Promise.all([
          getTicket(id),
          getSessions(),
          getTicketMessages(id).catch(() => ({ data: [] })),
        ]);
        const t = ticketRes.data;
        setTicket(t);
        setStatusUpdate(t.status);
        setMessages(Array.isArray(msgRes.data) ? msgRes.data : []);

        const ticketSessions = sessionsRes.data.filter((s) => String(s.ticketId) === String(id));
        setSessions(ticketSessions);

        const recs = {};
        for (const s of ticketSessions) {
          try {
            const r = await getRecordingsBySession(s.id);
            recs[s.id] = r.data;
          } catch {
            recs[s.id] = [];
          }
        }
        setRecordings(recs);
      } catch (e) {
        const msg =
          e.response?.data?.message ||
          (e.response?.status === 403 ? 'Access denied.' : null) ||
          'Failed to load ticket details';
        setError(msg);
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, [id]);

  const reloadMessages = async () => {
    try {
      const res = await getTicketMessages(id);
      setMessages(res.data || []);
    } catch {
      /* silent */
    }
  };

  const handleStatusUpdate = async () => {
    if (!statusUpdate || statusUpdate === ticket.status) return;
    try {
      setUpdating(true);
      setError(null);
      await updateTicket(id, { status: statusUpdate });
      setTicket({ ...ticket, status: statusUpdate });
    } catch {
      setError('Failed to update status');
    } finally {
      setUpdating(false);
    }
  };

  const handlePostMessage = async (e) => {
    e.preventDefault();
    if (!msgBody.trim()) return;
    try {
      setPostingMsg(true);
      setError(null);
      await postTicketMessage(id, msgBody.trim());
      setMsgBody('');
      await reloadMessages();
    } catch (e2) {
      const msg = e2.response?.data?.message || 'Failed to post message';
      setError(msg);
    } finally {
      setPostingMsg(false);
    }
  };

  if (loading) return <div style={styles.empty}>Loading ticket...</div>;
  if (error && !ticket) return <div style={{ ...styles.empty, color: colors.danger }}>{error}</div>;
  if (!ticket) return null;

  return (
    <div>
      <button style={styles.backBtn} type="button" onClick={() => navigate(-1)}>&larr; Back</button>

      {error && (
        <div
          role="alert"
          style={{
            background: `${colors.danger}12`,
            color: '#B91C1C',
            padding: '10px 14px',
            borderRadius: 8,
            marginBottom: 16,
            fontSize: 14,
            border: `1px solid ${colors.danger}35`,
          }}
        >
          {error}
        </div>
      )}

      <div style={styles.card}>
        <h1 style={styles.title}>{ticket.title}</h1>
        <div style={styles.meta}>
          <span style={{
            ...styles.badge,
            background: `${statusColors[ticket.status] || colors.gray500}18`,
            color: statusColors[ticket.status] || colors.gray500,
          }}>
            {ticket.status?.replace('_', ' ')}
          </span>
          <span style={{
            ...styles.badge,
            background: `${priorityColors[ticket.priority] || colors.gray500}18`,
            color: priorityColors[ticket.priority] || colors.gray500,
          }}>
            {ticket.priority}
          </span>
          <span style={{ fontSize: 13, color: colors.gray500, alignSelf: 'center' }}>
            Created {new Date(ticket.createdAt).toLocaleDateString()}
          </span>
        </div>

        <p style={styles.description}>{ticket.description || 'No description provided.'}</p>

        {canChangeStatus && (
          <div style={{ display: 'flex', alignItems: 'center', paddingTop: 12, borderTop: `1px solid ${colors.gray100}` }}>
            <span style={{ fontSize: 14, fontWeight: 600, color: colors.gray700, marginRight: 12 }}>Update Status:</span>
            <select
              style={styles.select}
              value={statusUpdate}
              onChange={(e) => setStatusUpdate(e.target.value)}
            >
              <option value="OPEN">Open</option>
              <option value="IN_PROGRESS">In Progress</option>
              <option value="RESOLVED">Resolved</option>
              <option value="CLOSED">Closed</option>
            </select>
            <button style={styles.btn} type="button" onClick={handleStatusUpdate} disabled={updating}>
              {updating ? 'Saving...' : 'Save'}
            </button>
          </div>
        )}
      </div>

      <div style={styles.card}>
        <h3 style={styles.sectionTitle}>Conversation</h3>
        {messages.length === 0 ? (
          <div style={styles.empty}>No replies yet.</div>
        ) : (
          messages.map((m) => (
            <div key={m.id} style={styles.msgBox}>
              <div style={{ fontSize: 12, fontWeight: 600, color: colors.gray700, marginBottom: 6 }}>
                {m.authorName}
                <span style={{ fontWeight: 400, color: colors.gray500, marginLeft: 8 }}>
                  {m.createdAt ? new Date(m.createdAt).toLocaleString() : ''}
                </span>
              </div>
              <div style={{ fontSize: 14, color: colors.gray900, whiteSpace: 'pre-wrap', lineHeight: 1.5 }}>
                {m.body}
              </div>
            </div>
          ))
        )}
        <form onSubmit={handlePostMessage} style={{ marginTop: 16, paddingTop: 16, borderTop: `1px solid ${colors.gray100}` }}>
          <textarea
            style={styles.textarea}
            placeholder="Write an update visible to everyone on this ticket..."
            value={msgBody}
            maxLength={4096}
            onChange={(e) => setMsgBody(e.target.value)}
          />
          <button style={styles.btn} type="submit" disabled={postingMsg || !msgBody.trim()}>
            {postingMsg ? 'Sending…' : 'Post reply'}
          </button>
        </form>
      </div>

      <div style={styles.card}>
        <h3 style={styles.sectionTitle}>Sessions</h3>
        {sessions.length === 0 ? (
          <div style={styles.empty}>No sessions for this ticket</div>
        ) : (
          sessions.map((s) => (
            <div key={s.id}>
              <div style={styles.sessionRow}>
                <div>
                  <div style={{ fontWeight: 600, fontSize: 14 }}>Session #{s.id}</div>
                  <div style={{ fontSize: 12, color: colors.gray500, marginTop: 2 }}>
                    {s.startTime ? new Date(s.startTime).toLocaleString() : 'N/A'}
                    {s.endTime ? ` — ${new Date(s.endTime).toLocaleString()}` : ' — Active'}
                  </div>
                </div>
                <span style={{
                  ...styles.badge,
                  background: s.endTime ? `${colors.success}18` : `${colors.warning}18`,
                  color: s.endTime ? colors.success : colors.warning,
                }}>
                  {s.endTime ? 'Completed' : 'Active'}
                </span>
              </div>

              {recordings[s.id] && recordings[s.id].length > 0 && (
                <div style={{ paddingLeft: 16, marginBottom: 12 }}>
                  {recordings[s.id].map((r) => (
                    <div key={r.id} style={{
                      display: 'flex', alignItems: 'center', gap: 12,
                      padding: '8px 12px', fontSize: 13, color: colors.gray700,
                      borderLeft: `3px solid ${colors.primary}`, marginTop: 6, background: colors.gray100, borderRadius: '0 6px 6px 0',
                    }}>
                      <span>&#x1F3AC;</span>
                      <span style={{ fontWeight: 500 }}>Recording #{r.id}</span>
                      {r.s3Key && <span style={{ color: colors.gray500, fontSize: 12 }}>{r.s3Key}</span>}
                    </div>
                  ))}
                </div>
              )}
            </div>
          ))
        )}
      </div>
    </div>
  );
}
