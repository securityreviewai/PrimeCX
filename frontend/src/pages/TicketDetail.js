import React, { useState, useEffect, useMemo } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getTicket, updateTicket, getSessions, getRecordingsBySession } from '../services/api';

const colors = {
  primary: '#4F46E5', success: '#10B981', warning: '#F59E0B',
  danger: '#EF4444', gray100: '#F3F4F6', gray200: '#E5E7EB',
  gray500: '#6B7280', gray700: '#374151', gray900: '#111827',
};

const priorityColors = { LOW: colors.success, MEDIUM: colors.warning, HIGH: '#F97316', CRITICAL: colors.danger };
const statusColors = { OPEN: colors.primary, IN_PROGRESS: colors.warning, RESOLVED: colors.success, CLOSED: colors.gray500 };

const TICKET_CATEGORY_LABELS = {
  GENERAL: 'General',
  BILLING: 'Billing',
  TECHNICAL: 'Technical',
  ACCOUNT: 'Account',
  PRODUCT_FEEDBACK: 'Product feedback',
};

const TICKET_CATEGORY_OPTIONS = [
  ['GENERAL', 'General'],
  ['BILLING', 'Billing'],
  ['TECHNICAL', 'Technical'],
  ['ACCOUNT', 'Account'],
  ['PRODUCT_FEEDBACK', 'Product feedback'],
];

function sessionEndedAtMs(s) {
  if (!s?.endTime) return 0;
  const t = new Date(s.endTime).getTime();
  return Number.isNaN(t) ? 0 : t;
}

