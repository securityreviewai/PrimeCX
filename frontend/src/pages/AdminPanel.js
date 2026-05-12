import React, { useState, useEffect } from 'react';
import { getDashboard, getUsers, updateUserRole, getTickets, getSessions, getRecordingsBySession } from '../services/api';

const colors = {
  primary: '#4F46E5', success: '#10B981', warning: '#F59E0B',
  danger: '#EF4444', gray100: '#F3F4F6', gray200: '#E5E7EB',
  gray500: '#6B7280', gray700: '#374151', gray900: '#111827',
};

const statusColors = { OPEN: colors.primary, IN_PROGRESS: colors.warning, RESOLVED: colors.success, CLOSED: colors.gray500 };

const CATEGORY_LABELS = {
  GENERAL: 'General',
  BILLING: 'Billing',
  TECHNICAL: 'Technical',
  ACCOUNT: 'Account',
  PRODUCT_FEEDBACK: 'Product feedback',
};

const styles = {
  heading: { fontSize: 24, fontWeight: 700, color: colors.gray900, marginBottom: 24 },
  statsGrid: { display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(140px, 1fr))', gap: 16, marginBottom: 32 },
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
  empty: { color: colors.gray500, fontSize: 14, textAlign: 'center', padding: 32 },
  activeToggle: {
    padding: '4px 12px', borderRadius: 6, border: 'none', fontSize: 12,
    fontWeight: 600, cursor: 'pointer',
  },
};

const ROLES = ['user', 'support_executive', 'support_manager', 'support_admin'];
const STATUS_FILTERS = ['ALL', 'OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'];

export default function AdminPanel({ user }) {
  const [stats, setStats] = useState(null);
  const [users, setUsers] = useState([]);
  const [tickets, setTickets] = useState([]);
  const [recordings, setRecordings] = useState([]);
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchData = async () => {
      try {
        setLoading(true);
        const [dashRes, usersRes, ticketsRes, sessionsRes] = await Promise.all([
          getDashboard(), getUsers(), getTickets(), getSessions(),
        ]);
        setStats(dashRes.data);
        setUsers(usersRes.data);
        setTickets(ticketsRes.data);

        const recs = [];
        for (const s of sessionsRes.data.slice(0, 20)) {
          try {
            const r = await getRecordingsBySession(s.id);
            recs.push(...r.data.map((rec) => ({ ...rec, sessionId: s.id })));
          } catch { /* skip */ }
        }
        setRecordings(recs);
      } catch {
        setError('Failed to load admin data');
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, []);

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

  const filteredTickets = statusFilter === 'ALL'
    ? tickets
    : tickets.filter((t) => t.status === statusFilter);

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
          <div style={{ ...styles.statValue, color: colors.danger }}>{stats?.openEscalatedTickets ?? 0}</div>
          <div style={styles.statLabel}>Open · Escalated</div>
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
        <div style={styles.filterRow}>
          {STATUS_FILTERS.map((f) => (
            <button
              key={f}
              style={{
                ...styles.filterBtn,
                ...(statusFilter === f ? styles.filterBtnActive : {}),
              }}
              onClick={() => setStatusFilter(f)}
            >
              {f === 'ALL' ? 'All' : f.replace('_', ' ')}
            </button>
          ))}
        </div>
        {filteredTickets.length === 0 ? (
          <div style={styles.empty}>No tickets found</div>
        ) : (
          <table style={styles.table}>
            <thead>
              <tr>
                <th style={styles.th}>ID</th>
                <th style={styles.th}>Title</th>
                <th style={styles.th}>Category</th>
                <th style={styles.th}>Priority</th>
                <th style={styles.th}>Status</th>
                <th style={styles.th}>Escalated</th>
                <th style={styles.th}>Created</th>
              </tr>
            </thead>
            <tbody>
              {filteredTickets.slice(0, 20).map((t) => (
                <tr key={t.id}>
                  <td style={styles.td}>#{t.id}</td>
                  <td style={styles.td}>{t.title}</td>
                  <td style={styles.td}>
                    <span style={{ fontSize: 13, color: colors.gray700 }}>
                      {CATEGORY_LABELS[t.category] || t.category || '—'}
                    </span>
                  </td>
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
                    {t.escalated ? (
                      <span style={{
                        ...styles.badge,
                        background: `${colors.danger}22`,
                        color: colors.danger,
                      }}>Yes</span>
                    ) : (
                      <span style={{ fontSize: 13, color: colors.gray500 }}>—</span>
                    )}
                  </td>
                  <td style={styles.td}>{t.createdAt ? new Date(t.createdAt).toLocaleDateString() : '—'}</td>
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
                <div style={{ fontWeight: 600, fontSize: 14 }}>Recording #{r.id}</div>
                <div style={{ fontSize: 12, color: colors.gray500, marginTop: 2 }}>
                  Session #{r.sessionId} &middot; {r.s3Key || 'N/A'}
                </div>
              </div>
              <a
                href={r.playbackUrl || '#'}
                target="_blank"
                rel="noopener noreferrer"
                style={{ color: colors.primary, fontSize: 13, fontWeight: 600, textDecoration: 'none' }}
              >
                &#9654; Play
              </a>
            </div>
          ))
        )}
      </div>
    </div>
  );
}
