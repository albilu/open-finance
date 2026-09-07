/**
 * Tests for the useTranches hook (Task 8).
 * Mirrors the apiClient-mocking patterns of useTransactions.test.tsx.
 */
import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import React from 'react';
import { useTranches } from './useTranches';
import apiClient from '@/services/apiClient';

vi.mock('@/services/apiClient');
const mockedApiClient = apiClient as any;

const mockSessionStorage = {
  getItem: vi.fn(),
  setItem: vi.fn(),
  removeItem: vi.fn(),
  clear: vi.fn(),
  key: vi.fn(),
  length: 0,
};
Object.defineProperty(window, 'sessionStorage', {
  value: mockSessionStorage,
});

const mockTranches = [
  {
    id: 1,
    liabilityId: 5,
    trancheNo: 1,
    plannedAmount: 100000,
    drawnAmount: 100000,
    remaining: 40000,
    status: 'DRAWN',
    currency: 'USD',
  },
  {
    id: 2,
    liabilityId: 5,
    trancheNo: 2,
    plannedAmount: 50000,
    drawnAmount: null,
    remaining: 50000,
    status: 'PLANNED',
    currency: 'USD',
  },
];

describe('useTranches', () => {
  let queryClient: QueryClient;

  beforeEach(() => {
    queryClient = new QueryClient({
      defaultOptions: {
        queries: {
          retry: false,
        },
      },
    });
    vi.clearAllMocks();
    mockSessionStorage.getItem.mockReturnValue('test-encryption-key');
  });

  const wrapper = ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );

  it('fetches tranches from the liability tranches endpoint', async () => {
    mockedApiClient.get.mockResolvedValue({ data: mockTranches });

    const { result } = renderHook(() => useTranches(5), { wrapper });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(mockedApiClient.get).toHaveBeenCalledWith('/liabilities/5/tranches', {
      headers: {
        'X-Encryption-Session': 'test-encryption-key',
      },
    });
    expect(result.current.data).toEqual(mockTranches);
  });

  it('does not fetch when the liability id is null', async () => {
    const { result } = renderHook(() => useTranches(null), { wrapper });

    expect(result.current.isLoading).toBe(false);
    expect(mockedApiClient.get).not.toHaveBeenCalled();
  });

  it('exposes the error when the request fails', async () => {
    mockedApiClient.get.mockRejectedValue(new Error('boom'));

    const { result } = renderHook(() => useTranches(5), { wrapper });

    await waitFor(() => {
      expect(result.current.isError).toBe(true);
    });
    expect(result.current.error).toBeInstanceOf(Error);
  });
});
