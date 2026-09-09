import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, within } from '@testing-library/react';
import { renderWithProviders, mockAuthentication, userEvent } from '@/test/test-utils';

const { mockNavigate } = vi.hoisted(() => ({ mockNavigate: vi.fn() }));

vi.mock('react-router', async importOriginal => {
  const actual = await importOriginal<typeof import('react-router')>();
  return { ...actual, useNavigate: () => mockNavigate };
});

vi.mock('@/hooks/useAccounts', () => ({
  useAccounts: vi.fn(() => ({
    data: [
      {
        id: 1,
        name: 'Checking',
        institution: { id: 1, name: 'BNP' },
        balance: 5000,
        ownBalance: 5000,
        balanceInBaseCurrency: 5000,
        currency: 'EUR',
      },
      {
        id: 2,
        name: 'Savings',
        institution: { id: 1, name: 'BNP' },
        balance: 10000,
        ownBalance: 10000,
        balanceInBaseCurrency: 10000,
        currency: 'EUR',
      },
      {
        id: 3,
        name: 'Trade',
        institution: null,
        balance: 3000,
        ownBalance: 3000,
        balanceInBaseCurrency: 3000,
        currency: 'EUR',
      },
    ],
    isLoading: false,
    error: null,
  })),
}));

vi.mock('@/hooks/useLiabilities', () => ({
  useLiabilities: vi.fn(() => ({ data: [], isLoading: false, error: null })),
}));

vi.mock('@/hooks/useAssets', () => ({
  useAssets: vi.fn(() => ({
    data: [],
    isLoading: false,
    error: null,
  })),
}));

vi.mock('@/hooks/useSecondaryConversion', () => ({
  useSecondaryConversion: () => ({
    convert: (v: number) => v,
    secondaryCurrency: null,
    secondaryExchangeRate: null,
  }),
}));

vi.mock('@/components/ui/ConvertedAmount', () => ({
  ConvertedAmount: ({ amount }: { amount: number }) => <span>{amount}</span>,
}));

import InstitutionBreakdown from '@/components/dashboard/InstitutionBreakdown';
import { useLiabilities } from '@/hooks/useLiabilities';
import { useAccounts } from '@/hooks/useAccounts';
import { useAssets } from '@/hooks/useAssets';
import type { Account } from '@/types/account';
import type { Asset } from '@/types/asset';
import type { Liability } from '@/types/liability';

