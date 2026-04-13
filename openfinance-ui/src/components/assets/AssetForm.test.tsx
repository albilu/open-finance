/**
 * AssetForm Validation Tests
 * TASK-13.2.3: Form validation tests for AssetForm component
 *
 * Tests the AssetForm's Zod validation, rendering, and user interactions.
 * Complex child components (CurrencySelector, AccountSelector, ExchangeRateDisplay)
 * are mocked to isolate form logic testing.
 */
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { screen, waitFor, fireEvent } from '@testing-library/react';
import { renderWithProviders, mockAuthentication, clearAuthentication, userEvent } from '@/test/test-utils';
import { AssetForm } from '@/components/assets/AssetForm';

// Mock complex child components that use their own hooks internally
vi.mock('@/components/ui/CurrencySelector', () => ({
  CurrencySelector: ({ value, onValueChange, placeholder }: any) => (
    <select
      data-testid="currency-selector"
      value={value || ''}
      onChange={(e) => onValueChange(e.target.value)}
    >
      <option value="">{placeholder || 'Select currency'}</option>
      <option value="USD">USD</option>
      <option value="EUR">EUR</option>
    </select>
  ),
}));

vi.mock('@/components/ui/AccountSelector', () => ({
  AccountSelector: ({ value, onValueChange, placeholder }: any) => (
    <select
      data-testid="account-selector"
      value={value || ''}
      onChange={(e) => onValueChange(e.target.value ? Number(e.target.value) : undefined)}
    >
      <option value="">{placeholder || 'Select account'}</option>
      <option value="1">Account 1</option>
    </select>
  ),
}));

vi.mock('@/components/ui/ExchangeRateDisplay', () => ({
  ExchangeRateInline: () => <span data-testid="exchange-rate-inline" />,
}));

const mockOnSubmit = vi.fn();
const mockOnCancel = vi.fn();

