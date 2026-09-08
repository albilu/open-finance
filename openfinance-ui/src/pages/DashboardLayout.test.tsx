import { useEffect, type ReactNode } from 'react';
import { fireEvent, screen, waitFor } from '@testing-library/react';
import { beforeEach, expect, it, vi } from 'vitest';
import { renderWithProviders, mockAuthentication } from '@/test/test-utils';
import DashboardPage from '@/pages/DashboardPage';

interface Item {
  i: string;
  x: number;
  y: number;
  w: number;
  h: number;
}
interface GridProps {
  children: ReactNode;
  layouts: Record<string, Item[]>;
  onLayoutChange?: (current: Item[], all: Record<string, Item[]>) => void;
  onBreakpointChange: (breakpoint: string) => void;
  onDragStop: (layout: Item[]) => void;
}

vi.mock('react-grid-layout/legacy', () => ({
  WidthProvider: (component: unknown) => component,
  Responsive: ({
    children,
    layouts,
    onLayoutChange,
    onBreakpointChange,
    onDragStop,
  }: GridProps) => {
    // A controlled widget may echo equivalent automatic layouts after receiving props.
    useEffect(() => {
      const current = layouts.lg.map(item => ({ ...item }));
      onLayoutChange?.(current, { ...layouts, lg: current });
    }, [layouts, onLayoutChange]);
    return (
      <div>
        <button
          onClick={() => {
            onBreakpointChange('lg');
            onDragStop([{ i: 'netWorth', x: 2, y: 0, w: 4, h: 5 }]);
          }}
        >
          Complete card move
        </button>
        {children}
      </div>
    );
  },
}));

beforeEach(() => {
  localStorage.clear();
  mockAuthentication();
});

it('ignores automatic layout echoes and preserves other cards when saving a completed move', async () => {
  renderWithProviders(<DashboardPage />);
  const move = await screen.findByRole('button', { name: 'Complete card move' });
  expect(localStorage.getItem('open_finance_dashboard_layouts')).toBeNull();
  fireEvent.click(move);
  await waitFor(() => {
    const saved = JSON.parse(localStorage.getItem('open_finance_dashboard_layouts') ?? '{}');
    expect(saved.lg.find((item: Item) => item.i === 'netWorth').x).toBe(2);
    expect(saved.lg.some((item: Item) => item.i === 'cashFlow')).toBe(true);
  });
});
