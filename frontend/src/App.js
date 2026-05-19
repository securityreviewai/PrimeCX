import React, { useEffect, useState } from 'react';
import { Routes, Route, useNavigate } from 'react-router-dom';
import { Security, LoginCallback } from '@okta/okta-react';
import { OktaAuth, toRelativeUrl } from '@okta/okta-auth-js';
import oktaConfig from './config';
import { setAuthClient, getMe } from './services/api';
import Navbar from './components/Navbar';
import ProtectedRoute from './components/ProtectedRoute';
import Login from './pages/Login';
import UserPortal from './pages/UserPortal';
import SupportConsole from './pages/SupportConsole';
import ManagerDashboard from './pages/ManagerDashboard';
import AdminPanel from './pages/AdminPanel';
import TicketDetail from './pages/TicketDetail';
import AIInsights from './pages/AIInsights';
import RecordingReview from './pages/RecordingReview';

const oktaAuth = new OktaAuth(oktaConfig);
setAuthClient(oktaAuth);

function AppRoutes() {
  const [user, setUser] = useState(null);

  useEffect(() => {
    const fetchUser = async () => {
      try {
        const authState = await oktaAuth.authStateManager.getAuthState();
        if (authState?.isAuthenticated) {
          const res = await getMe();
          setUser(res.data);
        }
      } catch {
        setUser(null);
      }
    };
    fetchUser();

    const sub = oktaAuth.authStateManager.subscribe((authState) => {
      if (authState.isAuthenticated) {
        getMe().then((res) => setUser(res.data)).catch(() => setUser(null));
      } else {
        setUser(null);
      }
    });
    return () => oktaAuth.authStateManager.unsubscribe(sub);
  }, []);

  return (
    <>
      <Navbar user={user} oktaAuth={oktaAuth} />
      <div style={{ padding: '24px', maxWidth: 1200, margin: '0 auto' }}>
        <Routes>
          <Route path="/login" element={<Login oktaAuth={oktaAuth} />} />
          <Route path="/login/callback" element={<LoginCallback />} />
          <Route
            path="/"
            element={
              <ProtectedRoute oktaAuth={oktaAuth}>
                <UserPortal user={user} />
              </ProtectedRoute>
            }
          />
          <Route
            path="/tickets"
            element={
              <ProtectedRoute oktaAuth={oktaAuth}>
                <UserPortal user={user} />
              </ProtectedRoute>
            }
          />
          <Route
            path="/tickets/:id"
            element={
              <ProtectedRoute oktaAuth={oktaAuth}>
                <TicketDetail user={user} />
              </ProtectedRoute>
            }
          />
          <Route
            path="/console"
            element={
              <ProtectedRoute oktaAuth={oktaAuth}>
                <SupportConsole user={user} />
              </ProtectedRoute>
            }
          />
          <Route
            path="/dashboard"
            element={
              <ProtectedRoute oktaAuth={oktaAuth}>
                <ManagerDashboard user={user} />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin"
            element={
              <ProtectedRoute oktaAuth={oktaAuth}>
                <AdminPanel user={user} />
              </ProtectedRoute>
            }
          />
          <Route
            path="/recordings/:id"
            element={
              <ProtectedRoute oktaAuth={oktaAuth}>
                <RecordingReview user={user} />
              </ProtectedRoute>
            }
          />
          <Route
            path="/ai-insights"
            element={
              <ProtectedRoute oktaAuth={oktaAuth}>
                <AIInsights />
              </ProtectedRoute>
            }
          />
        </Routes>
      </div>
    </>
  );
}

function App() {
  const navigate = useNavigate();
  const restoreOriginalUri = (_oktaAuth, originalUri) => {
    navigate(toRelativeUrl(originalUri || '/', window.location.origin));
  };

  return (
    <Security oktaAuth={oktaAuth} restoreOriginalUri={restoreOriginalUri}>
      <AppRoutes />
    </Security>
  );
}

export default App;
