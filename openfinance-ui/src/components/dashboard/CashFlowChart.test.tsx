import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import CashFlowChart from './CashFlowChart';
import { MemoryRouter } from 'react-router';
import { I18nextProvider } from 'react-i18next';
import i18n from '@/i18n';
import { VisibilityProvider } from '@/context/VisibilityContext';
import { NumberFormatProvider } from '@/context/NumberFormatContext';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

// Mock Recharts to avoid rendering complexities in test


// Mock UserSettings hook which is used by NumberFormatProvider
vi.mock('@/hooks/useUserSettings', () => ({
  useUserSettings: () => ({ data: { numberFormat: '1,234.56' }, isLoading: false }),
  useUpdateUserSettings: () => ({ mutate: vi.fn() }),
}));

const queryClient = new QueryClient();

const mockCashFlow = {
  income: 5000,
  expenses: 3000,
  netCashFlow: 2000,
};

const renderWithProviders = (ui: React.ReactElement) => {
  return render(
    <QueryClientProvider client={queryClient}>
      <VisibilityProvider>
        <I18nextProvider i18n={i18n}>
          <NumberFormatProvider>
            <MemoryRouter>
              {ui}
            </MemoryRouter>
          </NumberFormatProvider>
        </I18nextProvider>
      </VisibilityProvider>
    </QueryClientProvider>
  );
};

describe('CashFlowChart Smoke Test', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    Element.prototype.scrollIntoView = vi.fn();
    i18n.changeLanguage('en');
  });

  it('renders correctly with data and shows HelpTooltip', () => {
    renderWithProviders(<CashFlowChart cashFlow={mockCashFlow} />);

    // Check if title is present
    expect(screen.getByText(/cashFlowChart.title/i)).toBeInTheDocument();

    // Check if HelpTooltip icon is present
    const helpButton = screen.getByRole('button', { name: /help/i });
    expect(helpButton).toBeInTheDocument();
  });
});
