import React, { useState, useEffect, useMemo, useCallback } from 'react';
import {
  getDashboard,
  getUsers,
  updateUserRole,
  getTickets,
  getSessions,
  getRecordingsBySession,
  getRecordingDownloadUrl,
  getAdminRecordings,
  getStorageDefaults,
  listOrganizations,
  createOrganization,
  upsertRetentionPolicy,
  assignUserOrganization,
  setRecordingLegalHold,
  softDeleteRecordingStorage,
  getCannedResponses,
  createCannedResponse,
  updateCannedResponse,
  deleteCannedResponse,
} from '../services/api';
import { formatTicketLastTouch } from '../utils/formatTicketLastTouch';

const normalizeRole = (role) => (role || '').replace(/^ROLE_/i, '').toLowerCase();

const colors = {
  primary: '#4F46E5', success: '#10B981', warning: '#F59E0B',
  danger: '#EF4444', gray100: '#F3F4F6', gray200: '#E5E7EB',
  gray500: '#6B7280', gray700: '#374151', gray900: '#111827',
};

const statusColors = { OPEN: colors.primary, IN_PROGRESS: colors.warning, RESOLVED: colors.success, CLOSED: colors.gray500 };

const styles = {
  heading: { fontSize: 24, fontWeight: 700, color: colors.gray900, marginBottom: 24 },
  statsGrid: { display: 'grid', gridTemplateColumns: 'repeat(5, 1fr)', gap: 16, marginBottom: 32 },
  statCard: {
    background: '#fff', borderRadius: 12, padding: '20px 24px',
    boxShadow: '0 1px 4px rgba(0,0,0,0.06)',
  },
  statValue: { fontSize: 28, fontWeight: 700, marginBottom: 4 },
  statLabel: { fontSize: 13, color: colors.gray500, fontWeight: 500 },
  card: { background: '#fff', borderRadius: 12, padding: 24, boxShadow: '0 1px 4px rgba(0,0,0,0.06)', marginBottom: 24 },
  cardTitle: { fontSize: 16, fontWeight: 700, color: colors.gray900, marginBottom: 16 },
  table: { width: '100%', borderCollapse: 'collapse' },
  th: {
    textAlign: 'left', padding: '10px 14px', fontSize: 12, fontWeight: 600,
    color: colors.gray500, textTransform: 'uppercase', letterSpacing: '0.5px',
    borderBottom: `2px solid ${colors.gray200}`,
  },
  td: { padding: '12px 14px', fontSize: 14, color: colors.gray700, borderBottom: `1px solid ${colors.gray100}` },
  select: {
    padding: '6px 10px', borderRadius: 6, border: `1px solid ${colors.gray200}`,
    fontSize: 13, outline: 'none', background: '#fff',
  },
  badge: {
    display: 'inline-block', padding: '3px 10px', borderRadius: 20, fontSize: 12,
    fontWeight: 600, textTransform: 'uppercase',
  },
  filterRow: { display: 'flex', gap: 8, marginBottom: 16 },
  filterBtn: {
    padding: '6px 14px', borderRadius: 20, border: `1px solid ${colors.gray200}`,
    background: '#fff', fontSize: 13, fontWeight: 500, cursor: 'pointer', color: colors.gray700,
  },
  filterBtnActive: {
    background: colors.primary, color: '#fff', borderColor: colors.primary,
  },
  row: {
    display: 'flex', justifyContent: 'space-between', alignItems: 'center',
    padding: '12px 16px', borderRadius: 8, border: `1px solid ${colors.gray200}`, marginBottom: 8,
  },
  playBtn: {
    background: 'none', border: 'none', color: colors.primary, fontSize: 13, fontWeight: 600,
    cursor: 'pointer', padding: 0,
  },
  playBtnDisabled: { opacity: 0.55, cursor: 'not-allowed' },
  empty: { color: colors.gray500, fontSize: 14, textAlign: 'center', padding: 32 },
  activeToggle: {
    padding: '4px 12px', borderRadius: 6, border: 'none', fontSize: 12,
    fontWeight: 600, cursor: 'pointer',
  },
  input: {
    padding: '8px 10px', borderRadius: 6, border: `1px solid ${colors.gray200}`, fontSize: 13, minWidth: 120,
  },
  smallBtn: {
    padding: '6px 10px', borderRadius: 6, border: `1px solid ${colors.gray200}`,
    background: '#fff', fontSize: 12, fontWeight: 600, cursor: 'pointer', color: colors.gray700,
  },
  textarea: {
    width: '100%', padding: '8px 10px', borderRadius: 6, border: `1px solid ${colors.gray200}`,
    fontSize: 13, outline: 'none', resize: 'vertical', fontFamily: 'inherit',
  },
};

