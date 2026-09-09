import { useEffect, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { useAssets } from '@/hooks/useAssets';
import { useLiabilities } from '@/hooks/useLiabilities';
import apiClient from '@/services/apiClient';
import { ConvertedAmount } from '@/components/ui/ConvertedAmount';
import { Button } from '@/components/ui/Button';
import { sum, subtract } from '@/utils/money';

interface FinancingLink {
  assetId: number;
  liabilityId?: number;
  relationship: 'FINANCING' | 'COLLATERAL';
  allocationPercentage: number;
  assetName?: string;
  liabilityName?: string;
  allocatedBalance?: number;
  assetCurrency?: string;
}

export function AssetFinancingSection({
  assetId,
  liabilityId,
}: {
  assetId?: number;
  liabilityId?: number;
}) {
  const { t } = useTranslation('assets');
  const client = useQueryClient();
  const assets = useAssets();
  const liabilities = useLiabilities();
  const [chosenLoan, setChosenLoan] = useState<number | undefined>(liabilityId);
  const [draft, setDraft] = useState<FinancingLink[] | null>(null);
  useEffect(() => {
    setChosenLoan(liabilityId);
    setDraft(null);
  }, [liabilityId, assetId]);
  const linked = useQuery<FinancingLink[]>({
    queryKey: ['assetFinancing', chosenLoan ? 'liability' : 'asset', chosenLoan ?? assetId],
    queryFn: async () =>
      (
        await apiClient.get<FinancingLink[]>(
          chosenLoan ? `/liabilities/${chosenLoan}/asset-links` : `/assets/${assetId}/liabilities`
        )
      ).data,
    enabled: chosenLoan != null || assetId != null,
  });
  const save = useMutation({
    mutationFn: async (rows: FinancingLink[]) =>
      apiClient.put(`/liabilities/${chosenLoan}/asset-links`, rows),
    onSuccess: async () => {
      setDraft(null);
      for (const key of [
        'assetFinancing',
        'liabilities',
        'realEstate',
        'assets',
        'dashboard',
        'networth',
      ]) {
        await client.invalidateQueries({ queryKey: [key] });
      }
    },
  });
  const update = (index: number, patch: Partial<FinancingLink>) =>
    setDraft(rows => rows!.map((row, i) => (i === index ? { ...row, ...patch } : row)));
  const allocated = sum(
    (draft ?? [])
      .filter(row => row.relationship === 'FINANCING')
      .map(row => row.allocationPercentage)
  );
  const asset = assets.data?.find(item => item.id === assetId);
  const allocatedDebt = sum((linked.data ?? []).map(row => row.allocatedBalance ?? 0));
  return (
    <section className="rounded-lg border border-border bg-surface p-4 space-y-3">
      <h3 className="font-semibold">{t('financing.title')}</h3>
      <p className="text-sm text-text-secondary">{t('financing.hint')}</p>
      {asset && asset.type !== 'REAL_ESTATE' && !chosenLoan && linked.data && (
        <dl className="grid grid-cols-3 gap-3 text-sm">
          <div>
            <dt>{t('financing.value')}</dt>
            <dd>
              <ConvertedAmount amount={asset.totalValue} currency={asset.currency} inline />
            </dd>
          </div>
          <div>
            <dt>{t('financing.debt')}</dt>
            <dd>
              <ConvertedAmount amount={allocatedDebt} currency={asset.currency} inline />
            </dd>
          </div>
          <div>
            <dt>{t('financing.equity')}</dt>
            <dd>
              <ConvertedAmount
                amount={subtract(asset.totalValue, allocatedDebt)}
                currency={asset.currency}
                inline
              />
            </dd>
          </div>
        </dl>
      )}
      {!liabilityId && (
        <label className="block text-sm">
          {t('financing.loan')}
          <select
            value={chosenLoan ?? ''}
            onChange={e => {
              setChosenLoan(Number(e.target.value) || undefined);
              setDraft(null);
            }}
            className="w-full rounded border border-border bg-surface p-2"
          >
            <option value="">{t('financing.allLoans')}</option>
            {liabilities.data?.map(loan => (
              <option key={loan.id} value={loan.id}>
                {loan.name}
              </option>
            ))}
          </select>
        </label>
      )}
      {linked.isLoading ? (
        <p role="status">{t('financing.loading')}</p>
      ) : linked.error ? (
        <p role="alert">{t('financing.error')}</p>
      ) : draft ? (
        <>
          {draft.map((row, index) => (
            <div className="flex flex-wrap gap-2 items-end" key={index}>
              <label className="flex-1 text-sm">
                {t('financing.asset')}
                <select
                  value={row.assetId}
                  onChange={e => update(index, { assetId: Number(e.target.value) })}
                  className="w-full rounded border border-border bg-surface p-2"
                >
                  {assets.data?.map(asset => (
                    <option key={asset.id} value={asset.id}>
                      {asset.name}
                    </option>
                  ))}
                </select>
              </label>
              <label className="text-sm">
                {t('financing.relationship')}
                <select
                  value={row.relationship}
                  onChange={e =>
                    update(index, {
                      relationship: e.target.value as FinancingLink['relationship'],
                      allocationPercentage: e.target.value === 'FINANCING' ? 100 : 0,
                    })
                  }
                  className="block rounded border border-border bg-surface p-2"
                >
                  <option value="FINANCING">{t('financing.FINANCING')}</option>
                  <option value="COLLATERAL">{t('financing.COLLATERAL')}</option>
                </select>
              </label>
              {row.relationship === 'FINANCING' && (
                <label className="text-sm">
                  {t('financing.share')}
                  <input
                    type="number"
                    min="0.01"
                    max="100"
                    step="0.01"
                    value={row.allocationPercentage}
                    onChange={e => update(index, { allocationPercentage: Number(e.target.value) })}
                    className="block w-24 rounded border border-border bg-surface p-2"
                  />
                </label>
              )}
              <Button variant="ghost" onClick={() => setDraft(draft.filter((_, i) => i !== index))}>
                {t('financing.remove')}
              </Button>
            </div>
          ))}
          <p className={allocated > 100 ? 'text-error' : 'text-text-secondary'}>
            {t('financing.allocated', { percentage: allocated })}
          </p>
          <div className="flex gap-2 flex-wrap">
            <Button
              variant="secondary"
              disabled={!assets.data?.length}
              onClick={() =>
                setDraft([
                  ...draft,
                  {
                    assetId: assetId ?? assets.data![0].id,
                    relationship: 'FINANCING',
                    allocationPercentage: Math.max(100 - allocated, 0),
                  },
                ])
              }
            >
              {t('financing.add')}
            </Button>
            <Button
              disabled={
                save.isPending ||
                allocated > 100 ||
                draft.some(row => row.relationship === 'FINANCING' && row.allocationPercentage <= 0)
              }
              onClick={() => save.mutate(draft)}
            >
              {t('financing.save')}
            </Button>
            <Button variant="ghost" onClick={() => setDraft(null)}>
              {t('financing.cancel')}
            </Button>
          </div>
          {save.error && (
            <p role="alert" className="text-error">
              {t('financing.error')}
            </p>
          )}
        </>
      ) : (
        <>
          {linked.data?.length ? (
            <ul className="space-y-2">
              {linked.data.map((row, index) => (
                <li key={index} className="flex gap-3 justify-between text-sm">
                  <span>
                    {row.assetName} · {row.liabilityName} · {t(`financing.${row.relationship}`)}
                    {row.relationship === 'FINANCING' ? ` (${row.allocationPercentage}%)` : ''}
                  </span>
                  {row.relationship === 'FINANCING' && (
                    <ConvertedAmount
                      amount={row.allocatedBalance ?? 0}
                      currency={row.assetCurrency ?? 'EUR'}
                      inline
                    />
                  )}
                </li>
              ))}
            </ul>
          ) : (
            <p className="text-sm text-text-secondary">{t('financing.empty')}</p>
          )}
          {chosenLoan && (
            <Button
              variant="secondary"
              onClick={() => setDraft((linked.data ?? []).map(row => ({ ...row })))}
            >
              {t('financing.edit')}
            </Button>
          )}
        </>
      )}
    </section>
  );
}
