import React from 'react';

const styles = {
  wrapper: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    minHeight: 'calc(100vh - 60px)',
    background: 'linear-gradient(135deg, #EEF2FF 0%, #E0E7FF 100%)',
  },
  card: {
    background: '#fff',
    borderRadius: 16,
    padding: '48px 40px',
    boxShadow: '0 4px 24px rgba(0,0,0,0.08)',
    textAlign: 'center',
    maxWidth: 420,
    width: '100%',
  },
  logo: {
    fontSize: 36,
    fontWeight: 800,
    color: '#4F46E5',
    marginBottom: 8,
    letterSpacing: '-1px',
  },
  subtitle: {
    color: '#6B7280',
    fontSize: 15,
    marginBottom: 32,
    lineHeight: 1.5,
  },
  button: {
    display: 'inline-block',
    background: '#4F46E5',
    color: '#fff',
    border: 'none',
    padding: '14px 36px',
    borderRadius: 10,
    fontSize: 16,
    fontWeight: 600,
    cursor: 'pointer',
    width: '100%',
    transition: 'background 0.15s',
  },
  footer: {
    marginTop: 24,
    fontSize: 13,
    color: '#9CA3AF',
  },
};

export default function Login({ oktaAuth }) {
  const handleLogin = () => {
    oktaAuth.signInWithRedirect();
  };

  return (
    <div style={styles.wrapper}>
      <div style={styles.card}>
        <div style={styles.logo}>PrimeCX</div>
        <p style={styles.subtitle}>
          Customer experience platform for seamless support.
          <br />
          Sign in to manage tickets, sessions, and recordings.
        </p>
        <button
          style={styles.button}
          onClick={handleLogin}
          onMouseEnter={(e) => (e.target.style.background = '#4338CA')}
          onMouseLeave={(e) => (e.target.style.background = '#4F46E5')}
        >
          Sign in with SSO
        </button>
        <p style={styles.footer}>Secured with Okta OIDC</p>
      </div>
    </div>
  );
}
