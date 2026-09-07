/**
 * Shared types of the buy-property wizard steps (Task 9).
 */

/** How the purchase is financed. */
export type FundingSource = 'new' | 'existing' | 'none';

/** Where the bank sends the loan funds. */
export type DisbursementRoute = 'direct' | 'account';

/** Step 1 state: property identity and valuation. */
export interface PropertyStepState {
  name: string;
  address: string;
  propertyType: string;
  purchasePrice: string;
  purchaseDate: string;
  currentValue: string;
  currency: string;
}

/** Step 2 state: funding, disbursement route and optional down payment. */
export interface FundingStepState {
  source: FundingSource;
  mortgageName: string;
  loanAmount: string;
  interestRate: string;
  existingMortgageId?: number;
  route: DisbursementRoute;
  downPaymentAmount: string;
  downPaymentAccountId?: number;
}

/** IDs of resources already created by a previous confirm attempt (retry dedupe). */
export interface CreatedIdsState {
  liabilityId?: number;
  propertyId?: number;
  /**
   * Liability whose disbursement already completed. A retry after a later failure (e.g. the
   * down-payment transaction) must not replay it.
   */
  disbursedLiabilityId?: number;
}

export const PROPERTY_TYPE_OPTIONS = [
  'RESIDENTIAL',
  'COMMERCIAL',
  'LAND',
  'MIXED_USE',
  'INDUSTRIAL',
  'OTHER',
];
