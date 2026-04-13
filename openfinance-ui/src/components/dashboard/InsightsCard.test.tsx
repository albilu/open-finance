import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import InsightsCard from './InsightsCard';
import { MemoryRouter } from 'react-router';
import { I18nextProvider } from 'react-i18next';
import i18n from '@/i18n';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { InsightType, InsightPriority } from '@/types/insight';

// Mock hooks
vi.mock('@/hooks/useInsights', () => ({
  useTopInsights: vi.fn(),
  useGenerateInsights: () => ({ mutateAsync: vi.fn(), isPending: false }),
  useDismissInsight: () => ({ mutateAsync: vi.fn(), isPending: false }),
}));

const queryClient = new QueryClient();

const renderWithProviders = (ui: React.ReactElement) => {
  return render(
    <QueryClientProvider client={queryClient}>
      <I18nextProvider i18n={i18n}>
        <MemoryRouter>
          {ui}
        </MemoryRouter>
      </I18nextProvider>
    </QueryClientProvider>
  );
};

describe('InsightsCard Smoke Test', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    Element.prototype.scrollIntoView = vi.fn();
    i18n.changeLanguage('en');
  });

  it('renders correctly with data and shows HelpTooltip', async () => {
    const { useTopInsights } = await import('@/hooks/useInsights');
    (useTopInsights as any).mockReturnValue({
      data: [
        {
          id: 1,
          title: 'High spending in Restaurants',
          description: 'You spent 40% more than usual this week.',
          priority: InsightPriority.HIGH,
          type: InsightType.SPENDING_ANOMALY,
          createdAt: new Date().toISOString(),
        },
      ],
      isLoading: false,
      error: null,
      refetch: vi.fn(),
    });

    renderWithProviders(<InsightsCard />);

    // Check if title is present
    expect(screen.getByText(/insightsCard.title/i)).toBeInTheDocument();

    // Check if HelpTooltip icon is present
    const helpButton = screen.getByRole('button', { name: /help/i });
    expect(helpButton).toBeInTheDocument();
  });
});
