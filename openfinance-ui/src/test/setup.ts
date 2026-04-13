/**
 * Test Setup Configuration
 * 
 * Configures the testing environment for all test files.
 * This file is imported automatically by Vitest.
 */

import { afterAll, afterEach, beforeAll, vi } from 'vitest';
import React from 'react';
import { server } from './mocks/server';
import '@testing-library/jest-dom/vitest';

// Mock ResizeObserver for jsdom
global.ResizeObserver = class ResizeObserver {
  observe() {}
  unobserve() {}
  disconnect() {}
};

// Global mock for recharts to avoid testing errors with JSDOM and ESM imports
vi.mock('recharts', async () => {
  const MockComponent = ({ children, 'data-testid': testId }: { children?: React.ReactNode; 'data-testid'?: string }) => 
    React.createElement('div', { 'data-testid': testId }, children);
  
  return {
    ResponsiveContainer: ({ children }: any) => React.createElement('div', { 'data-testid': 'responsive-container' }, children),
    AreaChart: ({ children }: any) => React.createElement('div', { 'data-testid': 'area-chart' }, children),
    BarChart: ({ children }: any) => React.createElement('div', { 'data-testid': 'bar-chart' }, children),
    LineChart: ({ children }: any) => React.createElement('div', { 'data-testid': 'line-chart' }, children),
    PieChart: ({ children }: any) => React.createElement('div', { 'data-testid': 'pie-chart' }, children),
    Treemap: ({ children }: any) => React.createElement('div', { 'data-testid': 'tree-map' }, children),
    Sankey: ({ children }: any) => React.createElement('div', { 'data-testid': 'sankey' }, children),
    RadarChart: ({ children }: any) => React.createElement('div', { 'data-testid': 'radar-chart' }, children),
    ScatterChart: ({ children }: any) => React.createElement('div', { 'data-testid': 'scatter-chart' }, children),
    ComposedChart: ({ children }: any) => React.createElement('div', { 'data-testid': 'composed-chart' }, children),
    Area: MockComponent,
    XAxis: MockComponent,
    YAxis: MockComponent,
    CartesianGrid: MockComponent,
    Tooltip: MockComponent,
    Legend: MockComponent,
    Bar: MockComponent,
    Line: MockComponent,
    Pie: MockComponent,
    Cell: MockComponent,
    Sector: MockComponent,
    ReferenceLine: MockComponent,
    ReferenceArea: MockComponent,
    ReferenceDot: MockComponent,
    Label: MockComponent,
    LabelList: MockComponent,
  };
});

// Global mock for react-grid-layout to avoid measurement issues in JSDOM
vi.mock('react-grid-layout/legacy', () => ({
  Responsive: ({ children }: any) => React.createElement('div', { 'data-testid': 'responsive-grid-layout' }, children),
  WidthProvider: (component: any) => component,
}));

vi.mock('react-grid-layout', () => ({
  Responsive: ({ children }: any) => React.createElement('div', { 'data-testid': 'responsive-grid-layout' }, children),
  WidthProvider: (component: any) => component,
}));

/**
 * Start MSW server before all tests
 */
beforeAll(() => {
  server.listen({ onUnhandledRequest: 'warn' });
});

/**
 * Reset handlers after each test to ensure test isolation
 */
afterEach(() => {
  server.resetHandlers();
});

/**
 * Clean up and close server after all tests
 */
afterAll(() => {
  server.close();
});
