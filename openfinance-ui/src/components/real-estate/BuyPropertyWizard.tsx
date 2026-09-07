/**
 * BuyPropertyWizard Component (Task 9)
 *
 * Optional guided flow for acquiring a property in three steps:
 *  1. Property — name, address, type, price/date/value/currency
 *  2. Funding — new/existing mortgage (or cash), disbursement route
 *     (bank pays the seller directly vs. pays my account) and an optional
 *     down payment from a checking account
 *  3. Review — executes the endpoint sequence and closes
 *
 * Endpoint sequence (documented decision): create liability → create property
 * (carrying mortgageId) → disburse → down-payment CAPITAL_IMPROVEMENT
 * transaction. A direct disbursement needs the property to exist, so the
 * disbursement always follows the property creation.
 */
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Building2, Home, Landmark, Wallet } from 'lucide-react';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { DateInput } from '@/components/ui/DateInput';
import { NumberInput } from '@/components/ui/NumberInput';
import { CurrencySelector } from '@/components/ui/CurrencySelector';
import { AccountSelector } from '@/components/ui/AccountSelector';
import { LiabilitySelector } from '@/components/ui/LiabilitySelector';
import {
  useLiabilities,
  useCreateLiability,
  useDisburseLiability,
} from '@/hooks/useLiabilities';
import { useCreateProperty } from '@/hooks/useRealEstate';
import { useCreateTransaction } from '@/hooks/useTransactions';
import { useAuthContext } from '@/context/AuthContext';
import { DEFAULT_CURRENCY } from '@/utils/currency';
import { getToday } from '@/utils/date';
import type { Account } from '@/types/account';
import type { RealEstatePropertyRequest } from '@/types/realEstate';
import type { TransactionRequest } from '@/types/transaction';

type FundingSource = 'new' | 'existing' | 'none';
type DisbursementRoute = 'direct' | 'account';

interface PropertyStepState {
  name: string;
  address: string;
  propertyType: string;
  purchasePrice: string;
  purchaseDate: string;
  currentValue: string;
  currency: string;
}

interface FundingStepState {
  source: FundingSource;
  mortgageName: string;
  loanAmount: string;
  interestRate: string;
  existingMortgageId?: number;
  route: DisbursementRoute;
  downPaymentAmount: string;
  downPaymentAccountId?: number;
}

/** IDs of resources already created by a previous confirm attempt (retry dedupe). */
interface CreatedIdsState {
  liabilityId?: number;
  propertyId?: number;
}

const PROPERTY_TYPE_OPTIONS = [
  'RESIDENTIAL',
  'COMMERCIAL',
  'LAND',
  'MIXED_USE',
  'INDUSTRIAL',
  'OTHER',
];

const STEP_ICONS = [Building2, Landmark, Wallet];

