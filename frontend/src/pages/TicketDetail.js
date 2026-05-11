import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  getTicket,
  updateTicket,
  getSessions,
  getRecordingsBySession,
  getRecordingDownloadUrl,
  getTicketComments,
  createTicketComment,
  updateTicketComment,
  deleteTicketComment,
} from '../services/api';
import AiDataHandlingNotice from '../components/AiDataHandlingNotice';
import { formatTicketLastTouch } from '../utils/formatTicketLastTouch';

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
  linkishBtn: {
    background: 'none', border: 'none', color: colors.primary, fontSize: 12, fontWeight: 600,
    cursor: 'pointer', padding: 0, marginLeft: 8,
  },
};

function toDatetimeLocalValue(iso) {
  if (!iso) return '';
  const d = new Date(iso);
  const pad = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

export default function TicketDetail({ user }) {
  const { id } = useParams();
  const navigate = useNavigate();
  const [ticket, setTicket] = useState(null);
  const [sessions, setSessions] = useState([]);
  const [recordings, setRecordings] = useState({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [statusUpdate, setStatusUpdate] = useState('');
  const [updating, setUpdating] = useState(false);
  const [openingRecordingId, setOpeningRecordingId] = useState(null);
  const [comments, setComments] = useState([]);
  const [commentsLoading, setCommentsLoading] = useState(false);
  const [newCommentBody, setNewCommentBody] = useState('');
  const [newCommentInternal, setNewCommentInternal] = useState(false);
  const [commentSubmitting, setCommentSubmitting] = useState(false);
  const [editingComment, setEditingComment] = useState(null);
  const [dueLocal, setDueLocal] = useState('');
  const [dueSaving, setDueSaving] = useState(false);

  const normalizeRole = (role) => (role || '').replace(/^ROLE_/i, '').toLowerCase();
  const canChangeStatus = normalizeRole(user?.role) === 'support_executive'
    || normalizeRole(user?.role) === 'support_admin';
  const canDownloadRecording = ['support_executive', 'support_manager', 'support_admin'].includes(
    normalizeRole(user?.role),
  );
  const isStaff = ['ROLE_SUPPORT_EXECUTIVE', 'ROLE_SUPPORT_ADMIN', 'ROLE_SUPPORT_MANAGER'].includes(
    user?.role,
  );
  const canModerateComments = ['ROLE_SUPPORT_ADMIN', 'ROLE_SUPPORT_MANAGER'].includes(user?.role);
  const canEditComment = (c) => {
    if (!user) return false;
    if (canModerateComments) return true;
    return Number(c.authorUserId) === Number(user.id);
  };

  useEffect(() => {
    const fetchData = async () => {
      try {
        setLoading(true);
        setError(null);
        const [ticketRes, sessionsRes] = await Promise.all([getTicket(id), getSessions()]);
        setTicket(ticketRes.data);
        setStatusUpdate(ticketRes.data.status);
        setDueLocal(toDatetimeLocalValue(ticketRes.data.dueAt));

        setCommentsLoading(true);
        try {
          const commentsRes = await getTicketComments(id);
          setComments(commentsRes.data || []);
        } catch {
          setComments([]);
        } finally {
          setCommentsLoading(false);
        }

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
      } catch (err) {
        const status = err.response?.status;
        if (status === 403 || status === 404) {
          setError('Ticket not found');
        } else {
          setError('Failed to load ticket details');
        }
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, [id]);

  const openRecordingDownload = async (recordingId) => {
    try {
      setOpeningRecordingId(recordingId);
      const res = await getRecordingDownloadUrl(recordingId);
      const url = res.data?.downloadUrl;
      if (typeof url === 'string' && /^https:\/\//i.test(url)) {
        window.open(url, '_blank', 'noopener,noreferrer');
      } else {
        window.alert('Download link unavailable.');
      }
    } catch {
      window.alert('Unable to open recording. You may not have access.');
    } finally {
      setOpeningRecordingId(null);
    }
  };

  const handleAddComment = async (e) => {
    e.preventDefault();
    const body = newCommentBody.trim();
    if (!body || commentSubmitting) return;
    try {
      setCommentSubmitting(true);
      const payload = { body, internal: isStaff ? newCommentInternal : false };
      const res = await createTicketComment(id, payload);
      setComments((prev) => [...prev, res.data]);
      setNewCommentBody('');
      setNewCommentInternal(false);
      try {
        const ticketRes = await getTicket(id);
        setTicket(ticketRes.data);
        setDueLocal(toDatetimeLocalValue(ticketRes.data.dueAt));
        setStatusUpdate(ticketRes.data.status);
      } catch {
        /* ignore */
      }
    } catch {
      window.alert('Could not add comment.');
    } finally {
      setCommentSubmitting(false);
    }
  };

  const handleSaveEdit = async () => {
    if (!editingComment) return;
    const body = (editingComment.body || '').trim();
    if (!body) return;
    try {
      const payload = { body, internal: isStaff ? editingComment.internal : undefined };
      const res = await updateTicketComment(id, editingComment.id, payload);
      setComments((prev) => prev.map((c) => (c.id === res.data.id ? res.data : c)));
      setEditingComment(null);
      try {
        const ticketRes = await getTicket(id);
        setTicket(ticketRes.data);
        setDueLocal(toDatetimeLocalValue(ticketRes.data.dueAt));
        setStatusUpdate(ticketRes.data.status);
      } catch {
        /* ignore */
      }
    } catch {
      window.alert('Could not update comment.');
    }
  };

  const handleDeleteComment = async (commentId) => {
    if (!window.confirm('Delete this comment?')) return;
    try {
      await deleteTicketComment(id, commentId);
      setComments((prev) => prev.filter((c) => c.id !== commentId));
      try {
        const ticketRes = await getTicket(id);
        setTicket(ticketRes.data);
        setDueLocal(toDatetimeLocalValue(ticketRes.data.dueAt));
        setStatusUpdate(ticketRes.data.status);
      } catch {
        /* ignore */
      }
    } catch {
      window.alert('Could not delete comment.');
    }
  };

  const handleStatusUpdate = async () => {
    if (!statusUpdate || statusUpdate === ticket.status) return;
    try {
      setUpdating(true);
      const res = await updateTicket(id, { status: statusUpdate });
      setTicket(res.data);
      setDueLocal(toDatetimeLocalValue(res.data.dueAt));
    } catch {
      setError('Failed to update status');
    } finally {
      setUpdating(false);
    }
  };

  const canEditDue = isStaff;

  const handleDueSave = async () => {
    try {
      setDueSaving(true);
      const payload = dueLocal.trim()
        ? { dueAt: `${dueLocal.trim()}:00` }
        : { clearDueAt: true };
      const res = await updateTicket(id, payload);
      setTicket(res.data);
      setDueLocal(toDatetimeLocalValue(res.data.dueAt));
    } catch {
      setError('Failed to update due date');
    } finally {
      setDueSaving(false);
    }
  };

  if (loading) return <div style={styles.empty}>Loading ticket...</div>;
  if (error) return <div style={{ ...styles.empty, color: colors.danger }}>{error}</div>;
  if (!ticket) return null;

  return (
    <div>
      <button style={styles.backBtn} onClick={() => navigate(-1)}>&larr; Back</button>

      {isStaff && (
        <div style={{ marginBottom: 16 }}>
          <AiDataHandlingNotice compact initiallyOpen={false} enabled={isStaff} />
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
          <span style={{ fontSize: 13, color: colors.gray500, alignSelf: 'center' }}>
            Last touch · {formatTicketLastTouch(ticket)}
          </span>
          {ticket.dueAt && (
            <span style={{
              fontSize: 13,
              alignSelf: 'center',
              color: ['RESOLVED', 'CLOSED'].includes(ticket.status)
                ? colors.gray500
                : (new Date(ticket.dueAt) < new Date() ? colors.danger : colors.gray700),
              fontWeight: ['RESOLVED', 'CLOSED'].includes(ticket.status) ? 400 : 600,
            }}
            >
              Due {new Date(ticket.dueAt).toLocaleString()}
              {!['RESOLVED', 'CLOSED'].includes(ticket.status) && new Date(ticket.dueAt) < new Date() ? ' · Overdue' : ''}
            </span>
          )}
        </div>

        <p style={styles.description}>{ticket.description || 'No description provided.'}</p>

        {canEditDue && (
          <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: 12, paddingTop: 12, borderTop: `1px solid ${colors.gray100}` }}>
            <span style={{ fontSize: 14, fontWeight: 600, color: colors.gray700 }}>SLA due:</span>
            <input
              type="datetime-local"
              style={styles.select}
              value={dueLocal}
              onChange={(e) => setDueLocal(e.target.value)}
            />
            <button type="button" style={styles.btn} onClick={handleDueSave} disabled={dueSaving}>
              {dueSaving ? 'Saving…' : 'Save due date'}
            </button>
            <button
              type="button"
              style={{ ...styles.btn, background: colors.gray500 }}
              onClick={() => setDueLocal('')}
              disabled={dueSaving}
            >
              Clear
            </button>
          </div>
        )}

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
            <button style={styles.btn} onClick={handleStatusUpdate} disabled={updating}>
              {updating ? 'Saving...' : 'Save'}
            </button>
          </div>
        )}
      </div>

      <div style={styles.card}>
        <h3 style={styles.sectionTitle}>Comments &amp; internal notes</h3>
        <p style={{ fontSize: 13, color: colors.gray500, marginBottom: 16 }}>
          Public comments are visible to the customer. Internal notes are only visible to support staff
          and do not change the ticket description.
        </p>
        {commentsLoading ? (
          <div style={styles.empty}>Loading comments…</div>
        ) : comments.length === 0 ? (
          <div style={styles.empty}>No comments yet.</div>
        ) : (
          <div style={{ marginBottom: 20 }}>
            {comments.map((c) => (
              <div
                key={c.id}
                style={{
                  padding: '12px 14px',
                  borderRadius: 8,
                  border: `1px solid ${colors.gray200}`,
                  marginBottom: 10,
                  background: c.internal ? '#FFFBEB' : colors.gray100,
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 8 }}>
                  <div>
                    <span style={{ fontWeight: 600, fontSize: 14, color: colors.gray900 }}>
                      {c.authorDisplayName || 'Unknown'}
                    </span>
                    <span style={{ fontSize: 12, color: colors.gray500, marginLeft: 8 }}>
                      {c.createdAt ? new Date(c.createdAt).toLocaleString() : ''}
                    </span>
                    {c.internal && (
                      <span style={{
                        ...styles.badge,
                        marginLeft: 8,
                        background: `${colors.warning}22`,
                        color: colors.warning,
                        fontSize: 11,
                      }}
                      >
                        Internal
                      </span>
                    )}
                    {!c.internal && (
                      <span style={{
                        ...styles.badge,
                        marginLeft: 8,
                        background: `${colors.primary}18`,
                        color: colors.primary,
                        fontSize: 11,
                      }}
                      >
                        Public
                      </span>
                    )}
                  </div>
                  {canEditComment(c) && editingComment?.id !== c.id && (
                    <div>
                      <button
                        type="button"
                        style={styles.linkishBtn}
                        onClick={() => setEditingComment({
                          id: c.id,
                          body: c.body,
                          internal: !!c.internal,
                        })}
                      >
                        Edit
                      </button>
                      <button
                        type="button"
                        style={{ ...styles.linkishBtn, color: colors.danger }}
                        onClick={() => handleDeleteComment(c.id)}
                      >
                        Delete
                      </button>
                    </div>
                  )}
                </div>
                {editingComment?.id === c.id ? (
                  <div style={{ marginTop: 10 }}>
                    <textarea
                      value={editingComment.body}
                      onChange={(e) => setEditingComment({ ...editingComment, body: e.target.value })}
                      rows={4}
                      style={{
                        width: '100%',
                        padding: 10,
                        borderRadius: 8,
                        border: `1px solid ${colors.gray200}`,
                        fontSize: 14,
                        resize: 'vertical',
                      }}
                    />
                    {isStaff && (
                      <label style={{ display: 'flex', alignItems: 'center', gap: 8, marginTop: 8, fontSize: 13 }}>
                        <input
                          type="checkbox"
                          checked={!!editingComment.internal}
                          onChange={(e) => setEditingComment({
                            ...editingComment,
                            internal: e.target.checked,
                          })}
                        />
                        Internal note (not visible to customer)
                      </label>
                    )}
                    <div style={{ marginTop: 8 }}>
                      <button type="button" style={styles.btn} onClick={handleSaveEdit}>Save</button>
                      <button
                        type="button"
                        style={{ ...styles.btn, background: colors.gray500, marginLeft: 8 }}
                        onClick={() => setEditingComment(null)}
                      >
                        Cancel
                      </button>
                    </div>
                  </div>
                ) : (
                  <p style={{ ...styles.description, marginBottom: 0, marginTop: 8, whiteSpace: 'pre-wrap' }}>
                    {c.body}
                  </p>
                )}
              </div>
            ))}
          </div>
        )}

        <form onSubmit={handleAddComment} style={{ borderTop: `1px solid ${colors.gray100}`, paddingTop: 16 }}>
          <label style={{ fontSize: 13, fontWeight: 600, color: colors.gray700, display: 'block', marginBottom: 8 }}>
            Add a comment
          </label>
          <textarea
            value={newCommentBody}
            onChange={(e) => setNewCommentBody(e.target.value)}
            placeholder="Write a public reply or internal note…"
            rows={4}
            style={{
              width: '100%',
              padding: 10,
              borderRadius: 8,
              border: `1px solid ${colors.gray200}`,
              fontSize: 14,
              resize: 'vertical',
              marginBottom: 10,
            }}
          />
          {isStaff && (
            <label style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 12, fontSize: 13 }}>
              <input
                type="checkbox"
                checked={newCommentInternal}
                onChange={(e) => setNewCommentInternal(e.target.checked)}
              />
              Internal note (not visible to customer)
            </label>
          )}
          <button type="submit" style={styles.btn} disabled={commentSubmitting || !newCommentBody.trim()}>
            {commentSubmitting ? 'Posting…' : 'Post comment'}
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
                      {r.fileName && (
                        <span style={{ color: colors.gray500, fontSize: 12 }}>{r.fileName}</span>
                      )}
                      {canDownloadRecording && (
                        <button
                          type="button"
                          style={{
                            ...styles.linkishBtn,
                            opacity: openingRecordingId === r.id ? 0.55 : 1,
                          }}
                          disabled={openingRecordingId === r.id}
                          onClick={() => openRecordingDownload(r.id)}
                        >
                          {openingRecordingId === r.id ? 'Opening…' : 'Download / play'}
                        </button>
                      )}
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
