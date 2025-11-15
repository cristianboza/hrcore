import Keycloak from 'keycloak-js';
import type { AuthResponse } from '../types';
import apiClient from './apiClient';

interface KeycloakConfig {
  url: string;
  realm: string;
  clientId: string;
}

class KeycloakService {
  private keycloak: Keycloak.KeycloakInstance | null = null;
  private config: KeycloakConfig;

  constructor() {
    this.config = {
      url: import.meta.env.VITE_KEYCLOAK_URL || 'http://localhost:9080',
      realm: import.meta.env.VITE_KEYCLOAK_REALM || 'hrcore',
      clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID || 'hrcore-app',
    };
  }

  async initialize(): Promise<boolean> {
    try {
      this.keycloak = new Keycloak({
        url: this.config.url,
        realm: this.config.realm,
        clientId: this.config.clientId,
      });

      const authenticated = await this.keycloak.init({
        onLoad: 'login-required',
        checkLoginIframe: true,
        flow: 'standard',
        pkceMethod: 'S256',
      });

      if (authenticated && this.keycloak.token) {
        await this.exchangeToken(this.keycloak.token);
      }

      return authenticated;
    } catch (error) {
      console.error('Keycloak initialization error:', error);
      return false;
    }
  }

  private async exchangeToken(keycloakToken: string): Promise<AuthResponse | null> {
    try {
      const response = await apiClient.post<AuthResponse>('/auth/login', {
        token: keycloakToken,
      });
      
      if (response.data.token) {
        localStorage.setItem('authToken', response.data.token);
        apiClient.defaults.headers.common['Authorization'] = `Bearer ${response.data.token}`;
      }

      return response.data;
    } catch (error) {
      console.error('Token exchange error:', error);
      return null;
    }
  }

  login(): void {
    if (this.keycloak) {
      this.keycloak.login();
    }
  }

  logout(): void {
    if (this.keycloak) {
      this.keycloak.logout({
        redirectUri: `${window.location.origin}/`,
      });
    }
  }

  getToken(): string | undefined {
    return this.keycloak?.token;
  }

  isAuthenticated(): boolean {
    return this.keycloak?.authenticated ?? false;
  }

  getUsername(): string | undefined {
    return this.keycloak?.tokenParsed?.preferred_username;
  }

  getEmail(): string | undefined {
    return this.keycloak?.tokenParsed?.email;
  }

  getRoles(): string[] {
    return this.keycloak?.tokenParsed?.realm_access?.roles ?? [];
  }

  hasRole(role: string): boolean {
    return this.getRoles().includes(role);
  }

  refreshToken(): Promise<void> {
    return new Promise((resolve, reject) => {
      if (this.keycloak) {
        this.keycloak
          .updateToken(30)
          .then(() => resolve())
          .catch(() => reject(new Error('Failed to refresh token')));
      } else {
        reject(new Error('Keycloak not initialized'));
      }
    });
  }
}

export const keycloakService = new KeycloakService();
