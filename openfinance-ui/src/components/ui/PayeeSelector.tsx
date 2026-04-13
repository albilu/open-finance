/**
 * PayeeSelector component
 * 
 * A dropdown component for selecting payees with search functionality.
 * Supports grouping by category and allows custom payee entry.
 */

import { useState, useMemo, useEffect } from 'react';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/Select';
import { markSelectInteraction } from '@/utils/selectClickGuard';
import { useActivePayees, useFindOrCreatePayee } from '@/hooks/usePayees';
import { Loader2, Search, User } from 'lucide-react';
import type { Payee } from '@/types/payee';
import { cn } from '@/lib/utils';

interface PayeeSelectorProps {
  value?: string;
  onValueChange: (value: string | undefined) => void;
  placeholder?: string;
  disabled?: boolean;
  className?: string;
  /**
   * Show "None" option at the top
   */
  allowNone?: boolean;
  /**
   * Enable free-text entry for new payees
   */
  allowNewPayee?: boolean;
}

const CATEGORY_NAMES: Record<string, string> = {
  shopping: 'Shopping',
  entertainment: 'Entertainment',
  utilities: 'Utilities',
  groceries: 'Groceries',
  transport: 'Transport',
  restaurants: 'Restaurants',
  health: 'Health',
  financial: 'Financial',
  insurance: 'Insurance',
  education: 'Education',
};

