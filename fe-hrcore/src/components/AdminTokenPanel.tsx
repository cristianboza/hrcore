import { useState, useEffect } from 'react';
import { useAuthStore } from '../store/authStore';
import apiClient from '../services/apiClient';

interface LoggedUser {
  tokenId: number;
  userId: number;
  email: string;
  firstName: string;
  lastName: string;
  issuedAt: string;
  expiresAt: string;
}

export const AdminTokenPanel = () => {
  const currentUser = useAuthStore((state) => state.currentUser);
  const [loggedUsers, setLoggedUsers] = useState<LoggedUser[]>([]);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  useEffect(() => {
    if (currentUser?.role === 'SUPER_ADMIN') {
      loadLoggedUsers();
      const interval = setInterval(loadLoggedUsers, 30000);
      return () => clearInterval(interval);
    }
  }, [currentUser]);

  const loadLoggedUsers = async () => {
    try {
      setLoading(true);
      const response = await apiClient.get<LoggedUser[]>('/admin/logged-users');
      setLoggedUsers(response.data);
    } catch (error) {
      console.error('Failed to load logged users:', error);
      setMessage({ type: 'error', text: 'Failed to load logged users' });
    } finally {
      setLoading(false);
    }
  };

  const forceLogoutSession = async (tokenId: number) => {
    try {
      setLoading(true);
      await apiClient.post(`/admin/logged-users/logout-session/${tokenId}`, {});
      setMessage({ type: 'success', text: 'Session successfully terminated' });
      await loadLoggedUsers();
    } catch (error) {
      setMessage({ type: 'error', text: 'Failed to terminate session' });
      console.error(error);
    } finally {
      setLoading(false);
    }
  };

  const cleanupExpiredTokens = async () => {
    try {
      setLoading(true);
      await apiClient.delete('/admin/logged-users/cleanup');
      setMessage({ type: 'success', text: 'Expired tokens cleaned up successfully' });
      setTimeout(() => setMessage(null), 3000);
      await loadLoggedUsers();
    } catch (error) {
      setMessage({ type: 'error', text: 'Failed to cleanup tokens' });
      console.error(error);
    } finally {
      setLoading(false);
    }
  };

  if (currentUser?.role !== 'SUPER_ADMIN') {
    return null;
  }

  return (
    <div className="bg-white rounded-lg shadow-md p-6">
      <h2 className="text-2xl font-bold mb-4 text-gray-800">👥 Active User Sessions</h2>
      
      {message && (
        <div className={`mb-4 p-4 rounded-lg ${
          message.type === 'success' 
            ? 'bg-green-100 border border-green-200 text-green-700' 
            : 'bg-red-100 border border-red-200 text-red-700'
        }`}>
          {message.text}
        </div>
      )}

      <div className="mb-6 space-y-3">
        <button
          onClick={loadLoggedUsers}
          disabled={loading}
          className="w-full bg-blue-600 text-white py-2 rounded-lg hover:bg-blue-700 transition disabled:bg-gray-400 font-medium"
        >
          {loading ? 'Loading...' : '🔄 Refresh Sessions'}
        </button>
        <button
          onClick={cleanupExpiredTokens}
          disabled={loading}
          className="w-full bg-orange-600 text-white py-2 rounded-lg hover:bg-orange-700 transition disabled:bg-gray-400 font-medium"
        >
          {loading ? 'Processing...' : '🧹 Cleanup Expired Tokens'}
        </button>
      </div>

      <div className="bg-gray-50 rounded-lg p-4">
        <h3 className="font-semibold text-gray-800 mb-4">
          Currently Logged In: {loggedUsers.length} {loggedUsers.length === 1 ? 'user' : 'users'}
        </h3>
        
        {loggedUsers.length === 0 ? (
          <p className="text-gray-500 text-sm">No users currently logged in.</p>
        ) : (
          <div className="space-y-3 max-h-96 overflow-y-auto">
            {loggedUsers.map((user) => (
              <div key={user.tokenId} className="bg-white p-4 rounded border border-gray-200">
                <div className="flex justify-between items-start">
                  <div className="flex-1">
                    <p className="font-semibold text-gray-900">
                      {user.firstName} {user.lastName}
                    </p>
                    <p className="text-sm text-gray-600">{user.email}</p>
                    <div className="text-xs text-gray-500 mt-2">
                      <p>Session Started: {new Date(user.issuedAt).toLocaleString()}</p>
                      <p>Expires: {new Date(user.expiresAt).toLocaleString()}</p>
                    </div>
                  </div>
                  <div className="flex gap-2 ml-4">
                    <button
                      onClick={() => forceLogoutSession(user.tokenId)}
                      disabled={loading}
                      className="px-3 py-1 text-sm bg-red-600 text-white rounded hover:bg-red-700 transition disabled:bg-gray-400"
                    >
                      Force Logout
                    </button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      <div className="mt-6 bg-blue-50 border border-blue-200 rounded-lg p-4">
        <h3 className="font-semibold text-blue-900 mb-2">💡 Session Management</h3>
        <ul className="text-sm text-blue-800 space-y-1 list-disc list-inside">
          <li>View all currently active user sessions in real-time</li>
          <li>Force logout individual sessions when needed</li>
          <li>Cleanup automatically removes expired token records</li>
        </ul>
      </div>
    </div>
  );
};
