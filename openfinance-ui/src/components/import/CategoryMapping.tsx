/**
 * CategoryMapping Component
 * Task 7.4.11: Create CategoryMapping component
 * 
 * UI for mapping imported categories to existing user categories
 */
import { useState, useMemo } from 'react';
import { Plus, AlertCircle } from 'lucide-react';
import { SimpleSelect } from '@/components/ui/SimpleSelect';
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/Dialog';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { useCategories, useCreateCategory } from '@/hooks/useTransactions';
import type { ImportTransactionDTO } from '@/types/import';

interface CategoryMappingProps {
  transactions: ImportTransactionDTO[];
  categoryMappings: Record<string, number>;
  onMappingChange: (sourceCategory: string, targetCategoryId: number | null) => void;
}

export function CategoryMapping({
  transactions,
  categoryMappings,
  onMappingChange,
}: CategoryMappingProps) {
  const [showCreateDialog, setShowCreateDialog] = useState(false);
  const [newCategoryName, setNewCategoryName] = useState('');
  const [newCategoryType, setNewCategoryType] = useState<'INCOME' | 'EXPENSE'>('EXPENSE');
  const [creatingForSource, setCreatingForSource] = useState<string | null>(null);

  const { data: categories = [] } = useCategories();
  const createCategory = useCreateCategory();

  // Extract unique categories from transactions
  const uniqueCategories = useMemo(() => {
    const categoryMap = new Map<string, number>();

    transactions.forEach(transaction => {
      if (transaction.category) {
        const count = categoryMap.get(transaction.category) || 0;
        categoryMap.set(transaction.category, count + 1);
      }

      // Also count split categories
      if (transaction.splitTransaction && transaction.splits) {
        transaction.splits.forEach(split => {
          if (split.category) {
            const count = categoryMap.get(split.category) || 0;
            categoryMap.set(split.category, count + 1);
          }
        });
      }
    });

    return Array.from(categoryMap.entries())
      .map(([category, count]) => ({ category, count }))
      .sort((a, b) => b.count - a.count); // Sort by count descending
  }, [transactions]);

  const handleCreateCategory = async () => {
    if (!newCategoryName.trim() || !creatingForSource) return;

    try {
      const result = await createCategory.mutateAsync({
        name: newCategoryName.trim(),
        type: newCategoryType,
      });

      // Map to the newly created category
      onMappingChange(creatingForSource, result.id);

      // Reset form
      setShowCreateDialog(false);
      setNewCategoryName('');
      setNewCategoryType('EXPENSE');
      setCreatingForSource(null);
    } catch (error) {
      console.error('Failed to create category:', error);
    }
  };

  const openCreateDialog = (sourceCategory: string) => {
    setCreatingForSource(sourceCategory);
    setNewCategoryName(sourceCategory); // Pre-fill with source category name
    setShowCreateDialog(true);
  };

  const getMappedCategoryName = (sourceCategory: string): string => {
    const categoryId = categoryMappings[sourceCategory];
    if (!categoryId) return 'Not mapped';
    
    const category = categories.find(c => c.id === categoryId);
    return category ? category.name : 'Unknown';
  };

  const unmappedCount = uniqueCategories.filter(
    uc => !categoryMappings[uc.category]
  ).length;

  return (
    <div className="space-y-4">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h3 className="text-lg font-semibold text-text-primary">Map Categories</h3>
          <p className="text-sm text-text-secondary mt-1">
            Map imported categories to your existing categories
          </p>
        </div>
        {unmappedCount > 0 && (
          <div className="flex items-center space-x-2 text-amber-600">
            <AlertCircle className="h-4 w-4" />
            <span className="text-sm font-medium">{unmappedCount} unmapped</span>
          </div>
        )}
      </div>

      {/* Mapping List */}
      <div className="border border-border rounded-lg divide-y divide-border">
        {uniqueCategories.map(({ category, count }) => {
          const mappedCategoryId = categoryMappings[category];
          const isMapped = mappedCategoryId !== undefined && mappedCategoryId !== null;

          return (
            <div
              key={category}
              className={`p-4 ${!isMapped ? 'bg-amber-500/5' : ''}`}
            >
              <div className="flex flex-col sm:flex-row sm:items-center justify-between space-y-3 sm:space-y-0 sm:space-x-4">
                {/* Source Category */}
                <div className="flex-1 min-w-0">
                  <div className="flex items-center space-x-2">
                    <span className="font-medium text-text-primary truncate">
                      {category}
                    </span>
                    <span className="flex-shrink-0 text-xs text-text-tertiary bg-surface-elevated px-2 py-1 rounded">
                      {count} transaction{count !== 1 ? 's' : ''}
                    </span>
                  </div>
                </div>

                {/* Arrow */}
                <div className="hidden sm:block text-text-tertiary">→</div>

                {/* Target Category Selection */}
                <div className="flex items-center space-x-2 flex-1 sm:flex-none sm:w-80">
                  <div className="flex-1">
                    <SimpleSelect
                      value={mappedCategoryId?.toString() || ''}
                      onChange={(e) => {
                        const value = e.target.value;
                        if (value === 'create') {
                          openCreateDialog(category);
                        } else if (value === 'skip') {
                          onMappingChange(category, null);
                        } else if (value) {
                          onMappingChange(category, parseInt(value, 10));
                        }
                      }}
                      className={`w-full ${!isMapped ? 'border-amber-500' : ''}`}
                    >
                      <option value="">Select category...</option>
                      {categories.map((cat) => (
                        <option key={cat.id} value={cat.id}>
                          {cat.name} ({cat.type})
                        </option>
                      ))}
                      <option value="create">+ Create new category</option>
                      <option value="skip">Skip (no category)</option>
                    </SimpleSelect>
                  </div>
                  
                  {!isMapped && (
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => openCreateDialog(category)}
                      title="Create new category"
                    >
                      <Plus className="h-4 w-4" />
                    </Button>
                  )}
                </div>
              </div>

              {/* Show current mapping */}
              {isMapped && mappedCategoryId && (
                <div className="mt-2 text-xs text-text-tertiary">
                  Mapped to: <span className="text-text-secondary">{getMappedCategoryName(category)}</span>
                </div>
              )}
            </div>
          );
        })}
      </div>

      {/* Summary */}
      <div className="bg-blue-500/5 border border-blue-500/20 rounded-lg p-4">
        <p className="text-sm text-text-secondary">
          <strong className="text-text-primary">{uniqueCategories.length}</strong> unique categories found.{' '}
          <strong className="text-text-primary">
            {uniqueCategories.length - unmappedCount}
          </strong>{' '}
          mapped, <strong className="text-amber-600">{unmappedCount}</strong> unmapped.
        </p>
        <p className="text-xs text-text-tertiary mt-1">
          Unmapped categories will not be assigned to transactions. You can skip them or create new categories.
        </p>
      </div>

      {/* Create Category Dialog */}
      <Dialog
        open={showCreateDialog}
        onOpenChange={(open) => {
          if (!open) {
            setShowCreateDialog(false);
            setNewCategoryName('');
            setCreatingForSource(null);
          }
        }}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Create New Category</DialogTitle>
          </DialogHeader>
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-text-primary mb-2">
                Category Name
              </label>
              <Input
                value={newCategoryName}
                onChange={(e) => setNewCategoryName(e.target.value)}
                placeholder="Enter category name"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-text-primary mb-2">
                Type
              </label>
              <SimpleSelect
                value={newCategoryType}
                onChange={(e) => setNewCategoryType(e.target.value as 'INCOME' | 'EXPENSE')}
              >
                <option value="EXPENSE">Expense</option>
                <option value="INCOME">Income</option>
              </SimpleSelect>
            </div>

            <div className="flex justify-end space-x-3 pt-4">
              <Button
                variant="secondary"
                onClick={() => {
                  setShowCreateDialog(false);
                  setNewCategoryName('');
                  setCreatingForSource(null);
                }}
              >
                Cancel
              </Button>
              <Button
                variant="primary"
                onClick={handleCreateCategory}
                isLoading={createCategory.isPending}
                disabled={!newCategoryName.trim()}
              >
                Create Category
              </Button>
            </div>
          </div>
        </DialogContent>
      </Dialog>
    </div>
  );
}
