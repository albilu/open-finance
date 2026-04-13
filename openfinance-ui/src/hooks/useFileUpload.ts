/**
 * File upload hooks
 * Task 7.1.8: Create useFileUpload hook
 * 
 * Provides React Query hooks for file upload operations
 */
import { useMutation } from '@tanstack/react-query';
import apiClient from '@/services/apiClient';
import type { FileUploadResponse, FileUploadError } from '@/types/import';

/**
 * Upload a file for transaction import
 * 
 * @example
 * const uploadFile = useFileUpload();
 * 
 * const handleUpload = (file: File) => {
 *   uploadFile.mutate(file, {
 *     onSuccess: (data) => {
 *       console.log('Upload successful:', data.uploadId);
 *     },
 *     onError: (error) => {
 *       console.error('Upload failed:', error.message);
 *     },
 *   });
 * };
 */
export function useFileUpload() {
  return useMutation<FileUploadResponse, FileUploadError, File>({
    mutationFn: async (file: File) => {
      // Create FormData for multipart upload
      const formData = new FormData();
      formData.append('file', file);

      // Upload with authorization header (no encryption key needed for file upload)
      const response = await apiClient.post<FileUploadResponse>(
        '/import/upload',
        formData,
        {
          headers: {
            'Content-Type': 'multipart/form-data',
          },
        }
      );

      // Check if upload was successful
      if (response.data.status === 'INVALID' || response.data.status === 'ERROR') {
        throw {
          message: response.data.message,
          status: response.status,
        } as FileUploadError;
      }

      return response.data;
    },
  });
}

/**
 * Upload a file with progress tracking
 * 
 * Note: Axios progress tracking is not fully supported in all browsers.
 * This is a placeholder for future enhancement.
 * 
 * @example
 * const { uploadFileWithProgress, uploadProgress } = useFileUploadWithProgress();
 * 
 * uploadFileWithProgress.mutate(file, {
 *   onSuccess: (data) => {
 *     console.log('Upload complete:', data.uploadId);
 *   },
 * });
 */
export function useFileUploadWithProgress() {
  const uploadMutation = useFileUpload();

  return {
    uploadFileWithProgress: uploadMutation,
    uploadProgress: 0, // TODO: Implement progress tracking with Axios onUploadProgress
  };
}
