import React, { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  getTicket,
  updateTicket,
  getSessions,
  getRecordingsBySession,
  getTicketMessages,
  postTicketMessage,
  getTicketAttachments,
  requestTicketAttachmentUploadUrl,
  confirmTicketAttachment,
  deleteTicketAttachment,
  submitTicketSatisfaction,
  getTicketActivity,
  applyTicketAiClassification,
  getSavedReplies,
  createSavedReply,
} from '../services/api';

const colors = {
  primary: '#4F46E5', success: '#10B981', warning: '#F59E0B',
  danger: '#EF4444', gray100: '#F3F4F6', gray200: '#E5E7EB',
  gray500: '#6B7280', gray700: '#374151', gray900: '#111827',
};

const priorityColors = { LOW: colors.success, MEDIUM: colors.warning, HIGH: '#F97316', CRITICAL: colors.danger };
const statusColors = { OPEN: colors.primary, IN_PROGRESS: colors.warning, RESOLVED: colors.success, CLOSED: colors.gray500 };

const TICKET_CATEGORY_OPTIONS = [
  { value: 'GENERAL_INQUIRY', label: 'General inquiry' },
  { value: 'BILLING', label: 'Billing' },
  { value: 'TECHNICAL', label: 'Technical' },
  { value: 'ACCOUNT', label: 'Account' },
  { value: 'PRODUCT_FEEDBACK', label: 'Product feedback' },
  { value: 'SERVICE_OUTAGE', label: 'Service outage' },
  { value: 'COMPLAINT', label: 'Complaint' },
  { value: 'FEATURE_REQUEST', label: 'Feature request' },
];

function formatCategoryLabel(cat) {
  if (!cat) return '';
  return String(cat)
    .replace(/_/g, ' ')
    .toLowerCase()
    .replace(/\b\w/g, (ch) => ch.toUpperCase());
}

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
  btnGhost: {
    padding: '8px 16px', borderRadius: 8, border: `1px solid ${colors.gray200}`,
    fontSize: 13, fontWeight: 600, cursor: 'pointer', background: '#fff', color: colors.gray700,
  },
  sessionRow: {
    padding: '12px 16px', borderRadius: 8, border: `1px solid ${colors.gray200}`,
    marginBottom: 8, display: 'flex', justifyContent: 'space-between', alignItems: 'center',
  },
  empty: { color: colors.gray500, fontSize: 14, textAlign: 'center', padding: 32 },
  slaBanner: {
    borderRadius: 10,
    padding: '12px 16px',
    marginBottom: 16,
    fontSize: 14,
    fontWeight: 500,
    border: `1px solid ${colors.danger}40`,
    background: `${colors.danger}10`,
    color: '#991B1B',
  },
  activityRow: {
    padding: '10px 0',
    borderBottom: `1px solid ${colors.gray100}`,
    fontSize: 14,
    color: colors.gray700,
    lineHeight: 1.45,
  },
  activityMeta: { fontSize: 12, color: colors.gray500, marginTop: 4 },
  msgBoxInternal: {
    padding: '12px 14px', borderRadius: 10, background: `${colors.warning}12`,
    marginBottom: 10, border: `1px solid ${colors.warning}55`,
  },
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

function guessMime(file) {
  if (file.type) return file.type;
  const n = file.name.toLowerCase();
  if (n.endsWith('.pdf')) return 'application/pdf';
  if (n.endsWith('.png')) return 'image/png';
  if (n.endsWith('.jpg') || n.endsWith('.jpeg')) return 'image/jpeg';
  if (n.endsWith('.gif')) return 'image/gif';
  if (n.endsWith('.webp')) return 'image/webp';
  if (n.endsWith('.txt')) return 'text/plain';
  return '';
}

function roleKey(r) {
  if (r == null || r === '') return '';
  let s = String(r);
  if (s.startsWith('ROLE_')) {
    s = s.slice(5);
  }
  return s.toLowerCase();
}

