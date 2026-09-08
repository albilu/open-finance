import { QueryClient } from '@tanstack/react-query';
import { describe, beforeEach, expect, it } from 'vitest';
import { useAuthContext } from '@/context/AuthContext';
import {
  fireEvent,
  mockAuthentication,
  renderWithProviders,
  screen,
  waitFor,
} from '@/test/test-utils';

function SessionControls() {
  const { clearAuth, setAuth, user } = useAuthContext();
  return (
    <>
      <span>{user?.username ?? 'signed out'}</span>
      <button onClick={clearAuth}>Sign out</button>
      <button
        onClick={() =>
          setAuth({ id: 2, username: 'second', email: '', createdAt: '' }, 'second-token')
        }
      >
        Switch account
      </button>
    </>
  );
}

describe('Private data at authentication boundaries', () => {
  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
    mockAuthentication();
  });

  it.each(['Sign out', 'Switch account'])(
    '%s removes previous private data and pending results',
    async action => {
      const client = new QueryClient({
        defaultOptions: { queries: { retry: false, gcTime: Infinity } },
      });
      renderWithProviders(<SessionControls />, { queryClient: client });
      await screen.findByText('testuser');
      client.setQueryData(['accounts', 'search'], ['Private account']);
      expect(client.getQueryData(['accounts', 'search'])).toEqual(['Private account']);
      let finish: (value: string[]) => void = () => {};
      const pending = client
        .fetchQuery({
          queryKey: ['transactions', 'search'],
          queryFn: () =>
            new Promise<string[]>(resolve => {
              finish = resolve;
            }),
        })
        .catch(() => undefined);

      fireEvent.click(screen.getByRole('button', { name: action }));
      finish(['Private transaction']);
      await pending;

      await waitFor(() => {
        expect(client.getQueryData(['accounts', 'search'])).toBeUndefined();
        expect(client.getQueryData(['transactions', 'search'])).toBeUndefined();
      });
    }
  );
});
