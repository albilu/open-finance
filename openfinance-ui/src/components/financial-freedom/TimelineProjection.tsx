import type { YearlyProjection } from '../../types/calculator';
import { useFormatCurrency } from '@/hooks/useFormatCurrency';
import { cn } from '../../lib/utils';

interface TimelineProjectionProps {
  projections: YearlyProjection[];
  targetAmount: number;
  currency: string;
  yearsToFreedom?: number;
}

/**
 * TimelineProjection Component
 * 
 * Displays a detailed year-by-year table showing:
 * - Year number
 * - Projected savings balance
 * - Total contributions made
 * - Investment returns earned
 * - Whether financial freedom is achieved
 */
export function TimelineProjection({
  projections,
  targetAmount,
  currency,
  yearsToFreedom,
}: TimelineProjectionProps) {
  const { format: formatCurrency } = useFormatCurrency();
  // Calculate progress percentage
  const calculateProgress = (savings: number) => {
    return Math.min((savings / targetAmount) * 100, 100);
  };

  return (
    <div className="overflow-x-auto">
      <table className="w-full text-sm">
        <thead>
          <tr className="border-b border-border">
            <th className="text-left py-3 px-4 font-medium text-text-secondary">Year</th>
            <th className="text-right py-3 px-4 font-medium text-text-secondary">Savings Balance</th>
            <th className="text-right py-3 px-4 font-medium text-text-secondary">Contributions</th>
            <th className="text-right py-3 px-4 font-medium text-text-secondary">Investment Returns</th>
            <th className="text-right py-3 px-4 font-medium text-text-secondary">Progress</th>
            <th className="text-center py-3 px-4 font-medium text-text-secondary">Status</th>
          </tr>
        </thead>
        <tbody>
          {projections.map((projection) => {
            const progress = calculateProgress(projection.endingBalance);
            const isFreedomYear = yearsToFreedom && projection.year <= yearsToFreedom;
            const isBeyondFreedom = yearsToFreedom && projection.year > yearsToFreedom;

            return (
              <tr
                key={projection.year}
                className={cn(
                  'border-b border-border/50 transition-colors',
                  isFreedomYear && 'bg-primary/5',
                  'hover:bg-surface'
                )}
              >
                <td className="py-3 px-4 font-medium text-text-primary">
                  {projection.year === 0 ? 'Now' : `+${projection.year}`}
                </td>
                <td className="py-3 px-4 text-right text-text-primary font-medium">
                  {formatCurrency(projection.endingBalance, currency)}
                </td>
                <td className="py-3 px-4 text-right text-text-secondary">
                  {formatCurrency(projection.contributions, currency)}
                </td>
                <td className="py-3 px-4 text-right text-success">
                  +{formatCurrency(projection.investmentReturns, currency)}
                </td>
                <td className="py-3 px-4">
                  <div className="flex items-center gap-2">
                    <div className="w-24 h-2 bg-surface rounded-full overflow-hidden">
                      <div
                        className={cn(
                          'h-full rounded-full transition-all duration-300',
                          progress >= 100 ? 'bg-success' : 'bg-primary'
                        )}
                        style={{ width: `${progress}%` }}
                      />
                    </div>
                    <span className="text-xs text-text-secondary w-10 text-right">
                      {progress.toFixed(0)}%
                    </span>
                  </div>
                </td>
                <td className="py-3 px-4 text-center">
                  {isBeyondFreedom ? (
                    <span className="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-success/10 text-success">
                      ✓ Achieved
                    </span>
                  ) : isFreedomYear ? (
                    <span className="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-primary/10 text-primary animate-pulse">
                      Target Year
                    </span>
                  ) : (
                    <span className="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-surface text-text-secondary">
                      Building
                    </span>
                  )}
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
