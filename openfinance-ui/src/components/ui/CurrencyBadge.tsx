/**
 * CurrencyBadge — DEPRECATED
 *
 * This component has been superseded by the tooltip functionality built directly
 * into {@link ConvertedAmount}. The badge/icon approach was removed in favour of
 * a pure CSS hover/focus tooltip on the amount element itself.
 *
 * Reference: REQ-9.1, REQ-9.2
 *
 * @deprecated Do not use. Kept as an empty stub to avoid breaking any remaining
 *             import statements during the migration period. All usages should be
 *             removed; the stub export will be deleted in a future cleanup.
 */

/**
 * Props interface retained for type-compatibility with any lingering imports.
 * @deprecated See module-level deprecation notice.
 */
export interface CurrencyBadgeProps {
  fromCurrency: string;
  toCurrency: string;
  exchangeRate?: number;
  nativeAmount?: number;
  variant?: 'subtle' | 'prominent' | 'inline';
  className?: string;
}

/**
 * No-op stub that renders nothing.
 *
 * Requirement REQ-9.1: No badge or icon must be rendered next to amounts.
 * @deprecated See module-level deprecation notice.
 */
export function CurrencyBadge(_props: CurrencyBadgeProps): null {
  return null;
}
