/**
 * Tests for PrivateAmount component
 */
import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { PrivateAmount } from './PrivateAmount';
import { VisibilityProvider } from '@/context/VisibilityContext';

describe('PrivateAmount', () => {
  it('should render children when amounts are visible', () => {
    render(
      <VisibilityProvider>
        <PrivateAmount>$1,234.56</PrivateAmount>
      </VisibilityProvider>
    );

    expect(screen.getByText('$1,234.56')).toBeInTheDocument();
  });

  it('should apply blur class when amounts are hidden', () => {
    // Set localStorage to hide amounts
    localStorage.setItem('open_finance_amounts_visible', 'false');

    const { container } = render(
      <VisibilityProvider>
        <PrivateAmount>$1,234.56</PrivateAmount>
      </VisibilityProvider>
    );

    const span = container.querySelector('span');
    expect(span).toHaveClass('blur-md');
    expect(span).toHaveClass('select-none');
  });

  it('should apply inline display when inline prop is true', () => {
    const { container } = render(
      <VisibilityProvider>
        <PrivateAmount inline>$1,234.56</PrivateAmount>
      </VisibilityProvider>
    );

    const span = container.querySelector('span');
    expect(span).toHaveClass('inline-block');
  });

  it('should apply block display by default', () => {
    const { container } = render(
      <VisibilityProvider>
        <PrivateAmount>$1,234.56</PrivateAmount>
      </VisibilityProvider>
    );

    const span = container.querySelector('span');
    expect(span).toHaveClass('block');
  });

  it('should apply custom className', () => {
    const { container } = render(
      <VisibilityProvider>
        <PrivateAmount className="custom-class">$1,234.56</PrivateAmount>
      </VisibilityProvider>
    );

    const span = container.querySelector('span');
    expect(span).toHaveClass('custom-class');
  });

  it('should have transition classes for smooth animation', () => {
    const { container } = render(
      <VisibilityProvider>
        <PrivateAmount>$1,234.56</PrivateAmount>
      </VisibilityProvider>
    );

    const span = container.querySelector('span');
    expect(span).toHaveClass('transition-all');
    expect(span).toHaveClass('duration-300');
    expect(span).toHaveClass('ease-in-out');
  });
});
