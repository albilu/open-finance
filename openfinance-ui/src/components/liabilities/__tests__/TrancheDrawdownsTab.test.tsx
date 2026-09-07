/**
 * Unit tests for TrancheDrawdownsTab (Task 8)
 *
 * Verifies the Drawdowns tab of the liability detail dialog: tranche rows with
 * T{n} label, status badge, planned/drawn/remaining amounts, linked property
 * label and interest-only flag.
 */
import { screen } from '@testing-library/react';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { renderWithProviders } from '@/test/test-utils';
import { TrancheDrawdownsTab } from '../TrancheDrawdownsTab';
import * as useTranchesModule from '@/hooks/useTranches';
import type { Liability, LiabilityTranche } from '@/types/liability';

vi.mock('@/hooks/useTranches', async importOriginal => {
  const actual = await importOriginal<typeof useTranchesModule>();
  return {
    ...actual,
    useTranches: vi.fn(() => ({ data: [], isLoading: false, error: null })),
  };
});

const mockUseTranches = vi.mocked(useTranchesModule.useTranches);

const mockLiability: Liability = {
  id: 5,
  name: 'Construction loan',
  type: 'LOAN',
  principal: 150000,
  currentBalance: 90000,
  currency: 'USD',
  startDate: '2025-01-01',
  createdAt: '2025-01-01T00:00:00Z',
  updatedAt: '2025-01-01T00:00:00Z',
};

const mockTranches: LiabilityTranche[] = [
  {
    id: 1,
    liabilityId: 5,
    trancheNo: 1,
    plannedAmount: 100000,
    drawnAmount: 100000,
    remaining: 40000,
    status: 'DRAWN',
    realEstateId: 7,
    currency: 'USD',
  },
  {
    id: 2,
    liabilityId: 5,
    trancheNo: 2,
    plannedAmount: 50000,
    drawnAmount: null,
    remaining: 50000,
    status: 'PLANNED',
    interestOnly: true,
    currency: 'USD',
  },
];

describe('TrancheDrawdownsTab', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockUseTranches.mockReturnValue({
      data: mockTranches,
      isLoading: false,
      error: null,
    } as unknown as ReturnType<typeof useTranchesModule.useTranches>);
  });

  it('renders one row per tranche with its T{n} label and status badge', () => {
    renderWithProviders(<TrancheDrawdownsTab liability={mockLiability} />);

    expect(screen.getByText('T1')).toBeInTheDocument();
    expect(screen.getByText('T2')).toBeInTheDocument();
    expect(screen.getByText('Drawn')).toBeInTheDocument();
    expect(screen.getByText('Planned')).toBeInTheDocument();
  });

  it('renders planned, drawn and remaining amounts for each tranche', () => {
    renderWithProviders(<TrancheDrawdownsTab liability={mockLiability} />);

    expect(screen.getAllByText('$100,000.00').length).toBeGreaterThanOrEqual(2); // planned + drawn of T1
    expect(screen.getByText('$40,000.00')).toBeInTheDocument(); // remaining T1
    expect(screen.getAllByText('$50,000.00').length).toBeGreaterThanOrEqual(2); // planned + remaining of T2
  });

  it('shows the linked property label when a tranche carries a realEstateId', () => {
    renderWithProviders(<TrancheDrawdownsTab liability={mockLiability} />);

    expect(screen.getByText('Property #7')).toBeInTheDocument();
  });

  it('shows the interest-only flag on interest-only tranches', () => {
    renderWithProviders(<TrancheDrawdownsTab liability={mockLiability} />);

    expect(screen.getByText('Interest-only')).toBeInTheDocument();
  });

  it('renders the empty state when the liability has no tranches', () => {
    mockUseTranches.mockReturnValue({
      data: [],
      isLoading: false,
      error: null,
    } as unknown as ReturnType<typeof useTranchesModule.useTranches>);

    renderWithProviders(<TrancheDrawdownsTab liability={mockLiability} />);

    expect(screen.getByText(/no planned drawdowns/i)).toBeInTheDocument();
  });
});
