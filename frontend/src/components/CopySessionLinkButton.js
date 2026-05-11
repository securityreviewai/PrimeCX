import React, { useState } from 'react';
import { createSessionShareLink } from '../services/api';

const ALLOWED_SCHEMES = ['https:', 'http:'];

/**
 * Build an absolute https/http URL for the share link using the current origin.
 *
 * We never splice server-provided strings into an <a href> without scheme
 * validation first — even though `shortPath` comes from our own backend, this
 * guards against accidental regressions that would let javascript:/data: URLs
 * flow into anchors or clipboard.
 */
function buildAbsoluteShareUrl(shortPath) {
  if (typeof shortPath !== 'string' || !shortPath.startsWith('/s/')) {
    return null;
  }
  try {
    const url = new URL(shortPath, window.location.origin);
    if (!ALLOWED_SCHEMES.includes(url.protocol)) {
      return null;
    }
    return url.toString();
  } catch {
    return null;
  }
}

async function copyToClipboard(text) {
  if (navigator.clipboard && window.isSecureContext) {
    try {
      await navigator.clipboard.writeText(text);
      return true;
    } catch {
      // fall through to fallback
    }
  }
  try {
    const textarea = document.createElement('textarea');
    textarea.value = text;
    textarea.setAttribute('readonly', '');
    textarea.style.position = 'absolute';
    textarea.style.left = '-9999px';
    document.body.appendChild(textarea);
    textarea.select();
    const ok = document.execCommand('copy');
    document.body.removeChild(textarea);
    return ok;
  } catch {
    return false;
  }
}

export default function CopySessionLinkButton({ sessionId, style, label = 'Copy link' }) {
  const [status, setStatus] = useState('idle');

  const handleClick = async () => {
    if (!sessionId) return;
    setStatus('loading');
    try {
      const res = await createSessionShareLink(sessionId);
      const shortPath = res?.data?.shortPath;
      const absolute = buildAbsoluteShareUrl(shortPath);
      if (!absolute) {
        setStatus('error');
        return;
      }
      const ok = await copyToClipboard(absolute);
      setStatus(ok ? 'copied' : 'error');
      if (ok) {
        setTimeout(() => setStatus('idle'), 2000);
      }
    } catch (err) {
      const code = err?.response?.status;
      setStatus(code === 403 ? 'forbidden' : 'error');
    }
  };

  let text = label;
  if (status === 'loading') text = 'Copying…';
  else if (status === 'copied') text = 'Link copied';
  else if (status === 'forbidden') text = 'Not allowed';
  else if (status === 'error') text = 'Copy failed';

  return (
    <button
      type="button"
      onClick={handleClick}
      disabled={status === 'loading'}
      style={{
        padding: '6px 12px',
        borderRadius: 6,
        border: '1px solid #E5E7EB',
        background: status === 'copied' ? '#ECFDF5' : '#F9FAFB',
        color: status === 'copied' ? '#059669' : '#374151',
        fontSize: 12,
        fontWeight: 600,
        cursor: status === 'loading' ? 'wait' : 'pointer',
        ...style,
      }}
    >
      {text}
    </button>
  );
}
