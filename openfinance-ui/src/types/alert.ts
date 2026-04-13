/**
 * Types for budget alerts
 * 
 * Backend mapping:
 * - BudgetAlertRequest.java
 * - BudgetAlertResponse.java
 */

export interface BudgetAlert {
  id: string;
  budgetId: number;
  budgetName: string;
  categoryName: string;
  threshold: number;
  isEnabled: boolean;
  lastTriggered: string | null;
  isRead: boolean;
  currentSpentPercentage: number | null;
  message: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateAlertRequest {
  threshold: number;
  isEnabled?: boolean;
}

export interface UpdateAlertRequest {
  threshold?: number;
  isEnabled?: boolean;
}

export type AlertSeverity = 'warning' | 'critical' | 'exceeded';

export function getAlertSeverity(threshold: number, spentPercentage: number | null): AlertSeverity {
  if (spentPercentage === null) return 'warning';
  
  if (threshold >= 100 && spentPercentage >= 100) {
    return 'exceeded';
  } else if (threshold >= 90) {
    return 'critical';
  }
  return 'warning';
}

export function getAlertColor(severity: AlertSeverity): string {
  switch (severity) {
    case 'exceeded':
      return '#ef4444'; // red-500
    case 'critical':
      return '#f59e0b'; // amber-500
    case 'warning':
      return '#eab308'; // yellow-500
  }
}
