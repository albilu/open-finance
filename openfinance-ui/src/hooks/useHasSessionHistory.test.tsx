import { renderHook, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { useHasSessionHistory } from './useHasSessionHistory';
import { historyService } from '@/services/historyService';
import { createTestQueryClient } from '@/test/test-utils';
import { QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider } from '@/context/AuthContext';
import { VisibilityProvider } from '@/context/VisibilityContext';
import React from 'react';

vi.mock('@/services/historyService', () => ({
  historyService: {
    getHistory: vi.fn(),
  },
}));

const mockedGetHistory = vi.mocked(historyService.getHistory);

function createWrapper(sessionStartTime: string | null) {
  // Pre-seed sessionStorage so AuthProvider initialises with a given sessionStartTime
  if (sessionStartTime) {
    sessionStorage.setItem('session_start_time', sessionStartTime);
  } else {
    sessionStorage.removeItem('session_start_time');
  }

  const queryClient = createTestQueryClient();
  return ({ children }: { children: React.ReactNode }) => (
    <VisibilityProvider>
      <AuthProvider>
        <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
      </AuthProvider>
    </VisibilityProvider>
  );
}

describe('useHasSessionHistory', () => {
  beforeEach(() => {
    sessionStorage.clear();
    localStorage.clear();
    vi.clearAllMocks();
  });

  it('returns false immediately when sessionStartTime is null (no query fired)', () => {
    const { result } = renderHook(() => useHasSessionHistory(), {
      wrapper: createWrapper(null),
    });
    expect(result.current).toBe(false);
    expect(mockedGetHistory).not.toHaveBeenCalled();
  });

  it('returns false when totalElements is 0', async () => {
    mockedGetHistory.mockResolvedValue({
      content: [],
      totalElements: 0,
      totalPages: 0,
      size: 1,
      number: 0,
    } as any);

    const { result } = renderHook(() => useHasSessionHistory(), {
      wrapper: createWrapper('2026-01-01T00:00:00.000Z'),
    });

    await waitFor(() => {
      expect(mockedGetHistory).toHaveBeenCalled();
      expect(result.current).toBe(false);
    });
  });

  it('returns true when totalElements is 1 or more', async () => {
    mockedGetHistory.mockResolvedValue({
      content: [{}],
      totalElements: 1,
      totalPages: 1,
      size: 1,
      number: 0,
    } as any);

    const { result } = renderHook(() => useHasSessionHistory(), {
      wrapper: createWrapper('2026-01-01T00:00:00.000Z'),
    });

    await waitFor(() => expect(result.current).toBe(true));
    expect(mockedGetHistory).toHaveBeenCalledWith(0, 1, undefined, '2026-01-01T00:00:00.000Z');
  });
});
