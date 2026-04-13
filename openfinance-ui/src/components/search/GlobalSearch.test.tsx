/**
 * Tests for GlobalSearch component
 * Verifies search functionality, keyboard navigation, and goto actions
 */

import { screen, fireEvent, waitFor } from '@testing-library/react';
import { QueryClient } from '@tanstack/react-query';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { renderWithProviders } from '@/test/test-utils';
import { GlobalSearch } from './GlobalSearch';
import * as useSearchHook from '../../hooks/useSearch';
import type { GlobalSearchResponse } from '../../types/search';

const mockNavigate = vi.fn();

vi.mock('react-router', async () => {
  const actual = await vi.importActual('react-router');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: false,
    },
  },
});

describe('GlobalSearch', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
  });

  it('renders search input with correct placeholder', () => {
    renderWithProviders(<GlobalSearch />, { queryClient });
    
    const input = screen.getByPlaceholderText(/search accounts/i);
    expect(input).toBeInTheDocument();
  });

  it('shows recent searches when input is focused with no query', async () => {
    // Add recent searches to localStorage
    localStorage.setItem('recent-searches', JSON.stringify(['investment', 'groceries']));
    
    renderWithProviders(<GlobalSearch />, { queryClient });
    
    const input = screen.getByPlaceholderText(/search accounts/i);
    fireEvent.focus(input);
    
    await waitFor(() => {
      expect(screen.getByText('Recent Searches')).toBeInTheDocument();
      expect(screen.getByText('investment')).toBeInTheDocument();
      expect(screen.getByText('groceries')).toBeInTheDocument();
    });
  });

  it('navigates to account detail page when account result is clicked', async () => {
    const mockSearchResult: GlobalSearchResponse = {
      query: 'checking',
      totalResults: 1,
      resultsByType: {
        ACCOUNT: [
          {
            resultType: 'ACCOUNT',
            id: 123,
            title: 'Checking Account',
            subtitle: 'Bank of America',
            amount: 5000.00,
            currency: 'USD',
            icon: 'Wallet',
            color: '#3b82f6',
            createdAt: '2024-01-01T00:00:00',
          },
        ],
      },
      countsPerType: {
        ACCOUNT: 1,
      },
      executionTimeMs: 10,
      hasMore: false,
      limit: 50,
    };

    const mockUpdateQuery = vi.fn();
    const mockSaveRecentSearch = vi.fn();

    vi.spyOn(useSearchHook, 'useSearchWithDebounce').mockReturnValue({
      query: 'checking',
      debouncedQuery: 'checking',
      isDebouncing: false,
      updateQuery: mockUpdateQuery,
      searchResult: {
        data: mockSearchResult,
        isLoading: false,
        error: null,
        isError: false,
        isSuccess: true,
      } as any,
      saveRecentSearch: mockSaveRecentSearch,
      getRecentSearches: () => [],
    });

    renderWithProviders(<GlobalSearch />, { queryClient });
    
    const input = screen.getByPlaceholderText(/search accounts/i);
    
    // Focus to open dropdown
    fireEvent.focus(input);
    
    await waitFor(() => {
      expect(screen.getByText('Checking Account')).toBeInTheDocument();
    });

    const resultButton = screen.getByText('Checking Account').closest('button');
    fireEvent.click(resultButton!);

    expect(mockNavigate).toHaveBeenCalledWith('/accounts?highlight=123');
    expect(mockSaveRecentSearch).toHaveBeenCalledWith('checking');
  });

  it('navigates to budget detail page when budget result is clicked', async () => {
    const mockSearchResult: GlobalSearchResponse = {
      query: 'monthly',
      totalResults: 1,
      resultsByType: {
        BUDGET: [
          {
            resultType: 'BUDGET',
            id: 456,
            title: 'Monthly Budget',
            subtitle: 'January 2024',
            amount: 3000.00,
            currency: 'USD',
            icon: 'PieChart',
            color: '#10b981',
            createdAt: '2024-01-01T00:00:00',
          },
        ],
      },
      countsPerType: {
        BUDGET: 1,
      },
      executionTimeMs: 10,
      hasMore: false,
      limit: 50,
    };

    vi.spyOn(useSearchHook, 'useSearchWithDebounce').mockReturnValue({
      query: 'monthly',
      debouncedQuery: 'monthly',
      isDebouncing: false,
      updateQuery: vi.fn(),
      searchResult: {
        data: mockSearchResult,
        isLoading: false,
        error: null,
        isError: false,
        isSuccess: true,
      } as any,
      saveRecentSearch: vi.fn(),
      getRecentSearches: () => [],
    });

    renderWithProviders(<GlobalSearch />, { queryClient });
    
    const input = screen.getByPlaceholderText(/search accounts/i);
    fireEvent.focus(input); // Focus to open dropdown
    fireEvent.change(input, { target: { value: 'monthly' } });
    
    await waitFor(() => {
      expect(screen.getByText('Monthly Budget')).toBeInTheDocument();
    });

    const resultButton = screen.getByText('Monthly Budget').closest('button');
    fireEvent.click(resultButton!);

    expect(mockNavigate).toHaveBeenCalledWith('/budget/456');
  });

  it('navigates to assets page when asset result is clicked', async () => {
    const mockSearchResult: GlobalSearchResponse = {
      query: 'stock',
      totalResults: 1,
      resultsByType: {
        ASSET: [
          {
            resultType: 'ASSET',
            id: 789,
            title: 'Apple Stock',
            subtitle: 'AAPL',
            amount: 15000.00,
            currency: 'USD',
            icon: 'TrendingUp',
            color: '#8b5cf6',
            createdAt: '2024-01-01T00:00:00',
          },
        ],
      },
      countsPerType: {
        ASSET: 1,
      },
      executionTimeMs: 10,
      hasMore: false,
      limit: 50,
    };

    vi.spyOn(useSearchHook, 'useSearchWithDebounce').mockReturnValue({
      query: 'stock',
      debouncedQuery: 'stock',
      isDebouncing: false,
      updateQuery: vi.fn(),
      searchResult: {
        data: mockSearchResult,
        isLoading: false,
        error: null,
        isError: false,
        isSuccess: true,
      } as any,
      saveRecentSearch: vi.fn(),
      getRecentSearches: () => [],
    });

    renderWithProviders(<GlobalSearch />, { queryClient });
    
    const input = screen.getByPlaceholderText(/search accounts/i);
    fireEvent.focus(input); // Focus to open dropdown
    fireEvent.change(input, { target: { value: 'stock' } });
    
    await waitFor(() => {
      expect(screen.getByText('Apple Stock')).toBeInTheDocument();
    });

    const resultButton = screen.getByText('Apple Stock').closest('button');
    fireEvent.click(resultButton!);

    expect(mockNavigate).toHaveBeenCalledWith('/assets?highlight=789');
  });

  it('navigates to real estate page when real estate result is clicked', async () => {
    const mockSearchResult: GlobalSearchResponse = {
      query: 'apartment',
      totalResults: 1,
      resultsByType: {
        REAL_ESTATE: [
          {
            resultType: 'REAL_ESTATE',
            id: 101,
            title: 'Downtown Apartment',
            subtitle: '123 Main St',
            amount: 350000.00,
            currency: 'USD',
            icon: 'Home',
            color: '#f59e0b',
            createdAt: '2024-01-01T00:00:00',
          },
        ],
      },
      countsPerType: {
        REAL_ESTATE: 1,
      },
      executionTimeMs: 10,
      hasMore: false,
      limit: 50,
    };

    vi.spyOn(useSearchHook, 'useSearchWithDebounce').mockReturnValue({
      query: 'apartment',
      debouncedQuery: 'apartment',
      isDebouncing: false,
      updateQuery: vi.fn(),
      searchResult: {
        data: mockSearchResult,
        isLoading: false,
        error: null,
        isError: false,
        isSuccess: true,
      } as any,
      saveRecentSearch: vi.fn(),
      getRecentSearches: () => [],
    });

    renderWithProviders(<GlobalSearch />, { queryClient });
    
    const input = screen.getByPlaceholderText(/search accounts/i);
    fireEvent.focus(input); // Focus to open dropdown
    fireEvent.change(input, { target: { value: 'apartment' } });
    
    await waitFor(() => {
      expect(screen.getByText('Downtown Apartment')).toBeInTheDocument();
    });

    const resultButton = screen.getByText('Downtown Apartment').closest('button');
    fireEvent.click(resultButton!);

    expect(mockNavigate).toHaveBeenCalledWith('/real-estate?highlight=101');
  });

  it('navigates to liabilities page when liability result is clicked', async () => {
    const mockSearchResult: GlobalSearchResponse = {
      query: 'mortgage',
      totalResults: 1,
      resultsByType: {
        LIABILITY: [
          {
            resultType: 'LIABILITY',
            id: 202,
            title: 'Home Mortgage',
            subtitle: 'Wells Fargo',
            amount: 250000.00,
            currency: 'USD',
            icon: 'CreditCard',
            color: '#ef4444',
            createdAt: '2024-01-01T00:00:00',
          },
        ],
      },
      countsPerType: {
        LIABILITY: 1,
      },
      executionTimeMs: 10,
      hasMore: false,
      limit: 50,
    };

    vi.spyOn(useSearchHook, 'useSearchWithDebounce').mockReturnValue({
      query: 'mortgage',
      debouncedQuery: 'mortgage',
      isDebouncing: false,
      updateQuery: vi.fn(),
      searchResult: {
        data: mockSearchResult,
        isLoading: false,
        error: null,
        isError: false,
        isSuccess: true,
      } as any,
      saveRecentSearch: vi.fn(),
      getRecentSearches: () => [],
    });

    renderWithProviders(<GlobalSearch />, { queryClient });
    
    const input = screen.getByPlaceholderText(/search accounts/i);
    fireEvent.focus(input); // Focus to open dropdown
    fireEvent.change(input, { target: { value: 'mortgage' } });
    
    await waitFor(() => {
      expect(screen.getByText('Home Mortgage')).toBeInTheDocument();
    });

    const resultButton = screen.getByText('Home Mortgage').closest('button');
    fireEvent.click(resultButton!);

    expect(mockNavigate).toHaveBeenCalledWith('/liabilities?highlight=202');
  });

  it('navigates to transactions page when transaction result is clicked', async () => {
    const mockSearchResult: GlobalSearchResponse = {
      query: 'grocery',
      totalResults: 1,
      resultsByType: {
        TRANSACTION: [
          {
            resultType: 'TRANSACTION',
            id: 303,
            title: 'Whole Foods',
            subtitle: 'Groceries',
            amount: 85.50,
            currency: 'USD',
            date: '2024-01-15',
            icon: 'ShoppingCart',
            color: '#10b981',
            createdAt: '2024-01-15T00:00:00',
          },
        ],
      },
      countsPerType: {
        TRANSACTION: 1,
      },
      executionTimeMs: 10,
      hasMore: false,
      limit: 50,
    };

    vi.spyOn(useSearchHook, 'useSearchWithDebounce').mockReturnValue({
      query: 'grocery',
      debouncedQuery: 'grocery',
      isDebouncing: false,
      updateQuery: vi.fn(),
      searchResult: {
        data: mockSearchResult,
        isLoading: false,
        error: null,
        isError: false,
        isSuccess: true,
      } as any,
      saveRecentSearch: vi.fn(),
      getRecentSearches: () => [],
    });

    renderWithProviders(<GlobalSearch />, { queryClient });
    
    const input = screen.getByPlaceholderText(/search accounts/i);
    fireEvent.focus(input); // Focus to open dropdown
    fireEvent.change(input, { target: { value: 'grocery' } });
    
    await waitFor(() => {
      expect(screen.getByText('Whole Foods')).toBeInTheDocument();
    });

    const resultButton = screen.getByText('Whole Foods').closest('button');
    fireEvent.click(resultButton!);

    expect(mockNavigate).toHaveBeenCalledWith('/transactions?highlight=303');
  });

  it('supports keyboard navigation with arrow keys', async () => {
    const mockSearchResult: GlobalSearchResponse = {
      query: 'test',
      totalResults: 2,
      resultsByType: {
        ACCOUNT: [
          {
            resultType: 'ACCOUNT',
            id: 1,
            title: 'Account 1',
            createdAt: '2024-01-01T00:00:00',
          },
          {
            resultType: 'ACCOUNT',
            id: 2,
            title: 'Account 2',
            createdAt: '2024-01-01T00:00:00',
          },
        ],
      },
      countsPerType: {
        ACCOUNT: 2,
      },
      executionTimeMs: 10,
      hasMore: false,
      limit: 50,
    };

    vi.spyOn(useSearchHook, 'useSearchWithDebounce').mockReturnValue({
      query: 'test',
      debouncedQuery: 'test',
      isDebouncing: false,
      updateQuery: vi.fn(),
      searchResult: {
        data: mockSearchResult,
        isLoading: false,
        error: null,
        isError: false,
        isSuccess: true,
      } as any,
      saveRecentSearch: vi.fn(),
      getRecentSearches: () => [],
    });

    renderWithProviders(<GlobalSearch />, { queryClient });
    
    const input = screen.getByPlaceholderText(/search accounts/i);
    fireEvent.focus(input); // Focus to open dropdown
    fireEvent.change(input, { target: { value: 'test' } });
    
    await waitFor(() => {
      expect(screen.getByText('Account 1')).toBeInTheDocument();
    });

    // Press arrow down to select first result
    fireEvent.keyDown(input, { key: 'ArrowDown' });
    
    // Press enter to navigate
    fireEvent.keyDown(input, { key: 'Enter' });

    expect(mockNavigate).toHaveBeenCalledWith('/accounts?highlight=1');
  });

  it('clears search and closes dropdown when clear button is clicked', async () => {
    const mockUpdateQuery = vi.fn();
    
    vi.spyOn(useSearchHook, 'useSearchWithDebounce').mockReturnValue({
      query: 'test query',
      debouncedQuery: 'test query',
      isDebouncing: false,
      updateQuery: mockUpdateQuery,
      searchResult: {
        data: undefined,
        isLoading: false,
        error: null,
        isError: false,
        isSuccess: false,
      } as any,
      saveRecentSearch: vi.fn(),
      getRecentSearches: () => [],
    });

    renderWithProviders(<GlobalSearch />, { queryClient });
    
    const clearButton = screen.getByRole('button', { name: '' });
    fireEvent.click(clearButton);

    expect(mockUpdateQuery).toHaveBeenCalledWith('');
  });
});
