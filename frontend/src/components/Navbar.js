import React from 'react';
import { Link, useLocation } from 'react-router-dom';

const styles = {
  nav: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    padding: '0 24px',
    height: 60,
    background: '#4F46E5',
    color: '#fff',
    boxShadow: '0 2px 8px rgba(79,70,229,0.3)',
    position: 'sticky',
    top: 0,
    zIndex: 100,
  },
  brand: {
    fontSize: 20,
    fontWeight: 700,
    letterSpacing: '-0.5px',
    textDecoration: 'none',
    color: '#fff',
  },
  links: {
    display: 'flex',
    gap: 8,
    alignItems: 'center',
  },
  link: {
    color: 'rgba(255,255,255,0.85)',
    textDecoration: 'none',
    padding: '6px 14px',
    borderRadius: 6,
    fontSize: 14,
    fontWeight: 500,
    transition: 'background 0.15s',
  },
  activeLink: {
    background: 'rgba(255,255,255,0.2)',
    color: '#fff',
  },
  right: {
    display: 'flex',
    alignItems: 'center',
    gap: 16,
  },
  userName: {
    fontSize: 14,
    fontWeight: 500,
    opacity: 0.9,
  },
  logoutBtn: {
    background: 'rgba(255,255,255,0.15)',
    border: '1px solid rgba(255,255,255,0.3)',
    color: '#fff',
    padding: '6px 16px',
    borderRadius: 6,
    fontSize: 13,
    fontWeight: 500,
    cursor: 'pointer',
  },
};

function NavLink({ to, children }) {
  const location = useLocation();
  const isActive = location.pathname === to;
  return (
    <Link
      to={to}
      style={{ ...styles.link, ...(isActive ? styles.activeLink : {}) }}
    >
      {children}
    </Link>
  );
}

export default function Navbar({ user, oktaAuth }) {
  const role = user?.role;

  const handleLogout = async () => {
    await oktaAuth.signOut();
  };

  return (
    <nav style={styles.nav}>
      <Link to="/" style={styles.brand}>PrimeCX</Link>

      <div style={styles.links}>
        {role === 'user' && (
          <>
            <NavLink to="/tickets">Tickets</NavLink>
            <NavLink to="/kb">Help Center</NavLink>
          </>
        )}
        {role === 'support_executive' && (
          <>
            <NavLink to="/console">Console</NavLink>
            <NavLink to="/tickets">Tickets</NavLink>
            <NavLink to="/kb">Help Center</NavLink>
          </>
        )}
        {role === 'support_manager' && (
          <>
            <NavLink to="/dashboard">Dashboard</NavLink>
            <NavLink to="/ai-insights">AI Insights</NavLink>
            <NavLink to="/tickets">Tickets</NavLink>
            <NavLink to="/kb">Help Center</NavLink>
          </>
        )}
        {role === 'support_admin' && (
          <>
            <NavLink to="/admin">Admin Panel</NavLink>
            <NavLink to="/dashboard">Dashboard</NavLink>
            <NavLink to="/ai-insights">AI Insights</NavLink>
            <NavLink to="/console">Console</NavLink>
            <NavLink to="/tickets">Tickets</NavLink>
            <NavLink to="/kb">Help Center</NavLink>
          </>
        )}
      </div>

      <div style={styles.right}>
        {user && (
          <>
            <span style={styles.userName}>{user.name || user.email}</span>
            <button style={styles.logoutBtn} onClick={handleLogout}>
              Logout
            </button>
          </>
        )}
      </div>
    </nav>
  );
}
