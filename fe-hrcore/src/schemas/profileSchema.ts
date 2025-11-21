import { z } from 'zod';

export const profileEditSchema = z.object({
  firstName: z
    .string()
    .min(1, 'First name is required')
    .min(2, 'First name must be at least 2 characters')
    .max(50, 'First name must not exceed 50 characters')
    .regex(/^[a-zA-Z\s-']+$/, 'First name can only contain letters, spaces, hyphens, and apostrophes'),
  
  lastName: z
    .string()
    .min(1, 'Last name is required')
    .min(2, 'Last name must be at least 2 characters')
    .max(50, 'Last name must not exceed 50 characters')
    .regex(/^[a-zA-Z\s-']+$/, 'Last name can only contain letters, spaces, hyphens, and apostrophes'),
  
  email: z
    .string()
    .email('Invalid email address'),
  
  phone: z
    .string()
    .optional()
    .refine(
      (val) => !val || /^[\d\s()+-]+$/.test(val),
      'Phone number can only contain digits, spaces, and +-() characters'
    ),
  
  department: z
    .string()
    .max(100, 'Department must not exceed 100 characters')
    .optional(),
  
  role: z.enum(['EMPLOYEE', 'MANAGER', 'SUPER_ADMIN']),
  
  managerId: z
    .string()
    .uuid()
    .nullable()
    .optional(),
});

export type ProfileEditFormData = z.infer<typeof profileEditSchema>;
