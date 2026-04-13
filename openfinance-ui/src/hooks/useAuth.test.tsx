import React from 'react';
import { render, fireEvent, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router';
import { describe, it, expect, vi, beforeEach } from 'vitest';

// Mocks
const mockNavigate = vi.fn();
vi.mock('react-router', async () => {
  const actual = await vi.importActual<any>('react-router');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

// Mock API client
const postMock = vi.fn();
vi.mock('@/services/apiClient', () => ({
  default: { post: (...args: any[]) => postMock(...args) },
}));

import { useRegister, useLogin, useLogout } from './useAuth';
import { AuthProvider } from '@/context/AuthContext';
import { VisibilityProvider } from '@/context/VisibilityContext';

function renderWithProviders(ui: React.ReactElement) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  // Include AuthProvider so hooks using useAuthContext work correctly
  const Wrapper = ({ children }: any) => (
    <MemoryRouter>
      <QueryClientProvider client={qc}>
        <AuthProvider>
          <VisibilityProvider>{children}</VisibilityProvider>
        </AuthProvider>
      </QueryClientProvider>
    </MemoryRouter>
  );

  return render(ui, { wrapper: Wrapper });
}

describe('useAuth hooks', () => {
  beforeEach(() => {
    vi.resetAllMocks();
    localStorage.clear();
    sessionStorage.clear();
  });

  it('should register user and navigate to /login on success', async () => {
    // Arrange: mock API response
    const user = { id: 1, username: 'alice' };
    postMock.mockResolvedValueOnce({ data: { data: user } });

    const TestComponent = () => {
      const mutation = useRegister();
      return (
        <button onClick={() => mutation.mutateAsync({ username: 'alice', email: 'a@b.c', password: 'P@ssw0rd!', masterPassword: 'MasterP@ss1' } as any)}>
          go
        </button>
      );
    };

    const { getByText } = renderWithProviders(<TestComponent />);

    // Act
    fireEvent.click(getByText('go'));

    // Assert: post was called and navigate used
    await waitFor(() => expect(postMock).toHaveBeenCalledWith('/auth/register', expect.any(Object)));
    await waitFor(() => expect(mockNavigate).toHaveBeenCalledWith('/login', expect.objectContaining({ state: expect.any(Object) })));
  });

  it('should login, store tokens and navigate to dashboard on success', async () => {
    // Arrange
    const response = { token: 'jwt-token', encryptionKey: 'enc-key', userId: 1, username: 'bob' };
    postMock.mockResolvedValueOnce({ data: response });

    const TestComponent = () => {
      const mutation = useLogin();
      return <button onClick={() => mutation.mutateAsync({ username: 'bob', password: 'pw', masterPassword: 'mpw' } as any)}>login</button>;
    };

    const { getByText } = renderWithProviders(<TestComponent />);

    // Act
    fireEvent.click(getByText('login'));

    // Assert - wait for mutation to complete
    await waitFor(() => expect(postMock).toHaveBeenCalledWith('/auth/login', expect.any(Object)));
    
    // Wait for storage to be updated (setAuth is called in onSuccess)
    // Check both sessionStorage and localStorage as either could be used depending on rememberMe
    await waitFor(() => {
      const tokenInLocal = localStorage.getItem('auth_token');
      const tokenInSession = sessionStorage.getItem('auth_token');
      expect(tokenInLocal || tokenInSession).toBe('jwt-token');
    }, { timeout: 5000 });
    
    await waitFor(() => {
      const encKey = sessionStorage.getItem('encryption_key');
      expect(encKey).toBe('enc-key');
    }, { timeout: 5000 });
  });

  it('should clear storage on login error', async () => {
    // Arrange: set stale tokens first
    localStorage.setItem('auth_token', 'stale');
    sessionStorage.setItem('encryption_key', 'stale');
    postMock.mockRejectedValueOnce(new Error('bad creds'));

    const TestComponent = () => {
      const mutation = useLogin();
      // Use mutate (callback-style) to avoid unhandled promise rejection in tests
      return <button onClick={() => mutation.mutate({ username: 'x' } as any)}>loginErr</button>;
    };

    const { getByText } = renderWithProviders(<TestComponent />);

    // Act
    fireEvent.click(getByText('loginErr'));

    // Assert: storage cleared
    await waitFor(() => expect(localStorage.getItem('auth_token')).toBeNull());
    await waitFor(() => expect(sessionStorage.getItem('encryption_key')).toBeNull());
  });

  it('useLogout should clear storage and navigate to /login', async () => {
    localStorage.setItem('auth_token', 'x');
    sessionStorage.setItem('encryption_key', 'y');

    const TestComponent = () => {
      const logout = useLogout();
      return <button onClick={() => logout()}>logout</button>;
    };

    const { getByText } = renderWithProviders(<TestComponent />);

    // Act
    fireEvent.click(getByText('logout'));

    // Assert
    expect(localStorage.getItem('auth_token')).toBeNull();
    expect(sessionStorage.getItem('encryption_key')).toBeNull();
    expect(mockNavigate).toHaveBeenCalledWith('/login');
  });
});
