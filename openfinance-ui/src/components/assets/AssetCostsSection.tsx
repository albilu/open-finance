/**
 * AssetCostsSection Component (Task 8)
 *
 * Renders the asset's linked costs inside the detail modal Overview tab,
 * split into Capitalized (improvements) and Maintenance lists, fetched via
 * the assetId transaction filter.
 */
import { useTranslation } from 'react-i18next';
import { Hammer, Wrench } from 'lucide-react';
import { ConvertedAmount } from '@/components/ui/ConvertedAmount';
import { useTransactions } from '@/hooks/useTransactions';
import type { Asset } from '@/types/asset';
import type { Transaction } from '@/types/transaction';
import { cn } from '@/lib/utils';

function CostRow({ tx, label, currency }: { tx: Transaction; label: string; currency: string }) {
  return (
    <div className="flex items-center justify-between px-4 py-3 bg-background hover:bg-surface transition-colors">
      <div className="flex items-center gap-3 min-w-0">
        <div className="min-w-0">
          <div className="text-sm text-foreground truncate flex items-center gap-2">
            <span className="truncate">{tx.description || `Transaction #${tx.id}`}</span>
            <span className="inline-flex items-center px-2 py-0.5 rounded-md bg-surface text-muted-foreground border border-border text-xs font-medium flex-shrink-0">
              {label}
            </span>
          </div>
          <div className="text-xs text-muted-foreground">
            {new Date(tx.date).toLocaleDateString('en-US', {
              year: 'numeric',
              month: 'short',
              day: 'numeric',
            })}
          </div>
        </div>
      </div>
      <div className="text-sm font-mono font-semibold text-foreground ml-4 flex-shrink-0">
        <ConvertedAmount amount={tx.amount} currency={tx.currency || currency} inline />
      </div>
    </div>
  );
}

export function AssetCostsSection({ asset }: { asset: Asset }) {
  const { t } = useTranslation('assets');
  const { data: costsPage, isLoading } = useTransactions({
    assetId: asset.id,
    size: 200,
    sort: 'date,desc',
  });

  const costs = costsPage?.content ?? [];
  const capitalized = costs.filter(tx => tx.movementType === 'CAPITAL_IMPROVEMENT');
  const maintenance = costs.filter(tx => tx.movementType === 'MAINTENANCE');

  const groups = [
    {
      key: 'capitalized',
      title: t('costs.capitalized'),
      icon: <Hammer className="h-4 w-4 text-muted-foreground" />,
      transactions: capitalized,
    },
    {
      key: 'maintenance',
      title: t('costs.maintenance'),
      icon: <Wrench className="h-4 w-4 text-muted-foreground" />,
      transactions: maintenance,
    },
  ];

  return (
    <div className="bg-background border border-border rounded-lg p-6">
      <h3 className="text-lg font-semibold text-foreground mb-4">{t('costs.title')}</h3>
      {isLoading ? (
        <div className="space-y-2 animate-pulse">
          {[...Array(2)].map((_, i) => (
            <div key={i} className="h-12 bg-surface border border-border rounded-lg" />
          ))}
        </div>
      ) : capitalized.length === 0 && maintenance.length === 0 ? (
        <p className="text-sm text-muted-foreground">{t('costs.empty')}</p>
      ) : (
        <div className="space-y-4">
          {groups
            .filter(group => group.transactions.length > 0)
            .map(group => (
              <div key={group.key}>
                <h4
                  className={cn(
                    'text-sm font-semibold text-foreground mb-2 flex items-center gap-2'
                  )}
                >
                  {group.icon}
                  {group.title}
                </h4>
                <div className="divide-y divide-border border border-border rounded-lg overflow-hidden">
                  {group.transactions.map(tx => (
                    <CostRow
                      key={tx.id}
                      tx={tx}
                      label={t(`costs.movementTypes.${tx.movementType}`)}
                      currency={asset.currency}
                    />
                  ))}
                </div>
              </div>
            ))}
        </div>
      )}
    </div>
  );
}
