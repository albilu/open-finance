/**
 * Unit tests for AmortizationSchedule component
 * Focus: Test privacy implementation with PrivateAmount wrapping of currency displays
 */
import { screen } from '@testing-library/react';
import { vi, describe, it, expect } from 'vitest';
import React from 'react';
import { renderWithProviders } from '@/test/test-utils';
import { AmortizationSchedule } from '../AmortizationSchedule';
import type { AmortizationSchedule as AmortizationScheduleType } from '@/types/liability';

// Mock VisibilityContext
vi.mock('@/context/VisibilityContext', () => ({
  VisibilityProvider: ({ children }: { children: React.ReactNode }) => <>{children}</>,
  useVisibility: vi.fn(() => ({ isAmountsVisible: true })),
}));

import { useVisibility } from '@/context/VisibilityContext';

// Mock formatCurrency to return predictable strings
vi.mock('@/hooks/useLiabilities', () => ({
  formatCurrency: vi.fn((amount: number, currency: string) => `$${amount.toFixed(2)}`),
}));

// Test fixtures
const mockSchedule: AmortizationScheduleType = {
  liabilityId: 1,
  liabilityName: 'Home Mortgage',
  principal: 300000,
  interestRate: 3.5,
  termMonths: 360,
  monthlyPayment: 1500,
  totalPayments: 540000,
  totalInterest: 240000,
  totalAmount: 540000,
  currency: 'USD',
  payments: [
    {
      paymentNumber: 1,
      paymentDate: '2020-02-01',
      paymentAmount: 1500,
      principalPayment: 1200,
      interestPayment: 300,
      remainingBalance: 299800,
    },
    {
      paymentNumber: 2,
      paymentDate: '2020-03-01',
      paymentAmount: 1500,
      principalPayment: 1202,
      interestPayment: 298,
      remainingBalance: 299598,
    },
  ],
};

describe('AmortizationSchedule', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    (useVisibility as any).mockReturnValue({ isAmountsVisible: true });
  });

  describe('Privacy Implementation - PrivateAmount Wrapping', () => {
    it('should wrap currency displays with PrivateAmount component', () => {
      renderWithProviders(<AmortizationSchedule schedule={mockSchedule} />);

      // Check that PrivateAmount components are rendered for currency values
      const privateAmounts = document.querySelectorAll('.transition-all.duration-300');
      expect(privateAmounts.length).toBeGreaterThan(0);
    });

    it('should apply blur effect when amounts are hidden', () => {
      (useVisibility as any).mockReturnValue({ isAmountsVisible: false });

      renderWithProviders(<AmortizationSchedule schedule={mockSchedule} />);

      // Check that elements have blur class when amounts are hidden
      const blurredElements = document.querySelectorAll('.blur-md.select-none');
      expect(blurredElements.length).toBeGreaterThan(0);
    });

    it('should not apply blur effect when amounts are visible', () => {
      (useVisibility as any).mockReturnValue({ isAmountsVisible: true });

      renderWithProviders(<AmortizationSchedule schedule={mockSchedule} />);

      // Check that no elements have blur class when amounts are visible
      const blurredElements = document.querySelectorAll('.blur-md');
      expect(blurredElements.length).toBe(0);
    });

    it('should set aria-hidden when amounts are hidden', () => {
      (useVisibility as any).mockReturnValue({ isAmountsVisible: false });

      renderWithProviders(<AmortizationSchedule schedule={mockSchedule} />);

      // Check that PrivateAmount spans have aria-hidden=true
      const hiddenSpans = document.querySelectorAll('span[aria-hidden="true"]');
      expect(hiddenSpans.length).toBeGreaterThan(0);
    });
  });

  describe('Component Rendering', () => {
    it('should render summary section with key metrics', () => {
      renderWithProviders(<AmortizationSchedule schedule={mockSchedule} />);

      expect(screen.getByText('Home Mortgage - Amortization Schedule')).toBeInTheDocument();
      expect(screen.getByText('Interest Rate')).toBeInTheDocument();
      expect(screen.getByText('Monthly Payment')).toBeInTheDocument();
      expect(screen.getByText('3.50%')).toBeInTheDocument();
      expect(screen.getByText('360 months')).toBeInTheDocument();
    });

    it('should render amortization table with headers', () => {
      renderWithProviders(<AmortizationSchedule schedule={mockSchedule} />);

      // Check table headers specifically
      const table = document.querySelector('table');
      expect(table).toBeInTheDocument();

      const headers = table?.querySelectorAll('th');
      expect(headers?.length).toBe(6); // 6 columns

      expect(headers?.[0]).toHaveTextContent('Payment #');
      expect(headers?.[1]).toHaveTextContent('Date');
      expect(headers?.[2]).toHaveTextContent('Payment');
      expect(headers?.[3]).toHaveTextContent('Principal');
      expect(headers?.[4]).toHaveTextContent('Interest');
      expect(headers?.[5]).toHaveTextContent('Remaining Balance');
    });

    it('should display schedule data correctly', () => {
      renderWithProviders(<AmortizationSchedule schedule={mockSchedule} />);

      // Check that table contains data rows
      const table = document.querySelector('table');
      expect(table).toBeInTheDocument();

      // Should have multiple rows (header + data rows)
      const rows = table?.querySelectorAll('tbody tr');
      expect(rows?.length).toBeGreaterThan(0);
    });
  });

  describe('Edge Cases', () => {
    it('should handle liability with zero interest rate', () => {
      const zeroInterestSchedule = { ...mockSchedule, interestRate: 0 };

      renderWithProviders(<AmortizationSchedule schedule={zeroInterestSchedule} />);

      expect(screen.getByText('0.00%')).toBeInTheDocument();
    });

    it('should handle liability with zero balance', () => {
      const zeroBalanceSchedule = { ...mockSchedule, principal: 0 };

      renderWithProviders(<AmortizationSchedule schedule={zeroBalanceSchedule} />);

      expect(screen.getByText('$0.00')).toBeInTheDocument();
    });
  });

  describe('User Interactions', () => {
    it('should have scrollable table container', () => {
      renderWithProviders(<AmortizationSchedule schedule={mockSchedule} />);

      const scrollContainer = document.querySelector('.overflow-x-auto');
      expect(scrollContainer).toBeInTheDocument();
    });
  });
});