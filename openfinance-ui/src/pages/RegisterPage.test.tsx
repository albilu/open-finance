import '@testing-library/jest-dom';
import { screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderWithProviders } from '@/test/test-utils';

// Mock useRegister hook
const mutateAsync = vi.fn();
const useRegisterMock = vi.fn(() => ({ mutateAsync, isPending: false, isError: false, isSuccess: false, error: null }));
vi.mock('@/hooks/useAuth', () => ({ useRegister: () => useRegisterMock() }));

import RegisterPage from './RegisterPage';

function renderPage() {
  return renderWithProviders(<RegisterPage />);
}

describe('RegisterPage', () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });

  it('should display validation errors and prevent submission', async () => {
    await renderPage();

    // Submit without filling fields - use actual translated text "Create Account"
    const submitButton = screen.getByRole('button', { name: /create account/i });
    fireEvent.click(submitButton);

    // Expect multiple errors to be present
    await waitFor(() => expect(screen.getByText('Username must be at least 3 characters')).toBeInTheDocument());
    expect(screen.getByText('Email is required')).toBeInTheDocument();
    expect(screen.getByText('Password must be at least 8 characters')).toBeInTheDocument();

    expect(mutateAsync).not.toHaveBeenCalled();
  });

  it('should submit valid form and call register mutation', async () => {
    mutateAsync.mockResolvedValueOnce({ id: 1, username: 'ok' });
    await renderPage();

    // Use translated placeholders
    fireEvent.input(screen.getByPlaceholderText('Choose a username'), { target: { value: 'alice' } });
    fireEvent.input(screen.getByPlaceholderText(/enter.*email/i), { target: { value: 'alice@example.com' } });
    // There are two password inputs with same placeholder; target them by index
    const pwAll = screen.getAllByPlaceholderText('••••••••');
    fireEvent.input(pwAll[0], { target: { value: 'Aa1@abcd' } });
    if (pwAll.length >= 2) fireEvent.input(pwAll[1], { target: { value: 'Aa1@abcd' } });

    // Master password
    const masterAll = screen.getAllByPlaceholderText('••••••••••••');
    if (masterAll.length >= 2) {
      fireEvent.input(masterAll[0], { target: { value: 'Master1@3456' } });
      fireEvent.input(masterAll[1], { target: { value: 'Master1@3456' } });
    } else if (masterAll.length === 1) {
      fireEvent.input(masterAll[0], { target: { value: 'Master1@3456' } });
    }

    fireEvent.click(screen.getByRole('button', { name: /create account/i }));

    await waitFor(() => expect(mutateAsync).toHaveBeenCalled());
  });

  it('should show API error message when mutation has error', async () => {
    // Simulate error state from useRegister
    const apiError = { response: { data: { message: 'Email already in use' } } } as any;
    useRegisterMock.mockReturnValue({ mutateAsync, isPending: false, isError: true, isSuccess: false, error: apiError });

    // Render the page with error state
    renderPage();
    
    // Check that error message is displayed
    const errorMessage = await screen.findByText('Email already in use');
    expect(errorMessage).toBeInTheDocument();
  });
});
