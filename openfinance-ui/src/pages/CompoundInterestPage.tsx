import { useTranslation } from 'react-i18next';
import { PageHeader } from '@/components/layout/PageHeader';
import { useDocumentTitle } from '@/hooks/useDocumentTitle';
import { CompoundInterestCalculator } from '@/components/compound-interest/CompoundInterestCalculator';

export default function CompoundInterestPage() {
  const { t } = useTranslation('tools');

  useDocumentTitle(t('compoundInterest.title'));

  return (
    <div className="p-8">
      <PageHeader
        title={t('compoundInterest.title')}
        description={t('compoundInterest.description')}
      />
      <div className="mt-8">
        <CompoundInterestCalculator />
      </div>
    </div>
  );
}
