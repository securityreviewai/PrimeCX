import React, { useEffect, useState } from 'react';
import { Navigate } from 'react-router-dom';

export default function ProtectedRoute({ oktaAuth, children }) {
  const [authenticated, setAuthenticated] = useState(null);

  useEffect(() => {
    let cancelled = false;
    oktaAuth.isAuthenticated().then((isAuth) => {
      if (!cancelled) setAuthenticated(isAuth);
    });
    return () => { cancelled = true; };
  }, [oktaAuth]);

  if (authenticated === null) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', padding: 80 }}>
        <div style={{ color: '#6B7280', fontSize: 15 }}>Loading...</div>
      </div>
    );
  }

  if (!authenticated) {
    return <Navigate to="/login" replace />;
  }

  return children;
}
