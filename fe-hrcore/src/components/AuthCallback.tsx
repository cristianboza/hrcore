import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { authService } from '../services/authService';

export const AuthCallback = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const handleCallback = async () => {
      try {
        // Try to get code from query params
        let code = searchParams.get('code');
        
        // If not in query params, try to get from hash
        if (!code) {
          const hash = window.location.hash;
          const params = new URLSearchParams(hash.substring(1));
            console.log(JSON.stringify(params));
            code = params.get('code');
        }
        
        if (!code) {
          setError('No authorization code provided');
          return;
        }

        await authService.handleCallback(code);
        navigate('/');
      } catch (err) {
        setError('Authentication failed. Please try again.');
        console.error(err);
        setTimeout(() => navigate('/login'), 2000);
      }
    };

    handleCallback();
  }, [searchParams, navigate]);

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-100">
      <div className="bg-white p-8 rounded-lg shadow-md text-center">
        {error ? (
          <>
            <h1 className="text-2xl font-bold text-red-600 mb-4">Authentication Error</h1>
            <p className="text-gray-600">{error}</p>
            <p className="text-sm text-gray-500 mt-2">Redirecting to login...</p>
            <button
              onClick={() => navigate('/login')}
              className="mt-4 bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700"
            >
              Back to Login
            </button>
          </>
        ) : (
          <>
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
            <p className="text-gray-600">Processing login...</p>
          </>
        )}
      </div>
    </div>
  );
};