const ROLES = ['user', 'support_executive', 'support_manager', 'support_admin'];
const STATUS_FILTERS = ['ALL', 'OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'];

export default function AdminPanel({ user }) {
  const isPlatformAdmin = useMemo(() => normalizeRole(user?.role) === 'support_admin', [user?.role]);

  const [stats, setStats] = useState(null);
  const [users, setUsers] = useState([]);
  const [tickets, setTickets] = useState([]);
  const [recordings, setRecordings] = useState([]);
  const [organizations, setOrganizations] = useState([]);
  const [storageDefaults, setStorageDefaults] = useState(null);
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [overdueOnly, setOverdueOnly] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [openingRecordingId, setOpeningRecordingId] = useState(null);
  const [policyBucket, setPolicyBucket] = useState('');
  const [policyOrgId, setPolicyOrgId] = useState('');
  const [policyRetentionDays, setPolicyRetentionDays] = useState(2555);
  const [policyGraceDays, setPolicyGraceDays] = useState(30);
  const [newOrgName, setNewOrgName] = useState('');
  const [savingPolicy, setSavingPolicy] = useState(false);
  const [recBusyId, setRecBusyId] = useState(null);
  const [cannedResponses, setCannedResponses] = useState([]);
  const [crShortcode, setCrShortcode] = useState('');
  const [crTitle, setCrTitle] = useState('');
  const [crContent, setCrContent] = useState('');
  const [crCategory, setCrCategory] = useState('');
  const [editingCrId, setEditingCrId] = useState(null);
  const [crSaving, setCrSaving] = useState(false);

  const loadRecordings = useCallback(async (sessionsRes) => {
    if (isPlatformAdmin) {
      try {
        const r = await getAdminRecordings();
        setRecordings(r.data || []);
        return;
      } catch { /* fall through */ }
    }
    const recs = [];
    for (const s of (sessionsRes?.data || []).slice(0, 20)) {
      try {
        const resp = await getRecordingsBySession(s.id);
        recs.push(...(resp.data || []).map((rec) => ({ ...rec, sessionId: s.id })));
      } catch { /* skip */ }
    }
    setRecordings(recs);
  }, [isPlatformAdmin]);

  useEffect(() => {
    const fetchData = async () => {
      try {
        setLoading(true);
        const [dashRes, usersRes, ticketsRes, sessionsRes] = await Promise.all([
          getDashboard(),
          getUsers(),
          getTickets(overdueOnly ? { overdueOnly: true } : undefined),
          getSessions(),
        ]);
        setStats(dashRes.data);
        setUsers(usersRes.data);
        setTickets(ticketsRes.data);

        if (isPlatformAdmin) {
          try {
            const [defRes, orgRes] = await Promise.all([getStorageDefaults(), listOrganizations()]);
            setStorageDefaults(defRes.data);
            setOrganizations(orgRes.data || []);
          } catch { /* non-fatal */ }
        }
        await loadRecordings(sessionsRes);

        if (isPlatformAdmin) {
          try {
            const crRes = await getCannedResponses();
            setCannedResponses(crRes.data || []);
          } catch { /* non-fatal */ }
        }
      } catch {
        setError('Failed to load admin data');
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, [overdueOnly, isPlatformAdmin, loadRecordings]);

  const handleRoleChange = async (userId, newRole) => {
    try {
      await updateUserRole(userId, newRole);
      setUsers((prev) => prev.map((u) => (u.id === userId ? { ...u, role: newRole } : u)));
    } catch {
      setError('Failed to update role');
    }
  };

  const handleToggleActive = async (userId, currentlyActive) => {
    try {
      await updateUserRole(userId, undefined);
      setUsers((prev) =>
        prev.map((u) => (u.id === userId ? { ...u, active: !currentlyActive } : u))
      );
    } catch {
      setError('Failed to toggle user status');
    }
  };

  const handleCreateOrg = async () => {
    const name = newOrgName.trim();
    if (!name) return;
    try {
      const res = await createOrganization(name);
      setOrganizations((o) => [...o, res.data].sort((a, b) => a.name.localeCompare(b.name)));
      setNewOrgName('');
    } catch {
      setError('Failed to create organization');
    }
  };

  const handleAssignOrg = async (userId, value) => {
    if (value === '') return;
    const orgId = Number(value);
    if (Number.isNaN(orgId)) return;
    try {
      const res = await assignUserOrganization(userId, orgId);
      setUsers((prev) => prev.map((u) => (u.id === userId ? res.data : u)));
    } catch {
      setError('Failed to assign organization');
    }
  };

  const saveRetentionPolicy = async (e) => {
    e.preventDefault();
    const b = policyBucket.trim();
    if (!b) {
      window.alert('Enter S3 bucket name.');
      return;
    }
    setSavingPolicy(true);
    try {
      await upsertRetentionPolicy({
        s3Bucket: b,
        organizationId: policyOrgId === '' ? null : Number(policyOrgId),
        retentionDays: policyRetentionDays,
        softDeleteGraceDays: policyGraceDays,
      });
      const orgRes = await listOrganizations();
      setOrganizations(orgRes.data || []);
      window.alert('Retention policy saved.');
    } catch {
      window.alert('Failed to save policy. Check values and that you are a platform admin.');
    } finally {
      setSavingPolicy(false);
    }
  };

  const toggleHold = async (recordingId, next) => {
    setRecBusyId(recordingId);
    try {
      const res = await setRecordingLegalHold(recordingId, next);
      setRecordings((rs) => rs.map((r) => (r.id === recordingId ? { ...r, ...res.data } : r)));
    } catch {
      window.alert('Could not update legal hold.');
    } finally {
      setRecBusyId(null);
    }
  };

  const doSoftDelete = async (recordingId) => {
    if (!window.confirm('Mark this recording as soft-deleted? It can be purged by the retention job if not on hold.')) return;
    setRecBusyId(recordingId);
    try {
      await softDeleteRecordingStorage(recordingId);
      setRecordings((rs) => rs.map((r) => (r.id === recordingId
        ? { ...r, deletedAt: new Date().toISOString() }
        : r)));
    } catch {
      window.alert('Soft delete failed.');
    } finally {
      setRecBusyId(null);
    }
  };

  const resetCrForm = () => {
    setCrShortcode('');
    setCrTitle('');
    setCrContent('');
    setCrCategory('');
    setEditingCrId(null);
  };

  const handleSaveCannedResponse = async (e) => {
    e.preventDefault();
    if (!crShortcode.trim() || !crTitle.trim() || !crContent.trim()) return;
    setCrSaving(true);
    try {
      const payload = {
        shortcode: crShortcode.trim().toLowerCase(),
        title: crTitle.trim(),
        content: crContent.trim(),
        category: crCategory.trim() || null,
      };
      if (editingCrId) {
        const res = await updateCannedResponse(editingCrId, payload);
        setCannedResponses((prev) => prev.map((cr) => (cr.id === editingCrId ? res.data : cr)));
      } else {
        const res = await createCannedResponse(payload);
        setCannedResponses((prev) => [...prev, res.data]);
      }
      resetCrForm();
    } catch (err) {
      const msg = err.response?.data?.message || err.response?.data || 'Failed to save canned response';
      window.alert(typeof msg === 'string' ? msg : 'Failed to save canned response');
    } finally {
      setCrSaving(false);
    }
  };

  const handleEditCannedResponse = (cr) => {
    setEditingCrId(cr.id);
    setCrShortcode(cr.shortcode);
    setCrTitle(cr.title);
    setCrContent(cr.content);
    setCrCategory(cr.category || '');
  };

  const handleDeleteCannedResponse = async (id) => {
    if (!window.confirm('Delete this canned response?')) return;
    try {
      await deleteCannedResponse(id);
      setCannedResponses((prev) => prev.filter((cr) => cr.id !== id));
      if (editingCrId === id) resetCrForm();
    } catch {
      window.alert('Failed to delete canned response');
    }
  };

  const filteredTickets = statusFilter === 'ALL'
    ? tickets
    : tickets.filter((t) => t.status === statusFilter);

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

  if (loading) return <div style={styles.empty}>Loading admin panel...</div>;
  if (error) return <div style={{ ...styles.empty, color: colors.danger }}>{error}</div>;

  return (
    <div>
      <h1 style={styles.heading}>Admin Panel</h1>

      <div style={styles.statsGrid}>
        <div style={styles.statCard}>
          <div style={{ ...styles.statValue, color: colors.primary }}>{stats?.totalTickets ?? 0}</div>
          <div style={styles.statLabel}>Total Tickets</div>
        </div>
        <div style={styles.statCard}>
          <div style={{ ...styles.statValue, color: colors.warning }}>{stats?.openTickets ?? 0}</div>
          <div style={styles.statLabel}>Open Tickets</div>
        </div>
        <div style={styles.statCard}>
          <div style={{ ...styles.statValue, color: colors.danger }}>{stats?.overdueTickets ?? 0}</div>
          <div style={styles.statLabel}>Overdue (SLA)</div>
        </div>
        <div style={styles.statCard}>
          <div style={{ ...styles.statValue, color: colors.success }}>{users.length}</div>
          <div style={styles.statLabel}>Total Users</div>
        </div>
        <div style={styles.statCard}>
          <div style={{ ...styles.statValue, color: colors.danger }}>{recordings.length}</div>
          <div style={styles.statLabel}>Total Recordings</div>
        </div>
      </div>

      {isPlatformAdmin && (
        <div style={styles.card}>
          <h3 style={styles.cardTitle}>Storage &amp; retention</h3>
          <p style={{ fontSize: 13, color: colors.gray700, marginBottom: 16, lineHeight: 1.5 }}>
            Configure per S3 bucket (and optionally per organization) how long recordings are kept and how long
            soft-deleted items wait before the scheduled purge removes them from S3. Objects on legal hold are never
            purged. In production, align S3 lifecycle rules with these values so buckets do not expire objects
            before the application.
          </p>
          {storageDefaults && (
            <div style={{ fontSize: 12, color: colors.gray500, marginBottom: 12 }}>
              Defaults: retention {storageDefaults.defaultRetentionDays}d · soft-delete grace{' '}
              {storageDefaults.defaultSoftDeleteGraceDays}d · purge job{' '}
              {String(storageDefaults.purgeEnabled) === 'true' ? 'on' : 'off'} (cron:{' '}
              {storageDefaults.purgeCron || '—'})
            </div>
          )}
          <form onSubmit={saveRetentionPolicy} style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: 10, alignItems: 'center' }}>
              <label style={{ fontSize: 13, color: colors.gray700 }}>S3 bucket</label>
              <input
                style={styles.input}
                value={policyBucket}
                onChange={(e) => setPolicyBucket(e.target.value)}
                placeholder="e.g. primecx-recordings"
              />
              <label style={{ fontSize: 13, color: colors.gray700 }}>Org (optional)</label>
              <select
                style={styles.select}
                value={policyOrgId}
                onChange={(e) => setPolicyOrgId(e.target.value)}
              >
                <option value="">All orgs (bucket default)</option>
                {organizations.map((o) => (
                  <option key={o.id} value={o.id}>{o.name}</option>
                ))}
              </select>
            </div>
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: 10, alignItems: 'center' }}>
              <label style={{ fontSize: 13, color: colors.gray700 }}>Max retention (days from upload)</label>
              <input
                type="number"
                min={1}
                style={styles.input}
                value={policyRetentionDays}
                onChange={(e) => setPolicyRetentionDays(Number(e.target.value))}
              />
              <label style={{ fontSize: 13, color: colors.gray700 }}>Soft-delete grace (days)</label>
              <input
                type="number"
                min={1}
                style={styles.input}
                value={policyGraceDays}
                onChange={(e) => setPolicyGraceDays(Number(e.target.value))}
              />
            </div>
            <div>
              <button type="submit" style={{ ...styles.filterBtnActive, border: 'none' }} disabled={savingPolicy}>
                {savingPolicy ? 'Saving…' : 'Save policy'}
              </button>
            </div>
          </form>
          <div style={{ marginTop: 20, borderTop: `1px solid ${colors.gray200}`, paddingTop: 16 }}>
            <div style={{ fontSize: 13, fontWeight: 600, marginBottom: 8 }}>Organizations</div>
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8, alignItems: 'center' }}>
              <input
                style={styles.input}
                value={newOrgName}
                onChange={(e) => setNewOrgName(e.target.value)}
                placeholder="New organization name"
              />
              <button type="button" style={styles.smallBtn} onClick={handleCreateOrg}>
                Add org
              </button>
            </div>
            {organizations.length > 0 && (
              <ul style={{ margin: '8px 0 0 0', paddingLeft: 18, color: colors.gray700, fontSize: 13 }}>
                {organizations.map((o) => (
                  <li key={o.id}>{o.name} (id {o.id})</li>
                ))}
              </ul>
            )}
          </div>
        </div>
      )}

      {isPlatformAdmin && (
        <div style={styles.card}>
          <h3 style={styles.cardTitle}>Canned Responses</h3>
          <p style={{ fontSize: 13, color: colors.gray700, marginBottom: 16, lineHeight: 1.5 }}>
            Create shortcodes (e.g. <code style={{ background: colors.gray100, padding: '1px 5px', borderRadius: 4 }}>/refund-policy</code>) that
            agents can insert into session notes for consistent, pre-approved responses.
          </p>

          <form onSubmit={handleSaveCannedResponse} style={{ display: 'flex', flexDirection: 'column', gap: 10, marginBottom: 20 }}>
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: 10, alignItems: 'center' }}>
              <input
                style={{ ...styles.input, width: 160 }}
                value={crShortcode}
                onChange={(e) => setCrShortcode(e.target.value.replace(/[^a-z0-9-]/g, ''))}
                placeholder="shortcode (e.g. refund-policy)"
                required
              />
              <input
                style={{ ...styles.input, flex: 1, minWidth: 160 }}
                value={crTitle}
                onChange={(e) => setCrTitle(e.target.value)}
                placeholder="Title (e.g. Refund Policy Statement)"
                required
              />
              <input
                style={{ ...styles.input, width: 140 }}
                value={crCategory}
                onChange={(e) => setCrCategory(e.target.value)}
                placeholder="Category (optional)"
              />
            </div>
            <textarea
              style={styles.textarea}
              value={crContent}
              onChange={(e) => setCrContent(e.target.value)}
              placeholder="Response content..."
              required
              rows={3}
            />
            <div style={{ display: 'flex', gap: 8 }}>
              <button type="submit" style={{ ...styles.filterBtnActive, border: 'none' }} disabled={crSaving}>
                {crSaving ? 'Saving…' : editingCrId ? 'Update' : 'Create'}
              </button>
              {editingCrId && (
                <button type="button" style={styles.smallBtn} onClick={resetCrForm}>Cancel</button>
              )}
            </div>
          </form>

          {cannedResponses.length === 0 ? (
            <div style={styles.empty}>No canned responses yet</div>
          ) : (
            <table style={styles.table}>
              <thead>
                <tr>
                  <th style={styles.th}>Shortcode</th>
                  <th style={styles.th}>Title</th>
                  <th style={styles.th}>Category</th>
                  <th style={styles.th}>Updated</th>
                  <th style={styles.th}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {cannedResponses.map((cr) => (
                  <tr key={cr.id}>
                    <td style={styles.td}>
                      <code style={{ background: colors.gray100, padding: '2px 6px', borderRadius: 4, fontSize: 13 }}>
                        /{cr.shortcode}
                      </code>
                    </td>
                    <td style={styles.td}>{cr.title}</td>
                    <td style={styles.td}>
                      {cr.category ? (
                        <span style={{ ...styles.badge, background: `${colors.primary}18`, color: colors.primary }}>
                          {cr.category}
                        </span>
                      ) : '—'}
                    </td>
                    <td style={styles.td}>{cr.updatedAt ? new Date(cr.updatedAt).toLocaleDateString() : '—'}</td>
                    <td style={styles.td}>
                      <div style={{ display: 'flex', gap: 6 }}>
                        <button type="button" style={styles.smallBtn} onClick={() => handleEditCannedResponse(cr)}>
                          Edit
                        </button>
                        <button
                          type="button"
                          style={{ ...styles.smallBtn, color: colors.danger, borderColor: `${colors.danger}55` }}
                          onClick={() => handleDeleteCannedResponse(cr.id)}
                        >
                          Delete
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}

      <div style={styles.card}>
        <h3 style={styles.cardTitle}>User Management</h3>
        {users.length === 0 ? (
          <div style={styles.empty}>No users found</div>
        ) : (
          <table style={styles.table}>
            <thead>
              <tr>
                <th style={styles.th}>ID</th>
                <th style={styles.th}>Name</th>
                <th style={styles.th}>Email</th>
                <th style={styles.th}>Role</th>
                {isPlatformAdmin && <th style={styles.th}>Organization</th>}
                <th style={styles.th}>Status</th>
              </tr>
            </thead>
            <tbody>
              {users.map((u) => (
                <tr key={u.id}>
                  <td style={styles.td}>#{u.id}</td>
                  <td style={styles.td}>{u.name || '—'}</td>
                  <td style={styles.td}>{u.email || '—'}</td>
                  <td style={styles.td}>
                    <select
                      style={styles.select}
                      value={u.role}
                      onChange={(e) => handleRoleChange(u.id, e.target.value)}
                    >
                      {ROLES.map((r) => (
                        <option key={r} value={r}>{r.replace(/_/g, ' ')}</option>
                      ))}
                    </select>
                  </td>
                  {isPlatformAdmin && (
                    <td style={styles.td}>
                      <select
                        style={styles.select}
                        value={u.organizationId != null ? String(u.organizationId) : ''}
                        onChange={(e) => handleAssignOrg(u.id, e.target.value)}
                      >
                        <option value="">Unassigned</option>
                        {organizations.map((o) => (
                          <option key={o.id} value={o.id}>
                            {o.name}
                          </option>
                        ))}
                      </select>
                    </td>
                  )}
                  <td style={styles.td}>
                    <button
                      style={{
                        ...styles.activeToggle,
                        background: u.active !== false ? `${colors.success}18` : `${colors.danger}18`,
                        color: u.active !== false ? colors.success : colors.danger,
                      }}
                      onClick={() => handleToggleActive(u.id, u.active !== false)}
                    >
                      {u.active !== false ? 'Active' : 'Inactive'}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      <div style={styles.card}>
        <h3 style={styles.cardTitle}>Tickets Overview</h3>
        <div style={{ ...styles.filterRow, flexWrap: 'wrap' }}>
          {STATUS_FILTERS.map((f) => (
            <button
              key={f}
              type="button"
              style={{
                ...styles.filterBtn,
                ...(statusFilter === f ? styles.filterBtnActive : {}),
              }}
              onClick={() => setStatusFilter(f)}
            >
              {f === 'ALL' ? 'All' : f.replace('_', ' ')}
            </button>
          ))}
          <label style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 13, color: colors.gray700, marginLeft: 8 }}>
            <input
              type="checkbox"
              checked={overdueOnly}
              onChange={(e) => setOverdueOnly(e.target.checked)}
            />
            Overdue only
          </label>
        </div>
        {filteredTickets.length === 0 ? (
          <div style={styles.empty}>No tickets found</div>
        ) : (
          <table style={styles.table}>
            <thead>
              <tr>
                <th style={styles.th}>ID</th>
                <th style={styles.th}>Title</th>
                <th style={styles.th}>Priority</th>
                <th style={styles.th}>Status</th>
                <th style={styles.th}>Due (SLA)</th>
                <th style={styles.th}>Created</th>
                <th style={styles.th}>Last touch</th>
              </tr>
            </thead>
            <tbody>
              {filteredTickets.slice(0, 20).map((t) => (
                <tr key={t.id}>
                  <td style={styles.td}>#{t.id}</td>
                  <td style={styles.td}>{t.title}</td>
                  <td style={styles.td}>
                    <span style={{
                      ...styles.badge,
                      background: t.priority === 'CRITICAL' ? `${colors.danger}18` : t.priority === 'HIGH' ? '#F9731618' : `${colors.warning}18`,
                      color: t.priority === 'CRITICAL' ? colors.danger : t.priority === 'HIGH' ? '#F97316' : colors.warning,
                    }}>
                      {t.priority}
                    </span>
                  </td>
                  <td style={styles.td}>
                    <span style={{
                      ...styles.badge,
                      background: `${statusColors[t.status] || colors.gray500}18`,
                      color: statusColors[t.status] || colors.gray500,
                    }}>
                      {t.status?.replace('_', ' ')}
                    </span>
                  </td>
                  <td style={styles.td}>
                    {t.dueAt ? (
                      <span style={{ color: colors.gray700 }}>
                        {new Date(t.dueAt).toLocaleString()}
                        {['RESOLVED', 'CLOSED'].includes(t.status) ? '' : (new Date(t.dueAt) < new Date() ? (
                          <span style={{ color: colors.danger, fontWeight: 600, marginLeft: 6 }}>Overdue</span>
                        ) : null)}
                      </span>
                    ) : '—'}
                  </td>
                  <td style={styles.td}>{t.createdAt ? new Date(t.createdAt).toLocaleDateString() : '—'}</td>
                  <td style={{ ...styles.td, fontSize: 12, color: colors.gray700 }}>
                    {formatTicketLastTouch(t)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      <div style={styles.card}>
        <h3 style={styles.cardTitle}>Recordings Overview</h3>
        {recordings.length === 0 ? (
          <div style={styles.empty}>No recordings available</div>
        ) : (
          recordings.map((r) => (
            <div key={r.id} style={styles.row}>
              <div>
                <div style={{ fontWeight: 600, fontSize: 14 }}>
                  Recording #{r.id}
                  {r.legalHold && (
                    <span style={{ ...styles.badge, marginLeft: 8, background: `${colors.warning}22`, color: colors.warning }}>
                      Hold
                    </span>
                  )}
                  {r.deletedAt && (
                    <span style={{ ...styles.badge, marginLeft: 8, background: `${colors.danger}22`, color: colors.danger }}>
                      Soft-deleted
                    </span>
                  )}
                </div>
                <div style={{ fontSize: 12, color: colors.gray500, marginTop: 2 }}>
                  Session #{r.sessionId}
                  {r.fileName ? ` · ${r.fileName}` : ''}
                </div>
                {r.retentionPolicySummary && (
                  <div style={{ fontSize: 11, color: colors.gray500, marginTop: 4 }}>
                    {r.retentionPolicySummary}
                    {r.s3Bucket ? ` · ${r.s3Bucket}` : ''}
                  </div>
                )}
              </div>
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8, alignItems: 'center' }}>
                {isPlatformAdmin && (
                  <>
                    <button
                      type="button"
                      onClick={() => toggleHold(r.id, !r.legalHold)}
                      disabled={recBusyId === r.id}
                      style={styles.smallBtn}
                    >
                      {r.legalHold ? 'Remove hold' : 'Legal hold'}
                    </button>
                    <button
                      type="button"
                      onClick={() => doSoftDelete(r.id)}
                      disabled={recBusyId === r.id || r.deletedAt}
                      style={{ ...styles.smallBtn, color: colors.danger, borderColor: `${colors.danger}55` }}
                    >
                      Soft delete
                    </button>
                  </>
                )}
                <button
                  type="button"
                  onClick={() => openRecordingDownload(r.id)}
                  disabled={openingRecordingId === r.id || (r.deletedAt && !isPlatformAdmin)}
                  style={{
                    ...styles.playBtn,
                    ...(openingRecordingId === r.id || (r.deletedAt && !isPlatformAdmin) ? styles.playBtnDisabled : {}),
                  }}
                >
                  {openingRecordingId === r.id ? 'Opening…' : '▶ Download / play'}
                </button>
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
}
