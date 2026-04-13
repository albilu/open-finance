/**
 * ExpensesSection Component
 * 
 * Owner expenses parameters
 * Requirements: REQ-2.3.x
 */

import React from 'react';
import { Receipt, FileText, Shield, Percent, Building2, ChevronDown } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/Card';
import { Input } from '@/components/ui/Input';
import { Label } from '@/components/ui/Label';
import { Alert, AlertDescription } from '@/components/ui/Alert';
import { Separator } from '@/components/ui/Separator';
import type { OwnerExpensesInputs, ValidationError } from '@/types/realEstateTools';
import { useAuthContext } from '@/context/AuthContext';
import { useFormatCurrency } from '@/hooks/useFormatCurrency';

export interface ExpensesSectionProps {
  inputs: OwnerExpensesInputs;
  errors: ValidationError[];
  onUpdate: (field: keyof OwnerExpensesInputs, value: number) => void;
  isOpen: boolean;
  onToggle: () => void;
}

export const ExpensesSection: React.FC<ExpensesSectionProps> = ({
  inputs,
  errors,
  onUpdate,
  isOpen,
  onToggle,
}) => {
  const { baseCurrency } = useAuthContext();
  const { format: formatCurrency } = useFormatCurrency();

  const getFieldError = (field: string) => errors.find(e => e.field === `expenses.${field}`)?.message;

  const totalDeductibleExpenses =
    inputs.propertyTax +
    inputs.nonRecoverableCharges +
    inputs.annualMaintenance +
    inputs.cfe +
    inputs.cvae +
    inputs.managementFees +
    inputs.pnoInsurance +
    inputs.accountingFees;

  return (
    <Card className="h-full">
      <CardHeader
        className="bg-info/10 cursor-pointer select-none pb-4"
        onClick={onToggle}
      >
        <CardTitle className="flex items-center justify-between text-lg">
          <span className="flex items-center gap-2">
            <Receipt className="h-5 w-5" />
            Charges Propriétaire
          </span>
          <ChevronDown
            className={`h-4 w-4 transition-transform duration-200 ${isOpen ? 'rotate-180' : ''}`}
          />
        </CardTitle>
      </CardHeader>
      <div
        className={`overflow-hidden transition-all duration-300 ease-in-out ${isOpen ? 'max-h-[2000px] opacity-100' : 'max-h-0 opacity-0'
          }`}
      >
        <CardContent className="p-4 space-y-4">
          {/* Tax Section */}
          <div className="space-y-3">
            <h4 className="text-sm font-medium flex items-center gap-2">
              <FileText className="h-4 w-4" />
              Taxes et impôts
            </h4>

            <div className="space-y-2">
              <Label htmlFor="propertyTax">Taxe foncière</Label>
              <Input
                id="propertyTax"
                type="number"
                value={inputs.propertyTax}
                onChange={(e) => onUpdate('propertyTax', parseFloat(e.target.value) || 0)}
                min={0}
                step={100}
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="cfe">CFE (Cotisation Foncière des Entreprises)</Label>
              <Input
                id="cfe"
                type="number"
                value={inputs.cfe}
                onChange={(e) => onUpdate('cfe', parseFloat(e.target.value) || 0)}
                min={0}
                step={100}
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="cvae">CVAE (si applicable)</Label>
              <Input
                id="cvae"
                type="number"
                value={inputs.cvae}
                onChange={(e) => onUpdate('cvae', parseFloat(e.target.value) || 0)}
                min={0}
                step={100}
              />
            </div>
          </div>

          <Separator />

          {/* Building Charges */}
          <div className="space-y-3">
            <h4 className="text-sm font-medium flex items-center gap-2">
              <Building2 className="h-4 w-4" />
              Charges de copropriété
            </h4>

            <div className="space-y-2">
              <Label htmlFor="nonRecoverableCharges">Charges non récupérables</Label>
              <Input
                id="nonRecoverableCharges"
                type="number"
                value={inputs.nonRecoverableCharges}
                onChange={(e) => onUpdate('nonRecoverableCharges', parseFloat(e.target.value) || 0)}
                min={0}
                step={100}
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="annualMaintenance">Travaux d'entretien annuels</Label>
              <Input
                id="annualMaintenance"
                type="number"
                value={inputs.annualMaintenance}
                onChange={(e) => onUpdate('annualMaintenance', parseFloat(e.target.value) || 0)}
                min={0}
                step={100}
              />
            </div>
          </div>

          <Separator />

          {/* Management & Insurance */}
          <div className="space-y-3">
            <h4 className="text-sm font-medium flex items-center gap-2">
              <Shield className="h-4 w-4" />
              Gestion et assurance
            </h4>

            <div className="space-y-2">
              <Label htmlFor="managementFees">Frais de gestion locative</Label>
              <Input
                id="managementFees"
                type="number"
                value={inputs.managementFees}
                onChange={(e) => onUpdate('managementFees', parseFloat(e.target.value) || 0)}
                min={0}
                step={50}
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="pnoInsurance">Assurance PNO (Propriétaire Non Occupant)</Label>
              <Input
                id="pnoInsurance"
                type="number"
                value={inputs.pnoInsurance}
                onChange={(e) => onUpdate('pnoInsurance', parseFloat(e.target.value) || 0)}
                min={0}
                step={50}
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="accountingFees">Frais de comptabilité</Label>
              <Input
                id="accountingFees"
                type="number"
                value={inputs.accountingFees}
                onChange={(e) => onUpdate('accountingFees', parseFloat(e.target.value) || 0)}
                min={0}
                step={50}
              />
            </div>
          </div>

          <Separator />

          {/* Tax Rate */}
          <div className="space-y-2">
            <Label htmlFor="marginalTaxRate" className="flex items-center gap-2">
              <Percent className="h-4 w-4" />
              Taux marginal d'imposition (TMI)
            </Label>
            <Input
              id="marginalTaxRate"
              type="number"
              value={inputs.marginalTaxRate}
              onChange={(e) => onUpdate('marginalTaxRate', parseFloat(e.target.value) || 0)}
              min={0}
              max={60}
              step={0.5}
            />
            {getFieldError('marginalTaxRate') && (
              <Alert variant="error" className="py-2">
                <AlertDescription className="text-xs">{getFieldError('marginalTaxRate')}</AlertDescription>
              </Alert>
            )}
          </div>

          {/* Expenses Summary */}
          <div className="pt-2 border-t">
            <div className="flex justify-between">
              <span className="text-muted-foreground">Total charges déductibles</span>
              <span className="font-semibold text-error">{formatCurrency(totalDeductibleExpenses, baseCurrency)}</span>
            </div>
          </div>
        </CardContent>
      </div>
    </Card>
  );
};

export default ExpensesSection;
