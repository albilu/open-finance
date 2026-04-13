import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import React from 'react';
import {
  useInstitutions,
  useInstitutionsByCountry,
  useInstitutionSearch,
  useSystemInstitutions,
  useCustomInstitutions,
  useInstitutionCountries,
  useCreateInstitution,
  useUpdateInstitution,
  useDeleteInstitution,
} from './useInstitutions';
import apiClient from '@/services/apiClient';

vi.mock('@/services/apiClient');
const mockedApiClient = apiClient as any;

describe('useInstitutions hooks', () => {
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

  const mockInstitution = {
    id: 1,
    name: 'BNP Paribas',
    country: 'FR',
    bic: 'BNPAFRPP',
    isSystem: true,
  };

  // ── Query hooks ──────────────────────────────────────────────────────────
  describe('useInstitutions', () => {
    it('should fetch all institutions', async () => {
      mockedApiClient.get.mockResolvedValue({ data: [mockInstitution] });

      const { result } = renderHook(() => useInstitutions(), { wrapper });

      await waitFor(() => expect(result.current.isSuccess).toBe(true));
      expect(result.current.data).toEqual([mockInstitution]);
      expect(mockedApiClient.get).toHaveBeenCalledWith('/institutions');
    });

    it('should handle API error', async () => {
      mockedApiClient.get.mockRejectedValue(new Error('Network error'));

      const { result } = renderHook(() => useInstitutions(), { wrapper });

      await waitFor(() => expect(result.current.isError).toBe(true));
    });
  });

  describe('useInstitutionsByCountry', () => {
    it('should fetch institutions by country', async () => {
      mockedApiClient.get.mockResolvedValue({ data: [mockInstitution] });

      const { result } = renderHook(() => useInstitutionsByCountry('FR'), { wrapper });

      await waitFor(() => expect(result.current.isSuccess).toBe(true));
      expect(mockedApiClient.get).toHaveBeenCalledWith('/institutions/country/FR');
    });

    it('should be disabled when country is empty', () => {
      const { result } = renderHook(() => useInstitutionsByCountry(''), { wrapper });
      expect(result.current.isFetched).toBe(false);
    });
  });

  describe('useInstitutionSearch', () => {
    it('should search institutions with minimum 2 characters', async () => {
      mockedApiClient.get.mockResolvedValue({ data: [mockInstitution] });

      const { result } = renderHook(() => useInstitutionSearch('BN'), { wrapper });

      await waitFor(() => expect(result.current.isSuccess).toBe(true));
      expect(mockedApiClient.get).toHaveBeenCalledWith('/institutions/search', {
        params: { query: 'BN' },
      });
    });

    it('should be disabled when query is too short', () => {
      const { result } = renderHook(() => useInstitutionSearch('B'), { wrapper });
      expect(result.current.isFetched).toBe(false);
    });
  });

  describe('useSystemInstitutions', () => {
    it('should fetch system institutions', async () => {
      mockedApiClient.get.mockResolvedValue({ data: [mockInstitution] });

      const { result } = renderHook(() => useSystemInstitutions(), { wrapper });

      await waitFor(() => expect(result.current.isSuccess).toBe(true));
      expect(mockedApiClient.get).toHaveBeenCalledWith('/institutions/system');
    });
  });

  describe('useCustomInstitutions', () => {
    it('should fetch custom institutions', async () => {
      mockedApiClient.get.mockResolvedValue({ data: [] });

      const { result } = renderHook(() => useCustomInstitutions(), { wrapper });

      await waitFor(() => expect(result.current.isSuccess).toBe(true));
      expect(mockedApiClient.get).toHaveBeenCalledWith('/institutions/custom');
    });
  });

  describe('useInstitutionCountries', () => {
    it('should fetch institution countries', async () => {
      mockedApiClient.get.mockResolvedValue({ data: ['FR', 'DE', 'US'] });

      const { result } = renderHook(() => useInstitutionCountries(), { wrapper });

      await waitFor(() => expect(result.current.isSuccess).toBe(true));
      expect(result.current.data).toEqual(['FR', 'DE', 'US']);
    });
  });

  // ── Mutation hooks ─────────────────────────────────────────────────────────
  describe('useCreateInstitution', () => {
    it('should create an institution and invalidate queries', async () => {
      mockedApiClient.post.mockResolvedValue({ data: mockInstitution });
      const invalidateSpy = vi.spyOn(queryClient, 'invalidateQueries');

      const { result } = renderHook(() => useCreateInstitution(), { wrapper });

      result.current.mutate({ name: 'BNP Paribas', country: 'FR' } as any);

      await waitFor(() => expect(result.current.isSuccess).toBe(true));
      expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['institutions'] });
    });
  });

  describe('useUpdateInstitution', () => {
    it('should update an institution', async () => {
      const updated = { ...mockInstitution, name: 'BNP Updated' };
      mockedApiClient.put.mockResolvedValue({ data: updated });

      const { result } = renderHook(() => useUpdateInstitution(), { wrapper });

      result.current.mutate({ id: 1, data: { name: 'BNP Updated' } as any });

      await waitFor(() => expect(result.current.isSuccess).toBe(true));
      expect(mockedApiClient.put).toHaveBeenCalledWith('/institutions/1', expect.any(Object));
    });
  });

  describe('useDeleteInstitution', () => {
    it('should delete an institution and invalidate queries', async () => {
      mockedApiClient.delete.mockResolvedValue({});
      const invalidateSpy = vi.spyOn(queryClient, 'invalidateQueries');

      const { result } = renderHook(() => useDeleteInstitution(), { wrapper });

      result.current.mutate(1);

      await waitFor(() => expect(result.current.isSuccess).toBe(true));
      expect(mockedApiClient.delete).toHaveBeenCalledWith('/institutions/1');
      expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['institutions'] });
    });
  });
});
