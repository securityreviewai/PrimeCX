import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import {
  getPortalKBCategories,
  listPortalKBArticles,
  searchPortalKBArticles,
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
  heading: { fontSize: 24, fontWeight: 700, color: colors.gray900, marginBottom: 8 },
  sub: { color: colors.gray500, fontSize: 14, marginBottom: 20 },
  grid: { display: 'grid', gridTemplateColumns: '240px 1fr', gap: 24, alignItems: 'start' },
  card: { background: '#fff', borderRadius: 12, padding: 20, boxShadow: '0 1px 4px rgba(0,0,0,0.06)' },
  sectionTitle: { fontSize: 14, fontWeight: 700, color: colors.gray900, marginBottom: 12 },
  categoryBtn: (active) => ({
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    width: '100%',
    padding: '8px 12px',
    borderRadius: 8,
    border: 'none',
    background: active ? colors.primary : 'transparent',
    color: active ? '#fff' : colors.gray700,
    textAlign: 'left',
    fontSize: 13,
    fontWeight: 600,
    cursor: 'pointer',
    marginBottom: 4,
  }),
  badge: (active) => ({
    padding: '2px 8px',
    borderRadius: 12,
    fontSize: 11,
    fontWeight: 600,
    background: active ? 'rgba(255,255,255,0.25)' : colors.gray100,
    color: active ? '#fff' : colors.gray700,
  }),
  search: {
    width: '100%',
    padding: '10px 14px',
    borderRadius: 8,
    border: `1px solid ${colors.gray200}`,
    fontSize: 14,
    outline: 'none',
    boxSizing: 'border-box',
    marginBottom: 16,
  },
  articleRow: {
    display: 'block',
    width: '100%',
    padding: '14px 16px',
    border: `1px solid ${colors.gray200}`,
    borderRadius: 10,
    marginBottom: 10,
    background: '#fff',
    textAlign: 'left',
    cursor: 'pointer',
  },
  articleTitle: { fontSize: 15, fontWeight: 600, color: colors.gray900, marginBottom: 4 },
  articleMeta: { fontSize: 12, color: colors.gray500 },
  tag: {
    display: 'inline-block',
    padding: '2px 8px',
    marginRight: 6,
    background: colors.gray100,
    color: colors.gray700,
    borderRadius: 10,
    fontSize: 11,
  },
  empty: { color: colors.gray500, fontSize: 14, textAlign: 'center', padding: 40 },
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

function ArticleListRow({ article, onSelect }) {
  return (
    <button type="button" style={styles.articleRow} onClick={() => onSelect(article.id)}>
      <div style={styles.articleTitle}>{article.title}</div>
      <div style={styles.articleMeta}>
        {article.category ? CATEGORY_LABELS[article.category] || article.category : 'Uncategorized'}
        {Array.isArray(article.tags) && article.tags.length > 0 && (
          <span style={{ marginLeft: 8 }}>
            {article.tags.slice(0, 4).map((t) => (
              <span key={t} style={styles.tag}>{t}</span>
            ))}
          </span>
        )}
      </div>
    </button>
  );
}

export default function CustomerKBPortal() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();

  const [categories, setCategories] = useState([]);
  const [articles, setArticles] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [query, setQuery] = useState('');
  const [debouncedQuery, setDebouncedQuery] = useState('');

  const selectedCategory = searchParams.get('category') || '';
  const debounceRef = useRef(null);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const res = await getPortalKBCategories();
        if (!cancelled) setCategories(Array.isArray(res.data) ? res.data : []);
      } catch {
        if (!cancelled) setCategories([]);
      }
    })();
    return () => { cancelled = true; };
  }, []);

  useEffect(() => {
    if (debounceRef.current) clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(() => setDebouncedQuery(query.trim()), 280);
    return () => {
      if (debounceRef.current) clearTimeout(debounceRef.current);
    };
  }, [query]);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        setLoading(true);
        setError(null);
        if (debouncedQuery && debouncedQuery.length >= 2) {
          const res = await searchPortalKBArticles(debouncedQuery);
          if (!cancelled) setArticles(Array.isArray(res.data) ? res.data : []);
        } else {
          const res = await listPortalKBArticles({
            category: selectedCategory || undefined,
            size: 20,
          });
          if (!cancelled) setArticles(Array.isArray(res.data) ? res.data : []);
        }
      } catch {
        if (!cancelled) {
          setArticles([]);
          setError('Could not load knowledge base articles');
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => { cancelled = true; };
  }, [debouncedQuery, selectedCategory]);

  const onSelectCategory = useCallback((cat) => {
    const next = new URLSearchParams(searchParams);
    if (cat) {
      next.set('category', cat);
    } else {
      next.delete('category');
    }
    setSearchParams(next, { replace: true });
  }, [searchParams, setSearchParams]);

  const onSelectArticle = useCallback((id) => {
    navigate(`/kb/articles/${id}`);
  }, [navigate]);

  const totalArticles = useMemo(
    () => categories.reduce((acc, c) => acc + (c.articleCount || 0), 0),
    [categories],
  );

  return (
    <div>
      <h1 style={styles.heading}>Help Center</h1>
      <div style={styles.sub}>Browse our knowledge base or search for answers to common questions.</div>

      <div style={styles.grid}>
        <div style={styles.card}>
          <div style={styles.sectionTitle}>Categories</div>
          <button
            type="button"
            style={styles.categoryBtn(!selectedCategory)}
            onClick={() => onSelectCategory('')}
          >
            <span>All articles</span>
            <span style={styles.badge(!selectedCategory)}>{totalArticles}</span>
          </button>
          {categories.map((c) => (
            <button
              key={c.category}
              type="button"
              style={styles.categoryBtn(selectedCategory === c.category)}
              onClick={() => onSelectCategory(c.category)}
            >
              <span>{CATEGORY_LABELS[c.category] || c.category}</span>
              <span style={styles.badge(selectedCategory === c.category)}>{c.articleCount}</span>
            </button>
          ))}
        </div>

        <div>
          <input
            type="search"
            placeholder="Search the knowledge base…"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            style={styles.search}
            autoComplete="off"
            maxLength={100}
          />

          {error && <div style={styles.error}>{error}</div>}

          {loading ? (
            <div style={styles.empty}>Loading articles…</div>
          ) : articles.length === 0 ? (
            <div style={styles.empty}>
              {debouncedQuery && debouncedQuery.length >= 2
                ? 'No articles match that search.'
                : 'No articles available yet.'}
            </div>
          ) : (
            articles.map((article) => (
              <ArticleListRow
                key={article.id}
                article={article}
                onSelect={onSelectArticle}
              />
            ))
          )}
        </div>
      </div>
    </div>
  );
}