describe('AssetForm', () => {
  beforeEach(() => {
    clearAuthentication();
    mockAuthentication();
    mockOnSubmit.mockClear();
    mockOnCancel.mockClear();
  });

  describe('Rendering', () => {
    it('should render all required form fields', () => {
      renderWithProviders(
        <AssetForm onSubmit={mockOnSubmit} onCancel={mockOnCancel} />
      );

      expect(screen.getByLabelText(/Asset Name/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/Asset Type/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/Quantity/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/Purchase Price/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/Current Price/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/Purchase Date/i)).toBeInTheDocument();
    });

    it('should show "Create Asset" button for new asset', () => {
      renderWithProviders(
        <AssetForm onSubmit={mockOnSubmit} onCancel={mockOnCancel} />
      );

      expect(screen.getByRole('button', { name: /create asset/i })).toBeInTheDocument();
    });

    it('should show Cancel button', () => {
      renderWithProviders(
        <AssetForm onSubmit={mockOnSubmit} onCancel={mockOnCancel} />
      );

      expect(screen.getByRole('button', { name: /cancel/i })).toBeInTheDocument();
    });

    it('should render asset type options', () => {
      renderWithProviders(
        <AssetForm onSubmit={mockOnSubmit} onCancel={mockOnCancel} />
      );

      const typeSelect = screen.getByLabelText(/Asset Type/i) as HTMLSelectElement;
      // STOCK, ETF, CRYPTO, BOND, MUTUAL_FUND, COMMODITY, VEHICLE, JEWELRY, COLLECTIBLE, ELECTRONICS, FURNITURE, OTHER = 12
      expect(typeSelect.options.length).toBeGreaterThanOrEqual(10);
    });

    it('should show symbol field for financial assets', () => {
      renderWithProviders(
        <AssetForm onSubmit={mockOnSubmit} onCancel={mockOnCancel} />
      );

      // Default type is STOCK, so symbol should be visible
      expect(screen.getByLabelText(/Symbol/i)).toBeInTheDocument();
    });

    it('should render notes field', () => {
      renderWithProviders(
        <AssetForm onSubmit={mockOnSubmit} onCancel={mockOnCancel} />
      );

      expect(screen.getByLabelText(/Notes/i)).toBeInTheDocument();
    });
  });

  describe('Validation', () => {
    it('should show error when name is empty on submit', async () => {
      const user = userEvent.setup();
      renderWithProviders(
        <AssetForm onSubmit={mockOnSubmit} onCancel={mockOnCancel} />
      );

      // Wait for useEffect/reset to complete - purchase date gets set by defaultValues
      const purchaseDateInput = screen.getByLabelText(/Purchase Date/i);
      await waitFor(() => {
        expect(purchaseDateInput).toHaveValue();
      });

      // Ensure the name field is empty (it should be by default after reset)
      const nameInput = screen.getByLabelText(/Asset Name/i);
      await user.clear(nameInput);

      // Set currency to valid value via mock selector (Controller-based)
      const currencySelector = screen.getByTestId('currency-selector');
      fireEvent.change(currencySelector, { target: { value: 'USD' } });

      // Set valid numeric values so only name validation fails
      const quantityInput = screen.getByLabelText(/Quantity/i);
      await user.clear(quantityInput);
      await user.type(quantityInput, '10');

      const purchasePriceInput = screen.getByLabelText(/Purchase Price/i);
      await user.clear(purchasePriceInput);
      await user.type(purchasePriceInput, '100');

      const currentPriceInput = screen.getByLabelText(/Current Price/i);
      await user.clear(currentPriceInput);
      await user.type(currentPriceInput, '100');

      const submitButton = screen.getByRole('button', { name: /create asset/i });
      await user.click(submitButton);

      await waitFor(() => {
        expect(screen.getByText(/Name is required/i)).toBeInTheDocument();
      });
      expect(mockOnSubmit).not.toHaveBeenCalled();
    });

    it('should show error when quantity is zero', async () => {
      const user = userEvent.setup();
      renderWithProviders(
        <AssetForm onSubmit={mockOnSubmit} onCancel={mockOnCancel} />
      );

      // Wait for useEffect/reset to complete
      const purchaseDateInput = screen.getByLabelText(/Purchase Date/i);
      await waitFor(() => {
        expect(purchaseDateInput).toHaveValue();
      });

      // Fill required name
      const nameInput = screen.getByLabelText(/Asset Name/i);
      await user.type(nameInput, 'Test Asset');

      // Set currency to valid value
      const currencySelector = screen.getByTestId('currency-selector');
      fireEvent.change(currencySelector, { target: { value: 'USD' } });

      // Set quantity to 0
      const quantityInput = screen.getByLabelText(/Quantity/i);
      fireEvent.change(quantityInput, { target: { value: '0' } });

      // Set valid purchase and current prices so only quantity fails
      const purchasePriceInput = screen.getByLabelText(/Purchase Price/i);
      fireEvent.change(purchasePriceInput, { target: { value: '100' } });

      const currentPriceInput = screen.getByLabelText(/Current Price/i);
      fireEvent.change(currentPriceInput, { target: { value: '100' } });

      // Submit via fireEvent.submit on the form for reliable async handling
      const form = document.querySelector('form')!;
      fireEvent.submit(form);

      await waitFor(() => {
        expect(screen.getByText(/Quantity must be greater than 0/i)).toBeInTheDocument();
      }, { timeout: 3000 });
      expect(mockOnSubmit).not.toHaveBeenCalled();
    });

    it('should show error when purchase price is zero', async () => {
      const user = userEvent.setup();
      renderWithProviders(
        <AssetForm onSubmit={mockOnSubmit} onCancel={mockOnCancel} />
      );

      // Wait for useEffect/reset to complete
      const purchaseDateInput = screen.getByLabelText(/Purchase Date/i);
      await waitFor(() => {
        expect(purchaseDateInput).toHaveValue();
      });

      // Fill required fields
      const nameInput = screen.getByLabelText(/Asset Name/i);
      await user.type(nameInput, 'Test Asset');

      // Set currency to valid value
      const currencySelector = screen.getByTestId('currency-selector');
      fireEvent.change(currencySelector, { target: { value: 'USD' } });

      // Set quantity to valid value
      const quantityInput = screen.getByLabelText(/Quantity/i);
      fireEvent.change(quantityInput, { target: { value: '10' } });

      // Set purchase price to 0
      const purchasePriceInput = screen.getByLabelText(/Purchase Price/i);
      fireEvent.change(purchasePriceInput, { target: { value: '0' } });

      // Set current price to valid value
      const currentPriceInput = screen.getByLabelText(/Current Price/i);
      fireEvent.change(currentPriceInput, { target: { value: '100' } });

      // Submit via fireEvent.submit on the form for reliable async handling
      const form = document.querySelector('form')!;
      fireEvent.submit(form);

      await waitFor(() => {
        expect(screen.getByText(/Purchase price must be greater than 0/i)).toBeInTheDocument();
      }, { timeout: 3000 });
      expect(mockOnSubmit).not.toHaveBeenCalled();
    });

    it('should show error when current price is zero', async () => {
      const user = userEvent.setup();
      renderWithProviders(
        <AssetForm onSubmit={mockOnSubmit} onCancel={mockOnCancel} />
      );

      // Wait for useEffect/reset to complete
      const purchaseDateInput = screen.getByLabelText(/Purchase Date/i);
      await waitFor(() => {
        expect(purchaseDateInput).toHaveValue();
      });

      // Fill required fields
      const nameInput = screen.getByLabelText(/Asset Name/i);
      await user.type(nameInput, 'Test Asset');

      // Set currency to valid value
      const currencySelector = screen.getByTestId('currency-selector');
      fireEvent.change(currencySelector, { target: { value: 'USD' } });

      // Set quantity to valid value
      const quantityInput = screen.getByLabelText(/Quantity/i);
      fireEvent.change(quantityInput, { target: { value: '10' } });

      // Set purchase price to valid value
      const purchasePriceInput = screen.getByLabelText(/Purchase Price/i);
      fireEvent.change(purchasePriceInput, { target: { value: '100' } });

      // Set current price to 0
      const currentPriceInput = screen.getByLabelText(/Current Price/i);
      fireEvent.change(currentPriceInput, { target: { value: '0' } });

      // Submit via fireEvent.submit on the form for reliable async handling
      const form = document.querySelector('form')!;
      fireEvent.submit(form);

      await waitFor(() => {
        expect(screen.getByText(/Current price must be greater than 0/i)).toBeInTheDocument();
      }, { timeout: 3000 });
      expect(mockOnSubmit).not.toHaveBeenCalled();
    });
  });

  describe('Submission', () => {
    it('should call onCancel when Cancel button is clicked', async () => {
      const user = userEvent.setup();
      renderWithProviders(
        <AssetForm onSubmit={mockOnSubmit} onCancel={mockOnCancel} />
      );

      const cancelButton = screen.getByRole('button', { name: /cancel/i });
      await user.click(cancelButton);

      expect(mockOnCancel).toHaveBeenCalledTimes(1);
    });
  });

  describe('Loading state', () => {
    it('should disable Cancel button when isLoading is true', () => {
      renderWithProviders(
        <AssetForm onSubmit={mockOnSubmit} onCancel={mockOnCancel} isLoading={true} />
      );

      expect(screen.getByRole('button', { name: /cancel/i })).toBeDisabled();
    });
  });

  describe('Physical asset fields', () => {
    it('should show physical asset fields when type is VEHICLE', async () => {
      renderWithProviders(
        <AssetForm onSubmit={mockOnSubmit} onCancel={mockOnCancel} />
      );

      const typeSelect = screen.getByLabelText(/Asset Type/i);
      fireEvent.change(typeSelect, { target: { value: 'VEHICLE' } });

      await waitFor(() => {
        expect(screen.getByLabelText(/Condition/i)).toBeInTheDocument();
        expect(screen.getByLabelText(/Brand/i)).toBeInTheDocument();
        expect(screen.getByLabelText(/Model/i)).toBeInTheDocument();
      });
    });

    it('should hide symbol field for physical asset types', async () => {
      renderWithProviders(
        <AssetForm onSubmit={mockOnSubmit} onCancel={mockOnCancel} />
      );

      const typeSelect = screen.getByLabelText(/Asset Type/i);
      fireEvent.change(typeSelect, { target: { value: 'JEWELRY' } });

      await waitFor(() => {
        expect(screen.queryByLabelText(/Symbol/i)).not.toBeInTheDocument();
      });
    });
  });
});
