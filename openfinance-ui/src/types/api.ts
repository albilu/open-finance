/**
 * Common API response types
 */

export interface ApiResponse<T> {
  status: 'success' | 'error';
  message?: string;
  data: T;
}

export interface ApiError {
  status: string;
  message: string;
  errors?: Record<string, string[]>;
  timestamp: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
  first: boolean;
  last: boolean;
}

export interface HealthResponse {
  status: string;
  message: string;
  timestamp: string;
}
