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
import { ProfileCreate } from './components/ProfileCreate';
import { AbsencePage } from './components/AbsencePage';
import { AbsenceForm } from './components/AbsenceForm';
import { FeedbackPage } from './components/FeedbackPage';
import { Button } from './components/ui/button';
import { Badge } from './components/ui/badge';
import { Card, CardContent } from './components/ui/card';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from './components/ui/dialog';
import './App.css';

const HomePage = () => {
  const currentUser = useAuthStore((state) => state.currentUser);
  const [showAbsenceModal, setShowAbsenceModal] = useState(false);
  
  return (
    <div className="p-8">
      <h1 className="text-3xl font-bold mb-4">Welcome to HR Core</h1>
      <p className="text-muted-foreground mb-6">
        Manage employee profiles, feedback, and absence requests in one place.
      </p>
      
      {currentUser && (
        <>
          <Card className="max-w-2xl bg-gradient-to-r from-primary/10 to-primary/5 border-primary/20 mb-6">
            <CardContent className="p-6">
              <div className="flex items-center justify-between">
                <div>
                  <h3 className="text-lg font-semibold mb-1">Your Profile</h3>
                  <p className="text-sm text-muted-foreground mb-2">
                    {currentUser.firstName} {currentUser.lastName}
                  </p>
                  <p className="text-sm text-muted-foreground mb-2">
                    {currentUser.email}
                  </p>
                  {currentUser.department && (
                    <p className="text-sm text-muted-foreground mb-2">
                      Department: {currentUser.department}
                    </p>
                  )}
                  <Badge variant="secondary" className="mt-2">
                    {currentUser.role.replace('_', ' ')}
                  </Badge>
                </div>
                <Button asChild>
                  <Link to={`/profiles/${currentUser.id}`}>
                    View My Profile
                  </Link>
                </Button>
              </div>
            </CardContent>
          </Card>
          
          <Card className="max-w-2xl">
            <CardContent className="p-6">
              <h3 className="text-lg font-semibold mb-4">Quick Actions</h3>
              <div className="flex gap-3">
                <Dialog open={showAbsenceModal} onOpenChange={setShowAbsenceModal}>
                  <DialogTrigger asChild>
                    <Button>Request Absence</Button>
                  </DialogTrigger>
                  <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
                    <DialogHeader>
                      <DialogTitle>Request Absence</DialogTitle>
                      <DialogDescription>
                        Submit a new absence request for yourself. Your manager will review and approve it.
                      </DialogDescription>
                    </DialogHeader>
                    <AbsenceForm 
                      userId={currentUser.id}
                      onSuccess={() => setShowAbsenceModal(false)} 
                    />
                  </DialogContent>
                </Dialog>
              </div>
            </CardContent>
          </Card>
        </>
      )}
    </div>
  );
};

const FeedbackPage_Route = () => <FeedbackPage />;

const AdminPage = () => (
  <div className="p-8">
    <h1 className="text-3xl font-bold mb-4">Admin Dashboard</h1>
    <AdminTokenPanel />
  </div>
);

const queryClient = new QueryClient();

const canAccessProfiles = (role?: string): boolean => {
  return ['SUPER_ADMIN', 'MANAGER', 'EMPLOYEE'].includes(role || '');
};

const canAccessFeedback = (role?: string): boolean => {
  return ['SUPER_ADMIN', 'MANAGER'].includes(role || '');
};

const canAccessAbsence = (role?: string): boolean => {
  return ['SUPER_ADMIN', 'MANAGER'].includes(role || '');
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
      <div className="min-h-screen flex items-center justify-center bg-background">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary mx-auto mb-4"></div>
          <p className="text-muted-foreground">Initializing...</p>
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
          <div className="min-h-screen bg-background">
            <nav className="border-b bg-background">
              <div className="max-w-7xl mx-auto px-4 py-4 flex items-center justify-between">
                <h1 className="text-2xl font-bold">HR Core</h1>

                <div className="flex gap-6 items-center">
                  <Link to="/" className="text-foreground hover:text-primary transition">
                    Home
                  </Link>

                  {canAccessProfiles(currentUser?.role) && (
                    <Link to="/profiles" className="text-foreground hover:text-primary transition">
                      Profiles
                    </Link>
                  )}

                  {canAccessFeedback(currentUser?.role) && (
                    <Link to="/feedback" className="text-foreground hover:text-primary transition">
                      Feedback (Mgmt)
                    </Link>
                  )}

                  {canAccessAbsence(currentUser?.role) && (
                    <Link to="/absence" className="text-foreground hover:text-primary transition">
                      Absence (Mgmt)
                    </Link>
                  )}

                  {canAccessAdmin(currentUser?.role) && (
                    <Link to="/admin" className="text-foreground hover:text-primary transition">
                      🔐 Admin
                    </Link>
                  )}

                  <div className="flex items-center gap-2">
                    <Badge variant="secondary">
                      {currentUser?.firstName || 'User'} ({currentUser?.role || 'EMPLOYEE'})
                    </Badge>
                    <Button variant="ghost" size="sm" onClick={handleLogout}>
                      Logout
                    </Button>
                  </div>
                </div>
              </div>
            </nav>

            <div className="max-w-7xl mx-auto px-4 py-8">
              <Routes>
                <Route path="/" element={<HomePage />} />
                <Route path="/profiles" element={<ProfileList />} />
                <Route path="/profiles/create" element={<ProfileCreate />} />
                <Route path="/profiles/:userId" element={<ProfileDetail />} />
                <Route path="/profiles/:userId/edit" element={<ProfileEdit />} />
                <Route path="/feedback" element={<FeedbackPage_Route />} />
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
