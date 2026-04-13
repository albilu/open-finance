import { useQuery } from '@tanstack/react-query';
import { useAuthContext } from '@/context/AuthContext';
import { historyService } from '@/services/historyService';

/**
 * Returns true if the current login session has at least one history entry.
 * Used by Sidebar to conditionally show the History nav item.
 */
export function useHasSessionHistory(): boolean {
  const { sessionStartTime } = useAuthContext();
  const { data } = useQuery({
    queryKey: ['session-history-exists', sessionStartTime],
    queryFn: () => historyService.getHistory(0, 1, undefined, sessionStartTime!),
    enabled: !!sessionStartTime,
    staleTime: 30_000,
  });
  return (data?.totalElements ?? 0) > 0;
}
