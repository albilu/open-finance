/**
 * Unit tests for BudgetsPage component
 */
import { screen, fireEvent, waitFor } from '@testing-library/react';
import { QueryClient } from '@tanstack/react-query';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { renderWithProviders } from '@/test/test-utils';
import BudgetsPage from './BudgetsPage';

// Mock the hooks
vi.mock('@/hooks/useBudgets');
vi.mock('@/hooks/useDocumentTitle');
vi.mock('react-router', async () => {
  const actual = await vi.importActual<any>('react-router');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
    useSearchParams: vi.fn(() => [new URLSearchParams(), vi.fn()]),
  };
});

const mockNavigate = vi.fn();

// Mock LoadingSkeleton to have testId
vi.mock('@/components/LoadingComponents', () => ({
  LoadingSkeleton: ({ className }: { className: string }) => (
    <div data-testid="loading-skeleton" className={className} />
  ),
}));

// Mock other components that might be complex
vi.mock('@/components/budgets/BudgetCard', () => ({
  BudgetCard: ({ budget }: { budget: any }) => (
    <div data-testid={`budget-card-${budget.budgetId}`}>{budget.categoryName}</div>
  ),
}));

vi.mock('@/components/budgets/BudgetSummaryCard', () => ({
  BudgetSummaryCard: ({ summary }: { summary: any }) => (
    <div data-testid="budget-summary-card">Summary: {summary.totalBudgets} budgets</div>
  ),
}));

vi.mock('@/components/budgets/AlertBanner', () => ({
  AlertBanner: ({ title }: { title: string }) => (
    <div data-testid="alert-banner">{title}</div>
  ),
}));

vi.mock('@/components/budgets/BudgetForm', () => ({
  BudgetForm: () => <div data-testid="budget-form">Budget Form</div>,
}));

vi.mock('@/components/budgets/BudgetWizard', () => ({
  BudgetWizard: ({ open }: { open: boolean }) => (
    open ? <div data-testid="budget-wizard">Budget Wizard</div> : null
  ),
}));

vi.mock('@/components/ConfirmationDialog', () => ({
  ConfirmationDialog: ({ open, title }: { open: boolean; title: string }) => (
    open ? <div data-testid="confirmation-dialog">{title}</div> : null
  ),
}));

// Import after mocking
import {
  useBudgetSummary,
  useCreateBudget,
  useUpdateBudget,
  useDeleteBudget,
  useBudget,
} from '@/hooks/useBudgets';
import { useDocumentTitle } from '@/hooks/useDocumentTitle';
import { useNavigate } from 'react-router';
import type { BudgetSummaryResponse, BudgetProgressResponse } from '@/types/budget';

const mockUseBudgetSummary = vi.mocked(useBudgetSummary);
const mockUseCreateBudget = vi.mocked(useCreateBudget);
const mockUseUpdateBudget = vi.mocked(useUpdateBudget);
const mockUseDeleteBudget = vi.mocked(useDeleteBudget);
const mockUseBudget = vi.mocked(useBudget);
const mockUseDocumentTitle = vi.mocked(useDocumentTitle);
const mockUseNavigate = vi.mocked(useNavigate);

const createTestQueryClient = () => new QueryClient({
  defaultOptions: {
    queries: { retry: false },
    mutations: { retry: false },
  },
});

const mockBudgetSummary: BudgetSummaryResponse = {
  totalBudgets: 2,
  totalBudgeted: 1000,
  totalSpent: 650,
  totalRemaining: 350,
  currency: 'USD',
  budgets: [
    {
      budgetId: 1,
      categoryName: 'Groceries',
      amount: 500,
      spent: 350.25,
      remaining: 149.75,
      percentageSpent: 70.05,
      status: 'ON_TRACK',
      currency: 'USD',
      period: 'MONTHLY',
    },
    {
      budgetId: 2,
      categoryName: 'Entertainment',
      amount: 500,
      spent: 300,
      remaining: 200,
      percentageSpent: 60,
      status: 'ON_TRACK',
      currency: 'USD',
      period: 'MONTHLY',
    },
  ],
};

