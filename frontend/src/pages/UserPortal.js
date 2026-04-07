import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { getTickets, createTicket } from '../services/api';

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
};

export default function UserPortal({ user }) {
  const navigate = useNavigate();
  const [tickets, setTickets] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [form, setForm] = useState({ title: '', description: '', priority: 'MEDIUM' });
  const [submitting, setSubmitting] = useState(false);

  const fetchTickets = async () => {
    try {
      setLoading(true);
      const res = await getTickets();
      setTickets(res.data);
    } catch (err) {
      setError('Failed to load tickets');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchTickets(); }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.title.trim()) return;
    try {
      setSubmitting(true);
      await createTicket(form);
      setForm({ title: '', description: '', priority: 'MEDIUM' });
      fetchTickets();
    } catch {
      setError('Failed to create ticket');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div>
      <h1 style={styles.heading}>My Tickets</h1>
      <div style={styles.grid}>
        <div style={styles.card}>
          {loading ? (
            <div style={styles.empty}>Loading tickets...</div>
          ) : error ? (
            <div style={{ ...styles.empty, color: colors.danger }}>{error}</div>
          ) : tickets.length === 0 ? (
            <div style={styles.empty}>No tickets yet. Create your first ticket!</div>
          ) : (
            tickets.map((t) => (
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
                </div>
              </div>
            ))
          )}
        </div>

        <div style={styles.card}>
          <h3 style={{ fontSize: 16, fontWeight: 700, color: colors.gray900, marginBottom: 20 }}>
            Create New Ticket
          </h3>
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
            <button type="submit" style={styles.submitBtn} disabled={submitting}>
              {submitting ? 'Creating...' : 'Create Ticket'}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}
