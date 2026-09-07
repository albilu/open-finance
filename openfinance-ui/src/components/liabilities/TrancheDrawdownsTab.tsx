/**
 * TrancheDrawdownsTab Component (Task 8)
 *
 * Renders the "Drawdowns" tab of the liability detail dialog: one row per
 * tranche (staged drawdown) with its T{n} label, status badge, planned /
 * drawn / remaining amounts, linked property label and interest-only flag.
 */
import { useTranslation } from 'react-i18next';
import { Layers, AlertCircle } from 'lucide-react';
import { ConvertedAmount } from '@/components/ui/ConvertedAmount';
import { useTranches, getTrancheLabel } from '@/hooks/useTranches';
import type { Liability, TrancheStatus } from '@/types/liability';
import { cn } from '@/lib/utils';

/** Badge colors per tranche status. */
const statusBadgeClass: Record<TrancheStatus, string> = {
  PLANNED: 'bg-surface-elevated text-text-secondary border-border',
  DRAWN: 'bg-primary/10 text-primary border-primary/30',
  CANCELLED: 'bg-error/10 text-error border-error/30',
};

export function TrancheDrawdownsTab({ liability }: { liability: Liability }) {
  const { t } = useTranslation('liabilities');
  const { data: tranches = [], isLoading, error } = useTranches(liability.id);

  if (isLoading) {
    return (
      <div className="space-y-2 animate-pulse py-4">
        {[...Array(3)].map((_, i) => (
          <div key={i} className="h-12 bg-surface border border-border rounded-lg" />
        ))}
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex items-center gap-2 p-4 bg-error/10 border border-error/20 rounded-lg text-error text-sm">
        <AlertCircle className="h-4 w-4 flex-shrink-0" />
        <span>{t('drawdowns.error')}</span>
      </div>
    );
  }

  if (tranches.length === 0) {
    return (
      <div className="text-center py-12">
        <Layers className="h-10 w-10 text-text-tertiary mx-auto mb-3" />
        <p className="text-text-secondary text-sm">{t('drawdowns.empty')}</p>
      </div>
    );
  }

  return (
    <div className="divide-y divide-border border border-border rounded-lg overflow-hidden">
      {tranches.map(tranche => (
        <div
          key={tranche.id}
          className="flex items-center justify-between px-4 py-3 bg-surface hover:bg-surface-elevated transition-colors"
          data-testid={`tranche-row-${tranche.trancheNo}`}
        >
          <div className="flex items-center gap-3 min-w-0">
            <span className="text-sm font-semibold text-text-primary font-mono">
              {getTrancheLabel(tranche.trancheNo)}
            </span>
            <span
              className={cn(
                'inline-flex items-center px-2 py-0.5 rounded-md text-xs font-medium border',
                statusBadgeClass[tranche.status]
              )}
            >
              {t(`drawdowns.status.${tranche.status}`)}
            </span>
            {tranche.interestOnly && (
              <span className="inline-flex items-center px-2 py-0.5 rounded-md bg-info/10 text-info border border-info/30 text-xs font-medium">
                {t('drawdowns.interestOnly')}
              </span>
            )}
            {tranche.realEstateId != null && (
              <span className="text-xs text-text-tertiary">
                {t('drawdowns.linkedProperty', { id: tranche.realEstateId })}
              </span>
            )}
          </div>
          <div className="text-xs text-text-secondary ml-4 flex-shrink-0 flex items-center gap-4">
            <span>
              {t('drawdowns.planned')}:{' '}
              <span className="font-mono text-text-primary">
                <ConvertedAmount
                  amount={tranche.plannedAmount}
                  currency={tranche.currency ?? liability.currency}
                  inline
                />
              </span>
            </span>
            <span>
              {t('drawdowns.drawn')}:{' '}
              <span className="font-mono text-text-primary">
                <ConvertedAmount
                  amount={tranche.drawnAmount ?? 0}
                  currency={tranche.currency ?? liability.currency}
                  inline
                />
              </span>
            </span>
            <span>
              {t('drawdowns.remaining')}:{' '}
              <span className="font-mono font-semibold text-text-primary">
                <ConvertedAmount
                  amount={tranche.remaining}
                  currency={tranche.currency ?? liability.currency}
                  inline
                />
              </span>
            </span>
          </div>
        </div>
      ))}
    </div>
  );
}
