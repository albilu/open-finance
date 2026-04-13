/**
 * YearNAnalysisCard Component
 * 
 * Specific year analysis for target resale year
 * Requirements: REQ-1.6.6
 */

import React from 'react';
import { Calendar, TrendingUp, Home, Key, ArrowRight, AlertCircle } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/Card';
import { Alert, AlertDescription } from '@/components/ui/Alert';
import { Badge } from '@/components/ui/Badge';
import { Table, TableBody, TableCell, TableRow } from '@/components/ui/Table';
import type { YearNAnalysis } from '@/types/realEstateTools';
import { useAuthContext } from '@/context/AuthContext';
import { useFormatCurrency } from '@/hooks/useFormatCurrency';

export interface YearNAnalysisCardProps {
  analysis: YearNAnalysis;
  targetYear: number;
}

export const YearNAnalysisCard: React.FC<YearNAnalysisCardProps> = ({
  analysis,
  targetYear,
}) => {
  const { baseCurrency } = useAuthContext();
  const { format: formatCurrency } = useFormatCurrency();
  const buyAdvantage = analysis.netWorth > analysis.rentSavings;
  const minimumPriceAchievable = analysis.propertyValue >= analysis.minimumResalePrice;

  return (
    <div className="space-y-6">
      {/* Header */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Calendar className="h-5 w-5" />
            Analyse à l'année {targetYear}
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="flex items-center justify-center gap-4 py-4">
            {buyAdvantage ? (
              <Badge variant="success" className="text-lg px-4 py-2">
                <Home className="mr-2 h-5 w-5" />
                Achat recommandé
              </Badge>
            ) : (
              <Badge variant="warning" className="text-lg px-4 py-2">
                <Key className="mr-2 h-5 w-5" />
                Location recommandée
              </Badge>
            )}
          </div>
        </CardContent>
      </Card>

      {/* Key Metrics */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Buy Scenario */}
        <Card className="border-l-4 border-l-primary">
          <CardHeader className="bg-primary/10">
            <CardTitle className="flex items-center gap-2">
              <Home className="h-5 w-5" />
              Scénario Achat
            </CardTitle>
          </CardHeader>
          <CardContent className="p-4">
            <Table>
              <TableBody>
                <TableRow>
                  <TableCell className="font-medium">Valeur du bien</TableCell>
                  <TableCell className="text-right text-green-600">
                    {formatCurrency(analysis.propertyValue, baseCurrency)}
                  </TableCell>
                </TableRow>
                <TableRow>
                  <TableCell className="font-medium">Capital restant dû</TableCell>
                  <TableCell className="text-right text-red-600">
                    {formatCurrency(analysis.remainingCapital, baseCurrency)}
                  </TableCell>
                </TableRow>
                <TableRow>
                  <TableCell className="font-medium">Patrimoine net</TableCell>
                  <TableCell className="text-right font-bold text-primary">
                    {formatCurrency(analysis.netWorth, baseCurrency)}
                  </TableCell>
                </TableRow>
                <TableRow>
                  <TableCell className="font-medium">Coûts totaux</TableCell>
                  <TableCell className="text-right">
                    {formatCurrency(analysis.totalCostsBuy, baseCurrency)}
                  </TableCell>
                </TableRow>
                <TableRow>
                  <TableCell className="font-medium">Dépense nette</TableCell>
                  <TableCell className="text-right text-red-600">
                    {formatCurrency(analysis.netExpenseBuy, baseCurrency)}
                  </TableCell>
                </TableRow>
              </TableBody>
            </Table>

            {/* Resale Price Alert */}
            {!minimumPriceAchievable && (
              <Alert variant="warning" className="mt-4">
                <AlertCircle className="h-4 w-4" />
                <AlertDescription>
                   Le prix de revente minimum ({formatCurrency(analysis.minimumResalePrice, baseCurrency)}) 
                   est supérieur à la valeur du bien ({formatCurrency(analysis.propertyValue, baseCurrency)}). 
                  Vous ne pourrez pas réaliser le bénéfice souhaité.
                </AlertDescription>
              </Alert>
            )}

            {minimumPriceAchievable && (
              <Alert variant="success" className="mt-4">
                <TrendingUp className="h-4 w-4" />
                <AlertDescription>
                  Le prix de revente minimum est atteignable. 
                   Bénéfice possible: {formatCurrency(analysis.propertyValue - analysis.minimumResalePrice, baseCurrency)}.
                </AlertDescription>
              </Alert>
            )}
          </CardContent>
        </Card>

        {/* Rent Scenario */}
        <Card className="border-l-4 border-l-warning">
          <CardHeader className="bg-warning/10">
            <CardTitle className="flex items-center gap-2">
              <Key className="h-5 w-5" />
              Scénario Location
            </CardTitle>
          </CardHeader>
          <CardContent className="p-4">
            <Table>
              <TableBody>
                <TableRow>
                  <TableCell className="font-medium">Épargne accumulée</TableCell>
                  <TableCell className="text-right font-bold text-green-600">
                    {formatCurrency(analysis.rentSavings, baseCurrency)}
                   </TableCell>
                 </TableRow>
                 <TableRow>
                   <TableCell className="font-medium">Patrimoine net</TableCell>
                   <TableCell className="text-right font-bold text-warning">
                     {formatCurrency(analysis.rentSavings, baseCurrency)}
                  </TableCell>
                </TableRow>
                <TableRow>
                  <TableCell className="font-medium">Coûts totaux</TableCell>
                  <TableCell className="text-right">
                    {formatCurrency(analysis.totalCostsRent, baseCurrency)}
                  </TableCell>
                </TableRow>
                <TableRow>
                  <TableCell className="font-medium">Dépense nette</TableCell>
                  <TableCell className="text-right text-red-600">
                    {formatCurrency(analysis.netExpenseRent, baseCurrency)}
                  </TableCell>
                </TableRow>
              </TableBody>
            </Table>

            {/* Rent Advantage */}
            {!buyAdvantage && (
              <Alert className="mt-4">
                <TrendingUp className="h-4 w-4" />
                <AlertDescription>
                   La location permet d'économiser {formatCurrency(analysis.netExpenseRent - analysis.netExpenseBuy, baseCurrency)} 
                  par rapport à l'achat à cette échéance.
                </AlertDescription>
              </Alert>
            )}
          </CardContent>
        </Card>
      </div>

      {/* Comparison */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <ArrowRight className="h-5 w-5" />
            Comparaison directe
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6 text-center">
            <div>
              <p className="text-sm text-muted-foreground mb-2">Différence de patrimoine</p>
              <p className={`text-2xl font-bold ${buyAdvantage ? 'text-primary' : 'text-warning'}`}>
                 {buyAdvantage ? '+' : ''}{formatCurrency(analysis.netWorth - analysis.rentSavings, baseCurrency)}
              </p>
              <p className="text-sm text-muted-foreground mt-1">
                en faveur de {buyAdvantage ? "l'achat" : 'la location'}
              </p>
            </div>
            <div>
              <p className="text-sm text-muted-foreground mb-2">Différence de dépenses</p>
              <p className={`text-2xl font-bold ${analysis.netExpenseBuy < analysis.netExpenseRent ? 'text-primary' : 'text-warning'}`}>
                 {formatCurrency(Math.abs(analysis.netExpenseBuy - analysis.netExpenseRent), baseCurrency)}
              </p>
              <p className="text-sm text-muted-foreground mt-1">
                économisé avec {analysis.netExpenseBuy < analysis.netExpenseRent ? "l'achat" : 'la location'}
              </p>
            </div>
            <div>
              <p className="text-sm text-muted-foreground mb-2">Rentabilité annuelle</p>
              <p className="text-2xl font-bold">
                {analysis.annualProfitability.toFixed(2)}%
              </p>
              <p className="text-sm text-muted-foreground mt-1">
                pour le scénario achat
              </p>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Conclusion */}
      <Alert className={buyAdvantage ? 'border-primary' : 'border-warning'}>
        <AlertDescription className="text-center text-lg">
          À l'année {targetYear},{' '}
          <strong>{buyAdvantage ? "l'achat" : 'la location'}</strong>{' '}
          est plus avantageux avec un patrimoine net de{' '}
             <strong>
               {formatCurrency(buyAdvantage ? analysis.netWorth : analysis.rentSavings, baseCurrency)}
             </strong>
             {' '}contre{' '}
             <strong>
               {formatCurrency(buyAdvantage ? analysis.rentSavings : analysis.netWorth, baseCurrency)}
             </strong>
          .
        </AlertDescription>
      </Alert>
    </div>
  );
};

export default YearNAnalysisCard;
