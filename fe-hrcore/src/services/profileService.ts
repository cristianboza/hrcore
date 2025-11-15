import apiClient from './apiClient';
import { User } from './userService';

export interface PermissionsDto {
  canViewAll: boolean;
  canEdit: boolean;
  canDelete: boolean;
}

export const profileService = {
  /**
   * Get all profiles (with role-based filtering)
   */
  getAllProfiles: async (): Promise<User[]> => {
    const response = await apiClient.get('/profiles');
    return response.data;
  },

  /**
   * Get a specific profile
   */
  getProfile: async (userId: number): Promise<User> => {
    const response = await apiClient.get(`/profiles/${userId}`);
    return response.data;
  },

  /**
   * Update a profile
   */
  updateProfile: async (userId: number, updateData: Partial<User>): Promise<User> => {
    const response = await apiClient.put(`/profiles/${userId}`, updateData);
    return response.data;
  },

  /**
   * Delete a profile
   */
  deleteProfile: async (userId: number): Promise<void> => {
    await apiClient.delete(`/profiles/${userId}`);
  },

  /**
   * Get permissions for a profile
   */
  getProfilePermissions: async (userId: number): Promise<PermissionsDto> => {
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
};

