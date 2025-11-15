import apiClient from './apiClient';

export interface User {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  phone?: string;
  department?: string;
  role: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface CreateUserRequest {
  firstName: string;
  lastName: string;
  email: string;
  phone?: string;
  department?: string;
  role: string;
}

export const userService = {
  getAllUsers: async (): Promise<User[]> => {
    const response = await apiClient.get('/users');
    return response.data;
  },

  getUserById: async (id: number): Promise<User> => {
    const response = await apiClient.get(`/users/${id}`);
    return response.data;
  },

  createUser: async (user: CreateUserRequest): Promise<User> => {
    const response = await apiClient.post('/users', user);
    return response.data;
  },

  updateUser: async (id: number, user: Partial<User>): Promise<User> => {
    const response = await apiClient.put(`/users/${id}`, user);
    return response.data;
  },

  deleteUser: async (id: number): Promise<void> => {
    await apiClient.delete(`/users/${id}`);
  },
};

