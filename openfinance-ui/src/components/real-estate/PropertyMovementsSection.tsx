/**
 * PropertyMovementsSection Component (Task 8)
 *
 * Renders property-linked movements inside the property detail Overview tab:
 *  - "Costs": transactions linked via realEstateId, split into Capitalized
 *    (capital improvements) and Maintenance lists
 *  - "Loan movements": repayments / disbursements of the property's mortgage
 *    liability
 */
import { useTranslation } from 'react-i18next';
import { Hammer, Wrench, Landmark, AlertCircle } from 'lucide-react';
import { Card } from '@/components/ui/Card';
import { ConvertedAmount } from '@/components/ui/ConvertedAmount';
import { useTransactions } from '@/hooks/useTransactions';
import { useLiabilityTransactions } from '@/hooks/useLiabilities';
import type { RealEstateProperty } from '@/types/realEstate';
import type { Transaction } from '@/types/transaction';

/**
 * Page size for the movements fetch: a property's improvement/maintenance history
 * is bounded (dozens of rows), so 200 covers the practical lifetime without
 * pagination UI.
 */
const MOVEMENTS_PAGE_SIZE = 200;

/** Movement classifications shown in the Loan movements list. */
const LOAN_MOVEMENT_TYPES = new Set(['REPAYMENT', 'DISBURSEMENT']);

function MovementRow({
  tx,
  label,
  currency,
}: {
  tx: Transaction;
  label: string;
  currency: string;
}) {
  const { i18n } = useTranslation('realEstate');
  return (
    <div className="flex items-center justify-between px-4 py-3 bg-surface hover:bg-surface-elevated transition-colors">
      <div className="flex items-center gap-3 min-w-0">
        <div className="min-w-0">
          <div className="text-sm text-text-primary truncate flex items-center gap-2">
            <span className="truncate">{tx.description || `Transaction #${tx.id}`}</span>
            <span className="inline-flex items-center px-2 py-0.5 rounded-md bg-surface-elevated text-text-secondary border border-border text-xs font-medium flex-shrink-0">
              {label}
            </span>
          </div>
          <div className="text-xs text-text-tertiary">
            {new Date(tx.date).toLocaleDateString(i18n.language, {
              year: 'numeric',
              month: 'short',
              day: 'numeric',
            })}
          </div>
        </div>
      </div>
      <div className="text-sm font-mono font-semibold text-text-primary ml-4 flex-shrink-0">
        <ConvertedAmount amount={tx.amount} currency={tx.currency || currency} inline />
      </div>
    </div>
  );
}

function MovementList({
  title,
  icon,
  transactions,
  currency,
  movementLabel,
  emptyLabel,
}: {
  title: string;
  icon: React.ReactNode;
  transactions: Transaction[];
  currency: string;
  movementLabel: (tx: Transaction) => string;
  emptyLabel: string;
}) {
  return (
    <Card className="p-4">
      <h4 className="text-sm font-semibold text-text-primary mb-3 flex items-center gap-2">
        {icon}
        {title}
      </h4>
      {transactions.length === 0 ? (
        <p className="text-xs text-text-tertiary">{emptyLabel}</p>
      ) : (
        <div className="divide-y divide-border border border-border rounded-lg overflow-hidden">
          {transactions.map(tx => (
            <MovementRow key={tx.id} tx={tx} label={movementLabel(tx)} currency={currency} />
          ))}
        </div>
      )}
    </Card>
  );
}

export function PropertyMovementsSection({ property }: { property: RealEstateProperty }) {
  const { t } = useTranslation('realEstate');
  const {
    data: costsPage,
    isLoading: isLoadingCosts,
    error: costsError,
  } = useTransactions({
    realEstateId: property.id,
    size: MOVEMENTS_PAGE_SIZE,
    sort: 'date,desc',
  });
  const {
    data: loanTransactions = [],
    isLoading: isLoadingLoans,
    error: loansError,
  } = useLiabilityTransactions(property.mortgageId ?? null);

  const isLoading = isLoadingCosts || isLoadingLoans;
  const error = costsError ?? loansError;

  const costs = costsPage?.content ?? [];
  const capitalized = costs.filter(tx => tx.movementType === 'CAPITAL_IMPROVEMENT');
  const maintenance = costs.filter(tx => tx.movementType === 'MAINTENANCE');
  const loanMovements = loanTransactions.filter(
    tx => tx.movementType != null && LOAN_MOVEMENT_TYPES.has(tx.movementType)
  );

  const hasNoMovements =
    capitalized.length === 0 && maintenance.length === 0 && loanMovements.length === 0;

  return (
    <div className="space-y-4">
      <h3 className="text-lg font-semibold text-text-primary">{t('movements.costsTitle')}</h3>
      {isLoading ? (
        <div className="space-y-2 animate-pulse">
          {[...Array(2)].map((_, i) => (
            <div key={i} className="h-12 bg-surface border border-border rounded-lg" />
          ))}
        </div>
      ) : error ? (
        <div className="flex items-center gap-2 p-4 bg-error/10 border border-error/20 rounded-lg text-error text-sm">
          <AlertCircle className="h-4 w-4 flex-shrink-0" />
          <span>{t('movements.error')}</span>
        </div>
      ) : hasNoMovements ? (
        <p className="text-sm text-text-secondary">{t('movements.empty')}</p>
      ) : (
        <>
          <MovementList
            title={t('movements.capitalized')}
            icon={<Hammer className="h-4 w-4 text-text-secondary" />}
            transactions={capitalized}
            currency={property.currency}
            movementLabel={tx => t(`movements.movementTypes.${tx.movementType}`)}
            emptyLabel={t('movements.empty')}
          />
          <MovementList
            title={t('movements.maintenance')}
            icon={<Wrench className="h-4 w-4 text-text-secondary" />}
            transactions={maintenance}
            currency={property.currency}
            movementLabel={tx => t(`movements.movementTypes.${tx.movementType}`)}
            emptyLabel={t('movements.empty')}
          />
          {loanMovements.length > 0 && (
            <MovementList
              title={t('movements.loanMovements')}
              icon={<Landmark className="h-4 w-4 text-text-secondary" />}
              transactions={loanMovements}
              currency={property.currency}
              movementLabel={tx => t(`movements.movementTypes.${tx.movementType}`)}
              emptyLabel={t('movements.empty')}
            />
          )}
        </>
      )}
    </div>
  );
}