const mockEmptySummary: BudgetSummaryResponse = {
  totalBudgets: 0,
  totalBudgeted: 0,
  totalSpent: 0,
  totalRemaining: 0,
  currency: 'USD',
  budgets: [],
};

describe('BudgetsPage', () => {
  let queryClient: QueryClient;

  beforeEach(() => {
    queryClient = createTestQueryClient();
    vi.clearAllMocks();

    // Default mocks
    mockUseDocumentTitle.mockImplementation(() => {});
    mockUseBudget.mockReturnValue({
      data: undefined,
      isLoading: false,
      error: null,
    } as any);
  });

  describe('Loading State', () => {
    it('shows loading skeletons when summary is loading', () => {
      mockUseBudgetSummary.mockReturnValue({
        data: undefined,
        isLoading: true,
        error: null,
      } as any);

      mockUseCreateBudget.mockReturnValue({
        mutateAsync: vi.fn(),
        isPending: false,
      } as any);

      mockUseUpdateBudget.mockReturnValue({
        mutateAsync: vi.fn(),
        isPending: false,
      } as any);

      mockUseDeleteBudget.mockReturnValue({
        mutateAsync: vi.fn(),
        isPending: false,
      } as any);

      renderWithProviders(<BudgetsPage />, { queryClient });

      const skeletons = screen.getAllByTestId('loading-skeleton');
      expect(skeletons.length).toBeGreaterThan(0);
    });
  });

  describe('Empty State', () => {
    beforeEach(() => {
      mockUseBudgetSummary.mockReturnValue({
        data: mockEmptySummary,
        isLoading: false,
        error: null,
      } as any);

      mockUseCreateBudget.mockReturnValue({
        mutateAsync: vi.fn(),
        isPending: false,
      } as any);

      mockUseUpdateBudget.mockReturnValue({
        mutateAsync: vi.fn(),
        isPending: false,
      } as any);

      mockUseDeleteBudget.mockReturnValue({
        mutateAsync: vi.fn(),
        isPending: false,
      } as any);
    });

    it('shows "No budgets yet" empty state when there are no budgets', () => {
      renderWithProviders(<BudgetsPage />, { queryClient });

      expect(screen.getByText('No budgets yet')).toBeInTheDocument();
      expect(screen.getByText('Create your first budget to start tracking your spending')).toBeInTheDocument();
      expect(screen.getAllByRole('button', { name: 'Add Budget' })).toHaveLength(2); // Header and empty state
    });
  });

  describe('Filters Toggle', () => {
    beforeEach(() => {
      mockUseBudgetSummary.mockReturnValue({
        data: mockBudgetSummary,
        isLoading: false,
        error: null,
      } as any);

      mockUseCreateBudget.mockReturnValue({
        mutateAsync: vi.fn(),
        isPending: false,
      } as any);

      mockUseUpdateBudget.mockReturnValue({
        mutateAsync: vi.fn(),
        isPending: false,
      } as any);

      mockUseDeleteBudget.mockReturnValue({
        mutateAsync: vi.fn(),
        isPending: false,
      } as any);
    });

    it('renders Filters toggle button', () => {
      renderWithProviders(<BudgetsPage />, { queryClient });

      expect(screen.getByRole('button', { name: /filters/i })).toBeInTheDocument();
    });

    it('shows BudgetFilters panel when Filters button is clicked', () => {
      renderWithProviders(<BudgetsPage />, { queryClient });

      const filtersButton = screen.getByRole('button', { name: /filters/i });
      fireEvent.click(filtersButton);

      // BudgetFilters should be rendered (check for its content)
      expect(screen.getByLabelText('Search')).toBeInTheDocument();
      expect(screen.getByLabelText('Period')).toBeInTheDocument();
    });

    it('hides BudgetFilters panel when Filters button is clicked again', () => {
      renderWithProviders(<BudgetsPage />, { queryClient });

      const filtersButton = screen.getByRole('button', { name: /filters/i });
      fireEvent.click(filtersButton);
      fireEvent.click(filtersButton);

      // BudgetFilters should not be rendered
      expect(screen.queryByLabelText('Search')).not.toBeInTheDocument();
      expect(screen.queryByLabelText('Period')).not.toBeInTheDocument();
    });
  });

  describe('Add Budget Button', () => {
    beforeEach(() => {
      mockUseBudgetSummary.mockReturnValue({
        data: mockBudgetSummary,
        isLoading: false,
        error: null,
      } as any);

      mockUseCreateBudget.mockReturnValue({
        mutateAsync: vi.fn(),
        isPending: false,
      } as any);

      mockUseUpdateBudget.mockReturnValue({
        mutateAsync: vi.fn(),
        isPending: false,
      } as any);

      mockUseDeleteBudget.mockReturnValue({
        mutateAsync: vi.fn(),
        isPending: false,
      } as any);
    });

    it('opens create dialog when "Add Budget" button is clicked', () => {
      renderWithProviders(<BudgetsPage />, { queryClient });

      const addButton = screen.getByRole('button', { name: 'Add Budget' });
      fireEvent.click(addButton);

      expect(screen.getByText('Create Budget')).toBeInTheDocument();
      expect(screen.getByTestId('budget-form')).toBeInTheDocument();
    });
  });

  describe('Budget Cards Display', () => {
    beforeEach(() => {
      mockUseBudgetSummary.mockReturnValue({
        data: mockBudgetSummary,
        isLoading: false,
        error: null,
      } as any);

      mockUseCreateBudget.mockReturnValue({
        mutateAsync: vi.fn(),
        isPending: false,
      } as any);

      mockUseUpdateBudget.mockReturnValue({
        mutateAsync: vi.fn(),
        isPending: false,
      } as any);

      mockUseDeleteBudget.mockReturnValue({
        mutateAsync: vi.fn(),
        isPending: false,
      } as any);
    });

    it('shows budget cards when budgets exist', () => {
      renderWithProviders(<BudgetsPage />, { queryClient });

      expect(screen.getByTestId('budget-card-1')).toBeInTheDocument();
      expect(screen.getByTestId('budget-card-2')).toBeInTheDocument();
      expect(screen.getByText('Groceries')).toBeInTheDocument();
      expect(screen.getByText('Entertainment')).toBeInTheDocument();
    });

    it('shows budget summary card when budgets exist', () => {
      renderWithProviders(<BudgetsPage />, { queryClient });

      expect(screen.getByTestId('budget-summary-card')).toBeInTheDocument();
      expect(screen.getByText('Summary: 2 budgets')).toBeInTheDocument();
    });
  });

  describe('Document Title', () => {
    it('sets document title to "Budgets"', () => {
      mockUseBudgetSummary.mockReturnValue({
        data: mockEmptySummary,
        isLoading: false,
        error: null,
      } as any);

      mockUseCreateBudget.mockReturnValue({
        mutateAsync: vi.fn(),
        isPending: false,
      } as any);

      mockUseUpdateBudget.mockReturnValue({
        mutateAsync: vi.fn(),
        isPending: false,
      } as any);

      mockUseDeleteBudget.mockReturnValue({
        mutateAsync: vi.fn(),
        isPending: false,
      } as any);

      renderWithProviders(<BudgetsPage />, { queryClient });

      expect(mockUseDocumentTitle).toHaveBeenCalledWith('Budgets');
    });
  });

  describe('Error State', () => {
    it('shows error message when summary loading fails', () => {
      mockUseBudgetSummary.mockReturnValue({
        data: undefined,
        isLoading: false,
        error: new Error('Failed to load'),
      } as any);

      mockUseCreateBudget.mockReturnValue({
        mutateAsync: vi.fn(),
        isPending: false,
      } as any);

      mockUseUpdateBudget.mockReturnValue({
        mutateAsync: vi.fn(),
        isPending: false,
      } as any);

      mockUseDeleteBudget.mockReturnValue({
        mutateAsync: vi.fn(),
        isPending: false,
      } as any);

      renderWithProviders(<BudgetsPage />, { queryClient });

      expect(screen.getByText('Failed to load budgets. Please try again later.')).toBeInTheDocument();
    });
  });
});