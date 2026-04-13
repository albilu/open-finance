/**
 * Tests for VisibilityContext
 */
import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { VisibilityProvider, useVisibility } from './VisibilityContext';
import type { ReactNode } from 'react';

describe('VisibilityContext', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  afterEach(() => {
    localStorage.clear();
  });

  const wrapper = ({ children }: { children: ReactNode }) => (
    <VisibilityProvider>{children}</VisibilityProvider>
  );

  it('should initialize with amounts visible by default', () => {
    const { result } = renderHook(() => useVisibility(), { wrapper });

    expect(result.current.isAmountsVisible).toBe(true);
  });

  it('should toggle amounts visibility', () => {
    const { result } = renderHook(() => useVisibility(), { wrapper });

    expect(result.current.isAmountsVisible).toBe(true);

    act(() => {
      result.current.toggleAmountsVisibility();
    });

    expect(result.current.isAmountsVisible).toBe(false);

    act(() => {
      result.current.toggleAmountsVisibility();
    });

    expect(result.current.isAmountsVisible).toBe(true);
  });

  it('should set amounts visibility explicitly', () => {
    const { result } = renderHook(() => useVisibility(), { wrapper });

    act(() => {
      result.current.setAmountsVisible(false);
    });

    expect(result.current.isAmountsVisible).toBe(false);

    act(() => {
      result.current.setAmountsVisible(true);
    });

    expect(result.current.isAmountsVisible).toBe(true);
  });

  it('should persist visibility state to localStorage', () => {
    const { result } = renderHook(() => useVisibility(), { wrapper });

    act(() => {
      result.current.setAmountsVisible(false);
    });

    expect(localStorage.getItem('open_finance_amounts_visible')).toBe('false');

    act(() => {
      result.current.setAmountsVisible(true);
    });

    expect(localStorage.getItem('open_finance_amounts_visible')).toBe('true');
  });

  it('should initialize from localStorage', () => {
    localStorage.setItem('open_finance_amounts_visible', 'false');

    const { result } = renderHook(() => useVisibility(), { wrapper });

    expect(result.current.isAmountsVisible).toBe(false);
  });

  it('should throw error when used outside provider', () => {
    expect(() => {
      renderHook(() => useVisibility());
    }).toThrow('useVisibility must be used within VisibilityProvider');
  });
});
