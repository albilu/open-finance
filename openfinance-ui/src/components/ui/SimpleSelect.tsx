/**
 * Simple Select Wrapper
 * Wraps native HTML select element with consistent styling
 */
import React from 'react';
import { cn } from '@/lib/utils';

export interface SimpleSelectProps extends React.SelectHTMLAttributes<HTMLSelectElement> {
  children: React.ReactNode;
}

export const SimpleSelect = React.forwardRef<HTMLSelectElement, SimpleSelectProps>(
  ({ className, children, ...props }, ref) => {
    return (
      <select
        ref={ref}
        className={cn(
          'flex h-10 w-full items-center justify-between rounded-lg border border-border bg-surface px-3 py-2 text-sm text-text-primary',
          'focus:outline-none focus:ring-2 focus:ring-primary focus:ring-offset-2 focus:ring-offset-background',
          'disabled:cursor-not-allowed disabled:opacity-50',
          'transition-all duration-150',
          className
        )}
        {...props}
      >
        {children}
      </select>
    );
  }
);

SimpleSelect.displayName = 'SimpleSelect';
