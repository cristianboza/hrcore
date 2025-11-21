import apiClient from './apiClient';
import { User } from './userService';

export interface PermissionsDto {
  canViewAll: boolean;
  canEdit: boolean;
  canDelete: boolean;
  canGiveFeedback?: boolean;
  canRequestAbsence?: boolean;
}

export interface ProfileSearchFilters {
  search?: string;
  role?: string;
  managerId?: string;
  department?: string;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDirection?: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

export const profileService = {
  /**
   * Search profiles with filters and pagination
   */
  searchProfiles: async (filters?: ProfileSearchFilters): Promise<PageResponse<User>> => {
    const response = await apiClient.post('/profiles/search', {
      search: filters?.search,
      role: filters?.role,
      managerId: filters?.managerId,
      department: filters?.department,
      page: filters?.page || 0,
      size: filters?.size || 10,
      sortBy: filters?.sortBy || 'lastName',
      sortDirection: filters?.sortDirection || 'ASC',
    });
    return response.data;
  },

  /**
   * Get a specific profile
   */
  getProfile: async (userId: string): Promise<User> => {
    const response = await apiClient.get(`/profiles/${userId}`);
    return response.data;
  },

  /**
   * Update a profile
   */
  updateProfile: async (userId: string, updateData: Partial<User>): Promise<User> => {
    const response = await apiClient.put(`/profiles/${userId}`, updateData);
    return response.data;
  },

  /**
   * Delete a profile (Manager+ only)
   */
  deleteProfile: async (userId: string): Promise<void> => {
    await apiClient.delete(`/profiles/${userId}`);
  },

  /**
   * Get permissions for a profile
   */
  getProfilePermissions: async (userId: string): Promise<PermissionsDto> => {
    const response = await apiClient.get(`/profiles/${userId}/permissions`);
    return response.data;
  },

  /**
   * Get current authenticated user
   */
  getCurrentUser: async (): Promise<User> => {
    const response = await apiClient.get('/profiles/me');
    return response.data;
  },

  /**
   * Get direct reports for a user (employees who report to them)
   */
  getDirectReports: async (userId: string): Promise<User[]> => {
    const response = await apiClient.get(`/profiles/${userId}/direct-reports`);
    return response.data;
  },

  /**
   * Get available managers (users who can be assigned as managers)
   */
  getAvailableManagers: async (): Promise<User[]> => {
    const response = await apiClient.get('/profiles/available-managers');
    return response.data;
  },

  /**
   * Get the manager of a specific user
   */
  getManager: async (userId: string): Promise<User | null> => {
    try {
      const response = await apiClient.get(`/profiles/${userId}/manager`);
      return response.data;
    } catch (error: any) {
      if (error.response?.status === 404) {
        return null;
      }
      throw error;
    }
  },

  /**
   * Assign a manager to a user (Manager+ only)
   */
  assignManager: async (userId: string, managerId: string): Promise<void> => {
    await apiClient.put(`/profiles/${userId}/manager/${managerId}`);
  },

  /**
   * Remove manager from a user (assign 0 as managerId)
   */
  removeManager: async (userId: string): Promise<void> => {
    await apiClient.put(`/profiles/${userId}/manager/00000000-0000-0000-0000-000000000000`);
  },

  /**
   * Create a new profile (Manager+ only)
   */
  createProfile: async (userData: {
    email: string;
    firstName: string;
    lastName: string;
    password: string;
    phone?: string;
    department?: string;
    role?: string;
    managerId?: string;
  }): Promise<User> => {
    const response = await apiClient.post('/profiles', userData);
    return response.data;
  },


};

