import axios, {
  AxiosInstance,
  AxiosResponse,
  AxiosError,
  InternalAxiosRequestConfig,
} from 'axios';
import { useAuthStore } from '../store/authStore';
import type { ApiError } from '../types';

const API_URL: string = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';

const apiClient: AxiosInstance = axios.create({
  baseURL: API_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Add interceptor to include token in all requests
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig): InternalAxiosRequestConfig => {
    const token: string | null = useAuthStore.getState().token;
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error: AxiosError): Promise<AxiosError> => {
    return Promise.reject(error);
  }
);

// Handle 401 responses by clearing auth
apiClient.interceptors.response.use(
  (response: AxiosResponse): AxiosResponse => response,
  (error: AxiosError<ApiError>): Promise<never> => {
    if (error.response?.status === 401) {
      useAuthStore.getState().logout();
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default apiClient;



