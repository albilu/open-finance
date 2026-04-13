/**
 * Institution-related types
 */

export interface Institution {
  id: number;
  name: string;
  bic?: string;
  country?: string;
  logo?: string;
  isSystem: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface InstitutionRequest {
  name: string;
  bic?: string;
  country?: string;
  logo?: string;
}
