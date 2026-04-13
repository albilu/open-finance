/**
 * YearlyTable Component
 * 
 * Detailed year-by-year results table
 * Requirements: REQ-1.6.4
 */

import React, { useState } from 'react';
import { ChevronDown, ChevronUp } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/Table';
import type { BuyRentResults } from '@/types/realEstateTools';
import { useAuthContext } from '@/context/AuthContext';
import { useFormatCurrency } from '@/hooks/useFormatCurrency';

export interface YearlyTableProps {
  results: BuyRentResults;
}

export const YearlyTable: React.FC<YearlyTableProps> = ({ results }) => {
  const { baseCurrency } = useAuthContext();
  const { format: formatCurrency } = useFormatCurrency();
  const [expandedYears, setExpandedYears] = useState<Set<number>>(new Set());

  const toggleYear = (year: number) => {
    const newExpanded = new Set(expandedYears);
    if (newExpanded.has(year)) {
      newExpanded.delete(year);
    } else {
      newExpanded.add(year);
    }
    setExpandedYears(newExpanded);
  };

  const isPriceAboveMinimum = (propertyValue: number, minimumPrice: number) => {
    return minimumPrice > propertyValue;
  };

  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between">
        <CardTitle>Détail année par année</CardTitle>
        <div className="text-sm text-muted-foreground">
          {results.years.length} années
        </div>
      </CardHeader>
      <CardContent>
        <div className="overflow-x-auto">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="w-16">Année</TableHead>
                <TableHead>Coût achat</TableHead>
                <TableHead>Cumulé achat</TableHead>
                <TableHead>Valeur bien</TableHead>
                <TableHead>Capital restant</TableHead>
                <TableHead>Prix revente min</TableHead>
                <TableHead>Coût location</TableHead>
                <TableHead>Cumulé location</TableHead>
                <TableHead>Épargne</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {results.years.map((year) => {
                const isExpanded = expandedYears.has(year.year);
                const showWarning = isPriceAboveMinimum(
                  year.buy.propertyValue,
                  year.buy.minimumResalePrice
                );

                return (
                  <React.Fragment key={year.year}>
                    <TableRow
                      className="cursor-pointer hover:bg-muted/50"
                      onClick={() => toggleYear(year.year)}
                    >
                      <TableCell className="font-medium">
                        <div className="flex items-center gap-1">
                          {isExpanded ? (
                            <ChevronUp className="h-4 w-4" />
                          ) : (
                            <ChevronDown className="h-4 w-4" />
                          )}
                          {year.year}
                        </div>
                      </TableCell>
                      <TableCell>{formatCurrency(year.buy.annualCost, baseCurrency)}</TableCell>
                      <TableCell>{formatCurrency(year.buy.cumulativeCost, baseCurrency)}</TableCell>
                      <TableCell className="text-green-600">
                        {formatCurrency(year.buy.propertyValue, baseCurrency)}
                      </TableCell>
                      <TableCell className="text-red-600">
                        {formatCurrency(year.buy.remainingCapital, baseCurrency)}
                      </TableCell>
                      <TableCell className={showWarning ? 'text-red-600 font-semibold' : ''}>
                        {formatCurrency(year.buy.minimumResalePrice, baseCurrency)}
                        {showWarning && ' ⚠️'}
                      </TableCell>
                      <TableCell>{formatCurrency(year.rent.annualCost, baseCurrency)}</TableCell>
                      <TableCell>{formatCurrency(year.rent.cumulativeCost, baseCurrency)}</TableCell>
                      <TableCell className="text-green-600">
                        {formatCurrency(year.rent.savings, baseCurrency)}
                      </TableCell>
                    </TableRow>
                    
                    {isExpanded && (
                      <TableRow>
                        <TableCell colSpan={9} className="bg-muted/30 p-4">
                          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
                            <div>
                              <p className="font-medium text-muted-foreground">Détails Achat</p>
                              <ul className="mt-2 space-y-1">
                                 <li>Mensualités: {formatCurrency(year.buy.details.mortgage, baseCurrency)}</li>
                                 <li>Assurance: {formatCurrency(year.buy.details.insurance, baseCurrency)}</li>
                                 <li>Taxe foncière: {formatCurrency(year.buy.details.propertyTax, baseCurrency)}</li>
                                 <li>Charges: {formatCurrency(year.buy.details.coOwnershipCharges, baseCurrency)}</li>
                                 <li>Entretien: {formatCurrency(year.buy.details.maintenance, baseCurrency)}</li>
                              </ul>
                            </div>
                            <div>
                              <p className="font-medium text-muted-foreground">Patrimoine</p>
                              <ul className="mt-2 space-y-1">
                                 <li>Valeur: {formatCurrency(year.buy.propertyValue, baseCurrency)}</li>
                                 <li>Capital dû: {formatCurrency(year.buy.remainingCapital, baseCurrency)}</li>
                                 <li className="font-semibold">
                                   Net: {formatCurrency(year.buy.propertyValue - year.buy.remainingCapital, baseCurrency)}
                                </li>
                              </ul>
                            </div>
                            <div>
                              <p className="font-medium text-muted-foreground">Détails Location</p>
                              <ul className="mt-2 space-y-1">
                                 <li>Loyer: {formatCurrency(year.rent.annualCost * 0.8, baseCurrency)}</li>
                                 <li>Charges: {formatCurrency(year.rent.annualCost * 0.2, baseCurrency)}</li>
                                 <li>Total: {formatCurrency(year.rent.annualCost, baseCurrency)}</li>
                              </ul>
                            </div>
                            <div>
                              <p className="font-medium text-muted-foreground">Comparaison</p>
                              <ul className="mt-2 space-y-1">
                                 <li>
                                   Différence: {formatCurrency(
                                     (year.buy.propertyValue - year.buy.remainingCapital) - year.rent.savings,
                                     baseCurrency
                                   )}
                                </li>
                                <li>
                                  Avantage: {(year.buy.propertyValue - year.buy.remainingCapital) > year.rent.savings
                                    ? 'Achat'
                                    : 'Location'}
                                </li>
                              </ul>
                            </div>
                          </div>
                        </TableCell>
                      </TableRow>
                    )}
                  </React.Fragment>
                );
              })}
            </TableBody>
          </Table>
        </div>
        
        <div className="mt-4 flex justify-center">
          <Button
            variant="outline"
            onClick={() => setExpandedYears(new Set(results.years.map(y => y.year)))}
            className="mr-2"
          >
            Tout développer
          </Button>
          <Button
            variant="outline"
            onClick={() => setExpandedYears(new Set())}
          >
            Tout réduire
          </Button>
        </div>
      </CardContent>
    </Card>
  );
};

export default YearlyTable;
