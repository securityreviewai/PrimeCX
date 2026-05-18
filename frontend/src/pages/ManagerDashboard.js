import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { getDashboard, getSessions, getRecordingsBySession, getSlaBreachedTickets } from '../services/api';

const colors = {
  primary: '#4F46E5', success: '#10B981', warning: '#F59E0B',
  danger: '#EF4444', gray100: '#F3F4F6', gray200: '#E5E7EB',
  gray500: '#6B7280', gray700: '#374151', gray900: '#111827',
};

const styles = {
  heading: { fontSize: 24, fontWeight: 700, color: colors.gray900, marginBottom: 24 },
  statsGrid: { display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 16, marginBottom: 32 },
  statCard: {
    background: '#fff', borderRadius: 12, padding: '20px 24px',
    boxShadow: '0 1px 4px rgba(0,0,0,0.06)',
  },
  statValue: { fontSize: 28, fontWeight: 700, marginBottom: 4 },
  statLabel: { fontSize: 13, color: colors.gray500, fontWeight: 500 },
  card: { background: '#fff', borderRadius: 12, padding: 24, boxShadow: '0 1px 4px rgba(0,0,0,0.06)', marginBottom: 24 },
  cardTitle: { fontSize: 16, fontWeight: 700, color: colors.gray900, marginBottom: 16 },
  filterRow: { display: 'flex', gap: 12, marginBottom: 20, alignItems: 'center' },
  select: {
    padding: '8px 14px', borderRadius: 8, border: `1px solid ${colors.gray200}`,
    fontSize: 14, outline: 'none', background: '#fff',
  },
  row: {
    display: 'flex', justifyContent: 'space-between', alignItems: 'center',
    padding: '12px 16px', borderRadius: 8, border: `1px solid ${colors.gray200}`, marginBottom: 8,
  },
  badge: {
    display: 'inline-block', padding: '3px 10px', borderRadius: 20, fontSize: 12,
    fontWeight: 600, textTransform: 'uppercase',
  },
  empty: { color: colors.gray500, fontSize: 14, textAlign: 'center', padding: 32 },
  table: { width: '100%', borderCollapse: 'collapse' },
  th: {
    textAlign: 'left', padding: '10px 14px', fontSize: 12, fontWeight: 600,
    color: colors.gray500, textTransform: 'uppercase', letterSpacing: '0.5px',
    borderBottom: `2px solid ${colors.gray200}`,
  },
  td: { padding: '12px 14px', fontSize: 14, color: colors.gray700, borderBottom: `1px solid ${colors.gray100}` },
};

