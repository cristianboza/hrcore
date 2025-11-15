import { useState } from 'react';
import { authService } from '../services/authService';

interface LoginCredential {
  username: string;
  password: string;
  role: string;
}

const credentials: LoginCredential[] = [
  { username: 'admin', password: 'admin123', role: '🔐 Super Admin' },
  { username: 'manager', password: 'manager123', role: '📋 Manager' },
  { username: 'employee1', password: 'employee123', role: '👤 Employee 1' },
  { username: 'employee2', password: 'employee123', role: '👤 Employee 2' },
];

export const LoginPage = () => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleLogin = async () => {
    try {
      setLoading(true);
      setError(null);
      const redirectUrl = await authService.getLoginRedirectUrl();
      window.location.href = redirectUrl;
    } catch (err) {
      setError('Failed to initiate login. Please try again.');
      setLoading(false);
      console.error(err);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-100">
      <div className="bg-white p-8 rounded-lg shadow-md w-full max-w-md">
        <h1 className="text-3xl font-bold mb-6 text-center text-blue-600">HR Core</h1>
        
        {error && (
          <div className="mb-4 p-4 bg-red-100 text-red-700 rounded">
            {error}
          </div>
        )}
        
        <div className="space-y-4">
          <button
            onClick={handleLogin}
            disabled={loading}
            className="w-full bg-blue-600 text-white py-2 rounded-lg hover:bg-blue-700 transition disabled:bg-gray-400 font-medium"
          >
            {loading ? 'Redirecting...' : 'Login with Keycloak'}
          </button>
        </div>

        <div className="mt-6 p-4 bg-blue-50 rounded-lg text-sm text-gray-700">
          <p className="font-semibold mb-3 text-blue-900">Test Credentials:</p>
          <div className="space-y-2">
            {credentials.map((cred) => (
              <div key={cred.username} className="text-xs bg-white p-2 rounded border border-blue-200">
                <p className="font-medium text-gray-800">{cred.role}</p>
                <p className="text-gray-600">User: <code className="bg-gray-100 px-1 rounded">{cred.username}</code></p>
                <p className="text-gray-600">Pass: <code className="bg-gray-100 px-1 rounded">{cred.password}</code></p>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};


