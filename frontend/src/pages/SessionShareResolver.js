import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { resolveSessionShareLink } from '../services/api';

// Token format must match the backend contract: base64url of 16 bytes (22 chars).
// The backend re-validates this; the client-side check is only UX guard-railing.
const TOKEN_PATTERN = /^[A-Za-z0-9_-]{22}$/;

const styles = {
  wrap: { display: 'flex', justifyContent: 'center', padding: 80 },
  card: {
    maxWidth: 520, width: '100%', background: '#fff', borderRadius: 12,
    padding: 32, boxShadow: '0 1px 4px rgba(0,0,0,0.08)', textAlign: 'center',
  },
  heading: { fontSize: 20, fontWeight: 700, color: '#111827', marginBottom: 12 },
  msg: { color: '#6B7280', fontSize: 14, marginBottom: 20 },
  danger: { color: '#EF4444' },
  btn: {
    padding: '10px 18px', borderRadius: 8, border: 'none', background: '#4F46E5',
    color: '#fff', fontSize: 14, fontWeight: 600, cursor: 'pointer',
  },
};

export default function SessionShareResolver() {
  const { token } = useParams();
  const navigate = useNavigate();
  const [state, setState] = useState({ status: 'loading', message: null });

  useEffect(() => {
    let cancelled = false;

    if (!token || !TOKEN_PATTERN.test(token)) {
      setState({
        status: 'error',
        message: 'This link is invalid. Please request a new share link.',
      });
      return undefined;
    }

    (async () => {
      try {
        const res = await resolveSessionShareLink(token);
        if (cancelled) return;
        const { ticketId, sessionId } = res.data || {};
        // Route the caller to the ticket detail view (handoff target).
        // Fall back to home if the resolved payload is missing a ticket id.
        if (typeof ticketId === 'number' && Number.isFinite(ticketId)) {
          navigate(`/tickets/${ticketId}`, {
            replace: true,
            state: { fromShareLink: true, sessionId },
          });
        } else {
          setState({
            status: 'error',
            message: 'This link no longer points to a valid ticket.',
          });
        }
      } catch (err) {
        if (cancelled) return;
        const status = err?.response?.status;
        // The backend intentionally collapses "not found", "expired", "revoked",
        // and "not authorized" into a single 404 to avoid leaking token existence.
        // Surface a single generic message matching that contract.
        if (status === 404) {
          setState({
            status: 'error',
            message:
              'This share link is invalid, expired, or you do not have access to it.',
          });
        } else if (status === 403) {
          setState({
            status: 'error',
            message: 'You do not have permission to view this session.',
          });
        } else {
          setState({
            status: 'error',
            message: 'Something went wrong while opening this link.',
          });
        }
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [token, navigate]);

  if (state.status === 'loading') {
    return (
      <div style={styles.wrap}>
        <div style={styles.card}>
          <div style={styles.heading}>Opening shared session…</div>
          <div style={styles.msg}>Verifying your access.</div>
        </div>
      </div>
    );
  }

  return (
    <div style={styles.wrap}>
      <div style={styles.card}>
        <div style={{ ...styles.heading, ...styles.danger }}>Can't open this link</div>
        <div style={styles.msg}>{state.message}</div>
        <button type="button" style={styles.btn} onClick={() => navigate('/')}>
          Go to dashboard
        </button>
      </div>
    </div>
  );
}
