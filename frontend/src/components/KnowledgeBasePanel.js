import React, { useState, useEffect, useCallback, useRef } from 'react';
import { searchKBArticles, getKBArticleById } from '../services/api';
import { getRecentArticleIds, recordArticleView } from '../utils/kbRecentArticles';

const colors = {
  primary: '#4F46E5',
  gray100: '#F3F4F6',
  gray200: '#E5E7EB',
  gray500: '#6B7280',
  gray700: '#374151',
  gray900: '#111827',
};

const styles = {
  wrap: {
    marginTop: 20,
    paddingTop: 20,
    borderTop: `1px solid ${colors.gray200}`,
  },
  title: { fontSize: 14, fontWeight: 700, color: colors.gray900, marginBottom: 10 },
  search: {
    width: '100%',
    padding: '8px 12px',
    borderRadius: 8,
    border: `1px solid ${colors.gray200}`,
    fontSize: 13,
    outline: 'none',
    boxSizing: 'border-box',
    marginBottom: 10,
  },
  recentRow: { display: 'flex', flexWrap: 'wrap', gap: 8, marginBottom: 12 },
  chip: {
    padding: '4px 10px',
    borderRadius: 16,
    fontSize: 12,
    fontWeight: 600,
    border: `1px solid ${colors.gray200}`,
    background: '#fff',
    color: colors.primary,
    cursor: 'pointer',
    maxWidth: '100%',
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
  },
  resultBtn: {
    display: 'block',
    width: '100%',
    textAlign: 'left',
    padding: '8px 10px',
    borderRadius: 8,
    border: `1px solid ${colors.gray200}`,
    background: colors.gray100,
    fontSize: 13,
    color: colors.gray900,
    cursor: 'pointer',
    marginBottom: 6,
  },
  articleBox: {
    marginTop: 12,
    padding: 12,
    borderRadius: 8,
    background: '#fff',
    border: `1px solid ${colors.gray200}`,
    maxHeight: 220,
    overflow: 'auto',
  },
  articleTitle: { fontSize: 14, fontWeight: 700, color: colors.gray900, marginBottom: 8 },
  articleBody: {
    fontSize: 13,
    color: colors.gray700,
    whiteSpace: 'pre-wrap',
    wordBreak: 'break-word',
    margin: 0,
    fontFamily: 'inherit',
  },
  hint: { fontSize: 12, color: colors.gray500, marginBottom: 8 },
  err: { fontSize: 12, color: '#B91C1C', marginBottom: 8 },
};

export default function KnowledgeBasePanel({ user }) {
  const [query, setQuery] = useState('');
  const [debounced, setDebounced] = useState('');
  const [searchResults, setSearchResults] = useState([]);
  const [searchLoading, setSearchLoading] = useState(false);
  const [searchError, setSearchError] = useState(null);

  const [recentIds, setRecentIds] = useState([]);
  const [recentMeta, setRecentMeta] = useState({});

  const [selected, setSelected] = useState(null);
  const [loadError, setLoadError] = useState(null);
  const [loadingArticle, setLoadingArticle] = useState(false);

  const debounceRef = useRef(null);

  useEffect(() => {
    if (!user?.id) return;
    setRecentIds(getRecentArticleIds(user.id));
  }, [user?.id]);

  useEffect(() => {
    if (debounceRef.current) clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(() => setDebounced(query.trim()), 280);
    return () => clearTimeout(debounceRef.current);
  }, [query]);

  useEffect(() => {
    if (!debounced) {
      setSearchResults([]);
      setSearchError(null);
      return;
    }
    let cancelled = false;
    (async () => {
      try {
        setSearchLoading(true);
        setSearchError(null);
        const res = await searchKBArticles(debounced);
        if (!cancelled) setSearchResults(res.data || []);
      } catch {
        if (!cancelled) {
          setSearchResults([]);
          setSearchError('Search failed');
        }
      } finally {
        if (!cancelled) setSearchLoading(false);
      }
    })();
    return () => { cancelled = true; };
  }, [debounced]);

  useEffect(() => {
    if (!user?.id || recentIds.length === 0) return;
    let cancelled = false;
    (async () => {
      const entries = await Promise.all(
        recentIds.map(async (id) => {
          try {
            const res = await getKBArticleById(id);
            return [id, res.data?.title ? String(res.data.title) : `Article #${id}`];
          } catch {
            return [id, `Article #${id}`];
          }
        }),
      );
      if (cancelled) return;
      const next = {};
      entries.forEach(([id, title]) => { next[id] = title; });
      setRecentMeta(next);
    })();
    return () => { cancelled = true; };
  }, [user?.id, recentIds]);

  const openArticle = useCallback(async (articleId) => {
    if (!user?.id) return;
    setLoadError(null);
    setLoadingArticle(true);
    try {
      const res = await getKBArticleById(articleId);
      const article = res.data;
      if (!article) {
        setLoadError('Article not found');
        setSelected(null);
        return;
      }
      recordArticleView(user.id, article.id);
      setRecentIds(getRecentArticleIds(user.id));
      setSelected(article);
    } catch {
      setLoadError('Could not load article');
      setSelected(null);
    } finally {
      setLoadingArticle(false);
    }
  }, [user?.id]);

  if (!user?.id) return null;

  return (
    <div style={styles.wrap}>
      <div style={styles.title}>Knowledge base</div>
      <p style={styles.hint}>Search while on a call; recently opened articles appear below for quick access.</p>

      <input
        type="search"
        style={styles.search}
        placeholder="Search articles…"
        value={query}
        onChange={(e) => setQuery(e.target.value)}
        autoComplete="off"
      />

      {recentIds.length > 0 && (
        <div>
          <div style={{ fontSize: 12, fontWeight: 600, color: colors.gray700, marginBottom: 6 }}>Recently viewed</div>
          <div style={styles.recentRow}>
            {recentIds.map((id) => (
              <button
                key={id}
                type="button"
                style={styles.chip}
                onClick={() => openArticle(id)}
                disabled={loadingArticle}
                title={recentMeta[id] || `Article #${id}`}
              >
                {recentMeta[id] || `#${id}`}
              </button>
            ))}
          </div>
        </div>
      )}

      {searchError && <div style={styles.err}>{searchError}</div>}
      {debounced && (
        <div>
          {searchLoading ? (
            <div style={{ fontSize: 12, color: colors.gray500 }}>Searching…</div>
          ) : searchResults.length === 0 ? (
            <div style={{ fontSize: 12, color: colors.gray500 }}>No matches</div>
          ) : (
            searchResults.map((a) => (
              <button
                key={a.id}
                type="button"
                style={styles.resultBtn}
                onClick={() => openArticle(a.id)}
                disabled={loadingArticle}
              >
                {a.title || `Article #${a.id}`}
              </button>
            ))
          )}
        </div>
      )}

      {loadError && <div style={styles.err}>{loadError}</div>}
      {selected && (
        <div style={styles.articleBox}>
          <div style={styles.articleTitle}>{selected.title}</div>
          <pre style={styles.articleBody}>{selected.content || ''}</pre>
        </div>
      )}
    </div>
  );
}
