/**
 * Backup management hooks
 * Task 12.5.8: Create useBackup hook
 * 
 * Provides React Query hooks for backup and restore operations
 */
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import apiClient from '@/services/apiClient';
import type { BackupResponse, BackupRequest } from '@/types/backup';

/**
 * Fetch all backups for the current user
 */
export function useListBackups() {
  return useQuery<BackupResponse[]>({
    queryKey: ['backups'],
    queryFn: async () => {
      const response = await apiClient.get<BackupResponse[]>('/backup/list');
      return response.data;
    },
  });
}

/**
 * Fetch a single backup by ID
 */
export function useBackup(backupId: number | null) {
  return useQuery<BackupResponse>({
    queryKey: ['backups', backupId],
    queryFn: async () => {
      if (!backupId) throw new Error('Backup ID is required');
      
      const response = await apiClient.get<BackupResponse>(`/backup/${backupId}`);
      return response.data;
    },
    enabled: !!backupId,
  });
}

/**
 * Create a new manual backup
 * 
 * @returns Mutation hook that accepts optional description
 */
export function useCreateBackup() {
  const queryClient = useQueryClient();
  
  return useMutation<BackupResponse, Error, string | undefined>({
    mutationFn: async (description?: string) => {
      const requestData: BackupRequest = description ? { description } : {};
      const response = await apiClient.post<BackupResponse>('/backup/create', requestData);
      return response.data;
    },
    onSuccess: () => {
      // Invalidate backups query to refetch the list
      queryClient.invalidateQueries({ queryKey: ['backups'] });
    },
  });
}

/**
 * Restore database from an existing backup
 * 
 * @returns Mutation hook that accepts backup ID
 */
export function useRestoreBackup() {
  return useMutation<string, Error, number>({
    mutationFn: async (backupId: number) => {
      const response = await apiClient.post<string>(`/backup/restore/${backupId}`);
      return response.data;
    },
  });
}

/**
 * Upload and restore database from a backup file
 * 
 * @returns Mutation hook that accepts File object
 */
export function useUploadAndRestoreBackup() {
  return useMutation<string, Error, File>({
    mutationFn: async (file: File) => {
      const formData = new FormData();
      formData.append('file', file);
      
      const response = await apiClient.post<string>('/backup/restore/upload', formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      });
      return response.data;
    },
  });
}

/**
 * Delete a backup
 * 
 * @returns Mutation hook that accepts backup ID
 */
export function useDeleteBackup() {
  const queryClient = useQueryClient();
  
  return useMutation<void, Error, number>({
    mutationFn: async (backupId: number) => {
      await apiClient.delete(`/backup/${backupId}`);
    },
    onSuccess: () => {
      // Invalidate backups query to refetch the list
      queryClient.invalidateQueries({ queryKey: ['backups'] });
    },
  });
}

/**
 * Download a backup file
 * 
 * @returns Mutation hook that accepts backup ID and filename
 */
export function useDownloadBackup() {
  return useMutation<void, Error, { backupId: number; filename: string }>({
    mutationFn: async ({ backupId, filename }) => {
      const response = await apiClient.get(`/backup/${backupId}/download`, {
        responseType: 'blob',
      });
      
      // Create blob URL and trigger download
      const blob = new Blob([response.data], { type: 'application/octet-stream' });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', filename);
      document.body.appendChild(link);
      link.click();
      
      // Cleanup
      link.remove();
      window.URL.revokeObjectURL(url);
    },
  });
}
