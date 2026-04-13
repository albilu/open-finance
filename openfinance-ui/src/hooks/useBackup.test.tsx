import { renderHook, waitFor, act } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import React from 'react';
import {
  useListBackups,
  useBackup,
  useCreateBackup,
  useRestoreBackup,
  useUploadAndRestoreBackup,
  useDeleteBackup,
  useDownloadBackup,
} from './useBackup';
import apiClient from '@/services/apiClient';

vi.mock('@/services/apiClient');
const mockedApiClient = apiClient as any;

describe('useBackup hooks', () => {
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

  const mockBackup = {
    id: 1,
    fileName: 'backup_2024-01-01.db',
    description: 'Manual backup',
    fileSize: 2048,
    createdAt: '2024-01-01T12:00:00Z',
    type: 'MANUAL',
  };

  // ── useListBackups ─────────────────────────────────────────────────
  describe('useListBackups', () => {
    it('should fetch all backups', async () => {
      mockedApiClient.get.mockResolvedValue({ data: [mockBackup] });

      const { result } = renderHook(() => useListBackups(), { wrapper });

      await waitFor(() => expect(result.current.isSuccess).toBe(true));
      expect(result.current.data).toEqual([mockBackup]);
      expect(mockedApiClient.get).toHaveBeenCalledWith('/backup/list');
    });
  });

  // ── useBackup ─────────────────────────────────────────────────
  describe('useBackup', () => {
    it('should fetch a single backup by id', async () => {
      mockedApiClient.get.mockResolvedValue({ data: mockBackup });

      const { result } = renderHook(() => useBackup(1), { wrapper });

      await waitFor(() => expect(result.current.isSuccess).toBe(true));
      expect(result.current.data).toEqual(mockBackup);
      expect(mockedApiClient.get).toHaveBeenCalledWith('/backup/1');
    });

    it('should be disabled when id is null', () => {
      const { result } = renderHook(() => useBackup(null), { wrapper });
      expect(result.current.fetchStatus).toBe('idle');
    });
  });

  // ── useCreateBackup ─────────────────────────────────────────────────
  describe('useCreateBackup', () => {
    it('should create backup with description', async () => {
      mockedApiClient.post.mockResolvedValue({ data: mockBackup });

      const { result } = renderHook(() => useCreateBackup(), { wrapper });

      await act(async () => {
        result.current.mutate('Manual backup');
      });

      await waitFor(() => expect(result.current.isSuccess).toBe(true));
      expect(mockedApiClient.post).toHaveBeenCalledWith('/backup/create', {
        description: 'Manual backup',
      });
    });

    it('should create backup without description', async () => {
      mockedApiClient.post.mockResolvedValue({ data: mockBackup });

      const { result } = renderHook(() => useCreateBackup(), { wrapper });

      await act(async () => {
        result.current.mutate(undefined);
      });

      await waitFor(() => expect(result.current.isSuccess).toBe(true));
      expect(mockedApiClient.post).toHaveBeenCalledWith('/backup/create', {});
    });

    it('should invalidate backups query on success', async () => {
      mockedApiClient.post.mockResolvedValue({ data: mockBackup });
      const invalidateSpy = vi.spyOn(queryClient, 'invalidateQueries');

      const { result } = renderHook(() => useCreateBackup(), { wrapper });

      await act(async () => {
        result.current.mutate('Test');
      });

      await waitFor(() => expect(result.current.isSuccess).toBe(true));
      expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['backups'] });
    });
  });

  // ── useRestoreBackup ─────────────────────────────────────────────────
  describe('useRestoreBackup', () => {
    it('should restore backup by id', async () => {
      mockedApiClient.post.mockResolvedValue({ data: 'Backup restored successfully' });

      const { result } = renderHook(() => useRestoreBackup(), { wrapper });

      await act(async () => {
        result.current.mutate(1);
      });

      await waitFor(() => expect(result.current.isSuccess).toBe(true));
      expect(result.current.data).toBe('Backup restored successfully');
      expect(mockedApiClient.post).toHaveBeenCalledWith('/backup/restore/1');
    });
  });

  // ── useUploadAndRestoreBackup ─────────────────────────────────────────────────
  describe('useUploadAndRestoreBackup', () => {
    it('should upload and restore backup from file', async () => {
      mockedApiClient.post.mockResolvedValue({ data: 'Restored from upload' });

      const { result } = renderHook(() => useUploadAndRestoreBackup(), { wrapper });

      const file = new File(['backup data'], 'backup.db', { type: 'application/octet-stream' });
      await act(async () => {
        result.current.mutate(file);
      });

      await waitFor(() => expect(result.current.isSuccess).toBe(true));
      expect(mockedApiClient.post).toHaveBeenCalledWith(
        '/backup/restore/upload',
        expect.any(FormData),
        expect.objectContaining({
          headers: { 'Content-Type': 'multipart/form-data' },
        }),
      );
    });
  });

  // ── useDeleteBackup ─────────────────────────────────────────────────
  describe('useDeleteBackup', () => {
    it('should delete backup by id', async () => {
      mockedApiClient.delete.mockResolvedValue({});

      const { result } = renderHook(() => useDeleteBackup(), { wrapper });

      await act(async () => {
        result.current.mutate(1);
      });

      await waitFor(() => expect(result.current.isSuccess).toBe(true));
      expect(mockedApiClient.delete).toHaveBeenCalledWith('/backup/1');
    });

    it('should invalidate backups query on success', async () => {
      mockedApiClient.delete.mockResolvedValue({});
      const invalidateSpy = vi.spyOn(queryClient, 'invalidateQueries');

      const { result } = renderHook(() => useDeleteBackup(), { wrapper });

      await act(async () => {
        result.current.mutate(1);
      });

      await waitFor(() => expect(result.current.isSuccess).toBe(true));
      expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['backups'] });
    });
  });

  // ── useDownloadBackup ─────────────────────────────────────────────────
  describe('useDownloadBackup', () => {
    it('should download backup and create download link', async () => {
      const blobData = new Blob(['backup content'], { type: 'application/octet-stream' });
      mockedApiClient.get.mockResolvedValue({ data: blobData });

      // Mock URL methods
      window.URL.createObjectURL = vi.fn(() => 'blob:mock-url');
      window.URL.revokeObjectURL = vi.fn();
      const clickSpy = vi.fn();
      const removeSpy = vi.fn();

      // Mock anchor element creation without breaking renderHook
      const originalCreateElement = document.createElement.bind(document);
      vi.spyOn(document, 'createElement').mockImplementation((tag: string) => {
        if (tag === 'a') {
          return {
            href: '',
            setAttribute: vi.fn(),
            click: clickSpy,
            remove: removeSpy,
          } as any;
        }
        return originalCreateElement(tag);
      });
      vi.spyOn(document.body, 'appendChild').mockImplementation((node) => node);

      const { result } = renderHook(() => useDownloadBackup(), { wrapper });

      await act(async () => {
        result.current.mutate({ backupId: 1, filename: 'backup.db' });
      });

      await waitFor(() => expect(result.current.isSuccess).toBe(true));
      expect(mockedApiClient.get).toHaveBeenCalledWith('/backup/1/download', {
        responseType: 'blob',
      });
      expect(clickSpy).toHaveBeenCalled();

      vi.restoreAllMocks();
    });
  });
});
