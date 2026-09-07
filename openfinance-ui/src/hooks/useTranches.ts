/**
 * Tranche hooks for staged liabilities (Task 8).
 *
 * TanStack Query wrappers around the liability tranche endpoints.
 */
import { useQuery } from '@tanstack/react-query';
import apiClient from '@/services/apiClient';
import { buildEncryptionHeaders } from '@/utils/encryption';
import type { LiabilityTranche } from '@/types/liability';

/**
 * Fetch the tranches (planned drawdowns) of a liability, ordered by tranche number.
 * The query is disabled while no liability is selected.
 */
export function useTranches(liabilityId: number | null) {
  return useQuery<LiabilityTranche[]>({
    queryKey: ['liabilities', liabilityId, 'tranches'],
    queryFn: async () => {
      if (!liabilityId) throw new Error('Liability ID is required');

      const response = await apiClient.get<LiabilityTranche[]>(
        `/liabilities/${liabilityId}/tranches`,
        {
          headers: buildEncryptionHeaders(),
        }
      );
      return response.data;
    },
    enabled: liabilityId != null,
  });
}

/**
 * Human-readable label of a tranche: T1, T2, …
 */
export const getTrancheLabel = (trancheNo: number | null | undefined): string =>
  trancheNo != null ? `T${trancheNo}` : '—';