function activityLabel(eventType) {
  const labels = {
    TICKET_CREATED: 'Ticket opened',
    TICKET_UPDATED: 'Ticket updated',
    CLAIMED: 'Claimed',
    MESSAGE_POSTED: 'Reply posted',
    ATTACHMENT_ADDED: 'Attachment added',
    ATTACHMENT_REMOVED: 'Attachment removed',
    SATISFACTION_SUBMITTED: 'Satisfaction submitted',
  };
  return labels[eventType] || eventType?.replace(/_/g, ' ') || 'Event';
}

function Stars({ value }) {
  const filled = Math.round(Number(value) || 0);
  return (
    <span style={{ letterSpacing: 2, color: colors.warning, fontSize: 18 }} aria-hidden>
      {'★'.repeat(filled)}{'☆'.repeat(Math.max(0, 5 - filled))}
    </span>
  );
}

export default function TicketDetail({ user }) {
  const { id } = useParams();
  const navigate = useNavigate();
  const fileRef = useRef(null);

  const [ticket, setTicket] = useState(null);
  const [sessions, setSessions] = useState([]);
  const [recordings, setRecordings] = useState({});
  const [messages, setMessages] = useState([]);
  const [attachments, setAttachments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [statusUpdate, setStatusUpdate] = useState('');
  const [updating, setUpdating] = useState(false);
  const [msgBody, setMsgBody] = useState('');
  const [postingMsg, setPostingMsg] = useState(false);
  const [attachBusy, setAttachBusy] = useState(false);
  const [satRating, setSatRating] = useState(5);
  const [satFeedback, setSatFeedback] = useState('');
  const [satSubmitting, setSatSubmitting] = useState(false);
  const [activity, setActivity] = useState(null);
  const [categoryDraft, setCategoryDraft] = useState('GENERAL_INQUIRY');
  const [catUpdating, setCatUpdating] = useState(false);
  const [aiApplying, setAiApplying] = useState(false);
  const [internalReply, setInternalReply] = useState(false);
  const [savedReplies, setSavedReplies] = useState([]);
  const [snippetChoice, setSnippetChoice] = useState('');
  const [macroTitle, setMacroTitle] = useState('');
  const [macroBody, setMacroBody] = useState('');
  const [macroSaving, setMacroSaving] = useState(false);
  const [tagsDraft, setTagsDraft] = useState('');
  const [tagsUpdating, setTagsUpdating] = useState(false);

  const rk = roleKey(user?.role);

  const canChangeStatus =
    rk === 'support_executive'
    || rk === 'support_admin'
    || rk === 'support_manager';

  const isCustomer = rk === 'user';
  const isAdmin = rk === 'support_admin';
  const canManageMacros = rk === 'support_admin' || rk === 'support_manager';

  const loadDetail = async () => {
    try {
      setLoading(true);
      setError(null);
      const [ticketRes, sessionsRes, msgRes, attRes, actRes] = await Promise.all([
        getTicket(id),
        getSessions(),
        getTicketMessages(id).catch(() => ({ data: [] })),
        getTicketAttachments(id).catch(() => ({ data: [] })),
        getTicketActivity(id).catch(() => ({ data: { content: [], totalElements: 0 } })),
      ]);
      const t = ticketRes.data;
      setTicket(t);
      setStatusUpdate(t.status);
      setCategoryDraft(t.category || 'GENERAL_INQUIRY');
      setTagsDraft(Array.isArray(t.tags) ? t.tags.join(', ') : '');
      setMessages(Array.isArray(msgRes.data) ? msgRes.data : []);
      setAttachments(Array.isArray(attRes.data) ? attRes.data : []);
      setActivity(actRes.data && typeof actRes.data === 'object' ? actRes.data : { content: [], totalElements: 0 });

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

      if (canChangeStatus) {
        try {
          const sr = await getSavedReplies();
          setSavedReplies(Array.isArray(sr.data) ? sr.data : []);
        } catch {
          setSavedReplies([]);
        }
      } else {
        setSavedReplies([]);
      }
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

  useEffect(() => {
    loadDetail();
  }, [id]);

  const reloadMessages = async () => {
    try {
      const res = await getTicketMessages(id);
      setMessages(res.data || []);
    } catch {
      /* silent */
    }
  };

  const reloadAttachments = async () => {
    try {
      const res = await getTicketAttachments(id);
      setAttachments(res.data || []);
    } catch {
      /* silent */
    }
  };

  const reloadActivity = async () => {
    try {
      const res = await getTicketActivity(id);
      setActivity(res.data || { content: [], totalElements: 0 });
    } catch {
      /* silent */
    }
  };

  const refreshTicketOnly = async () => {
    try {
      const res = await getTicket(id);
      setTicket(res.data);
      setStatusUpdate(res.data.status);
      setCategoryDraft(res.data.category || 'GENERAL_INQUIRY');
      setTagsDraft(Array.isArray(res.data.tags) ? res.data.tags.join(', ') : '');
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
      await refreshTicketOnly();
      await reloadActivity();
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
      const payload = { body: msgBody.trim(), ...(internalReply ? { internalNote: true } : {}) };
      await postTicketMessage(id, payload);
      setMsgBody('');
      setInternalReply(false);
      await reloadMessages();
      await reloadActivity();
    } catch (e2) {
      const msg = e2.response?.data?.message || 'Failed to post message';
      setError(msg);
    } finally {
      setPostingMsg(false);
    }
  };

  const handleSaveCategory = async () => {
    if (!ticket || categoryDraft === ticket.category) return;
    try {
      setCatUpdating(true);
      setError(null);
      await updateTicket(id, { category: categoryDraft });
      await refreshTicketOnly();
      await reloadActivity();
    } catch {
      setError('Failed to update category');
    } finally {
      setCatUpdating(false);
    }
  };

  const handleApplyAiClassification = async () => {
    try {
      setAiApplying(true);
      setError(null);
      await applyTicketAiClassification(id);
      await refreshTicketOnly();
      await reloadActivity();
    } catch (e) {
      const msg = e.response?.data?.message || 'Could not apply AI classification';
      setError(msg);
    } finally {
      setAiApplying(false);
    }
  };

  const handleSaveTags = async () => {
    if (!ticket) return;
    const tags = tagsDraft.split(',').map((s) => s.trim()).filter(Boolean);
    try {
      setTagsUpdating(true);
      setError(null);
      await updateTicket(id, { tags });
      await refreshTicketOnly();
      await reloadActivity();
    } catch {
      setError('Failed to update tags');
    } finally {
      setTagsUpdating(false);
    }
  };

  const handleInsertSnippet = () => {
    const rid = Number(snippetChoice);
    if (!rid) return;
    const row = savedReplies.find((r) => r.id === rid);
    if (!row) return;
    setMsgBody((prev) => (prev?.trim() ? `${prev.trimEnd()}\n\n${row.body}` : row.body));
  };

  const handleCreateMacro = async () => {
    if (!macroTitle.trim() || !macroBody.trim()) return;
    try {
      setMacroSaving(true);
      setError(null);
      await createSavedReply({ title: macroTitle.trim(), body: macroBody.trim() });
      const sr = await getSavedReplies();
      setSavedReplies(Array.isArray(sr.data) ? sr.data : []);
      setMacroTitle('');
      setMacroBody('');
    } catch {
      setError('Could not save reply to library');
    } finally {
      setMacroSaving(false);
    }
  };

  const handlePickFile = () => fileRef.current?.click();

  const handleFileSelected = async (e) => {
    const file = e.target.files?.[0];
    e.target.value = '';
    if (!file) return;
    const ct = guessMime(file);
    if (!ct) {
      setError('Could not detect file type. Use PNG, JPEG, GIF, WebP, PDF, or TXT.');
      return;
    }
    try {
      setAttachBusy(true);
      setError(null);
      const urlRes = await requestTicketAttachmentUploadUrl(id, { fileName: file.name, contentType: ct });
      const { uploadUrl, s3Key, contentTypeForUpload } = urlRes.data;
      const put = await fetch(uploadUrl, {
        method: 'PUT',
        body: file,
        headers: { 'Content-Type': contentTypeForUpload },
      });
      if (!put.ok) {
        throw new Error(`Upload failed (${put.status})`);
      }
      await confirmTicketAttachment(id, {
        s3Key,
        fileName: file.name,
        contentType: ct,
        fileSize: file.size,
      });
      await reloadAttachments();
      await reloadActivity();
    } catch {
      setError('Attachment upload failed. Check file type/size (max 25 MB) and try again.');
    } finally {
      setAttachBusy(false);
    }
  };

  const handleDeleteAttachment = async (attachmentId) => {
    if (!window.confirm('Delete this attachment from storage and database?')) return;
    try {
      setError(null);
      await deleteTicketAttachment(id, attachmentId);
      await reloadAttachments();
      await reloadActivity();
    } catch {
      setError('Could not delete attachment.');
    }
  };

  const handleSatisfactionSubmit = async (e) => {
    e.preventDefault();
    try {
      setSatSubmitting(true);
      setError(null);
      const payload = {
        rating: Number(satRating),
        ...(satFeedback.trim() ? { feedback: satFeedback.trim() } : {}),
      };
      await submitTicketSatisfaction(id, payload);
      await refreshTicketOnly();
      await reloadActivity();
      setSatFeedback('');
    } catch (e3) {
      const msg = e3.response?.data?.message || 'Could not submit feedback.';
      setError(msg);
    } finally {
      setSatSubmitting(false);
    }
  };

  const canSubmitSatisfaction =
    isCustomer
    && ticket
    && user?.id != null
    && ticket.userId === user.id
    && (ticket.status === 'RESOLVED' || ticket.status === 'CLOSED')
    && ticket.customerRating == null;

  if (loading) return <div style={styles.empty}>Loading ticket...</div>;
  if (error && !ticket) return <div style={{ ...styles.empty, color: colors.danger }}>{error}</div>;
  if (!ticket) return null;

  return (
    <div>
      <button style={styles.backBtn} type="button" onClick={() => navigate(-1)}>&larr; Back</button>

      <input
        ref={fileRef}
        type="file"
        accept=".png,.jpg,.jpeg,.gif,.webp,.pdf,.txt,image/png,image/jpeg,image/gif,image/webp,application/pdf,text/plain"
        style={{ display: 'none' }}
        onChange={handleFileSelected}
      />

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

      {ticket.slaBreached && (ticket.status === 'OPEN' || ticket.status === 'IN_PROGRESS') && (
        <div style={styles.slaBanner} role="status">
          First-response SLA missed — deadline was{' '}
          <strong>{ticket.slaRespondBy ? new Date(ticket.slaRespondBy).toLocaleString() : 'unknown'}</strong>.
          {' '}Priority was set to <strong>{ticket.priority}</strong>.
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
          <span style={{
            ...styles.badge,
            background: colors.gray100,
            color: colors.gray700,
            textTransform: 'none',
          }}>
            {formatCategoryLabel(ticket.category)}
          </span>
          <span style={{ fontSize: 13, color: colors.gray500, alignSelf: 'center' }}>
            Created {new Date(ticket.createdAt).toLocaleDateString()}
          </span>
          {ticket.slaRespondBy && (ticket.status === 'OPEN' || ticket.status === 'IN_PROGRESS') && (
            <span style={{ fontSize: 13, color: ticket.slaBreached ? colors.danger : colors.gray500, alignSelf: 'center', fontWeight: ticket.slaBreached ? 600 : 400 }}>
              Respond by {new Date(ticket.slaRespondBy).toLocaleString()}
            </span>
          )}
        </div>

        {Array.isArray(ticket.tags) && ticket.tags.length > 0 && (
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8, marginBottom: 16 }}>
            {ticket.tags.map((tag) => (
              <span
                key={tag}
                style={{
                  fontSize: 12,
                  fontWeight: 600,
                  color: colors.gray700,
                  background: colors.gray100,
                  padding: '4px 10px',
                  borderRadius: 6,
                  border: `1px solid ${colors.gray200}`,
                  fontFamily: 'ui-monospace, monospace',
                }}
              >
                {tag}
              </span>
            ))}
          </div>
        )}

        <p style={styles.description}>{ticket.description || 'No description provided.'}</p>

        {ticket.customerRating != null && (
          <div style={{ paddingTop: 12, marginBottom: 12, borderTop: `1px solid ${colors.gray100}` }}>
            <div style={{ fontSize: 13, fontWeight: 600, color: colors.gray700, marginBottom: 6 }}>
              Customer satisfaction
            </div>
            <Stars value={ticket.customerRating} />
            <span style={{ marginLeft: 10, fontSize: 14, color: colors.gray700 }}>
              {ticket.customerRating}/5
              {ticket.satisfactionSubmittedAt && (
                <span style={{ color: colors.gray500, marginLeft: 8 }}>
                  ({new Date(ticket.satisfactionSubmittedAt).toLocaleString()})
                </span>
              )}
            </span>
            {ticket.customerFeedback && (
              <p style={{ marginTop: 10, fontSize: 14, color: colors.gray700, fontStyle: 'italic', lineHeight: 1.5 }}>
                “{ticket.customerFeedback}”
              </p>
            )}
          </div>
        )}

        {canSubmitSatisfaction && (
          <div style={{ paddingTop: 12, marginBottom: 12, borderTop: `1px solid ${colors.gray100}` }}>
            <div style={{ fontSize: 14, fontWeight: 700, color: colors.gray900, marginBottom: 10 }}>
              How did we do?
            </div>
            <p style={{ fontSize: 13, color: colors.gray500, marginBottom: 12 }}>
              This ticket is closed out. Share a quick rating once — it helps us improve support.
            </p>
            <form onSubmit={handleSatisfactionSubmit}>
              <label htmlFor="sat-rating" style={{ fontSize: 13, fontWeight: 600, color: colors.gray700 }}>Rating</label>
              <select
                id="sat-rating"
                style={{ ...styles.select, display: 'block', marginBottom: 12 }}
                value={satRating}
                onChange={(ev) => setSatRating(ev.target.value)}
              >
                {[5, 4, 3, 2, 1].map((n) => (
                  <option key={n} value={n}>{n} — {n >= 4 ? 'Great' : n === 3 ? 'Okay' : 'Needs work'}</option>
                ))}
              </select>
              <textarea
                style={styles.textarea}
                placeholder="Optional comments for our team..."
                value={satFeedback}
                maxLength={2000}
                onChange={(ev) => setSatFeedback(ev.target.value)}
              />
              <button style={styles.btn} type="submit" disabled={satSubmitting}>
                {satSubmitting ? 'Submitting…' : 'Submit feedback'}
              </button>
            </form>
          </div>
        )}

        {canChangeStatus && (
          <>
            <div style={{ display: 'flex', alignItems: 'center', flexWrap: 'wrap', gap: 12, paddingTop: 12, borderTop: `1px solid ${colors.gray100}` }}>
              <span style={{ fontSize: 14, fontWeight: 600, color: colors.gray700 }}>Update Status:</span>
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
            <div style={{ paddingTop: 14, marginTop: 12, borderTop: `1px solid ${colors.gray100}` }}>
              <div style={{ fontSize: 14, fontWeight: 600, color: colors.gray700, marginBottom: 10 }}>Category & AI routing</div>
              <p style={{ fontSize: 13, color: colors.gray500, marginTop: 0, marginBottom: 12, maxWidth: 560, lineHeight: 1.5 }}>
                Used for reporting and queues. “Apply AI” runs OpenAI classification and updates category and priority on this ticket.
              </p>
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: 12, alignItems: 'center' }}>
                <select
                  style={styles.select}
                  value={categoryDraft}
                  onChange={(e) => setCategoryDraft(e.target.value)}
                  aria-label="Ticket category"
                >
                  {TICKET_CATEGORY_OPTIONS.map((o) => (
                    <option key={o.value} value={o.value}>{o.label}</option>
                  ))}
                </select>
                <button
                  style={styles.btn}
                  type="button"
                  onClick={handleSaveCategory}
                  disabled={catUpdating || categoryDraft === ticket.category}
                >
                  {catUpdating ? 'Saving…' : 'Save category'}
                </button>
                <button
                  style={{ ...styles.btnGhost, borderColor: colors.primary, color: colors.primary }}
                  type="button"
                  onClick={handleApplyAiClassification}
                  disabled={aiApplying}
                >
                  {aiApplying ? 'Running AI…' : 'Apply AI classification'}
                </button>
              </div>
            </div>
            <div style={{ paddingTop: 14, marginTop: 12, borderTop: `1px solid ${colors.gray100}` }}>
              <div style={{ fontSize: 14, fontWeight: 600, color: colors.gray700, marginBottom: 10 }}>Tags</div>
              <p style={{ fontSize: 13, color: colors.gray500, marginTop: 0, marginBottom: 12, maxWidth: 560, lineHeight: 1.5 }}>
                Comma-separated labels (letters, numbers, hyphens — normalized server-side). Used for filtering and exports. Up to 20 tags.
              </p>
              <input
                type="text"
                aria-label="Ticket tags"
                value={tagsDraft}
                onChange={(e) => setTagsDraft(e.target.value)}
                placeholder="vip, billing-follow-up, enterprise"
                style={{
                  width: '100%', maxWidth: 560, padding: '10px 14px', borderRadius: 8,
                  border: `1px solid ${colors.gray200}`, fontSize: 14, outline: 'none', marginBottom: 12,
                  boxSizing: 'border-box',
                }}
              />
              <button
                style={styles.btn}
                type="button"
                onClick={handleSaveTags}
                disabled={tagsUpdating}
              >
                {tagsUpdating ? 'Saving…' : 'Save tags'}
              </button>
            </div>
          </>
        )}
      </div>

      <div style={styles.card}>
        <h3 style={styles.sectionTitle}>Activity</h3>
        <p style={{ fontSize: 13, color: colors.gray500, marginTop: -6, marginBottom: 14 }}>
          Audit-style timeline for this ticket ({activity?.totalElements ?? activity?.content?.length ?? 0} events).
        </p>
        {!activity?.content?.length ? (
          <div style={styles.empty}>No activity recorded yet.</div>
        ) : (
          activity.content.map((ev) => (
            <div key={ev.id} style={styles.activityRow}>
              <div style={{ fontWeight: 600, color: colors.gray900 }}>
                {activityLabel(ev.eventType)}
              </div>
              <div>{ev.summary}</div>
              <div style={styles.activityMeta}>
                {ev.actorName}
                {ev.createdAt ? ` · ${new Date(ev.createdAt).toLocaleString()}` : ''}
              </div>
            </div>
          ))
        )}
      </div>

      <div style={styles.card}>
        <h3 style={styles.sectionTitle}>Attachments</h3>
        <p style={{ fontSize: 13, color: colors.gray500, marginTop: -6, marginBottom: 14 }}>
          Screenshots, PDFs, or notes up to 25 MB (stored securely in your recordings bucket).
        </p>
        <button type="button" style={{ ...styles.btnGhost, marginBottom: 16 }} onClick={handlePickFile} disabled={attachBusy}>
          {attachBusy ? 'Uploading…' : 'Add attachment'}
        </button>
        {attachments.length === 0 ? (
          <div style={styles.empty}>No files attached yet.</div>
        ) : (
          attachments.map((a) => (
            <div
              key={a.id}
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                flexWrap: 'wrap',
                gap: 10,
                padding: '12px 14px',
                borderRadius: 8,
                border: `1px solid ${colors.gray200}`,
                marginBottom: 8,
              }}
            >
              <div>
                <div style={{ fontWeight: 600, fontSize: 14 }}>{a.fileName}</div>
                <div style={{ fontSize: 12, color: colors.gray500, marginTop: 2 }}>
                  {a.uploadedByName} &middot; {a.uploadedAt ? new Date(a.uploadedAt).toLocaleString() : ''}
                  {a.fileSize ? ` · ${(a.fileSize / 1024).toFixed(1)} KB` : ''}
                </div>
              </div>
              <div style={{ display: 'flex', gap: 8 }}>
                {a.downloadUrl && (
                  <a href={a.downloadUrl} target="_blank" rel="noopener noreferrer" style={{ ...styles.btn, textDecoration: 'none', display: 'inline-block' }}>
                    Download
                  </a>
                )}
                {isAdmin && (
                  <button type="button" style={{ ...styles.btnGhost, color: colors.danger, borderColor: `${colors.danger}55` }} onClick={() => handleDeleteAttachment(a.id)}>
                    Delete
                  </button>
                )}
              </div>
            </div>
          ))
        )}
      </div>

      <div style={styles.card}>
        <h3 style={styles.sectionTitle}>Conversation</h3>
        {messages.length === 0 ? (
          <div style={styles.empty}>No replies yet.</div>
        ) : (
          messages.map((m) => (
            <div key={m.id} style={m.internalNote ? styles.msgBoxInternal : styles.msgBox}>
              {m.internalNote && (
                <div style={{ fontSize: 11, fontWeight: 700, color: '#B45309', marginBottom: 8, letterSpacing: '0.02em' }}>
                  Internal — customer cannot see this
                </div>
              )}
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
          {canManageMacros && (
            <div style={{ marginBottom: 14, padding: 14, borderRadius: 10, border: `1px solid ${colors.gray200}`, background: colors.gray100 }}>
              <div style={{ fontSize: 13, fontWeight: 700, color: colors.gray900, marginBottom: 10 }}>New saved reply</div>
              <input
                type="text"
                placeholder="Short title"
                value={macroTitle}
                onChange={(e) => setMacroTitle(e.target.value)}
                style={{
                  width: '100%', padding: '8px 12px', borderRadius: 8, border: `1px solid ${colors.gray200}`,
                  fontSize: 14, marginBottom: 8, boxSizing: 'border-box',
                }}
              />
              <textarea
                placeholder="Snippet body — executives can insert this into replies below."
                value={macroBody}
                onChange={(e) => setMacroBody(e.target.value)}
                style={{ ...styles.textarea, minHeight: 72, marginBottom: 8 }}
                maxLength={4096}
              />
              <button style={{ ...styles.btn, opacity: macroSaving ? 0.7 : 1 }} type="button" disabled={macroSaving || !macroTitle.trim() || !macroBody.trim()} onClick={handleCreateMacro}>
                {macroSaving ? 'Saving…' : 'Add to library'}
              </button>
            </div>
          )}
          {canChangeStatus && savedReplies.length > 0 && (
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: 12, alignItems: 'center', marginBottom: 12 }}>
              <select
                aria-label="Saved reply"
                style={{ ...styles.select, flex: '1 1 220px', marginRight: 0 }}
                value={snippetChoice}
                onChange={(e) => setSnippetChoice(e.target.value)}
              >
                <option value="">Insert saved reply…</option>
                {savedReplies.map((r) => (
                  <option key={r.id} value={String(r.id)}>{r.title}</option>
                ))}
              </select>
              <button type="button" style={styles.btnGhost} disabled={!snippetChoice} onClick={handleInsertSnippet}>
                Insert into reply
              </button>
            </div>
          )}
          {canChangeStatus && (
            <label style={{ display: 'flex', alignItems: 'center', gap: 10, fontSize: 13, color: colors.gray700, marginBottom: 12, cursor: 'pointer' }}>
              <input
                type="checkbox"
                checked={internalReply}
                onChange={(ev) => setInternalReply(ev.target.checked)}
              />
              Internal note (visible only to support staff)
            </label>
          )}
          <textarea
            style={styles.textarea}
            placeholder={
              canChangeStatus && internalReply
                ? 'Private note for your team — the customer will not see this…'
                : 'Write an update visible to everyone on this ticket…'
            }
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
