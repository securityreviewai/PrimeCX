import React, { useState, useEffect, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { getTickets, createTicket, getTicketStats, exportTicketsCsv } from '../services/api';

const colors = {
  primary: '#4F46E5',
  success: '#10B981',
  warning: '#F59E0B',
  danger: '#EF4444',
  gray100: '#F3F4F6',
  gray200: '#E5E7EB',
  gray500: '#6B7280',
  gray700: '#374151',
  gray900: '#111827',
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
  heading: { fontSize: 24, fontWeight: 700, color: colors.gray900, marginBottom: 24 },
  grid: { display: 'grid', gridTemplateColumns: '1fr 380px', gap: 24, alignItems: 'start' },
  card: { background: '#fff', borderRadius: 12, padding: 24, boxShadow: '0 1px 4px rgba(0,0,0,0.06)' },
  ticketRow: {
    display: 'flex', alignItems: 'center', justifyContent: 'space-between',
    padding: '14px 16px', borderRadius: 8, marginBottom: 8, cursor: 'pointer',
    border: `1px solid ${colors.gray200}`, transition: 'box-shadow 0.15s',
  },
  badge: {
    display: 'inline-block', padding: '3px 10px', borderRadius: 20, fontSize: 12,
    fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.5px',
  },
  input: {
    width: '100%', padding: '10px 14px', borderRadius: 8, border: `1px solid ${colors.gray200}`,
    fontSize: 14, outline: 'none', marginBottom: 12,
  },
  textarea: {
    width: '100%', padding: '10px 14px', borderRadius: 8, border: `1px solid ${colors.gray200}`,
    fontSize: 14, outline: 'none', marginBottom: 12, minHeight: 100, resize: 'vertical',
    fontFamily: 'inherit',
  },
  select: {
    width: '100%', padding: '10px 14px', borderRadius: 8, border: `1px solid ${colors.gray200}`,
    fontSize: 14, outline: 'none', marginBottom: 16, background: '#fff',
  },
  submitBtn: {
    background: colors.primary, color: '#fff', border: 'none', padding: '12px 24px',
    borderRadius: 8, fontSize: 14, fontWeight: 600, cursor: 'pointer', width: '100%',
  },
  label: { fontSize: 13, fontWeight: 600, color: colors.gray700, marginBottom: 6, display: 'block' },
  empty: { color: colors.gray500, fontSize: 14, textAlign: 'center', padding: 40 },
  toolbar: {
    display: 'flex', flexWrap: 'wrap', gap: 12, marginBottom: 20, alignItems: 'center',
  },
  searchInput: {
    flex: '1 1 200px', minWidth: 180, padding: '10px 14px', borderRadius: 8,
    border: `1px solid ${colors.gray200}`, fontSize: 14, outline: 'none',
  },
  filterSelect: {
    padding: '10px 14px', borderRadius: 8, border: `1px solid ${colors.gray200}`,
    fontSize: 14, outline: 'none', background: '#fff', minWidth: 140,
  },
  statRow: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fill, minmax(140px, 1fr))',
    gap: 12,
    marginBottom: 20,
  },
  statCard: {
    background: colors.gray100,
    borderRadius: 10,
    padding: '14px 16px',
    border: `1px solid ${colors.gray200}`,
  },
  statValue: { fontSize: 22, fontWeight: 700, color: colors.gray900, lineHeight: 1.2 },
  statLabel: { fontSize: 12, fontWeight: 600, color: colors.gray500, marginTop: 4, textTransform: 'uppercase', letterSpacing: '0.4px' },
  bannerSuccess: {
    background: `${colors.success}18`,
    color: '#047857',
    padding: '12px 16px',
    borderRadius: 8,
    marginBottom: 16,
    fontSize: 14,
    fontWeight: 500,
    border: `1px solid ${colors.success}40`,
  },
};

