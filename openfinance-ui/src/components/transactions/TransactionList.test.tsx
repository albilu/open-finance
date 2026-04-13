/**
 * Unit tests for TransactionList component
 *
 * Tests transaction list rendering, split transaction display and expansion,
 * and split-specific functionality.
 */
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi, describe, it, expect, beforeAll, beforeEach } from 'vitest';
import { act } from 'react';
import { TransactionList } from './TransactionList';
import * as usePayeesModule from '@/hooks/usePayees';
import { renderWithProviders } from '@/test/test-utils';
import type { Transaction, TransactionSplitResponse } from '@/types/transaction';
import type { Payee } from '@/types/payee';

// ── Polyfills ─────────────────────────────────────────────────────────────────

beforeAll(() => {
  window.HTMLElement.prototype.scrollIntoView = vi.fn();
});

// ── Mock hooks ────────────────────────────────────────────────────────────────

vi.mock('@/hooks/usePayees', async (importOriginal) => {
  const actual = await importOriginal<typeof usePayeesModule>();
  return { ...actual, useActivePayees: vi.fn() };
});

// ── Mock components ───────────────────────────────────────────────────────────

vi.mock('@/components/ui/PrivateAmount', () => ({
  PrivateAmount: ({ children, inline }: { children: React.ReactNode; inline?: boolean }) => (
    <span data-testid="private-amount" data-inline={inline}>
      {children}
    </span>
  ),
}));

vi.mock('./SplitDetail', () => ({
  SplitDetail: ({ splits, currency }: { splits: TransactionSplitResponse[]; currency: string }) => (
    <div data-testid="split-detail" data-currency={currency}>
      Split details for {splits.length} splits
    </div>
  ),
}));

// ── Typed mock references ─────────────────────────────────────────────────────

const mockUseActivePayees = vi.mocked(usePayeesModule.useActivePayees);

// ── Fixtures ──────────────────────────────────────────────────────────────────

const mockPayees: Payee[] = [
  {
    id: 1,
    name: 'Amazon',
    logo: 'https://example.com/amazon.png',
    categoryId: 10,
    categoryName: 'Shopping',
    isSystem: false,
    isActive: true,
  },
  {
    id: 2,
    name: 'Spotify',
    logo: 'https://example.com/spotify.png',
    categoryId: 20,
    categoryName: 'Entertainment',
    isSystem: false,
    isActive: true,
  },
];

const mockSplitTransaction: Transaction = {
  id: 1,
  userId: 1,
  accountId: 1,
  type: 'EXPENSE',
  amount: 75.00,
  currency: 'EUR',
  date: '2024-06-01',
  description: 'Split transaction test',
  payee: 'Amazon',
  isReconciled: false,
  createdAt: '2024-06-01',
  hasSplits: true,
  splits: [
    {
      id: 1,
      transactionId: 1,
      categoryId: 10,
      categoryName: 'Shopping',
      categoryColor: '#ff0000',
      categoryIcon: '🛒',
      amount: 50.00,
      description: 'Groceries',
    },
    {
      id: 2,
      transactionId: 1,
      categoryId: 20,
      categoryName: 'Entertainment',
      categoryColor: '#00ff00',
      categoryIcon: undefined,
      amount: 25.00,
      description: 'Music subscription',
    },
  ],
};

const mockRegularTransaction: Transaction = {
  id: 2,
  userId: 1,
  accountId: 1,
  type: 'EXPENSE',
  amount: 25.00,
  currency: 'EUR',
  date: '2024-06-01',
  description: 'Regular transaction',
  payee: 'Spotify',
  categoryId: 20,
  categoryName: 'Entertainment',
  categoryColor: '#00ff00',
  isReconciled: false,
  createdAt: '2024-06-01',
  hasSplits: false,
};

const mockTransactions = [mockSplitTransaction, mockRegularTransaction];

// ── Helpers ───────────────────────────────────────────────────────────────────

interface RenderListOptions {
  transactions?: Transaction[];
  onEdit?: ReturnType<typeof vi.fn>;
  onDelete?: ReturnType<typeof vi.fn>;
  highlightedId?: number | null;
}