export function BuyPropertyWizard({ accounts, onClose }: { accounts: Account[]; onClose: () => void }) {
  const { t } = useTranslation('realEstate');
  const { baseCurrency } = useAuthContext();
  const today = getToday();
  const [step, setStep] = useState(0);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [property, setProperty] = useState<PropertyStepState>({
    name: '',
    address: '',
    propertyType: 'RESIDENTIAL',
    purchasePrice: '',
    purchaseDate: today,
    currentValue: '',
    currency: baseCurrency || DEFAULT_CURRENCY,
  });
  const [funding, setFunding] = useState<FundingStepState>({
    source: 'new',
    mortgageName: '',
    loanAmount: '',
    interestRate: '',
    existingMortgageId: undefined,
    route: 'direct',
    downPaymentAmount: '',
    downPaymentAccountId: undefined,
  });

  // Memoized creation results: when a confirm attempt fails after the liability/property were
  // created, a retry must reuse them instead of creating duplicates.
  const [createdIds, setCreatedIds] = useState<CreatedIdsState>({});

  const { data: liabilities = [] } = useLiabilities();
  const createLiability = useCreateLiability();
  const createProperty = useCreateProperty();
  const createTransaction = useCreateTransaction();
  const disburse = useDisburseLiability();

  const updateProperty = (patch: Partial<PropertyStepState>) =>
    setProperty(prev => ({ ...prev, ...patch }));
  const updateFunding = (patch: Partial<FundingStepState>) =>
    setFunding(prev => ({ ...prev, ...patch }));

  const price = Number(property.purchasePrice);
  const loan = Number(funding.loanAmount);
  const down = Number(funding.downPaymentAmount);

  // The 'account' disbursement route sends the funds to a checking account — one must be
  // selected, otherwise the request would go out with an undefined toAccountId.
  const accountRouteMissingAccount =
    funding.source !== 'none' &&
    funding.route === 'account' &&
    funding.downPaymentAccountId == null;

  const fundingStepValid =
    funding.source === 'new'
      ? funding.mortgageName.trim() !== '' && loan > 0
      : funding.source === 'existing'
        ? funding.existingMortgageId != null && loan > 0
        : true;

  const stepValid =
    step === 0
      ? property.name.trim() !== '' &&
        property.address.trim() !== '' &&
        price > 0 &&
        Number(property.currentValue) > 0
      : step === 1
        ? fundingStepValid && !accountRouteMissingAccount
        : true;

  const handleConfirm = async () => {
    setIsSubmitting(true);
    setError(null);
    try {
      // Reuse resources from a previous attempt so a retry never duplicates them.
      let mortgageId = funding.existingMortgageId ?? createdIds.liabilityId;
      if (funding.source === 'new' && mortgageId == null) {
        const liability = await createLiability.mutateAsync({
          name: funding.mortgageName.trim(),
          type: 'MORTGAGE',
          principal: funding.loanAmount,
          currentBalance: '0',
          interestRate: funding.interestRate ? Number(funding.interestRate) : undefined,
          startDate: property.purchaseDate,
          currency: property.currency,
        });
        mortgageId = liability.id;
        setCreatedIds(prev => ({ ...prev, liabilityId: liability.id }));
      }

      let propertyId = createdIds.propertyId;
      if (propertyId == null) {
        const created = await createProperty.mutateAsync({
          name: property.name.trim(),
          address: property.address.trim(),
          propertyType: property.propertyType as RealEstatePropertyRequest['propertyType'],
          purchasePrice: property.purchasePrice,
          purchaseDate: property.purchaseDate,
          currentValue: property.currentValue,
          currency: property.currency,
          mortgageId: mortgageId ?? null,
          rentalIncome: null,
          notes: null,
          documents: null,
          latitude: null,
          longitude: null,
          isActive: true,
        });
        propertyId = created.id;
        setCreatedIds(prev => ({ ...prev, propertyId: created.id }));
      }

      if (mortgageId != null && loan > 0) {
        await disburse.mutateAsync({
          liabilityId: mortgageId,
          request: {
            directRealEstateId: funding.route === 'direct' ? propertyId : undefined,
            toAccountId: funding.route === 'account' ? funding.downPaymentAccountId : undefined,
            amount: loan,
            date: property.purchaseDate,
          },
        });
      }

      if (funding.downPaymentAccountId != null && down > 0) {
        const account = accounts.find(a => a.id === funding.downPaymentAccountId);
        const request: TransactionRequest = {
          accountId: funding.downPaymentAccountId,
          type: 'EXPENSE',
          amount: down,
          currency: account?.currency ?? property.currency,
          date: property.purchaseDate,
          description: t('wizard.downPaymentDescription', { name: property.name }),
          movementType: 'CAPITAL_IMPROVEMENT',
          realEstateId: propertyId,
        };
        await createTransaction.mutateAsync(request);
      }

      onClose();
    } catch (e) {
      setError(e instanceof Error ? e.message : t('wizard.error'));
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="space-y-6" data-testid="buy-property-wizard">
      {/* Stepper */}
      <ol className="flex items-center gap-2 text-sm">
        {['property', 'funding', 'review'].map((key, i) => {
          const Icon = STEP_ICONS[i];
          const active = step === i;
          return (
            <li
              key={key}
              className={`flex items-center gap-1.5 px-3 py-1.5 rounded-md border ${
                active
                  ? 'border-primary text-primary bg-primary/10'
                  : i < step
                    ? 'border-primary/30 text-primary'
                    : 'border-border text-text-secondary'
              }`}
            >
              <Icon className="h-4 w-4" />
              {t(`wizard.steps.${key}`)}
            </li>
          );
        })}
      </ol>

      {/* Step 1: property */}
      {step === 0 && (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div className="md:col-span-2">
            <label htmlFor="wizard-name" className="block text-sm font-medium mb-1.5">
              {t('form.propertyName')} *
            </label>
            <Input
              id="wizard-name"
              value={property.name}
              onChange={e => updateProperty({ name: e.target.value })}
              placeholder={t('form.propertyNamePlaceholder')}
            />
          </div>
          <div className="md:col-span-2">
            <label htmlFor="wizard-address" className="block text-sm font-medium mb-1.5">
              {t('form.address')} *
            </label>
            <Input
              id="wizard-address"
              value={property.address}
              onChange={e => updateProperty({ address: e.target.value })}
              placeholder={t('form.addressPlaceholder')}
            />
          </div>
          <div>
            <label htmlFor="wizard-type" className="block text-sm font-medium mb-1.5">
              {t('form.propertyType')} *
            </label>
            <select
              id="wizard-type"
              value={property.propertyType}
              onChange={e => updateProperty({ propertyType: e.target.value })}
              className="w-full h-10 px-3 rounded-lg bg-surface border border-border text-text-primary"
            >
              {PROPERTY_TYPE_OPTIONS.map(type => (
                <option key={type} value={type}>
                  {t(`filters.${type.toLowerCase()}`) !== `filters.${type.toLowerCase()}`
                    ? t(`filters.${type.toLowerCase()}`)
                    : type}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label htmlFor="wizard-currency" className="block text-sm font-medium mb-1.5">
              {t('form.currency')} *
            </label>
            <CurrencySelector
              value={property.currency}
              onValueChange={v => updateProperty({ currency: v })}
              className="w-full"
            />
          </div>
          <div>
            <label htmlFor="wizard-price" className="block text-sm font-medium mb-1.5">
              {t('form.purchasePrice')} *
            </label>
            <NumberInput
              id="wizard-price"
              value={property.purchasePrice}
              onChange={v => updateProperty({ purchasePrice: v, currentValue: v })}
              placeholder="0.00"
              min="0.01"
            />
          </div>
          <div>
            <label htmlFor="wizard-date" className="block text-sm font-medium mb-1.5">
              {t('form.purchaseDate')} *
            </label>
            <DateInput
              id="wizard-date"
              value={property.purchaseDate}
              onChange={v => updateProperty({ purchaseDate: v ?? today })}
              max={today}
            />
          </div>
          <div>
            <label htmlFor="wizard-value" className="block text-sm font-medium mb-1.5">
              {t('form.currentValue')} *
            </label>
            <NumberInput
              id="wizard-value"
              value={property.currentValue}
              onChange={v => updateProperty({ currentValue: v })}
              placeholder="0.00"
              min="0.01"
            />
          </div>
        </div>
      )}

      {/* Step 2: funding */}
      {step === 1 && (
        <div className="space-y-4">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label htmlFor="wizard-source" className="block text-sm font-medium mb-1.5">
                {t('wizard.fundingSource')} *
              </label>
              <select
                id="wizard-source"
                value={funding.source}
                onChange={e => updateFunding({ source: e.target.value as FundingSource })}
                className="w-full h-10 px-3 rounded-lg bg-surface border border-border text-text-primary"
              >
                <option value="new">{t('wizard.funding.new')}</option>
                <option value="existing">{t('wizard.funding.existing')}</option>
                <option value="none">{t('wizard.funding.none')}</option>
              </select>
            </div>
            <div>
              <label htmlFor="wizard-route" className="block text-sm font-medium mb-1.5">
                {t('wizard.disbursementRoute')}
              </label>
              <select
                id="wizard-route"
                value={funding.route}
                onChange={e => updateFunding({ route: e.target.value as DisbursementRoute })}
                disabled={funding.source === 'none'}
                className="w-full h-10 px-3 rounded-lg bg-surface border border-border text-text-primary disabled:opacity-50"
              >
                <option value="direct">{t('wizard.route.direct')}</option>
                <option value="account">{t('wizard.route.account')}</option>
              </select>
            </div>
          </div>

          {funding.source !== 'none' && (
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              {funding.source === 'new' && (
                <div>
                  <label
                    htmlFor="wizard-mortgage-name"
                    className="block text-sm font-medium mb-1.5"
                  >
                    {t('wizard.mortgageName')} *
                  </label>
                  <Input
                    id="wizard-mortgage-name"
                    value={funding.mortgageName}
                    onChange={e => updateFunding({ mortgageName: e.target.value })}
                  />
                </div>
              )}
              <div>
                <label htmlFor="wizard-loan" className="block text-sm font-medium mb-1.5">
                  {funding.source === 'new'
                    ? `${t('wizard.loanAmount')} *`
                    : t('wizard.disbursedAmount')}
                </label>
                <NumberInput
                  id="wizard-loan"
                  value={funding.loanAmount}
                  onChange={v => updateFunding({ loanAmount: v })}
                  placeholder="0.00"
                  min="0.01"
                />
              </div>
              {funding.source === 'new' && (
                <div>
                  <label htmlFor="wizard-rate" className="block text-sm font-medium mb-1.5">
                    {t('wizard.interestRate')}
                  </label>
                  <NumberInput
                    id="wizard-rate"
                    value={funding.interestRate}
                    onChange={v => updateFunding({ interestRate: v })}
                    placeholder="3.5"
                    min="0"
                  />
                </div>
              )}
            </div>
          )}

          {funding.source === 'existing' && (
            <div>
              <label className="block text-sm font-medium mb-1.5">{t('wizard.existingMortgage')}</label>
              <LiabilitySelector
                value={funding.existingMortgageId}
                onValueChange={v => updateFunding({ existingMortgageId: v })}
                placeholder={t('form.selectMortgage')}
                liabilityFilter={l => l.type === 'MORTGAGE'}
              />
            </div>
          )}

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium mb-1.5">{t('wizard.downPaymentAccount')}</label>
              {/* The backend improvement guard rejects a down-payment account whose currency
                  differs from the property's — users simply pick a matching account here. */}
              <AccountSelector
                value={funding.downPaymentAccountId}
                onValueChange={v => updateFunding({ downPaymentAccountId: v })}
                placeholder={t('wizard.downPaymentAccount')}
              />
            </div>
            <div>
              <label htmlFor="wizard-down" className="block text-sm font-medium mb-1.5">
                {t('wizard.downPaymentAmount')}
              </label>
              <NumberInput
                id="wizard-down"
                value={funding.downPaymentAmount}
                onChange={v => updateFunding({ downPaymentAmount: v })}
                placeholder="0.00"
                min="0"
              />
            </div>
          </div>

          {accountRouteMissingAccount && (
            <p role="alert" className="text-sm text-error">
              {t('wizard.routeAccountRequired')}
            </p>
          )}
        </div>
      )}

      {/* Step 3: review */}
      {step === 2 && (
        <dl className="grid grid-cols-2 gap-x-6 gap-y-2 text-sm border border-border rounded-lg p-4 bg-surface">
          <dt className="text-text-secondary">{t('form.propertyName')}</dt>
          <dd className="text-right text-text-primary">{property.name}</dd>
          <dt className="text-text-secondary">{t('form.purchasePrice')}</dt>
          <dd className="text-right text-text-primary font-mono">
            {property.purchasePrice} {property.currency}
          </dd>
          <dt className="text-text-secondary">{t('wizard.fundingSource')}</dt>
          <dd className="text-right text-text-primary">
            {t(`wizard.funding.${funding.source}`)}
            {funding.source !== 'none' && funding.loanAmount
              ? ` — ${funding.loanAmount} ${property.currency}`
              : ''}
          </dd>
          <dt className="text-text-secondary">{t('wizard.downPaymentAmount')}</dt>
          <dd className="text-right text-text-primary font-mono">
            {funding.downPaymentAmount || '0'} {property.currency}
          </dd>
        </dl>
      )}

      {error && (
        <p role="alert" className="text-sm text-error">
          {error}
        </p>
      )}

      {/* Actions */}
      <div className="flex justify-between pt-4 border-t border-border">
        <Button
          variant="ghost"
          type="button"
          onClick={step === 0 ? onClose : () => setStep(s => s - 1)}
          disabled={isSubmitting}
        >
          {step === 0 ? t('form.cancel') : t('wizard.back')}
        </Button>
        {step < 2 ? (
          <Button
            variant="primary"
            type="button"
            disabled={!stepValid}
            onClick={() => setStep(s => s + 1)}
          >
            {t('wizard.next')}
          </Button>
        ) : (
          <Button variant="primary" type="button" isLoading={isSubmitting} onClick={handleConfirm}>
            <Home className="h-4 w-4 mr-1.5" />
            {t('wizard.confirm')}
          </Button>
        )}
      </div>
    </div>
  );
}
