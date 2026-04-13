import { renderHook, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import React from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useCountryToolConfig } from './useCountryToolConfig';
import apiClient from '@/services/apiClient';
import { useAuthContext } from '@/context/AuthContext';

vi.mock('@/context/AuthContext');
const mockedUseAuthContext = useAuthContext as any;

vi.mock('@/services/apiClient');
const mockedApiClient = apiClient as any;

function createWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false, gcTime: 0 },
    },
  });
  return ({ children }: { children: React.ReactNode }) =>
    React.createElement(QueryClientProvider, { client: queryClient }, children);
}

describe('useCountryToolConfig', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockedUseAuthContext.mockReturnValue({ isAuthenticated: true });
  });

  it('should return French config when user settings have country FR', async () => {
    mockedApiClient.get.mockResolvedValue({
      data: {
        id: 1,
        userId: 1,
        theme: 'light',
        dateFormat: 'DD/MM/YYYY',
        numberFormat: '1 234,56',
        language: 'fr',
        timezone: 'Europe/Paris',
        country: 'FR',
        createdAt: '2025-01-01',
        updatedAt: '2025-01-01',
      },
    });

    const { result } = renderHook(() => useCountryToolConfig(), { wrapper: createWrapper() });

    await waitFor(() => {
      expect(result.current.countryCode).toBe('FR');
    });

    expect(result.current.buyVsRentConfig.countryCode).toBe('FR');
    expect(result.current.buyVsRentConfig.currency).toBe('EUR');
    expect(result.current.buyVsRentConfig.notaryFeesPercent).toBe(7);
    expect(result.current.isPropertyRentalAvailable).toBe(true);
    expect(result.current.earlyPayoffConfig.hasIRA).toBe(true);
    expect(result.current.earlyPayoffConfig.iraMonthsInterestCap).toBe(6);
    expect(result.current.earlyPayoffConfig.iraCapitalPercentCap).toBe(0.03);
  });

  it('should return US config when user settings have country US', async () => {
    mockedApiClient.get.mockResolvedValue({
      data: {
        id: 1,
        userId: 1,
        theme: 'dark',
        dateFormat: 'MM/DD/YYYY',
        numberFormat: '1,234.56',
        language: 'en',
        timezone: 'America/New_York',
        country: 'US',
        createdAt: '2025-01-01',
        updatedAt: '2025-01-01',
      },
    });

    const { result } = renderHook(() => useCountryToolConfig(), { wrapper: createWrapper() });

    await waitFor(() => {
      expect(result.current.countryCode).toBe('US');
    });

    expect(result.current.buyVsRentConfig.countryCode).toBe('US');
    expect(result.current.buyVsRentConfig.currency).toBe('USD');
    expect(result.current.buyVsRentConfig.interestRate).toBe(7.0);
    expect(result.current.isPropertyRentalAvailable).toBe(false);
    expect(result.current.earlyPayoffConfig.hasIRA).toBe(false);
  });

  it('should return GB config for United Kingdom', async () => {
    mockedApiClient.get.mockResolvedValue({
      data: {
        id: 1,
        userId: 1,
        theme: 'light',
        dateFormat: 'DD/MM/YYYY',
        numberFormat: '1,234.56',
        language: 'en',
        timezone: 'Europe/London',
        country: 'GB',
        createdAt: '2025-01-01',
        updatedAt: '2025-01-01',
      },
    });

    const { result } = renderHook(() => useCountryToolConfig(), { wrapper: createWrapper() });

    await waitFor(() => {
      expect(result.current.countryCode).toBe('GB');
    });

    expect(result.current.buyVsRentConfig.countryCode).toBe('GB');
    expect(result.current.buyVsRentConfig.currency).toBe('GBP');
    expect(result.current.isPropertyRentalAvailable).toBe(false);
    expect(result.current.earlyPayoffConfig.hasIRA).toBe(false);
  });

  it('should fall back to FR config for unknown country', async () => {
    mockedApiClient.get.mockResolvedValue({
      data: {
        id: 1,
        userId: 1,
        theme: 'light',
        dateFormat: 'DD/MM/YYYY',
        numberFormat: '1 234,56',
        language: 'fr',
        timezone: 'UTC',
        country: 'ZZ',
        createdAt: '2025-01-01',
        updatedAt: '2025-01-01',
      },
    });

    const { result } = renderHook(() => useCountryToolConfig(), { wrapper: createWrapper() });

    await waitFor(() => {
      expect(result.current.countryCode).toBe('ZZ');
    });

    // getBuyVsRentConfig falls back to FR for unknown codes
    expect(result.current.buyVsRentConfig.countryCode).toBe('FR');
    expect(result.current.buyVsRentConfig.currency).toBe('EUR');
    // Property rental not available for non-FR
    expect(result.current.isPropertyRentalAvailable).toBe(false);
    // Early payoff falls back to no-IRA default for unknown codes
    expect(result.current.earlyPayoffConfig.hasIRA).toBe(false);
  });

  it('should default to FR when user settings not loaded yet', () => {
    // Mock a pending (never-resolving) settings query
    mockedApiClient.get.mockReturnValue(new Promise(() => {}));

    const { result } = renderHook(() => useCountryToolConfig(), { wrapper: createWrapper() });

    // Before settings load, country defaults to 'FR'
    expect(result.current.countryCode).toBe('FR');
    expect(result.current.buyVsRentConfig.countryCode).toBe('FR');
    expect(result.current.isPropertyRentalAvailable).toBe(true);
    expect(result.current.earlyPayoffConfig.hasIRA).toBe(true);
  });

  it('should provide correct buyVsRentInitialInputs for French config', async () => {
    mockedApiClient.get.mockResolvedValue({
      data: {
        id: 1,
        userId: 1,
        theme: 'light',
        dateFormat: 'DD/MM/YYYY',
        numberFormat: '1 234,56',
        language: 'fr',
        timezone: 'Europe/Paris',
        country: 'FR',
        createdAt: '2025-01-01',
        updatedAt: '2025-01-01',
      },
    });

    const { result } = renderHook(() => useCountryToolConfig(), { wrapper: createWrapper() });

    await waitFor(() => {
      expect(result.current.countryCode).toBe('FR');
    });

    const inputs = result.current.buyVsRentInitialInputs;

    // purchase fields overridden by FR config
    expect(inputs.purchase.notaryFeesPercent).toBe(7);
    expect(inputs.purchase.guaranteeFees).toBe(2750);
    expect(inputs.purchase.applicationFees).toBe(2000);
    expect(inputs.purchase.accountFees).toBe(720);
    expect(inputs.purchase.garbageTax).toBe(150);
    expect(inputs.purchase.interestRate).toBe(4.2);

    // rental fields overridden by FR config
    expect(inputs.rental.garbageTax).toBe(150);

    // market fields overridden by FR config
    expect(inputs.market.priceEvolution).toBe(2);
    expect(inputs.market.rentEvolution).toBe(2);

    // non-overridden fields preserved from DEFAULT_BUY_RENT_INPUTS
    expect(inputs.purchase.propertyPrice).toBe(300000);
    expect(inputs.purchase.downPayment).toBe(60000);
    expect(inputs.rental.monthlyRent).toBe(1200);
    expect(inputs.resale.targetYear).toBe(10);
  });

  it('should provide correct buyVsRentInitialInputs for US config', async () => {
    mockedApiClient.get.mockResolvedValue({
      data: {
        id: 1,
        userId: 1,
        theme: 'dark',
        dateFormat: 'MM/DD/YYYY',
        numberFormat: '1,234.56',
        language: 'en',
        timezone: 'America/New_York',
        country: 'US',
        createdAt: '2025-01-01',
        updatedAt: '2025-01-01',
      },
    });

    const { result } = renderHook(() => useCountryToolConfig(), { wrapper: createWrapper() });

    await waitFor(() => {
      expect(result.current.countryCode).toBe('US');
    });

    const inputs = result.current.buyVsRentInitialInputs;

    // US-specific overrides
    expect(inputs.purchase.notaryFeesPercent).toBe(2.5);
    expect(inputs.purchase.guaranteeFees).toBe(0);
    expect(inputs.purchase.applicationFees).toBe(1000);
    expect(inputs.purchase.accountFees).toBe(0);
    expect(inputs.purchase.garbageTax).toBe(0);
    expect(inputs.purchase.interestRate).toBe(7.0);
    expect(inputs.rental.garbageTax).toBe(0);
    expect(inputs.market.priceEvolution).toBe(3);
    expect(inputs.market.rentEvolution).toBe(3);
  });

  it('should return Germany config with correct values', async () => {
    mockedApiClient.get.mockResolvedValue({
      data: {
        id: 1,
        userId: 1,
        theme: 'light',
        dateFormat: 'DD/MM/YYYY',
        numberFormat: '1.234,56',
        language: 'de',
        timezone: 'Europe/Berlin',
        country: 'DE',
        createdAt: '2025-01-01',
        updatedAt: '2025-01-01',
      },
    });

    const { result } = renderHook(() => useCountryToolConfig(), { wrapper: createWrapper() });

    await waitFor(() => {
      expect(result.current.countryCode).toBe('DE');
    });

    expect(result.current.buyVsRentConfig.countryCode).toBe('DE');
    expect(result.current.buyVsRentConfig.currency).toBe('EUR');
    expect(result.current.buyVsRentConfig.notaryFeesPercent).toBe(5);
    expect(result.current.buyVsRentConfig.interestRate).toBe(4.0);
    expect(result.current.isPropertyRentalAvailable).toBe(false);
    expect(result.current.earlyPayoffConfig.hasIRA).toBe(false);
  });
});
