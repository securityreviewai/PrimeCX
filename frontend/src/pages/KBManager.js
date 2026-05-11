import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { getAllKBArticles, searchKBArticles } from '../services/api';

const colors = {
  primary: '#4F46E5', success: '#10B981', warning: '#F59E0B',
  danger: '#EF4444', gray100: '#F3F4F6', gray200: '#E5E7EB',
  gray500: '#6B7280', gray700: '#374151', gray900: '#111827',
};

const styles = {
  heading: { fontSize: 24, fontWeight: 700, color: colors.gray900, marginBottom: 24 },
  card: { background: '#fff', borderRadius: 12, padding: 24, boxShadow: '0 1px 4px rgba(0,0,0,0.06)', marginBottom: 20 },
  cardTitle: { fontSize: 16, fontWeight: 700, color: colors.gray900, marginBottom: 16 },
  btn: {
    padding: '8px 16px', borderRadius: 8, border: 'none', fontSize: 13,
    fontWeight: 600, cursor: 'pointer', color: '#fff',
  },
  input: {
    width: '100%', padding: '10px 14px', borderRadius: 8, border: `1px solid ${colors.gray200}`,
    fontSize: 14, outline: 'none', marginBottom: 12,
  },
  textarea: {
    width: '100%', padding: '10px 14px', borderRadius: 8, border: `1px solid ${colors.gray200}`,
    fontSize: 14, outline: 'none', minHeight: 120, resize: 'vertical', fontFamily: 'inherit',
    marginBottom: 12,
  },
  empty: { color: colors.gray500, fontSize: 14, textAlign: 'center', padding: 40 },
};

export default function KBManager({ user }) {
  const navigate = useNavigate();
  const [articles, setArticles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [showForm, setShowForm] = useState(false);
  const [formData, setFormData] = useState({
    title: '',
    content: '',
    tags: '',
    category: 'GENERAL',
    visibility: 'INTERNAL_ONLY',
    published: false,
  });
  const [submitting, setSubmitting] = useState(false);

  const r = (user?.role || '').replace(/^ROLE_/i, '').toLowerCase();
  const canManage = r === 'support_admin' || r === 'support_manager';

  useEffect(() => {
    if (!canManage) {
      navigate('/');
      return;
    }
    loadArticles();
  }, [canManage, navigate]);

  const loadArticles = async () => {
    try {
      setLoading(true);
      const res = await getAllKBArticles();
      setArticles(res.data || []);
    } catch {
      setError('Failed to load articles');
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = async (query) => {
    try {
      const res = await searchKBArticles(query);
      setArticles(res.data || []);
    } catch {
      setError('Search failed');
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!formData.title.trim() || !formData.content.trim()) return;

    try {
      setSubmitting(true);
      console.log('Creating article:', formData);
      setFormData({
        title: '',
        content: '',
        tags: '',
        category: 'GENERAL',
        visibility: 'INTERNAL_ONLY',
        published: false,
      });
      setShowForm(false);
      loadArticles();
    } catch {
      setError('Failed to create article');
    } finally {
      setSubmitting(false);
    }
  };

  if (!canManage) return null;

  return (
    <div>
      <h1 style={styles.heading}>Knowledge Base Manager</h1>

      <div style={styles.card}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
          <h3 style={styles.cardTitle}>Articles</h3>
          <button
            style={{ ...styles.btn, background: colors.primary }}
            onClick={() => setShowForm(!showForm)}
          >
            {showForm ? 'Cancel' : 'Add Article'}
          </button>
        </div>

        <div style={{ marginBottom: 16 }}>
          <input
            type="text"
            placeholder="Search articles..."
            value={searchQuery}
            onChange={(e) => {
              setSearchQuery(e.target.value);
              handleSearch(e.target.value);
            }}
            style={styles.input}
          />
        </div>

        {showForm && (
          <form onSubmit={handleSubmit} style={{ marginBottom: 20, padding: 16, border: `1px solid ${colors.gray200}`, borderRadius: 8 }}>
            <input
              type="text"
              placeholder="Article title"
              value={formData.title}
              onChange={(e) => setFormData({ ...formData, title: e.target.value })}
              style={styles.input}
              required
            />
            <textarea
              placeholder="Article content"
              value={formData.content}
              onChange={(e) => setFormData({ ...formData, content: e.target.value })}
              style={styles.textarea}
              required
            />
            <input
              type="text"
              placeholder="Tags (comma-separated)"
              value={formData.tags}
              onChange={(e) => setFormData({ ...formData, tags: e.target.value })}
              style={styles.input}
            />
            <div style={{ display: 'flex', gap: 10, marginBottom: 12 }}>
              <select
                value={formData.category}
                onChange={(e) => setFormData({ ...formData, category: e.target.value })}
                style={{ ...styles.input, marginBottom: 0 }}
              >
                <option value="GETTING_STARTED">Getting started</option>
                <option value="ACCOUNT">Account</option>
                <option value="BILLING">Billing</option>
                <option value="TECHNICAL">Technical</option>
                <option value="TROUBLESHOOTING">Troubleshooting</option>
                <option value="GENERAL">General</option>
              </select>
              <select
                value={formData.visibility}
                onChange={(e) => setFormData({ ...formData, visibility: e.target.value })}
                style={{ ...styles.input, marginBottom: 0 }}
              >
                <option value="INTERNAL_ONLY">Internal only</option>
                <option value="PUBLIC">Customer portal</option>
              </select>
            </div>
            <label style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 13, color: colors.gray700, marginBottom: 12 }}>
              <input
                type="checkbox"
                checked={formData.published}
                onChange={(e) => setFormData({ ...formData, published: e.target.checked })}
              />
              Publish to customer portal (requires visibility = Customer portal)
            </label>
            <button
              type="submit"
              style={{ ...styles.btn, background: colors.success }}
              disabled={submitting}
            >
              {submitting ? 'Creating...' : 'Create Article'}
            </button>
          </form>
        )}

        {loading ? (
          <div style={styles.empty}>Loading...</div>
        ) : error ? (
          <div style={{ ...styles.empty, color: colors.danger }}>{error}</div>
        ) : articles.length === 0 ? (
          <div style={styles.empty}>No articles found</div>
        ) : (
          articles.map((article) => (
            <div key={article.id} style={{
              padding: 16,
              border: `1px solid ${colors.gray200}`,
              borderRadius: 8,
              marginBottom: 12,
            }}>
              <h4 style={{ margin: 0, marginBottom: 8, color: colors.gray900 }}>{article.title}</h4>
              <p style={{ margin: 0, marginBottom: 8, color: colors.gray700, fontSize: 14 }}>
                {article.content.substring(0, 200)}...
              </p>
              {article.tags && (
                <div style={{ fontSize: 12, color: colors.gray500 }}>
                  Tags: {article.tags}
                </div>
              )}
            </div>
          ))
        )}
      </div>
    </div>
  );
}