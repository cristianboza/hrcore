import apiClient from './apiClient';
import { useAuthStore } from '../store/authStore';
import type { AuthResponse, User, UserRole } from '../types';

interface LoginRedirectResponse {
  redirectUrl: string;
}

interface CallbackResponse {
  token: string;
  userId: number;
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
    const token = localStorage.getItem('authToken');
    try {
      if (token) {
        await apiClient.post('/auth/logout-keycloak', { token });
      } else {
        await apiClient.post('/auth/logout');
      }
    } catch (error) {
      console.error('Logout failed:', error);
    } finally {
      useAuthStore.getState().logout();
      localStorage.removeItem('authToken');
      delete apiClient.defaults.headers.common['Authorization'];
      
      try {
        const logoutResponse = await apiClient.get<{ logoutUrl: string }>('/auth/logout-redirect');
        const logoutUrl = logoutResponse.data.logoutUrl;
        window.location.href = logoutUrl;
      } catch (error) {
        console.error('Failed to get logout redirect URL:', error);
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