export function PayeeSelector({
  value,
  onValueChange,
  placeholder = 'Select payee',
  disabled = false,
  className,
  allowNone = true,
  allowNewPayee = true,
}: PayeeSelectorProps) {
  const { data: payees, isLoading, isError } = useActivePayees();
  const [searchQuery, setSearchQuery] = useState('');
  const [isOpen, setIsOpen] = useState(false);
  const [customInput, setCustomInput] = useState('');
  const [showCustomInput, setShowCustomInput] = useState(false);

  const findOrCreatePayee = useFindOrCreatePayee();

  // Filter and group payees by category
  const groupedPayees = useMemo(() => {
    if (!payees) return {};

    const normalizedQuery = searchQuery.trim().toLowerCase();
    const filtered = normalizedQuery
      ? payees.filter((p) => p.name.toLowerCase().includes(normalizedQuery))
      : payees;

    // Group by category
    const groups: Record<string, Payee[]> = {};
    filtered.forEach((payee) => {
      const category = payee.category || 'other';
      if (!groups[category]) {
        groups[category] = [];
      }
      groups[category].push(payee);
    });

    // Sort each group by name
    Object.keys(groups).forEach((category) => {
      groups[category].sort((a, b) => a.name.localeCompare(b.name));
    });

    return groups;
  }, [payees, searchQuery]);

  // Get sorted categories
  const sortedCategories = useMemo(() => {
    return Object.keys(groupedPayees).sort((a, b) => {
      const nameA = CATEGORY_NAMES[a] || a;
      const nameB = CATEGORY_NAMES[b] || b;
      return nameA.localeCompare(nameB);
    });
  }, [groupedPayees]);

  const selectedPayee = value
    ? payees?.find((p) => p.name === value)
    : undefined;

  // Show custom input when search doesn't match any existing payee
  useEffect(() => {
    if (allowNewPayee && searchQuery.trim() && payees) {
      const normalizedQuery = searchQuery.trim().toLowerCase();
      const hasMatch = payees.some((p) => p.name.toLowerCase() === normalizedQuery);
      setShowCustomInput(!hasMatch);
    } else {
      setShowCustomInput(false);
    }
  }, [searchQuery, payees, allowNewPayee]);

  // Handle creating a new payee
  const handleCreateNewPayee = async () => {
    const name = (customInput || searchQuery).trim();
    if (!name) return;

    try {
      const newPayee = await findOrCreatePayee.mutateAsync(name);
      onValueChange(newPayee.name);
      setCustomInput('');
      setSearchQuery('');
      setShowCustomInput(false);
    } catch (error) {
      console.error('Failed to create payee:', error);
    }
  };

  // Render logo
  const renderLogo = (logo?: string, size: 'sm' | 'md' = 'md') => {
    const sizeClasses = size === 'sm' ? 'h-5 w-5' : 'h-6 w-6';
    if (logo) {
      return (
        <img
          src={logo}
          alt=""
          className={cn('rounded object-contain bg-white', sizeClasses)}
        />
      );
    }
    return (
      <div
        className={cn(
          'flex items-center justify-center rounded bg-primary/10',
          sizeClasses
        )}
      >
        <User className="h-3 w-3 text-primary" />
      </div>
    );
  };

  const handleValueChange = (val: string) => {
    if (val === '__none__') {
      onValueChange(undefined);
    } else if (val === '__new__') {
      setShowCustomInput(true);
    } else {
      onValueChange(val);
    }
  };

  if (isLoading) {
    return (
      <div className="flex h-10 w-full items-center justify-center rounded-lg border border-border bg-surface px-3 py-2">
        <Loader2 className="h-4 w-4 animate-spin text-text-muted" />
        <span className="ml-2 text-sm text-text-muted">Loading payees...</span>
      </div>
    );
  }

  if (isError || !payees) {
    return (
      <div className="flex h-10 w-full items-center justify-center rounded-lg border border-error bg-surface px-3 py-2">
        <span className="text-sm text-error">Failed to load payees</span>
      </div>
    );
  }

  return (
    <Select
      value={value || '__none__'}
      onValueChange={handleValueChange}
      disabled={disabled}
      onOpenChange={(open) => {
        setIsOpen(open);
        if (!open) {
          markSelectInteraction();
          setSearchQuery('');
          setShowCustomInput(false);
          setCustomInput('');
        }
      }}
    >
      <SelectTrigger className={className}>
        <SelectValue placeholder={placeholder}>
          {value && selectedPayee ? (
            <span className="flex items-center gap-2">
              {renderLogo(selectedPayee.logo, 'sm')}
              <span>{selectedPayee.name}</span>
              {selectedPayee.category && (
                <span className="text-text-muted text-xs">
                  ({CATEGORY_NAMES[selectedPayee.category] || selectedPayee.category})
                </span>
              )}
            </span>
          ) : value === undefined && allowNone ? (
            <span className="text-text-muted">None</span>
          ) : (
            placeholder
          )}
        </SelectValue>
      </SelectTrigger>
      <SelectContent 
        className="p-0 flex flex-col" 
        viewportClassName="p-1"
        headerSlot={
          <div className="shrink-0 border-b border-border bg-surface p-2">
            <div className="relative">
              <Search className="pointer-events-none absolute left-2.5 top-1/2 h-4 w-4 -translate-y-1/2 text-text-muted" />
              <input
                type="text"
                value={searchQuery}
                onChange={(event) => setSearchQuery(event.target.value)}
                onKeyDown={(event) => {
                  event.stopPropagation();
                  // Prevent Enter from triggering Radix Select's keyboard selection
                  if (event.key === 'Enter') event.preventDefault();
                }}
                placeholder="Search payee"
                className="h-9 w-full rounded-md border border-border bg-background pl-8 pr-2 text-sm text-text-primary placeholder:text-text-muted focus:outline-none focus:ring-2 focus:ring-primary"
                autoFocus={isOpen}
              />
            </div>
          </div>
        }
        footerSlot={
          allowNewPayee && showCustomInput ? (
            <div className="shrink-0 border-t border-border bg-surface p-2">
              <div className="px-2 py-1 text-xs font-semibold text-primary">
                Create new payee
              </div>
              <div className="flex gap-2 px-2 pb-2">
                <input
                  type="text"
                  value={customInput || searchQuery}
                  onChange={(event) => setCustomInput(event.target.value)}
                  onKeyDown={(event) => {
                    event.stopPropagation();
                    if (event.key === 'Escape') setIsOpen(false);
                    if (event.key === 'Enter') {
                      handleCreateNewPayee();
                    }
                  }}
                  placeholder="Enter payee name"
                  className="flex-1 rounded-md border border-border bg-background px-2 py-1.5 text-sm text-text-primary placeholder:text-text-muted focus:outline-none focus:ring-2 focus:ring-primary"
                  autoFocus
                />
                <button
                  onClick={handleCreateNewPayee}
                  disabled={!(customInput || searchQuery).trim() || findOrCreatePayee.isPending}
                  className="rounded-md bg-primary px-3 py-1.5 text-sm font-medium text-white hover:bg-primary/90 disabled:opacity-50"
                >
                  {findOrCreatePayee.isPending ? (
                    <Loader2 className="h-4 w-4 animate-spin" />
                  ) : (
                    'Add'
                  )}
                </button>
              </div>
            </div>
          ) : null
        }
      >
        {/* "None" option */}
        {allowNone && (
          <SelectItem value="__none__">
            <div className="flex items-center gap-2">
              <span className="flex h-4 w-4 items-center justify-center shrink-0">
                <User className="h-4 w-4 text-text-muted" />
              </span>
              <span className="text-text-muted">None</span>
            </div>
          </SelectItem>
        )}

        {/* Grouped payees */}
        {sortedCategories.map((category) => (
          <div key={category} className="mt-2">
            {/* Category header */}
            <div className="px-2 py-1 text-xs font-semibold text-text-muted">
              {CATEGORY_NAMES[category] || category}
            </div>

            {/* Payees in this category */}
            {groupedPayees[category].map((payee) => (
              <SelectItem
                key={payee.id}
                value={payee.name}
              >
                <div className="flex items-center gap-2">
                  <span className="flex h-4 w-4 items-center justify-center shrink-0">
                    {renderLogo(payee.logo, 'sm')}
                  </span>
                  <span className="text-sm">{payee.name}</span>
                </div>
              </SelectItem>
            ))}
          </div>
        ))}

        {/* Empty state */}
        {sortedCategories.length === 0 && !showCustomInput && (
          <div className="p-2 text-center text-sm text-text-muted">
            No payees match your search
            {allowNewPayee && searchQuery.trim() && (
              <div className="mt-1 text-xs">
                Press Enter to create "{searchQuery}"
              </div>
            )}
          </div>
        )}
      </SelectContent>
    </Select>
  );
}