function toDatetimeLocalValue(iso) {
  if (!iso) return '';
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return '';
  const pad = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

function isFollowUpOverdue(ticket) {
  if (!ticket?.followUpDueAt) return false;
  const due = new Date(ticket.followUpDueAt).getTime();
  if (Number.isNaN(due)) return false;
  if (!['OPEN', 'IN_PROGRESS'].includes(ticket.status)) return false;
  return Date.now() > due;
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
  formLabel: { fontSize: 13, fontWeight: 600, color: colors.gray700, marginBottom: 6, display: 'block' },
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
  notesArea: {
    width: '100%', minHeight: 100, padding: 12, borderRadius: 8,
    border: `1px solid ${colors.gray200}`, fontSize: 14, fontFamily: 'inherit', resize: 'vertical',
    marginBottom: 12, boxSizing: 'border-box',
  },
  hint: { fontSize: 12, color: colors.gray500, marginBottom: 12 },
  banner: {
    background: `${colors.danger}12`, color: colors.danger, padding: 12, borderRadius: 8,
    marginBottom: 16, fontSize: 14,
  },
  copyBtn: {
    fontSize: 13, fontWeight: 600, color: colors.primary, background: colors.gray100,
    border: `1px solid ${colors.gray200}`, borderRadius: 8, padding: '6px 12px',
    cursor: 'pointer', marginTop: 8,
  },
  escalationRow: {
    display: 'flex', alignItems: 'center', gap: 10, marginTop: 12, fontSize: 14,
    color: colors.gray700, cursor: 'pointer', userSelect: 'none',
  },
  wrapUpBody: {
    fontSize: 14, color: colors.gray700, lineHeight: 1.65, whiteSpace: 'pre-wrap',
    background: colors.gray100, padding: 14, borderRadius: 8, border: `1px solid ${colors.gray200}`,
  },
  supportReplyPublic: {
    marginBottom: 20, padding: 16, background: `${colors.primary}0d`, borderRadius: 12,
    border: `1px solid ${colors.primary}33`,
  },
  staffSplitRow: {
    display: 'flex', flexWrap: 'wrap', gap: 12, alignItems: 'flex-end', marginTop: 12,
  },
  datetimeInput: {
    padding: '8px 12px', borderRadius: 8, border: `1px solid ${colors.gray200}`,
    fontSize: 14, marginTop: 8, marginBottom: 8,
  },
  starRow: { display: 'flex', gap: 8, flexWrap: 'wrap', marginBottom: 12 },
  starBtn: {
    width: 40, height: 40, borderRadius: 8, border: `1px solid ${colors.gray200}`,
    background: '#fff', cursor: 'pointer', fontSize: 16, fontWeight: 700,
  },
  starBtnActive: { background: `${colors.warning}28`, borderColor: colors.warning, color: colors.warning },
};

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
  const [internalNotes, setInternalNotes] = useState('');
  const [notesSaving, setNotesSaving] = useState(false);
  const [copyDone, setCopyDone] = useState(false);
  const [escalationSaving, setEscalationSaving] = useState(false);
  const [supportReplyText, setSupportReplyText] = useState('');
  const [supportReplySaving, setSupportReplySaving] = useState(false);
  const [categoryEdit, setCategoryEdit] = useState('GENERAL');
  const [categorySaving, setCategorySaving] = useState(false);
  const [followUpDueLocal, setFollowUpDueLocal] = useState('');
  const [clearFollowUpCheck, setClearFollowUpCheck] = useState(false);
  const [followUpSaving, setFollowUpSaving] = useState(false);
  const [satisfactionPick, setSatisfactionPick] = useState(null);
  const [satisfactionCommentText, setSatisfactionCommentText] = useState('');
  const [satisfactionSaving, setSatisfactionSaving] = useState(false);

  const staffRoles = ['support_executive', 'support_admin', 'support_manager'];
  const canChangeStatus = staffRoles.includes(user?.role);
  const canEditInternalNotes = staffRoles.includes(user?.role);

  const latestWrapUpSession = useMemo(() => {
    const candidates = sessions.filter(
      (s) => s.endTime && s.notes && String(s.notes).trim().length > 0,
    );
    if (candidates.length === 0) return null;
    return [...candidates].sort((a, b) => sessionEndedAtMs(b) - sessionEndedAtMs(a))[0];
  }, [sessions]);

  useEffect(() => {
    const fetchData = async () => {
      try {
        setLoading(true);
        setError(null);
        const [ticketRes, sessionsRes] = await Promise.all([getTicket(id), getSessions()]);
        const found = ticketRes.data;
        setTicket(found);
        setStatusUpdate(found.status);
        setInternalNotes(found.internalNotes || '');
        setSupportReplyText(found.supportReply || '');
        setCategoryEdit(found.category || 'GENERAL');
        setFollowUpDueLocal(toDatetimeLocalValue(found.followUpDueAt));
        setClearFollowUpCheck(false);
        setSatisfactionPick(null);
        setSatisfactionCommentText('');

        const ticketSessions = sessionsRes.data.filter((s) => String(s.ticketId) === String(found.id));
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
      } catch {
        setError('Failed to load ticket details');
        setTicket(null);
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, [id]);

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

  const handleSaveInternalNotes = async () => {
    if (!canEditInternalNotes) return;
    try {
      setNotesSaving(true);
      setError(null);
      await updateTicket(id, { internalNotes });
      setTicket({ ...ticket, internalNotes });
    } catch {
      setError('Failed to save internal notes');
    } finally {
      setNotesSaving(false);
    }
  };

  const handleEscalationToggle = async (e) => {
    if (!canEditInternalNotes) return;
    const next = e.target.checked;
    try {
      setEscalationSaving(true);
      setError(null);
      await updateTicket(id, { escalated: next });
      setTicket({ ...ticket, escalated: next });
    } catch {
      setError('Failed to update escalation flag');
    } finally {
      setEscalationSaving(false);
    }
  };

  const handleSaveSupportReply = async () => {
    if (!canEditInternalNotes) return;
    try {
      setSupportReplySaving(true);
      setError(null);
      await updateTicket(id, { supportReply: supportReplyText });
      setTicket({ ...ticket, supportReply: supportReplyText });
    } catch {
      setError('Failed to save support reply');
    } finally {
      setSupportReplySaving(false);
    }
  };

  const handleSaveCategory = async () => {
    if (!canEditInternalNotes) return;
    try {
      setCategorySaving(true);
      setError(null);
      await updateTicket(id, { category: categoryEdit });
      setTicket({ ...ticket, category: categoryEdit });
    } catch {
      setError('Failed to update category');
    } finally {
      setCategorySaving(false);
    }
  };

  const handleSaveFollowUp = async () => {
    if (!canEditInternalNotes) return;
    try {
      setFollowUpSaving(true);
      setError(null);
      if (clearFollowUpCheck) {
        await updateTicket(id, { clearFollowUpDueAt: true });
        setTicket({ ...ticket, followUpDueAt: null });
        setFollowUpDueLocal('');
        setClearFollowUpCheck(false);
      } else if (!followUpDueLocal?.trim()) {
        setError('Pick a follow-up date and time, or check “Clear follow-up date”.');
      } else {
        const iso = new Date(followUpDueLocal).toISOString();
        await updateTicket(id, { followUpDueAt: iso });
        setTicket({ ...ticket, followUpDueAt: iso });
      }
    } catch {
      setError('Failed to save follow-up date');
    } finally {
      setFollowUpSaving(false);
    }
  };

  const handleSubmitSatisfaction = async () => {
    if (!satisfactionPick) {
      setError('Choose a rating from 1 to 5.');
      return;
    }
    try {
      setSatisfactionSaving(true);
      setError(null);
      await updateTicket(id, {
        satisfactionRating: satisfactionPick,
        satisfactionComment: satisfactionCommentText.trim() || undefined,
      });
      setTicket({
        ...ticket,
        satisfactionRating: satisfactionPick,
        satisfactionComment: satisfactionCommentText.trim() || null,
      });
      setSatisfactionPick(null);
      setSatisfactionCommentText('');
    } catch {
      setError('Could not submit feedback. It may already be submitted or the ticket may not be resolved yet.');
    } finally {
      setSatisfactionSaving(false);
    }
  };

  const handleCopyReference = async () => {
    if (!ticket) return;
    const line = `#${ticket.id} · ${ticket.title}`;
    const text = `${line}\n${window.location.href}`;
    try {
      await navigator.clipboard.writeText(text);
      setError(null);
      setCopyDone(true);
      window.setTimeout(() => setCopyDone(false), 2000);
    } catch {
      setError('Unable to copy to clipboard');
    }
  };

  if (loading) return <div style={styles.empty}>Loading ticket...</div>;
  if (!ticket && error) return <div style={{ ...styles.empty, color: colors.danger }}>{error}</div>;
  if (!ticket) return null;

  const canRateTicket = (user?.role === 'user' || user?.role === 'ROLE_USER')
    && Number(user?.id) === Number(ticket.userId)
    && ['RESOLVED', 'CLOSED'].includes(ticket.status)
    && !ticket.satisfactionRating;

  return (
    <div>
      <button style={styles.backBtn} onClick={() => navigate(-1)}>&larr; Back</button>

      {error && (
        <div style={styles.banner}>{error}</div>
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
            border: `1px solid ${colors.gray200}`,
            textTransform: 'none',
          }}>
            {TICKET_CATEGORY_LABELS[ticket.category] || 'General'}
          </span>
          {canEditInternalNotes && ticket.escalated && (
            <span style={{
              ...styles.badge,
              background: `${colors.danger}22`,
              color: colors.danger,
            }}>
              Escalated
            </span>
          )}
          <span style={{ fontSize: 13, color: colors.gray500, alignSelf: 'center' }}>
            Created {new Date(ticket.createdAt).toLocaleDateString()}
          </span>
          {ticket.followUpDueAt && (
            <span style={{
              fontSize: 13,
              alignSelf: 'center',
              color: isFollowUpOverdue(ticket) ? colors.danger : colors.gray500,
              fontWeight: isFollowUpOverdue(ticket) ? 600 : 400,
            }}>
              Follow-up: {new Date(ticket.followUpDueAt).toLocaleString()}
              {isFollowUpOverdue(ticket) ? ' · Overdue' : ''}
            </span>
          )}
        </div>

        <button type="button" style={styles.copyBtn} onClick={handleCopyReference}>
          {copyDone ? 'Copied!' : 'Copy reference (ID, title & link)'}
        </button>

        {canEditInternalNotes && (
          <label style={styles.escalationRow}>
            <input
              type="checkbox"
              checked={!!ticket.escalated}
              onChange={handleEscalationToggle}
              disabled={escalationSaving}
            />
            <span>
              Escalated to leadership review
              {escalationSaving ? ' (saving…)' : ''}
            </span>
          </label>
        )}

        <p style={styles.description}>{ticket.description || 'No description provided.'}</p>

        {ticket.supportReply && String(ticket.supportReply).trim() && (
          <div style={styles.supportReplyPublic}>
            <h3 style={{ ...styles.sectionTitle, marginTop: 0 }}>Message from support</h3>
            <div style={{ ...styles.wrapUpBody, background: '#fff' }}>{ticket.supportReply.trim()}</div>
          </div>
        )}

        {canEditInternalNotes && (
          <div style={{ paddingTop: 8, marginBottom: 16, borderTop: `1px solid ${colors.gray100}` }}>
            <h3 style={{ fontSize: 15, fontWeight: 700, color: colors.gray900, marginBottom: 8 }}>
              Customer-visible reply
            </h3>
            <p style={styles.hint}>Customers see this on the ticket. Uses the same field as “Message from support” above.</p>
            <textarea
              style={styles.notesArea}
              value={supportReplyText}
              onChange={(e) => setSupportReplyText(e.target.value)}
              placeholder="Updates, next steps, or answers for the customer…"
            />
            <button type="button" style={styles.btn} onClick={handleSaveSupportReply} disabled={supportReplySaving}>
              {supportReplySaving ? 'Saving…' : 'Save reply'}
            </button>
            <div style={styles.staffSplitRow}>
              <div>
                <span style={{ fontSize: 13, fontWeight: 600, color: colors.gray700, display: 'block', marginBottom: 6 }}>Category</span>
                <select
                  style={styles.select}
                  value={categoryEdit}
                  onChange={(e) => setCategoryEdit(e.target.value)}
                >
                  {TICKET_CATEGORY_OPTIONS.map(([value, label]) => (
                    <option key={value} value={value}>{label}</option>
                  ))}
                </select>
              </div>
              <button type="button" style={styles.btn} onClick={handleSaveCategory} disabled={categorySaving}>
                {categorySaving ? 'Saving…' : 'Save category'}
              </button>
            </div>
            <div style={{ marginTop: 20, paddingTop: 16, borderTop: `1px solid ${colors.gray100}` }}>
              <h4 style={{ fontSize: 14, fontWeight: 700, color: colors.gray900, marginBottom: 4 }}>Follow-up due</h4>
              <p style={styles.hint}>Customers see this target on the ticket. Used for overdue counts on dashboards.</p>
              <input
                type="datetime-local"
                style={styles.datetimeInput}
                value={followUpDueLocal}
                onChange={(e) => {
                  setFollowUpDueLocal(e.target.value);
                  setClearFollowUpCheck(false);
                }}
                disabled={clearFollowUpCheck}
              />
              <label style={{ ...styles.escalationRow, marginTop: 4 }}>
                <input
                  type="checkbox"
                  checked={clearFollowUpCheck}
                  onChange={(e) => {
                    setClearFollowUpCheck(e.target.checked);
                    if (e.target.checked) setFollowUpDueLocal('');
                  }}
                />
                <span>Clear follow-up date</span>
              </label>
              <button type="button" style={styles.btn} onClick={handleSaveFollowUp} disabled={followUpSaving}>
                {followUpSaving ? 'Saving…' : 'Save follow-up'}
              </button>
            </div>
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

      {!!ticket.satisfactionRating && (
        <div style={styles.card}>
          <h3 style={styles.sectionTitle}>Satisfaction</h3>
          <p style={{ fontSize: 15, color: colors.gray700 }}>
            Rating: <strong>{ticket.satisfactionRating}</strong> / 5
          </p>
          {ticket.satisfactionComment && (
            <div style={{ ...styles.wrapUpBody, background: '#fff', marginTop: 8 }}>
              {ticket.satisfactionComment}
            </div>
          )}
        </div>
      )}

      {canRateTicket && (
        <div style={styles.card}>
          <h3 style={styles.sectionTitle}>How did we do?</h3>
          <p style={styles.hint}>This ticket is resolved. Please rate your experience (one time).</p>
          <div style={styles.starRow}>
            {[1, 2, 3, 4, 5].map((n) => (
              <button
                key={n}
                type="button"
                style={{
                  ...styles.starBtn,
                  ...(satisfactionPick === n ? styles.starBtnActive : {}),
                }}
                onClick={() => setSatisfactionPick(n)}
              >
                {n}
              </button>
            ))}
          </div>
          <label style={styles.formLabel}>Optional comment</label>
          <textarea
            style={styles.notesArea}
            value={satisfactionCommentText}
            onChange={(e) => setSatisfactionCommentText(e.target.value)}
            placeholder="What went well or what could be better?"
          />
          <button type="button" style={styles.btn} onClick={handleSubmitSatisfaction} disabled={satisfactionSaving}>
            {satisfactionSaving ? 'Submitting…' : 'Submit feedback'}
          </button>
        </div>
      )}

      {canEditInternalNotes && (
        <div style={styles.card}>
          <h3 style={styles.sectionTitle}>Internal notes</h3>
          <p style={styles.hint}>Visible only to support staff. Not shown to customers.</p>
          <textarea
            style={styles.notesArea}
            value={internalNotes}
            onChange={(e) => setInternalNotes(e.target.value)}
            placeholder="Handoff context, troubleshooting steps, customer context…"
          />
          <button type="button" style={styles.btn} onClick={handleSaveInternalNotes} disabled={notesSaving}>
            {notesSaving ? 'Saving…' : 'Save notes'}
          </button>
        </div>
      )}

      {canEditInternalNotes && latestWrapUpSession && (
        <div style={styles.card}>
          <h3 style={styles.sectionTitle}>Latest session wrap-up</h3>
          <p style={styles.hint}>
            From session #{latestWrapUpSession.id}
            {latestWrapUpSession.supportExecutiveName
              ? ` · ${latestWrapUpSession.supportExecutiveName}`
              : ''}
            {latestWrapUpSession.endTime
              ? ` · ended ${new Date(latestWrapUpSession.endTime).toLocaleString()}`
              : ''}
          </p>
          <div style={styles.wrapUpBody}>{latestWrapUpSession.notes.trim()}</div>
        </div>
      )}

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
