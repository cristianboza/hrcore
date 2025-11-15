/**
 * Component Props and Event Types
 */

import React from 'react';
import type { User } from './auth';

export interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  loading?: boolean;
  variant?: 'primary' | 'secondary' | 'danger' | 'success';
}

export interface FormInputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
  helperText?: string;
}

export interface FormSelectProps extends React.SelectHTMLAttributes<HTMLSelectElement> {
  label?: string;
  error?: string;
  options: Array<{ value: string | number; label: string }>;
}

export interface CardProps extends React.HTMLAttributes<HTMLDivElement> {
  title?: string;
  subtitle?: string;
  loading?: boolean;
}

export interface ModalProps extends React.HTMLAttributes<HTMLDivElement> {
  isOpen: boolean;
  onClose: () => void;
  title?: string;
  footer?: React.ReactNode;
}

export interface BaseComponentProps {
  className?: string;
  children?: React.ReactNode;
}

export interface AuthContextType {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  error: string | null;
  login: (email: string) => Promise<void>;
  logout: () => Promise<void>;
}

export interface ToastMessage {
  id: string;
  type: 'success' | 'error' | 'info' | 'warning';
  message: string;
  duration?: number;
}

export interface ConfirmDialogProps {
  isOpen: boolean;
  title: string;
  message: string;
  onConfirm: () => void;
  onCancel: () => void;
  loading?: boolean;
}

export type EventHandler<T = HTMLElement> = (
  event: React.SyntheticEvent<T>
) => void;

export type ClickEventHandler = (
  event: React.MouseEvent<HTMLButtonElement>
) => void;

export type ChangeEventHandler = (
  event: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>
) => void;
