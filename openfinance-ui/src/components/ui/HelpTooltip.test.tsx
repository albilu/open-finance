import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { HelpTooltip } from './HelpTooltip';
import { TooltipProvider } from '@/components/ui/Tooltip';

describe('HelpTooltip', () => {
  beforeEach(() => {
    // Mock scrollIntoView as it's not implemented in jsdom
    Element.prototype.scrollIntoView = vi.fn();
  });

  it('renders the info icon button', () => {
    render(
      <TooltipProvider>
        <HelpTooltip text="Test help text" />
      </TooltipProvider>
    );

    const button = screen.getByRole('button', { name: /help/i });
    expect(button).toBeInTheDocument();
    expect(button).toHaveClass('cursor-help');
  });

  it('has correct aria-label for accessibility', () => {
    render(
      <TooltipProvider>
        <HelpTooltip text="Test help text" />
      </TooltipProvider>
    );

    const button = screen.getByRole('button', { name: /help/i });
    expect(button).toHaveAttribute('aria-label', 'help');
  });

  it('renders the Info icon from lucide-react', () => {
    const { container } = render(
      <TooltipProvider>
        <HelpTooltip text="Test help text" />
      </TooltipProvider>
    );

    const svg = container.querySelector('svg');
    expect(svg).toBeInTheDocument();
    expect(svg).toHaveAttribute('aria-hidden', 'true');
  });

  it('applies custom className to the button', () => {
    render(
      <TooltipProvider>
        <HelpTooltip text="Test help text" className="custom-test-class" />
      </TooltipProvider>
    );

    const button = screen.getByRole('button', { name: /help/i });
    expect(button).toHaveClass('custom-test-class');
  });

  it('is a button of type button to prevent form submission', () => {
    render(
      <TooltipProvider>
        <HelpTooltip text="Test help text" />
      </TooltipProvider>
    );

    const button = screen.getByRole('button', { name: /help/i });
    expect(button).toHaveAttribute('type', 'button');
  });
});
