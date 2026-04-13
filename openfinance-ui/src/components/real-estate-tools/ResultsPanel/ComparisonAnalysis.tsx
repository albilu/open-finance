/**
 * ComparisonAnalysis Component
 * 
 * Detailed comparative analysis panel
 * Requirements: REQ-1.6.3
 */

import React from 'react';
import { TrendingUp, Wallet, Calendar, CheckCircle } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/Card';
import { Progress } from '@/components/ui/Progress';
import { Badge } from '@/components/ui/Badge';
import type { BuyRentResults } from '@/types/realEstateTools';
import { useAuthContext } from '@/context/AuthContext';
import { useFormatCurrency } from '@/hooks/useFormatCurrency';

export interface ComparisonAnalysisProps {
  results: BuyRentResults;
}

export const ComparisonAnalysis: React.FC<ComparisonAnalysisProps> = ({ results }) => {
  const { baseCurrency } = useAuthContext();
  const { format: formatCurrency } = useFormatCurrency();
  const { buy, rent, comparison } = results.summary;


  const buyAdvantage = comparison.netWorthDifference > 0;
  const worthDifference = Math.abs(comparison.netWorthDifference);
  const worthDifferencePercent = (worthDifference / Math.max(buy.netWorth, rent.netWorth)) * 100;

  return (
    <div className="space-y-6">
      {/* Winner Banner */}
      <Card className={buyAdvantage ? 'border-green-500 border-2' : 'border-yellow-500 border-2'}>
        <CardContent className="p-6">
          <div className="flex items-center justify-center gap-4">
            {buyAdvantage ? (
              <>
                <CheckCircle className="h-12 w-12 text-green-500" />
                <div className="text-center">
                  <p className="text-2xl font-bold text-green-600">
                    L'achat est plus avantageux
                  </p>
                  <p className="text-muted-foreground">
                    Patrimoine net supérieur de {formatCurrency(worthDifference, baseCurrency)} après {results.years.length} ans
                  </p>
                </div>
              </>
            ) : (
              <>
                <CheckCircle className="h-12 w-12 text-yellow-500" />
                <div className="text-center">
                  <p className="text-2xl font-bold text-yellow-600">
                    La location est plus avantageuse
                  </p>
                  <p className="text-muted-foreground">
                    Économie nette de {formatCurrency(worthDifference, baseCurrency)} après {results.years.length} ans
                  </p>
                </div>
              </>
            )}
          </div>
        </CardContent>
      </Card>

      {/* Key Metrics Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {/* Net Worth Comparison */}
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium flex items-center gap-2">
              <Wallet className="h-4 w-4" />
              Comparaison du patrimoine
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              <div>
                <div className="flex justify-between mb-1">
                  <span className="text-sm">Achat</span>
                  <span className="text-sm font-semibold">{formatCurrency(buy.netWorth, baseCurrency)}</span>
                </div>
                <Progress value={(buy.netWorth / Math.max(buy.netWorth, rent.netWorth)) * 100} />
              </div>
              <div>
                <div className="flex justify-between mb-1">
                  <span className="text-sm">Location</span>
                  <span className="text-sm font-semibold">{formatCurrency(rent.netWorth, baseCurrency)}</span>
                </div>
                <Progress value={(rent.netWorth / Math.max(buy.netWorth, rent.netWorth)) * 100} />
              </div>
              <div className="pt-2 border-t">
                <p className="text-sm text-muted-foreground">Différence</p>
                <p className={`text-xl font-bold ${comparison.netWorthDifference >= 0 ? 'text-green-600' : 'text-red-600'}`}>
                  {comparison.netWorthDifference >= 0 ? '+' : ''}{formatCurrency(comparison.netWorthDifference, baseCurrency)}
                </p>
                <p className="text-xs text-muted-foreground">
                  ({worthDifferencePercent.toFixed(1)}%)
                </p>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Monthly Cost Comparison */}
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium flex items-center gap-2">
              <Calendar className="h-4 w-4" />
              Coût mensuel moyen
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              <div className="flex justify-between items-center">
                <span className="text-sm">Achat</span>
                <Badge variant={buy.averageMonthlyCost < rent.averageMonthlyCost ? 'default' : 'secondary'}>
                  {formatCurrency(buy.averageMonthlyCost, baseCurrency)}/mois
                </Badge>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-sm">Location</span>
                <Badge variant={rent.averageMonthlyCost < buy.averageMonthlyCost ? 'default' : 'secondary'}>
                  {formatCurrency(rent.averageMonthlyCost, baseCurrency)}/mois
                </Badge>
              </div>
              <div className="pt-2 border-t">
                <p className="text-sm text-muted-foreground">Écart mensuel</p>
                <p className={`text-xl font-bold ${comparison.monthlyGap >= 0 ? 'text-green-600' : 'text-red-600'}`}>
                  {comparison.monthlyGap >= 0 ? '+' : ''}{formatCurrency(comparison.monthlyGap, baseCurrency)}/mois
                </p>
                <p className="text-xs text-muted-foreground">
                  {comparison.monthlyGap >= 0
                    ? "L'achat coûte moins cher"
                    : 'La location coûte moins cher'}
                </p>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Total Cost Analysis */}
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium flex items-center gap-2">
              <TrendingUp className="h-4 w-4" />
              Analyse des coûts
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-3">
              <div>
                <p className="text-sm text-muted-foreground">Coût total achat</p>
                <p className="text-lg font-semibold">{formatCurrency(buy.totalCost, baseCurrency)}</p>
              </div>
              <div>
                <p className="text-sm text-muted-foreground">Coût total location</p>
                <p className="text-lg font-semibold">{formatCurrency(rent.totalCost, baseCurrency)}</p>
              </div>
              <div className="pt-2 border-t">
                <p className="text-sm text-muted-foreground">Dépense nette achat</p>
                <p className="text-lg font-semibold text-red-600">{formatCurrency(buy.netExpense, baseCurrency)}</p>
              </div>
              <div>
                <p className="text-sm text-muted-foreground">Dépense nette location</p>
                <p className="text-lg font-semibold text-red-600">{formatCurrency(rent.netExpense, baseCurrency)}</p>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Pros and Cons */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-green-600">
              <CheckCircle className="h-5 w-5" />
              Avantages de l'achat
            </CardTitle>
          </CardHeader>
          <CardContent>
            <ul className="space-y-2">
              <li className="flex items-start gap-2">
                <TrendingUp className="h-4 w-4 mt-1 text-green-600" />
                <span>Constitution d'un patrimoine immobilier</span>
              </li>
              <li className="flex items-start gap-2">
                <CheckCircle className="h-4 w-4 mt-1 text-green-600" />
                <span>Propriété à la fin du prêt</span>
              </li>
              <li className="flex items-start gap-2">
                <CheckCircle className="h-4 w-4 mt-1 text-green-600" />
                <span>Stabilité du logement (pas de propriétaire)</span>
              </li>
              {buy.netWorth > rent.netWorth && (
                <li className="flex items-start gap-2">
                  <TrendingUp className="h-4 w-4 mt-1 text-green-600" />
                  <span>Meilleure performance financière sur {results.years.length} ans</span>
                </li>
              )}
            </ul>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-blue-600">
              <CheckCircle className="h-5 w-5" />
              Avantages de la location
            </CardTitle>
          </CardHeader>
          <CardContent>
            <ul className="space-y-2">
              <li className="flex items-start gap-2">
                <Wallet className="h-4 w-4 mt-1 text-blue-600" />
                <span>Flexibilité pour déménager</span>
              </li>
              <li className="flex items-start gap-2">
                <CheckCircle className="h-4 w-4 mt-1 text-blue-600" />
                <span>Pas de charges d'entretien importantes</span>
              </li>
              <li className="flex items-start gap-2">
                <CheckCircle className="h-4 w-4 mt-1 text-blue-600" />
                <span>Épargne disponible pour autres investissements</span>
              </li>
              {rent.netWorth > buy.netWorth && (
                <li className="flex items-start gap-2">
                  <TrendingUp className="h-4 w-4 mt-1 text-blue-600" />
                  <span>Meilleure performance financière sur {results.years.length} ans</span>
                </li>
              )}
            </ul>
          </CardContent>
        </Card>
      </div>
    </div>
  );
};

export default ComparisonAnalysis;
