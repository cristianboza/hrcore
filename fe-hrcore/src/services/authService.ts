import apiClient from './apiClient';
import { useAuthStore } from '../store/authStore';
import type { AuthResponse, User, UserRole } from '../types';

interface LoginRedirectResponse {
  redirectUrl: string;
}

interface CallbackResponse {
  token: string;
  userId: string;
  email: string;
  firstName: string;
  lastName: string;
  role: UserRole;
  phone?: string;
  department?: string;
}

const authService = {
  async getLoginRedirectUrl(): Promise<string> {
    const response = await apiClient.get<LoginRedirectResponse>('/auth/login-redirect');
    return response.data.redirectUrl;
  },

  async handleCallback(code: string): Promise<CallbackResponse> {
    const response = await apiClient.post<CallbackResponse>('/auth/callback', { code });
    const { token, ...userData } = response.data;

    const user: User = {
      id: userData.userId,
      email: userData.email,
      firstName: userData.firstName,
      lastName: userData.lastName,
      role: userData.role,
      phone: userData.phone,
      department: userData.department,
    };

    useAuthStore.getState().setToken(token);
    useAuthStore.getState().setCurrentUser(user);
    localStorage.setItem('authToken', token);
    apiClient.defaults.headers.common['Authorization'] = `Bearer ${token}`;

    return response.data;
  },

  async logout(): Promise<void> {
    try {
      // Call backend logout endpoint with token in header
      const response = await apiClient.post<{ logoutUrl: string }>('/auth/logout');
      
      // Clear local state
      useAuthStore.getState().logout();
      localStorage.removeItem('authToken');
      delete apiClient.defaults.headers.common['Authorization'];
      
      // Redirect to Keycloak logout URL
      const logoutUrl = response.data.logoutUrl;
      if (logoutUrl) {
        window.location.href = logoutUrl;
      } else {
        window.location.href = '/';
      }
    } catch (error) {
      console.error('Logout failed:', error);
      
      // Clear local state anyway
      useAuthStore.getState().logout();
      localStorage.removeItem('authToken');
      delete apiClient.defaults.headers.common['Authorization'];
      
      // Try to get logout URL as fallback
      try {
        const logoutResponse = await apiClient.get<{ logoutUrl: string }>('/auth/logout-redirect');
        window.location.href = logoutResponse.data.logoutUrl;
      } catch (redirectError) {
        console.error('Failed to get logout redirect URL:', redirectError);
        window.location.href = '/';
      }
    }
  },

  async getCurrentUser(): Promise<AuthResponse> {
    const response = await apiClient.get<AuthResponse>('/auth/me');

    const user: User = {
      id: response.data.userId,
      email: response.data.email,
      firstName: response.data.firstName,
      lastName: response.data.lastName,
      role: response.data.role,
      phone: response.data.phone,
      department: response.data.department,
    };

    useAuthStore.getState().setCurrentUser(user);

    return response.data;
  },
};

export { authService };
