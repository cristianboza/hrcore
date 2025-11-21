import apiClient from './apiClient';

export interface User {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  phone?: string;
  department?: string;
  role: string;
  managerId?: string;
  manager?: NamedUser;
  createdAt?: string;
  updatedAt?: string;
}

export interface NamedUser {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
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

  getUserById: async (id: string): Promise<User> => {
    const response = await apiClient.get(`/users/${id}`);
    return response.data;
  },

  createUser: async (user: CreateUserRequest): Promise<User> => {
    const response = await apiClient.post('/users', user);
    return response.data;
  },

  updateUser: async (id: string, user: Partial<User>): Promise<User> => {
    const response = await apiClient.put(`/users/${id}`, user);
    return response.data;
  },

  deleteUser: async (id: string): Promise<void> => {
    await apiClient.delete(`/users/${id}`);
  },
};

