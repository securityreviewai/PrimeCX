/**
 * Persists the last N KB article IDs per signed-in user (browser localStorage).
 * IDs are non-sensitive; article body always comes from the authenticated API.
 */

const STORAGE_VERSION = 'v1';
const MAX_RECENT = 10;

function storageKey(userId) {
  return `primecx.kb.recentArticleIds.${STORAGE_VERSION}:${String(userId)}`;
}

function parsePositiveLongId(value) {
  if (value === null || value === undefined) return null;
  const n = Number(value);
  if (!Number.isFinite(n) || n <= 0 || n > Number.MAX_SAFE_INTEGER) return null;
  if (!Number.isInteger(n)) return null;
  return n;
}

/**
 * @param {unknown} userId
 * @returns {number[]}
 */
export function getRecentArticleIds(userId) {
  const uid = userId != null ? String(userId) : '';
  if (!uid) return [];
  try {
    const raw = window.localStorage.getItem(storageKey(uid));
    if (!raw) return [];
    const parsed = JSON.parse(raw);
    if (!Array.isArray(parsed)) return [];
    const out = [];
    const seen = new Set();
    for (const item of parsed) {
      const id = parsePositiveLongId(item);
      if (id == null || seen.has(id)) continue;
      seen.add(id);
      out.push(id);
      if (out.length >= MAX_RECENT) break;
    }
    return out;
  } catch {
    return [];
  }
}

/**
 * Move id to front; cap at MAX_RECENT.
 * @param {unknown} userId
 * @param {unknown} articleId
 */
export function recordArticleView(userId, articleId) {
  const uid = userId != null ? String(userId) : '';
  const id = parsePositiveLongId(articleId);
  if (!uid || id == null) return;
  try {
    const prev = getRecentArticleIds(uid);
    const next = [id, ...prev.filter((x) => x !== id)].slice(0, MAX_RECENT);
    window.localStorage.setItem(storageKey(uid), JSON.stringify(next));
  } catch {
    // quota / private mode — ignore
  }
}
