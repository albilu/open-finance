import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import React from 'react';
import {
  usePayees,
  useActivePayees,
  usePayeesByCategory,
  usePayeeSearch,
  useSystemPayees,
  useCustomPayees,
  usePayeeCategories,
  useFindOrCreatePayee,
  useCreatePayee,
  useUpdatePayee,
  useTogglePayeeActive,
  useDeletePayee,
} from './usePayees';
import apiClient from '@/services/apiClient';

vi.mock('@/services/apiClient');
const mockedApiClient = apiClient as any;

describe('usePayees hooks', () => {
  let queryClient: QueryClient;

  beforeEach(() => {
    queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });
    vi.clearAllMocks();
  });

  const wrapper = ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );

  const mockPayee = {
    id: 1,
    name: 'Amazon',
    category: 'Shopping',
    isActive: true,
    isSystem: false,
  };

  // ── Query hooks ──────────────────────────────────────────────────────────
  describe('usePayees', () => {
    it('should fetch all payees', async () => {
      mockedApiClient.get.mockResolvedValue({ data: [mockPayee] });

      const { result } = renderHook(() => usePayees(), { wrapper });

      await waitFor(() => expect(result.current.isSuccess).toBe(true));
      expect(result.current.data).toEqual([mockPayee]);
      expect(mockedApiClient.get).toHaveBeenCalledWith('/payees');
    });
  });

  describe('useActivePayees', () => {
    it('should fetch active payees', async () => {
      mockedApiClient.get.mockResolvedValue({ data: [mockPayee] });

      const { result } = renderHook(() => useActivePayees(), { wrapper });

      await waitFor(() => expect(result.current.isSuccess).toBe(true));
      expect(mockedApiClient.get).toHaveBeenCalledWith('/payees/active');
    });
  });

  describe('usePayeesByCategory', () => {
    it('should fetch payees by category', async () => {
      mockedApiClient.get.mockResolvedValue({ data: [mockPayee] });

      const { result } = renderHook(() => usePayeesByCategory('Shopping'), { wrapper });

      await waitFor(() => expect(result.current.isSuccess).toBe(true));
      expect(mockedApiClient.get).toHaveBeenCalledWith('/payees/category/Shopping');
    });

    it('should be disabled when category is empty', () => {
      const { result } = renderHook(() => usePayeesByCategory(''), { wrapper });
      expect(result.current.isFetched).toBe(false);
    });
  });

  describe('usePayeeSearch', () => {
    it('should search payees when query has at least 2 characters', async () => {
      mockedApiClient.get.mockResolvedValue({ data: [mockPayee] });

      const { result } = renderHook(() => usePayeeSearch('Am'), { wrapper });

      await waitFor(() => expect(result.current.isSuccess).toBe(true));
      expect(mockedApiClient.get).toHaveBeenCalledWith('/payees/search', {
        params: { query: 'Am' },
      });
    });

    it('should be disabled when query is too short', () => {
      const { result } = renderHook(() => usePayeeSearch('A'), { wrapper });
      expect(result.current.isFetched).toBe(false);
    });
  });

  describe('useSystemPayees', () => {
    it('should fetch system payees', async () => {
      mockedApiClient.get.mockResolvedValue({ data: [mockPayee] });

      const { result } = renderHook(() => useSystemPayees(), { wrapper });

      await waitFor(() => expect(result.current.isSuccess).toBe(true));
      expect(mockedApiClient.get).toHaveBeenCalledWith('/payees/system');
    });
  });

  describe('useCustomPayees', () => {
    it('should fetch custom payees', async () => {
      mockedApiClient.get.mockResolvedValue({ data: [mockPayee] });

      const { result } = renderHook(() => useCustomPayees(), { wrapper });

      await waitFor(() => expect(result.current.isSuccess).toBe(true));
      expect(mockedApiClient.get).toHaveBeenCalledWith('/payees/custom');
    });
  });

  describe('usePayeeCategories', () => {
    it('should fetch payee categories', async () => {
      mockedApiClient.get.mockResolvedValue({ data: ['Shopping', 'Food', 'Transport'] });

      const { result } = renderHook(() => usePayeeCategories(), { wrapper });

      await waitFor(() => expect(result.current.isSuccess).toBe(true));
      expect(result.current.data).toEqual(['Shopping', 'Food', 'Transport']);
    });
  });

  // ── Mutation hooks ─────────────────────────────────────────────────────────
  describe('useFindOrCreatePayee', () => {
    it('should find or create a payee by name', async () => {
      mockedApiClient.post.mockResolvedValue({ data: mockPayee });

      const { result } = renderHook(() => useFindOrCreatePayee(), { wrapper });

      result.current.mutate('Amazon');

      await waitFor(() => expect(result.current.isSuccess).toBe(true));

      expect(mockedApiClient.post).toHaveBeenCalledWith('/payees/find-or-create', null, {
        params: { name: 'Amazon' },
      });
    });
  });

  describe('useCreatePayee', () => {
    it('should create a payee and invalidate queries', async () => {
      mockedApiClient.post.mockResolvedValue({ data: mockPayee });
      const invalidateSpy = vi.spyOn(queryClient, 'invalidateQueries');

      const { result } = renderHook(() => useCreatePayee(), { wrapper });

      result.current.mutate({ name: 'Amazon', category: 'Shopping' } as any);

      await waitFor(() => expect(result.current.isSuccess).toBe(true));

      expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['payees'] });
    });
  });

  describe('useUpdatePayee', () => {
    it('should update a payee', async () => {
      const updated = { ...mockPayee, name: 'Amazon Prime' };
      mockedApiClient.put.mockResolvedValue({ data: updated });

      const { result } = renderHook(() => useUpdatePayee(), { wrapper });

      result.current.mutate({ id: 1, data: { name: 'Amazon Prime' } as any });

      await waitFor(() => expect(result.current.isSuccess).toBe(true));
      expect(mockedApiClient.put).toHaveBeenCalledWith('/payees/1', expect.any(Object));
    });
  });

  describe('useTogglePayeeActive', () => {
    it('should toggle payee active status', async () => {
      mockedApiClient.patch.mockResolvedValue({ data: { ...mockPayee, isActive: false } });

      const { result } = renderHook(() => useTogglePayeeActive(), { wrapper });

      result.current.mutate(1);

      await waitFor(() => expect(result.current.isSuccess).toBe(true));
      expect(mockedApiClient.patch).toHaveBeenCalledWith('/payees/1/toggle-active');
    });
  });

  describe('useDeletePayee', () => {
    it('should delete a payee and invalidate queries', async () => {
      mockedApiClient.delete.mockResolvedValue({});
      const invalidateSpy = vi.spyOn(queryClient, 'invalidateQueries');

      const { result } = renderHook(() => useDeletePayee(), { wrapper });

      result.current.mutate(1);

      await waitFor(() => expect(result.current.isSuccess).toBe(true));
      expect(mockedApiClient.delete).toHaveBeenCalledWith('/payees/1');
      expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['payees'] });
    });
  });
});
