/**
 * Zod validation schemas for authentication forms
 */
import { z } from 'zod';

/**
 * Password strength requirements:
 * - Minimum 8 characters
 * - At least one uppercase letter
 * - At least one lowercase letter  
 * - At least one number
 * - At least one special character
 */
const passwordSchema = z
  .string()
  .min(8, 'validation:password.minLength')
  .regex(/[A-Z]/, 'validation:password.uppercase')
  .regex(/[a-z]/, 'validation:password.lowercase')
  .regex(/[0-9]/, 'validation:password.number')
  .regex(/[^A-Za-z0-9]/, 'validation:password.special');

/**
 * Master password requirements (more strict):
 * - Minimum 12 characters for master password (higher security)
 */
const masterPasswordSchema = z
  .string()
  .min(12, 'validation:masterPassword.minLength')
  .regex(/[A-Z]/, 'validation:masterPassword.uppercase')
  .regex(/[a-z]/, 'validation:masterPassword.lowercase')
  .regex(/[0-9]/, 'validation:masterPassword.number')
  .regex(/[^A-Za-z0-9]/, 'validation:masterPassword.special');

/**
 * Registration form validation schema
 */
export const registerSchema = z
  .object({
    username: z
      .string()
      .trim()
      .min(3, 'validation:username.minLength')
      .max(50, 'validation:username.maxLength')
      .regex(/^[a-zA-Z0-9_-]+$/, {
        message: 'validation:username.pattern',
      }),
    email: z
      .string()
      .trim()
      .min(1, 'validation:email.required')
      .email('validation:email.invalid'),
    password: passwordSchema,
    confirmPassword: z
      .string()
      .min(1, 'validation:password.confirm'),
    masterPassword: masterPasswordSchema,
    confirmMasterPassword: z
      .string()
      .min(1, 'validation:masterPassword.confirm'),
    baseCurrency: z.string().length(3).optional(),
  })
  .refine((data) => data.password === data.confirmPassword, {
    message: 'validation:password.mismatch',
    path: ['confirmPassword'],
  })
  .refine((data) => data.masterPassword === data.confirmMasterPassword, {
    message: 'validation:masterPassword.mismatch',
    path: ['confirmMasterPassword'],
  });

/**
 * Login form validation schema
 */
export const loginSchema = z.object({
  username: z
    .string()
    .min(1, 'validation:username.required'),
  password: z
    .string()
    .min(1, 'validation:password.required'),
  masterPassword: z
    .string()
    .min(1, 'validation:masterPassword.required'),
  rememberMe: z.boolean().default(false),
});

/**
 * Profile update form validation schema
 * Note: Password change has been moved to Security Settings
 */
export const updateProfileSchema = z
  .object({
    email: z
      .string()
      .trim()
      .email('validation:email.invalid')
      .optional()
      .or(z.literal('')),
    currentPassword: z
      .string()
      .min(1, 'validation:password.required'),
  })
  .refine(
    (data) => {
      // At least email must be provided
      const hasEmail = data.email && data.email.length > 0;
      return hasEmail;
    },
    {
      message: 'validation:email.required',
      path: ['email'],
    }
  );

/**
 * TypeScript types inferred from schemas
 */
export type RegisterFormData = z.infer<typeof registerSchema>;
export type LoginFormData = z.infer<typeof loginSchema>;
export type UpdateProfileFormData = z.infer<typeof updateProfileSchema>;
