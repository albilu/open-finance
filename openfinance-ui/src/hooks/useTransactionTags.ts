/**
 * Hook for fetching transaction tags
 */
import { useQuery } from '@tanstack/react-query';
import apiClient from '@/services/apiClient';

interface TagInfo {
  tag: string;
  count: number;
}

/**
 * Fetches all tags for the authenticated user
 */
export function useTransactionTags() {
  return useQuery<TagInfo[]>({
    queryKey: ['transaction-tags'],
    queryFn: async () => {
      const response = await apiClient.get<TagInfo[]>('/transactions/tags');
      return response.data;
    },
    staleTime: 5 * 60 * 1000, // 5 minutes
  });
}

/**
 * Fetches popular tags for autocomplete
 */
export function usePopularTags(limit: number = 20) {
  return useQuery<string[]>({
    queryKey: ['transaction-tags-popular', limit],
    queryFn: async () => {
      const response = await apiClient.get<string[]>('/transactions/tags/popular', {
        params: { limit },
      });
      return response.data;
    },
    staleTime: 5 * 60 * 1000, // 5 minutes
  });
}
