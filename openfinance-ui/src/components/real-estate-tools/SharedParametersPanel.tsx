/**
 * SharedParametersPanel Component
 * 
 * Displays data inherited from Buy/Rent comparator
 * Requirements: REQ-4.2.1
 */

import React from 'react';
import { Link2, Home, Wallet, FileText } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import { useAuthContext } from '@/context/AuthContext';
import { useFormatCurrency } from '@/hooks/useFormatCurrency';
import type { SharedPropertyData } from '@/types/realEstateTools';

export interface SharedParametersPanelProps {
  sharedData: SharedPropertyData;
}

export const SharedParametersPanel: React.FC<SharedParametersPanelProps> = ({
  sharedData,
}) => {
  const { baseCurrency } = useAuthContext();
  const { format: formatCurrency } = useFormatCurrency();
  if (!sharedData) {
    return null;
  }

  return (
    <Card className="mb-6 border-primary/20 bg-primary/5">
      <CardHeader className="pb-3">
        <div className="flex items-center justify-between">
          <CardTitle className="text-base flex items-center gap-2">
            <Link2 className="h-4 w-4 text-primary" />
            Données importées du comparateur
          </CardTitle>
          <Badge variant="outline" className="text-xs">
            Lecture seule
          </Badge>
        </div>
      </CardHeader>
      <CardContent className="pt-0">
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
          {/* Property Price */}
          <div className="flex items-center gap-3 bg-background/50 p-3 rounded">
            <Home className="h-5 w-5 text-muted-foreground" />
            <div>
              <p className="text-xs text-muted-foreground">Prix du bien</p>
              <p className="font-medium">{formatCurrency(sharedData.totalPrice, baseCurrency)}</p>
            </div>
          </div>

          {/* Credit Payment */}
          <div className="flex items-center gap-3 bg-background/50 p-3 rounded">
            <Wallet className="h-5 w-5 text-muted-foreground" />
            <div>
              <p className="text-xs text-muted-foreground">Mensualité crédit</p>
              <p className="font-medium">{formatCurrency(sharedData.credit.monthlyPayment, baseCurrency)}/mois</p>
            </div>
          </div>

          {/* Property Tax */}
          <div className="flex items-center gap-3 bg-background/50 p-3 rounded">
            <FileText className="h-5 w-5 text-muted-foreground" />
            <div>
              <p className="text-xs text-muted-foreground">Taxe foncière</p>
              <p className="font-medium">{formatCurrency(sharedData.propertyTax, baseCurrency)}/an</p>
            </div>
          </div>

          {/* Co-ownership Charges */}
          <div className="flex items-center gap-3 bg-background/50 p-3 rounded">
            <FileText className="h-5 w-5 text-muted-foreground" />
            <div>
              <p className="text-xs text-muted-foreground">Charges copro (non récup.)</p>
              <p className="font-medium">{formatCurrency(sharedData.coOwnershipCharges, baseCurrency)}/an</p>
            </div>
          </div>
        </div>

        {/* Note */}
        <div className="mt-4 text-sm text-muted-foreground">
          <p>
            Ces valeurs ont été importées depuis le comparateur achat/location.
            Vous pouvez les modifier ci-dessous si nécessaire.
          </p>
        </div>
      </CardContent>
    </Card>
  );
};

export default SharedParametersPanel;