function renderList({
  transactions = mockTransactions,
  onEdit = vi.fn(),
  onDelete = vi.fn(),
  highlightedId = null,
}: RenderListOptions = {}) {
  renderWithProviders(
    <TransactionList
      transactions={transactions}
      onEdit={onEdit}
      onDelete={onDelete}
      highlightedId={highlightedId}
    />
  );
  return { onEdit, onDelete };
}

// ── Tests ─────────────────────────────────────────────────────────────────────

describe('TransactionList', () => {
  beforeEach(() => {
    vi.clearAllMocks();

    mockUseActivePayees.mockReturnValue({
      data: mockPayees,
      isLoading: false,
      isError: false,
    } as ReturnType<typeof usePayeesModule.useActivePayees>);
  });

  describe('Split Transaction Display', () => {
    it('shows "Split" badge for transactions with hasSplits=true', () => {
      renderList();

      const splitBadge = screen.getByText('Split');
      expect(splitBadge).toBeInTheDocument();
      expect(splitBadge.closest('button')).toHaveAttribute('aria-expanded', 'false');
    });

    it('does not show "Split" badge for regular transactions', () => {
      renderList({ transactions: [mockRegularTransaction] });

      expect(screen.queryByText('Split')).not.toBeInTheDocument();
    });

    it('shows chevron down icon when split details are collapsed', () => {
      renderList();

      const splitBadge = screen.getByText('Split').closest('button');
      const chevronDown = splitBadge?.querySelector('svg');
      expect(chevronDown).toBeInTheDocument();
    });

    it('shows scissors icon in split badge', () => {
      renderList();

      const splitBadge = screen.getByText('Split').closest('button');
      const scissorsIcon = splitBadge?.querySelector('svg');
      expect(scissorsIcon).toBeInTheDocument();
    });

    it('has correct accessibility attributes for split badge', () => {
      renderList();

      const splitBadge = screen.getByText('Split').closest('button');
      expect(splitBadge).toHaveAttribute('aria-expanded', 'false');
      expect(splitBadge).toHaveAttribute('aria-label', 'Toggle split details');
      expect(splitBadge).toHaveAttribute('title', 'This transaction is split across multiple categories');
    });

    it('toggles split details expansion when split badge is clicked', async () => {
      renderList();

      const splitBadge = screen.getByText('Split').closest('button') as HTMLButtonElement;

      // Initially collapsed - no split detail visible
      expect(screen.queryByTestId('split-detail')).not.toBeInTheDocument();

      // Click to expand
      await act(async () => {
        splitBadge.click();
      });

      // Should now be expanded - check for split detail component
      await waitFor(() => {
        expect(screen.getByTestId('split-detail')).toBeInTheDocument();
        expect(screen.getByTestId('split-detail')).toHaveTextContent('Split details for 2 splits');
      });
    });

    it('shows chevron up icon when split details are expanded', async () => {
      renderList();

      const splitBadge = screen.getByText('Split').closest('button') as HTMLButtonElement;

      await act(async () => {
        splitBadge.click();
      });

      await waitFor(() => {
        const chevronUp = splitBadge.querySelector('svg');
        expect(chevronUp).toBeInTheDocument();
      });
    });

    it('renders SplitDetail component with correct props when expanded', async () => {
      renderList();

      const splitBadge = screen.getByText('Split').closest('button') as HTMLButtonElement;

      await act(async () => {
        splitBadge.click();
      });

      await waitFor(() => {
        const splitDetail = screen.getByTestId('split-detail');
        expect(splitDetail).toBeInTheDocument();
        expect(splitDetail).toHaveAttribute('data-currency', 'EUR');
        expect(splitDetail).toHaveTextContent('Split details for 2 splits');
      });
    });

    it('only shows split details when transaction has both hasSplits=true and splits data', async () => {
      const transactionWithoutSplitsData: Transaction = {
        ...mockSplitTransaction,
        splits: undefined,
      };

      renderList({ transactions: [transactionWithoutSplitsData] });

      const splitBadge = screen.getByText('Split').closest('button') as HTMLButtonElement;

      await act(async () => {
        splitBadge.click();
      });

      // Should not show split details if splits data is missing
      expect(screen.queryByTestId('split-detail')).not.toBeInTheDocument();
    });

    it('handles multiple split transactions independently', async () => {
      const anotherSplitTransaction: Transaction = {
        ...mockSplitTransaction,
        id: 3,
        description: 'Another split transaction',
        splits: [
          {
            id: 3,
            transactionId: 3,
            categoryId: 10,
            categoryName: 'Shopping',
            categoryColor: '#ff0000',
            amount: 30.00,
            description: 'More groceries',
          },
        ],
      };

      renderList({ transactions: [mockSplitTransaction, anotherSplitTransaction] });

      const splitBadges = screen.getAllByText('Split');
      expect(splitBadges).toHaveLength(2);

      // Expand first split transaction
      await act(async () => {
        splitBadges[0].closest('button')?.click();
      });

      await waitFor(() => {
        expect(screen.getByTestId('split-detail')).toHaveTextContent('Split details for 2 splits');
      });

      // Second split transaction should still be collapsed
      const secondBadge = splitBadges[1].closest('button');
      expect(secondBadge).toHaveAttribute('aria-expanded', 'false');
    });
  });

  describe('Split Transaction Edge Cases', () => {
    it('handles split transaction with empty splits array', () => {
      const emptySplitsTransaction: Transaction = {
        ...mockSplitTransaction,
        splits: [],
      };

      renderList({ transactions: [emptySplitsTransaction] });

      const splitBadge = screen.getByText('Split');
      expect(splitBadge).toBeInTheDocument();
    });

    it('handles split transaction with single split', () => {
      const singleSplitTransaction: Transaction = {
        ...mockSplitTransaction,
        splits: [
          {
            id: 1,
            transactionId: 1,
            categoryId: 10,
            categoryName: 'Shopping',
            categoryColor: '#ff0000',
            amount: 75.00,
            description: 'Single split',
          },
        ],
      };

      renderList({ transactions: [singleSplitTransaction] });

      const splitBadge = screen.getByText('Split');
      expect(splitBadge).toBeInTheDocument();
    });

    it('handles split transaction with many splits', () => {
      const manySplits: TransactionSplitResponse[] = Array.from({ length: 10 }, (_, i) => ({
        id: i + 1,
        transactionId: 1,
        categoryId: 10,
        categoryName: `Category ${i + 1}`,
        categoryColor: '#ff0000',
        amount: 7.50,
        description: `Split ${i + 1}`,
      }));

      const manySplitsTransaction: Transaction = {
        ...mockSplitTransaction,
        splits: manySplits,
      };

      renderList({ transactions: [manySplitsTransaction] });

      const splitBadge = screen.getByText('Split');
      expect(splitBadge).toBeInTheDocument();
    });

    it('handles split transaction with negative amounts', () => {
      const negativeSplitTransaction: Transaction = {
        ...mockSplitTransaction,
        type: 'INCOME',
        amount: -75.00,
        splits: [
          {
            id: 1,
            transactionId: 1,
            categoryId: 10,
            categoryName: 'Shopping',
            categoryColor: '#ff0000',
            amount: -50.00,
            description: 'Negative split',
          },
          {
            id: 2,
            transactionId: 1,
            categoryId: 20,
            categoryName: 'Entertainment',
            categoryColor: '#00ff00',
            amount: -25.00,
            description: 'Another negative split',
          },
        ],
      };

      renderList({ transactions: [negativeSplitTransaction] });

      const splitBadge = screen.getByText('Split');
      expect(splitBadge).toBeInTheDocument();
    });
  });

  describe('Split Badge Interaction', () => {
    it('is clickable and has cursor pointer', () => {
      renderList();

      const splitBadge = screen.getByText('Split').closest('button');
      expect(splitBadge).toHaveClass('cursor-pointer');
      expect(splitBadge).toHaveClass('select-none');
    });

    it('prevents text selection on the badge', () => {
      renderList();

      const splitBadge = screen.getByText('Split').closest('button');
      expect(splitBadge).toHaveClass('select-none');
    });

    it('has info variant styling', () => {
      renderList();

      const splitBadge = screen.getByText('Split').closest('button');
      expect(splitBadge).toHaveClass('text-accent-blue');
    });
  });
});