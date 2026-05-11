import React, { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  getPortalKBArticle,
  getPortalKBRelatedArticles,
} from '../services/api';

const colors = {
  primary: '#4F46E5',
  gray100: '#F3F4F6',
  gray200: '#E5E7EB',
  gray500: '#6B7280',
  gray700: '#374151',
  gray900: '#111827',
  danger: '#B91C1C',
};

const styles = {
  backBtn: {
    background: 'transparent',
    border: 'none',
    color: colors.primary,
    fontWeight: 600,
    fontSize: 13,
    cursor: 'pointer',
    marginBottom: 12,
    padding: 0,
  },
  grid: { display: 'grid', gridTemplateColumns: '1fr 300px', gap: 24, alignItems: 'start' },
  card: { background: '#fff', borderRadius: 12, padding: 24, boxShadow: '0 1px 4px rgba(0,0,0,0.06)' },
  title: { fontSize: 24, fontWeight: 700, color: colors.gray900, marginBottom: 8 },
  meta: { color: colors.gray500, fontSize: 12, marginBottom: 20 },
  body: {
    fontSize: 14,
    lineHeight: 1.6,
    color: colors.gray700,
    whiteSpace: 'pre-wrap',
    wordBreak: 'break-word',
    fontFamily: 'inherit',
    margin: 0,
  },
  tag: {
    display: 'inline-block',
    padding: '2px 10px',
    marginRight: 6,
    background: colors.gray100,
    color: colors.gray700,
    borderRadius: 12,
    fontSize: 12,
  },
  relatedTitle: { fontSize: 14, fontWeight: 700, color: colors.gray900, marginBottom: 12 },
  relatedBtn: {
    display: 'block',
    width: '100%',
    textAlign: 'left',
    padding: '10px 12px',
    borderRadius: 8,
    border: `1px solid ${colors.gray200}`,
    marginBottom: 8,
    background: '#fff',
    color: colors.gray900,
    fontSize: 13,
    fontWeight: 500,
    cursor: 'pointer',
  },
  empty: { color: colors.gray500, fontSize: 13, padding: 12 },
  error: { color: colors.danger, fontSize: 13, marginBottom: 12 },
};

const CATEGORY_LABELS = {
  GETTING_STARTED: 'Getting started',
  ACCOUNT: 'Account',
  BILLING: 'Billing',
  TECHNICAL: 'Technical',
  TROUBLESHOOTING: 'Troubleshooting',
  GENERAL: 'General',
};

export default function CustomerKBArticle() {
  const navigate = useNavigate();
  const { id } = useParams();

  const [article, setArticle] = useState(null);
  const [related, setRelated] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const parsed = Number.parseInt(id, 10);
    if (!Number.isFinite(parsed) || parsed <= 0) {
      setError('Invalid article reference');
      setLoading(false);
      return undefined;
    }

    let cancelled = false;
    (async () => {
      try {
        setLoading(true);
        setError(null);
        const res = await getPortalKBArticle(parsed);
        if (cancelled) return;
        setArticle(res.data || null);
      } catch (err) {
        if (!cancelled) {
          setArticle(null);
          if (err?.response?.status === 404) {
            setError('Article not found or not available.');
          } else {
            setError('Could not load the article.');
          }
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();

    return () => { cancelled = true; };
  }, [id]);

  useEffect(() => {
    if (!article?.id) {
      setRelated([]);
      return undefined;
    }
    let cancelled = false;
    (async () => {
      try {
        const res = await getPortalKBRelatedArticles(article.id, 5);
        if (!cancelled) setRelated(Array.isArray(res.data) ? res.data : []);
      } catch {
        if (!cancelled) setRelated([]);
      }
    })();
    return () => { cancelled = true; };
  }, [article?.id]);

  const openArticle = useCallback((articleId) => {
    navigate(`/kb/articles/${articleId}`);
  }, [navigate]);

  return (
    <div>
      <button type="button" style={styles.backBtn} onClick={() => navigate('/kb')}>
        ← Back to Help Center
      </button>

      {loading ? (
        <div style={styles.empty}>Loading article…</div>
      ) : error ? (
        <div style={styles.error}>{error}</div>
      ) : !article ? (
        <div style={styles.empty}>Article not found.</div>
      ) : (
        <div style={styles.grid}>
          <div style={styles.card}>
            <div style={styles.title}>{article.title}</div>
            <div style={styles.meta}>
              {article.category ? (CATEGORY_LABELS[article.category] || article.category) : 'Uncategorized'}
              {article.updatedAt && (
                <span style={{ marginLeft: 8 }}>
                  · Updated {new Date(article.updatedAt).toLocaleDateString()}
                </span>
              )}
            </div>

            {Array.isArray(article.tags) && article.tags.length > 0 && (
              <div style={{ marginBottom: 16 }}>
                {article.tags.map((t) => (
                  <span key={t} style={styles.tag}>{t}</span>
                ))}
              </div>
            )}

            <pre style={styles.body}>{article.content || ''}</pre>
          </div>

          <div style={styles.card}>
            <div style={styles.relatedTitle}>Related articles</div>
            {related.length === 0 ? (
              <div style={styles.empty}>No related articles yet.</div>
            ) : (
              related.map((r) => (
                <button
                  key={r.id}
                  type="button"
                  style={styles.relatedBtn}
                  onClick={() => openArticle(r.id)}
                >
                  {r.title}
                </button>
              ))
            )}
          </div>
        </div>
      )}
    </div>
  );
}