export default function UserPortal({ user }) {
  const navigate = useNavigate();
  const [tickets, setTickets] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [createError, setCreateError] = useState(null);
  const [form, setForm] = useState({ title: '', description: '', priority: 'MEDIUM', category: 'GENERAL_INQUIRY' });
  const [submitting, setSubmitting] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [filterStatus, setFilterStatus] = useState('all');
  const [filterPriority, setFilterPriority] = useState('all');
  const [filterCategory, setFilterCategory] = useState('all');
  const [filterTag, setFilterTag] = useState('');
  const [sortBy, setSortBy] = useState('newest');
  const [createSuccess, setCreateSuccess] = useState(false);
  const [serverStats, setServerStats] = useState(null);
  const [exporting, setExporting] = useState(false);

  const fetchTickets = async () => {
    try {
      setLoading(true);
      setError(null);
      const [listRes, statsRes] = await Promise.all([
        getTickets(),
        getTicketStats().catch(() => null),
      ]);
      setTickets(listRes.data);
      if (statsRes?.data) setServerStats(statsRes.data);
      else setServerStats(null);
    } catch (err) {
      setError('Failed to load tickets');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchTickets(); }, []);

  useEffect(() => {
    if (!createSuccess) return undefined;
    const t = setTimeout(() => setCreateSuccess(false), 4500);
    return () => clearTimeout(t);
  }, [createSuccess]);

  const stats = useMemo(() => {
    if (serverStats && typeof serverStats.total === 'number') {
      const by = serverStats.byStatus || {};
      return {
        total: serverStats.total,
        active: serverStats.activeCount ?? 0,
        resolved: by.RESOLVED ?? 0,
        closed: by.CLOSED ?? 0,
        criticalOpen: tickets.filter(
          (t) =>
            (t.status === 'OPEN' || t.status === 'IN_PROGRESS') && t.priority === 'CRITICAL'
        ).length,
      };
    }
    const list = tickets;
    const active = list.filter((t) => t.status === 'OPEN' || t.status === 'IN_PROGRESS').length;
    const resolved = list.filter((t) => t.status === 'RESOLVED').length;
    const closed = list.filter((t) => t.status === 'CLOSED').length;
    const criticalOpen = list.filter(
      (t) => (t.status === 'OPEN' || t.status === 'IN_PROGRESS') && t.priority === 'CRITICAL'
    ).length;
    return { total: list.length, active, resolved, closed, criticalOpen };
  }, [serverStats, tickets]);

  const topCategories = useMemo(() => {
    const bc = serverStats?.byCategory;
    if (!bc || typeof bc !== 'object') return [];
    return Object.entries(bc)
      .filter(([, n]) => (Number(n) || 0) > 0)
      .sort((a, b) => (Number(b[1]) || 0) - (Number(a[1]) || 0))
      .slice(0, 5);
  }, [serverStats]);

  const filteredTickets = useMemo(() => {
    const q = searchQuery.trim().toLowerCase();
    let list = tickets.filter((t) => {
      const matchesText =
        !q ||
        t.title?.toLowerCase().includes(q) ||
        String(t.id).includes(q) ||
        (t.description && t.description.toLowerCase().includes(q));
      const matchesStatus = filterStatus === 'all' || t.status === filterStatus;
      const matchesPriority = filterPriority === 'all' || t.priority === filterPriority;
      const matchesCategory = filterCategory === 'all' || t.category === filterCategory;
      const tagNeedle = filterTag.trim().toLowerCase().replace(/[^a-z0-9-]/g, '');
      const matchesTag =
        !tagNeedle
        || (Array.isArray(t.tags) && t.tags.some((tg) => String(tg).toLowerCase().includes(tagNeedle)));
      return matchesText && matchesStatus && matchesPriority && matchesCategory && matchesTag;
    });
    list = [...list].sort((a, b) => {
      const ta = new Date(a.createdAt).getTime();
      const tb = new Date(b.createdAt).getTime();
      return sortBy === 'newest' ? tb - ta : ta - tb;
    });
    return list;
  }, [tickets, searchQuery, filterStatus, filterPriority, filterCategory, filterTag, sortBy]);

  const displayName = [user?.firstName, user?.lastName].filter(Boolean).join(' ') || user?.email || 'there';

  const handleExportCsv = async () => {
    try {
      setExporting(true);
      const res = await exportTicketsCsv();
      const blob = res.data instanceof Blob ? res.data : new Blob([res.data], { type: 'text/csv' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = 'primecx-tickets.csv';
      document.body.appendChild(a);
      a.click();
      a.remove();
      URL.revokeObjectURL(url);
      if (String(res.headers['x-export-truncated']).toLowerCase() === 'true') {
        window.alert(
          'Export completed. Rows were capped at 5,000; use filters on the backend or contact an admin if you need the full dataset.'
        );
      }
    } catch {
      window.alert('Could not export tickets. Ensure you still have access and try again.');
    } finally {
      setExporting(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.title.trim()) return;
    try {
      setSubmitting(true);
      setCreateError(null);
      await createTicket(form);
      setForm({ title: '', description: '', priority: 'MEDIUM', category: 'GENERAL_INQUIRY' });
      setCreateSuccess(true);
      fetchTickets();
    } catch {
      setCreateError('Failed to create ticket');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div>
      <h1 style={styles.heading}>
        My Tickets
        <span style={{ display: 'block', fontSize: 15, fontWeight: 500, color: colors.gray500, marginTop: 8 }}>
          Hi {displayName} — track requests and open new ones anytime.
        </span>
      </h1>
      {createSuccess && (
        <div style={styles.bannerSuccess} role="status">
          Ticket created successfully. It appears in your list below.
        </div>
      )}
      <div style={styles.grid}>
        <div style={styles.card}>
          {!loading && !error && tickets.length > 0 && (
            <>
              <div style={styles.statRow}>
                <div style={styles.statCard}>
                  <div style={styles.statValue}>{stats.total}</div>
                  <div style={styles.statLabel}>Total</div>
                </div>
                <div style={styles.statCard}>
                  <div style={{ ...styles.statValue, color: colors.primary }}>{stats.active}</div>
                  <div style={styles.statLabel}>Active</div>
                </div>
                <div style={styles.statCard}>
                  <div style={{ ...styles.statValue, color: colors.success }}>{stats.resolved}</div>
                  <div style={styles.statLabel}>Resolved</div>
                </div>
                <div style={styles.statCard}>
                  <div style={{ ...styles.statValue, color: colors.gray500 }}>{stats.closed}</div>
                  <div style={styles.statLabel}>Closed</div>
                </div>
                {stats.criticalOpen > 0 && (
                  <div style={{ ...styles.statCard, borderColor: `${colors.danger}55`, background: `${colors.danger}10` }}>
                    <div style={{ ...styles.statValue, color: colors.danger }}>{stats.criticalOpen}</div>
                    <div style={styles.statLabel}>Critical (open)</div>
                  </div>
                )}
              </div>
              {topCategories.length > 0 && (
                <div style={{ marginBottom: 16, padding: '12px 14px', borderRadius: 10, background: colors.gray100, border: `1px solid ${colors.gray200}` }}>
                  <div style={{ fontSize: 11, fontWeight: 700, color: colors.gray500, textTransform: 'uppercase', letterSpacing: '0.5px', marginBottom: 8 }}>
                    Tickets by category
                  </div>
                  <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
                    {topCategories.map(([cat, count]) => (
                      <span
                        key={cat}
                        style={{
                          fontSize: 12,
                          fontWeight: 600,
                          color: colors.gray700,
                          background: '#fff',
                          padding: '6px 10px',
                          borderRadius: 8,
                          border: `1px solid ${colors.gray200}`,
                        }}
                      >
                        {formatCategoryLabel(cat)} · {count}
                      </span>
                    ))}
                  </div>
                </div>
              )}
              <div style={styles.toolbar}>
                <input
                  type="search"
                  aria-label="Search tickets"
                  placeholder="Search by title, ID, or description..."
                  style={styles.searchInput}
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                />
                <select
                  style={styles.filterSelect}
                  value={filterStatus}
                  onChange={(e) => setFilterStatus(e.target.value)}
                  aria-label="Filter by status"
                >
                  <option value="all">All statuses</option>
                  <option value="OPEN">Open</option>
                  <option value="IN_PROGRESS">In progress</option>
                  <option value="RESOLVED">Resolved</option>
                  <option value="CLOSED">Closed</option>
                </select>
                <select
                  style={styles.filterSelect}
                  value={filterPriority}
                  onChange={(e) => setFilterPriority(e.target.value)}
                  aria-label="Filter by priority"
                >
                  <option value="all">All priorities</option>
                  <option value="LOW">Low</option>
                  <option value="MEDIUM">Medium</option>
                  <option value="HIGH">High</option>
                  <option value="CRITICAL">Critical</option>
                </select>
                <select
                  style={styles.filterSelect}
                  value={filterCategory}
                  onChange={(e) => setFilterCategory(e.target.value)}
                  aria-label="Filter by category"
                >
                  <option value="all">All categories</option>
                  {TICKET_CATEGORY_OPTIONS.map((o) => (
                    <option key={o.value} value={o.value}>{o.label}</option>
                  ))}
                </select>
                <input
                  type="search"
                  aria-label="Filter by tag"
                  placeholder="Tag contains…"
                  style={{ ...styles.searchInput, flex: '0 1 160px', minWidth: 140 }}
                  value={filterTag}
                  onChange={(e) => setFilterTag(e.target.value)}
                />
                <select
                  style={styles.filterSelect}
                  value={sortBy}
                  onChange={(e) => setSortBy(e.target.value)}
                  aria-label="Sort tickets"
                >
                  <option value="newest">Newest first</option>
                  <option value="oldest">Oldest first</option>
                </select>
                <button
                  type="button"
                  onClick={handleExportCsv}
                  disabled={exporting}
                  aria-label="Download tickets as CSV"
                  style={{
                    padding: '10px 18px',
                    borderRadius: 8,
                    border: `1px solid ${colors.gray200}`,
                    background: '#fff',
                    fontSize: 14,
                    fontWeight: 600,
                    cursor: exporting ? 'not-allowed' : 'pointer',
                    color: colors.primary,
                  }}
                >
                  {exporting ? 'Exporting…' : 'Export CSV'}
                </button>
              </div>
            </>
          )}
          {loading ? (
            <div style={styles.empty}>Loading tickets...</div>
          ) : error ? (
            <div style={{ ...styles.empty, color: colors.danger }}>
              {error}
              <button
                type="button"
                onClick={fetchTickets}
                style={{
                  display: 'block', margin: '16px auto 0', padding: '10px 20px',
                  background: colors.primary, color: '#fff', border: 'none', borderRadius: 8,
                  fontWeight: 600, cursor: 'pointer', fontSize: 14,
                }}
              >
                Try again
              </button>
            </div>
          ) : tickets.length === 0 ? (
            <div style={styles.empty}>No tickets yet. Create your first ticket!</div>
          ) : filteredTickets.length === 0 ? (
            <div style={styles.empty}>
              No tickets match your filters.
              <button
                type="button"
                onClick={() => {
                  setSearchQuery('');
                  setFilterStatus('all');
                  setFilterPriority('all');
                  setFilterCategory('all');
                  setFilterTag('');
                }}
                style={{
                  display: 'block', margin: '16px auto 0', background: 'none', border: 'none',
                  color: colors.primary, fontWeight: 600, cursor: 'pointer', fontSize: 14,
                }}
              >
                Clear filters
              </button>
            </div>
          ) : (
            filteredTickets.map((t) => (
              <div
                key={t.id}
                style={styles.ticketRow}
                onClick={() => navigate(`/tickets/${t.id}`)}
                onMouseEnter={(e) => (e.currentTarget.style.boxShadow = '0 2px 8px rgba(0,0,0,0.08)')}
                onMouseLeave={(e) => (e.currentTarget.style.boxShadow = 'none')}
              >
                <div>
                  <div style={{ fontWeight: 600, fontSize: 15, color: colors.gray900 }}>{t.title}</div>
                  <div style={{ fontSize: 13, color: colors.gray500, marginTop: 4 }}>
                    #{t.id} &middot; {new Date(t.createdAt).toLocaleDateString()}
                  </div>
                  {Array.isArray(t.tags) && t.tags.length > 0 && (
                    <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6, marginTop: 8 }}>
                      {t.tags.slice(0, 6).map((tg) => (
                        <span
                          key={tg}
                          style={{
                            fontSize: 11,
                            fontWeight: 600,
                            color: colors.gray700,
                            background: colors.gray100,
                            padding: '2px 8px',
                            borderRadius: 6,
                            border: `1px solid ${colors.gray200}`,
                            fontFamily: 'ui-monospace, monospace',
                          }}
                        >
                          {tg}
                        </span>
                      ))}
                      {t.tags.length > 6 && (
                        <span style={{ fontSize: 11, color: colors.gray500 }}>+{t.tags.length - 6}</span>
                      )}
                    </div>
                  )}
                </div>
                <div style={{ display: 'flex', gap: 8 }}>
                  <span style={{
                    ...styles.badge,
                    background: `${statusColors[t.status] || colors.gray500}18`,
                    color: statusColors[t.status] || colors.gray500,
                  }}>
                    {t.status?.replace('_', ' ')}
                  </span>
                  <span style={{
                    ...styles.badge,
                    background: `${priorityColors[t.priority] || colors.gray500}18`,
                    color: priorityColors[t.priority] || colors.gray500,
                  }}>
                    {t.priority}
                  </span>
                  {t.category && (
                    <span style={{
                      ...styles.badge,
                      background: `${colors.gray200}`,
                      color: colors.gray700,
                      textTransform: 'none',
                      fontWeight: 500,
                    }}>
                      {formatCategoryLabel(t.category)}
                    </span>
                  )}
                </div>
              </div>
            ))
          )}
        </div>

        <div style={styles.card}>
          <h3 style={{ fontSize: 16, fontWeight: 700, color: colors.gray900, marginBottom: 20 }}>
            Create New Ticket
          </h3>
          {createError && (
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
              {createError}
            </div>
          )}
          <form onSubmit={handleSubmit}>
            <label style={styles.label}>Title</label>
            <input
              style={styles.input}
              placeholder="Brief summary of your issue"
              value={form.title}
              onChange={(e) => setForm({ ...form, title: e.target.value })}
            />
            <label style={styles.label}>Description</label>
            <textarea
              style={styles.textarea}
              placeholder="Provide details about your issue..."
              value={form.description}
              onChange={(e) => setForm({ ...form, description: e.target.value })}
            />
            <label style={styles.label}>Priority</label>
            <select
              style={styles.select}
              value={form.priority}
              onChange={(e) => setForm({ ...form, priority: e.target.value })}
            >
              <option value="LOW">Low</option>
              <option value="MEDIUM">Medium</option>
              <option value="HIGH">High</option>
              <option value="CRITICAL">Critical</option>
            </select>
            <label style={styles.label}>Category</label>
            <select
              style={styles.select}
              value={form.category}
              onChange={(e) => setForm({ ...form, category: e.target.value })}
            >
              {TICKET_CATEGORY_OPTIONS.map((o) => (
                <option key={o.value} value={o.value}>{o.label}</option>
              ))}
            </select>
            <button type="submit" style={styles.submitBtn} disabled={submitting}>
              {submitting ? 'Creating...' : 'Create Ticket'}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}
