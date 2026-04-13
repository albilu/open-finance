/**
 * CategorySelect component
 * Task CAT-2.3: Create CategorySelect component
 * 
 * A dropdown component for selecting categories with search functionality.
 * Supports grouping by parent category, type filtering, and creating new categories.
 */

import { useState, useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/Select';
import { markSelectInteraction } from '@/utils/selectClickGuard';
import { useCategoryTree } from '@/hooks/useTransactions';
import { Loader2, Search, FolderOpen, Plus, ChevronRight } from 'lucide-react';
import type { CategoryTreeNode, TransactionType } from '@/types/transaction';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';

interface CategorySelectProps {
  value?: number;
  onValueChange: (value: number | undefined) => void;
  placeholder?: string;
  disabled?: boolean;
  className?: string;
  /**
   * Filter categories by type
   */
  type?: TransactionType;
  /**
   * Show "None" option at the top
   */
  allowNone?: boolean;
  /**
   * Enable option to create new category
   */
  allowCreateNew?: boolean;
  /**
   * Callback when create new is clicked
   */
  onCreateNew?: () => void;
  /**
   * Placeholder text for the search input inside the dropdown
   */
  searchPlaceholder?: string;
}

/**
 * Flatten tree into sorted list with depth information
 */
function flattenCategories(
  categories: CategoryTreeNode[],
  depth: number = 0,
  parentPath: string = ''
): Array<{ category: CategoryTreeNode; depth: number; path: string }> {
  const result: Array<{ category: CategoryTreeNode; depth: number; path: string }> = [];

  // Sort: by type first (INCOME before EXPENSE), then by name
  const sorted = [...categories].sort((a, b) => {
    const typeCompare = a.type.localeCompare(b.type);
    if (typeCompare !== 0) return typeCompare;
    return a.name.localeCompare(b.name);
  });

  for (const category of sorted) {
    const currentPath = parentPath ? `${parentPath} / ${category.name}` : category.name;
    result.push({ category, depth, path: currentPath });

    if (category.subcategories && category.subcategories.length > 0) {
      result.push(...flattenCategories(category.subcategories, depth + 1, currentPath));
    }
  }

  return result;
}

export function CategorySelect({
  value,
  onValueChange,
  placeholder = 'Select category',
  disabled = false,
  className,
  type,
  allowNone = true,
  allowCreateNew = false,
  onCreateNew,
  searchPlaceholder,
}: CategorySelectProps) {
  const { t } = useTranslation('categories');
  const { data: categories = [], isLoading, isError } = useCategoryTree();
  const [searchQuery, setSearchQuery] = useState('');
  const [isOpen, setIsOpen] = useState(false);

  // Filter categories by type
  const filteredByType = useMemo(() => {
    if (!type) return categories;
    return categories.filter(c => c.type === type);
  }, [categories, type]);

  // Flatten and filter by search query
  const flatCategories = useMemo(() => {
    const flattened = flattenCategories(filteredByType);

    if (!searchQuery.trim()) return flattened;

    const query = searchQuery.toLowerCase();
    return flattened.filter(
      ({ category, path }) =>
        category.name.toLowerCase().includes(query) ||
        path.toLowerCase().includes(query) ||
        (category.mccCode && category.mccCode.toLowerCase().includes(query))
    );
  }, [filteredByType, searchQuery]);

  // Find selected category
  const selectedCategory = useMemo(() => {
    if (!value) return null;
    const findInTree = (cats: CategoryTreeNode[]): CategoryTreeNode | null => {
      for (const cat of cats) {
        if (cat.id === value) return cat;
        if (cat.subcategories) {
          const found = findInTree(cat.subcategories);
          if (found) return found;
        }
      }
      return null;
    };
    return findInTree(categories);
  }, [value, categories]);

  const handleValueChange = (newValue: string) => {
    if (newValue === '__create_new__') {
      onCreateNew?.();
      return;
    }
    if (newValue === '__none__') {
      onValueChange(undefined);
      return;
    }
    onValueChange(newValue ? Number(newValue) : undefined);
  };

  return (
    <Select
      value={value?.toString() || (allowNone ? '__none__' : '')}
      onValueChange={handleValueChange}
      disabled={disabled}
      open={isOpen}
      onOpenChange={(open) => {
        setIsOpen(open);
        if (!open) markSelectInteraction();
      }}
    >
      <SelectTrigger className={className}>
        <SelectValue placeholder={placeholder}>
          {selectedCategory && (
            <div className="flex items-center gap-2">
              <div
                className="w-5 h-5 rounded flex items-center justify-center text-white text-xs"
                style={{ backgroundColor: selectedCategory.color || '#6B7280' }}
              >
                {selectedCategory.icon || <FolderOpen size={12} />}
              </div>
              <span>{selectedCategory.name}</span>
              <span className="text-xs text-text-tertiary">
                ({selectedCategory.transactionCount || 0} txns)
              </span>
            </div>
          )}
        </SelectValue>
      </SelectTrigger>

      <SelectContent 
        className="max-h-[400px] flex flex-col p-0"
        viewportClassName="p-1"
        headerSlot={
          <div className="p-2 border-b border-border bg-surface shrink-0">
            <div className="relative">
              <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-text-tertiary" />
              <Input
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder={searchPlaceholder ?? t('search.placeholder')}
                className="pl-9 h-9"
                onClick={(e) => e.stopPropagation()}
                onKeyDown={(e) => {
                  e.stopPropagation();
                  if (e.key === 'Escape') setIsOpen(false);
                  // Prevent Enter from triggering Radix Select's keyboard selection
                  // while the user is typing in the search box
                  if (e.key === 'Enter') e.preventDefault();
                }}
                autoFocus={isOpen}
              />
            </div>
          </div>
        }
        footerSlot={
          allowCreateNew && !isLoading && flatCategories.length > 0 ? (
            <div className="border-t border-border p-2 bg-surface shrink-0">
              <Button
                variant="ghost"
                size="sm"
                className="w-full justify-start text-primary"
                onClick={(e) => {
                  e.stopPropagation();
                  onCreateNew?.();
                }}
              >
                <Plus size={16} className="mr-2" />
                {t('select.createNew')}
              </Button>
            </div>
          ) : null
        }
      >
        {/* Loading State */}
        {isLoading && (
          <div className="flex items-center justify-center p-4">
            <Loader2 size={20} className="animate-spin text-text-tertiary" />
          </div>
        )}

        {/* Error State */}
        {isError && (
          <div className="p-4 text-center text-sm text-error">
            {t('loadError.title')}
          </div>
        )}

        {/* None Option - Use a special value since Radix doesn't allow empty string */}
        {allowNone && !isLoading && (
          <SelectItem value="__none__" className="cursor-pointer">
            <span className="text-text-tertiary">{t('select.noCategory')}</span>
          </SelectItem>
        )}

        {/* Category List */}
        {!isLoading && flatCategories.map(({ category, depth }) => (
          <SelectItem
            key={category.id}
            value={category.id.toString()}
            className="cursor-pointer"
          >
            <div
              className="flex items-center gap-2"
              style={{ paddingLeft: `${depth * 16}px` }}
            >
              {/* Expand indicator for subcategories */}
              {depth > 0 && (
                <ChevronRight size={14} className="text-text-tertiary" />
              )}

              {/* Icon */}
              <div
                className="w-6 h-6 rounded flex items-center justify-center text-white text-xs shrink-0"
                style={{ backgroundColor: category.color || '#6B7280' }}
              >
                {category.icon ? (
                  <span className="text-[10px]">{category.icon}</span>
                ) : (
                  <FolderOpen size={12} />
                )}
              </div>

              {/* Name */}
              <div className="flex-1 min-w-0">
                <div className="truncate">{category.name}</div>
                {category.mccCode && (
                  <div className="text-xs text-text-tertiary">MCC: {category.mccCode}</div>
                )}
              </div>

              {/* Transaction count */}
              <span className="text-xs text-text-tertiary">
                {category.transactionCount || 0}
              </span>
            </div>
          </SelectItem>
        ))}

        {/* Empty State */}
        {!isLoading && flatCategories.length === 0 && (
          <div className="p-4 text-center text-sm text-text-tertiary">
            {searchQuery ? 'No matching categories' : 'No categories available'}
          </div>
        )}
      </SelectContent>
    </Select>
  );
}
