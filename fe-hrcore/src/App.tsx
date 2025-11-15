import { useEffect, useState } from 'react';
import { BrowserRouter as Router, Routes, Route, Link, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import {useAuthStore} from './store/authStore';
import { authService } from './services/authService';
import { LoginPage } from './components/LoginPage';
import { AuthCallback } from './components/AuthCallback';
import { AdminTokenPanel } from './components/AdminTokenPanel';
import { ProfileList } from './components/ProfileList';
import { ProfileDetail } from './components/ProfileDetail';
import { ProfileEdit } from './components/ProfileEdit';
import { AbsenceManagerPanel } from './components/AbsenceManagerPanel';
import { AbsenceList } from './components/AbsenceList';
import { FeedbackList } from './components/FeedbackList';
import './App.css';

// Pages
const HomePage = () => (
  <div className="p-8">
    <h1 className="text-3xl font-bold mb-4">Welcome to HR Core</h1>
    <p className="text-gray-600">
      Manage employee profiles, feedback, and absence requests in one place.
    </p>
  </div>
);

const FeedbackPage = () => (
  <div className="p-8">
    <h1 className="text-3xl font-bold mb-4">Feedback</h1>
    <FeedbackList />
  </div>
);

const AbsencePage = () => (
  <div className="p-8">
    <h1 className="text-3xl font-bold mb-4">Absence Requests</h1>
    <AbsenceList />
    <AbsenceManagerPanel />
  </div>
);

const AdminPage = () => (
  <div className="p-8">
    <h1 className="text-3xl font-bold mb-4">Admin Dashboard</h1>
    <AdminTokenPanel />
  </div>
);

const queryClient = new QueryClient();

// Menu visibility helper based on roles
const canAccessProfiles = (role?: string): boolean => {
  return ['SUPER_ADMIN', 'MANAGER', 'EMPLOYEE'].includes(role || '');
};

const canAccessFeedback = (role?: string): boolean => {
  return ['SUPER_ADMIN', 'MANAGER', 'EMPLOYEE'].includes(role || '');
};

const canAccessAbsence = (role?: string): boolean => {
  return ['SUPER_ADMIN', 'MANAGER', 'EMPLOYEE'].includes(role || '');
};

const canAccessAdmin = (role?: string): boolean => {
  return role === 'SUPER_ADMIN';
};

function App() {
  const [initialized, setInitialized] = useState(false);
  const currentUser = useAuthStore((state) => state.currentUser);
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated());

  const handleLogout = async () => {
    await authService.logout();
    window.location.href = '/login';
  };

  useEffect(() => {
    const initializeAuth = async () => {
      const storedToken = localStorage.getItem('authToken');
      if (storedToken) {
        try {
          await authService.getCurrentUser();
        } catch (error) {
          console.error('Failed to restore session:', error);
          useAuthStore.getState().logout();
          localStorage.removeItem('authToken');
        }
      }
      setInitialized(true);
    };

    initializeAuth();
  }, []);

  if (!initialized) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-100">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
          <p className="text-gray-600">Initializing...</p>
        </div>
      </div>
    );
  }

  return (
    <QueryClientProvider client={queryClient}>
      <Router>
        {!isAuthenticated ? (
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route path="/auth/callback" element={<AuthCallback />} />
            <Route path="*" element={<Navigate to="/login" replace />} />
          </Routes>
        ) : (
          <div className="min-h-screen bg-gray-100">
            {/* Navigation */}
            <nav className="bg-white shadow">
              <div className="max-w-7xl mx-auto px-4 py-4 flex items-center justify-between">
                <h1 className="text-2xl font-bold text-blue-600">HR Core</h1>

                <div className="flex gap-6 items-center">
                  <Link to="/" className="text-gray-700 hover:text-blue-600 transition">
                    Home
                  </Link>

                  {canAccessProfiles(currentUser?.role) && (
                    <Link to="/profiles" className="text-gray-700 hover:text-blue-600 transition">
                      Profiles
                    </Link>
                  )}

                  {canAccessFeedback(currentUser?.role) && (
                    <Link to="/feedback" className="text-gray-700 hover:text-blue-600 transition">
                      Feedback
                    </Link>
                  )}

                  {canAccessAbsence(currentUser?.role) && (
                    <Link to="/absence" className="text-gray-700 hover:text-blue-600 transition">
                      Absence
                    </Link>
                  )}

                  {canAccessAdmin(currentUser?.role) && (
                    <Link to="/admin" className="text-gray-700 hover:text-blue-600 transition">
                      🔐 Admin
                    </Link>
                  )}

                  {/* Current User Display */}
                  <div className="flex items-center gap-2 bg-blue-100 px-4 py-2 rounded">
                    <span className="text-sm font-semibold text-blue-900">
                      {currentUser?.firstName || 'User'} ({currentUser?.role || 'EMPLOYEE'})
                    </span>
                    <button
                      onClick={handleLogout}
                      className="text-sm text-blue-600 hover:text-blue-800"
                    >
                      Logout
                    </button>
                  </div>
                </div>
              </div>
            </nav>

            {/* Main Content */}
            <div className="max-w-7xl mx-auto px-4 py-8">
              <Routes>
                <Route path="/" element={<HomePage />} />
                <Route path="/profiles" element={<ProfileList />} />
                <Route path="/profiles/:userId" element={<ProfileDetail />} />
                <Route path="/profiles/:userId/edit" element={<ProfileEdit />} />
                <Route path="/feedback" element={<FeedbackPage />} />
                <Route path="/absence" element={<AbsencePage />} />
                <Route path="/admin" element={<AdminPage />} />
              </Routes>
            </div>
          </div>
        )}
      </Router>
    </QueryClientProvider>
  );
}

export default App