describe('InstitutionBreakdown', () => {
  beforeEach(() => {
    mockAuthentication();
    vi.resetAllMocks();
  });

  it('renders institution names', () => {
    renderWithProviders(<InstitutionBreakdown baseCurrency="EUR" />);
    expect(screen.getByText('BNP')).toBeInTheDocument();
  });

  it('renders total balance', () => {
    renderWithProviders(<InstitutionBreakdown baseCurrency="EUR" />);
    // Grand total: 5000 + 10000 + 3000 = 18000
    expect(screen.getByText('Total (base currency)')).toBeInTheDocument();
    expect(screen.getByText('18000')).toBeInTheDocument();
  });

  it('shows one balance, account count and percentage per institution', () => {
    renderWithProviders(<InstitutionBreakdown baseCurrency="EUR" />);
    const institution = screen.getByRole('button', { name: 'View accounts at BNP' });
    expect(within(institution).getByText('15000')).toBeInTheDocument();
    expect(within(institution).getByText('(2 accounts)')).toBeInTheDocument();
    expect(within(institution).getByText('83.3% of total')).toBeInTheDocument();
    expect(screen.queryByRole('table')).not.toBeInTheDocument();
  });

  it('navigates to accounts filtered by institution when a row is clicked', async () => {
    renderWithProviders(<InstitutionBreakdown baseCurrency="EUR" />);
    const user = userEvent.setup();
    // Only the BNP group is clickable; the "Direct / No Institution" group is a plain div.
    await user.click(screen.getByRole('button', { name: /View accounts at/ }));
    expect(mockNavigate).toHaveBeenCalledTimes(1);
    expect(mockNavigate).toHaveBeenCalledWith('/accounts?institution=BNP');
  });

  it('renders the No Institution group as a non-clickable row', () => {
    renderWithProviders(<InstitutionBreakdown baseCurrency="EUR" />);
    // Fixtures include account id 3 with institution: null → "Direct / No Institution" group.
    const noInstitutionLabel = screen.getByText('Direct / No Institution');
    expect(noInstitutionLabel.closest('button')).toBeNull();
    const buttons = screen.queryAllByRole('button', { name: /View accounts at/ });
    buttons.forEach(b => expect(b.textContent).not.toContain('No Institution'));
  });

  it('subtracts linked debt from the two account balances in the institution amount', () => {
    vi.mocked(useAccounts).mockReturnValue({
      data: [
        {
          id: 1,
          name: 'Account A',
          ownBalance: 10000,
          balance: 10000,
          currency: 'EUR',
          institution: { id: 1, name: 'Institution X' },
        },
        {
          id: 2,
          name: 'Account B',
          ownBalance: 10000,
          balance: 10000,
          currency: 'EUR',
          institution: { id: 1, name: 'Institution X' },
        },
      ] as Account[],
      isLoading: false,
      error: null,
    } as ReturnType<typeof useAccounts>);
    vi.mocked(useLiabilities).mockReturnValue({
      data: [
        {
          id: 7,
          name: 'Liability C',
          currentBalance: 10000,
          currency: 'EUR',
          institution: { id: 1, name: 'Institution X' },
        } as Liability,
      ],
      isLoading: false,
      error: null,
    } as ReturnType<typeof useLiabilities>);
    renderWithProviders(<InstitutionBreakdown baseCurrency="EUR" />);
    const institution = screen.getByRole('button', { name: 'View accounts at Institution X' });
    expect(within(institution).getAllByText('10000')).toHaveLength(1);
    expect(within(institution).getByText('(2 accounts)')).toBeInTheDocument();
    expect(within(institution).getByText('100.0% of total')).toBeInTheDocument();
    expect(screen.getAllByText('10000')).toHaveLength(2);
    expect(screen.queryByText('20000')).not.toBeInTheDocument();
  });

  it('includes loan-only institutions with a signed net balance', () => {
    vi.mocked(useLiabilities).mockReturnValue({
      data: [
        {
          id: 7,
          name: 'Mortgage',
          currency: 'EUR',
          currentBalance: 25000,
          institution: { id: 2, name: 'Lender only' },
        } as Liability,
      ],
      isLoading: false,
      error: null,
    } as ReturnType<typeof useLiabilities>);
    renderWithProviders(<InstitutionBreakdown baseCurrency="EUR" />);
    expect(screen.getByText('Lender only').closest('button')).toBeNull();
    expect(screen.getByText('-25000')).toBeInTheDocument();
    expect(screen.getByText('-7000')).toBeInTheDocument();
  });

  it('does not add a liability whose debt is already tracked in an account', () => {
    vi.mocked(useAccounts).mockReturnValue({
      data: [
        {
          id: 1,
          name: 'Checking',
          currency: 'EUR',
          ownBalance: 5000,
          balance: 5000,
          institution: { id: 1, name: 'BNP' },
        },
        {
          id: 4,
          name: 'Card',
          currency: 'EUR',
          ownBalance: -500,
          balance: -500,
          institution: { id: 1, name: 'BNP' },
        },
      ] as Account[],
      isLoading: false,
      error: null,
    } as ReturnType<typeof useAccounts>);
    vi.mocked(useLiabilities).mockReturnValue({
      data: [
        {
          id: 7,
          currency: 'EUR',
          currentBalance: 500,
          representedByAccountId: 4,
          institution: { id: 1, name: 'BNP' },
        } as Liability,
      ],
      isLoading: false,
      error: null,
    } as ReturnType<typeof useLiabilities>);
    renderWithProviders(<InstitutionBreakdown baseCurrency="EUR" />);
    expect(
      within(screen.getByRole('button', { name: 'View accounts at BNP' })).getByText('4500')
    ).toBeInTheDocument();
    expect(screen.getAllByText('4500')).toHaveLength(2);
  });
  it('counts linked holdings and negative cash once in the net balance', () => {
    vi.mocked(useAccounts).mockReturnValue({
      data: [
        {
          id: 99,
          currency: 'EUR',
          balance: 100,
          ownBalance: -100,
          institution: { id: 4, name: 'Margin bank' },
        } as Account,
      ],
      isLoading: false,
      error: null,
    } as ReturnType<typeof useAccounts>);
    vi.mocked(useAssets).mockReturnValue({
      data: [{ id: 88, accountId: 99, totalValue: 200, currency: 'EUR' } as Asset],
      isLoading: false,
      error: null,
    } as ReturnType<typeof useAssets>);
    renderWithProviders(<InstitutionBreakdown baseCurrency="EUR" />);
    const institution = screen.getByRole('button', { name: 'View accounts at Margin bank' });
    expect(within(institution).getByText('100')).toBeInTheDocument();
    expect(screen.queryByText('200')).not.toBeInTheDocument();
  });
});
