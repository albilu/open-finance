import { describe, it, expect } from 'vitest';
import { registerSchema, loginSchema } from './authSchemas';

describe('authSchemas', () => {
  it('should validate a correct registration payload', () => {
    const data = {
      username: 'user_123',
      email: 'user@example.com',
      password: 'Aa1@abcd',
      confirmPassword: 'Aa1@abcd',
      masterPassword: 'Master1@3456',
      confirmMasterPassword: 'Master1@3456',
    };

    const parsed = registerSchema.safeParse(data);
    expect(parsed.success).toBe(true);
  });

  it('should reject mismatched passwords', () => {
    const data = {
      username: 'u',
      email: 'bad',
      password: 'Aa1@abcd',
      confirmPassword: 'different',
      masterPassword: 'Master1@3456',
      confirmMasterPassword: 'Master1@3456',
    };

    const result = registerSchema.safeParse(data);
    expect(result.success).toBe(false);
    if (!result.success) {
      // Expect at least one issue for confirmPassword
      expect(result.error.issues.some((i) => i.path.includes('confirmPassword'))).toBe(true);
    }
  });

  it('should enforce password complexity', () => {
    const data = {
      username: 'gooduser',
      email: 'ok@example.com',
      password: 'short',
      confirmPassword: 'short',
      masterPassword: 'shortmaster',
      confirmMasterPassword: 'shortmaster',
    };

    const result = registerSchema.safeParse(data);
    expect(result.success).toBe(false);
    if (!result.success) {
      // Should include password validation issues
      // Check for password path (minLength error)
      expect(result.error.issues.some((i) => i.path.includes('password'))).toBe(true);
      // Check for masterPassword path (minLength error)  
      expect(result.error.issues.some((i) => i.path.includes('masterPassword'))).toBe(true);
    }
  });

  it('should validate login schema', () => {
    const ok = loginSchema.safeParse({ username: 'x', password: 'y', masterPassword: 'z' });
    expect(ok.success).toBe(true);

    const bad = loginSchema.safeParse({ username: '', password: '', masterPassword: '' });
    expect(bad.success).toBe(false);
  });
});