export default function ManagerDashboard({ user }) {
  const [stats, setStats] = useState(null);
  const [sessions, setSessions] = useState([]);
  const [allRecordings, setAllRecordings] = useState([]);
  const [executiveFilter, setExecutiveFilter] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [slaBreached, setSlaBreached] = useState(null);

  useEffect(() => {
    const fetchData = async () => {
      try {
        setLoading(true);
        const [dashRes, sessionsRes, slaRes] = await Promise.all([
          getDashboard(),
          getSessions(),
          getSlaBreachedTickets().catch(() => ({ data: { content: [], totalElements: 0 } })),
        ]);
        setStats(dashRes.data);
        setSessions(sessionsRes.data);
        setSlaBreached(slaRes.data);

        const recs = [];
        for (const s of sessionsRes.data.slice(0, 20)) {
          try {
            const r = await getRecordingsBySession(s.id);
            recs.push(...r.data.map((rec) => ({ ...rec, sessionId: s.id, executiveId: s.executiveId })));
          } catch { /* skip */ }
        }
        setAllRecordings(recs);
      } catch {
        setError('Failed to load dashboard data');
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, []);

  const executives = [...new Set(sessions.map((s) => s.executiveId).filter(Boolean))];
  const filteredSessions = executiveFilter
    ? sessions.filter((s) => String(s.executiveId) === executiveFilter)
    : sessions;

  if (loading) return <div style={styles.empty}>Loading dashboard...</div>;
  if (error) return <div style={{ ...styles.empty, color: colors.danger }}>{error}</div>;

  return (
    <div>
      <h1 style={styles.heading}>Manager Dashboard</h1>

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
          <div style={{ ...styles.statValue, color: colors.success }}>{stats?.activeSessions ?? 0}</div>
          <div style={styles.statLabel}>Active Sessions</div>
        </div>
        <div style={styles.statCard}>
          <div style={{ ...styles.statValue, color: colors.danger }}>{stats?.totalRecordings ?? 0}</div>
          <div style={styles.statLabel}>Total Recordings</div>
        </div>
      </div>

      <div style={styles.card}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 16, flexWrap: 'wrap', gap: 12 }}>
          <div>
            <h3 style={{ ...styles.cardTitle, margin: 0 }}>SLA overdue (first response)</h3>
            <p style={{ fontSize: 13, color: colors.gray500, margin: '8px 0 0', maxWidth: 640, lineHeight: 1.5 }}>
              Open or in-progress tickets past the priority-based first-response deadline. Same visibility rules as search — executives only see tickets assigned to them.
            </p>
          </div>
          <div
            style={{
              ...styles.statCard,
              padding: '12px 18px',
              minWidth: 100,
              textAlign: 'center',
              boxShadow: 'none',
              border: `1px solid ${colors.gray200}`,
            }}
          >
            <div style={{ ...styles.statValue, fontSize: 24, color: colors.danger, marginBottom: 0 }}>
              {slaBreached?.totalElements ?? 0}
            </div>
            <div style={styles.statLabel}>Breached</div>
          </div>
        </div>
        {!slaBreached?.content?.length ? (
          <div style={styles.empty}>No SLA breaches in your view.</div>
        ) : (
          <table style={styles.table}>
            <thead>
              <tr>
                <th style={styles.th}>ID</th>
                <th style={styles.th}>Title</th>
                <th style={styles.th}>Priority</th>
                <th style={styles.th}>Status</th>
                <th style={styles.th}>Respond by</th>
              </tr>
            </thead>
            <tbody>
              {slaBreached.content.map((t) => (
                <tr key={t.id}>
                  <td style={styles.td}>
                    <Link to={`/tickets/${t.id}`} style={{ color: colors.primary, fontWeight: 600, textDecoration: 'none' }}>
                      #{t.id}
                    </Link>
                  </td>
                  <td style={styles.td}>{t.title}</td>
                  <td style={styles.td}>{t.priority}</td>
                  <td style={styles.td}>{t.status?.replace('_', ' ')}</td>
                  <td style={styles.td}>
                    {t.slaRespondBy ? new Date(t.slaRespondBy).toLocaleString() : '—'}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      <div style={styles.card}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
          <h3 style={{ ...styles.cardTitle, margin: 0 }}>Recent Sessions</h3>
          <div style={styles.filterRow}>
            <span style={{ fontSize: 13, color: colors.gray500 }}>Filter by executive:</span>
            <select
              style={styles.select}
              value={executiveFilter}
              onChange={(e) => setExecutiveFilter(e.target.value)}
            >
              <option value="">All</option>
              {executives.map((eid) => (
                <option key={eid} value={String(eid)}>Executive #{eid}</option>
              ))}
            </select>
          </div>
        </div>

        {filteredSessions.length === 0 ? (
          <div style={styles.empty}>No sessions found</div>
        ) : (
          <table style={styles.table}>
            <thead>
              <tr>
                <th style={styles.th}>Session</th>
                <th style={styles.th}>Ticket</th>
                <th style={styles.th}>Executive</th>
                <th style={styles.th}>Started</th>
                <th style={styles.th}>Status</th>
              </tr>
            </thead>
            <tbody>
              {filteredSessions.slice(0, 15).map((s) => (
                <tr key={s.id}>
                  <td style={styles.td}>#{s.id}</td>
                  <td style={styles.td}>#{s.ticketId}</td>
                  <td style={styles.td}>#{s.executiveId}</td>
                  <td style={styles.td}>{s.startTime ? new Date(s.startTime).toLocaleString() : '—'}</td>
                  <td style={styles.td}>
                    <span style={{
                      ...styles.badge,
                      background: s.endTime ? `${colors.success}18` : `${colors.warning}18`,
                      color: s.endTime ? colors.success : colors.warning,
                    }}>
                      {s.endTime ? 'Completed' : 'Active'}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      <div style={styles.card}>
        <h3 style={styles.cardTitle}>Session Recordings</h3>
        {allRecordings.length === 0 ? (
          <div style={styles.empty}>No recordings available</div>
        ) : (
          allRecordings.map((r) => (
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
                style={{
                  color: colors.primary, fontSize: 13, fontWeight: 600,
                  textDecoration: 'none',
                }}
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
