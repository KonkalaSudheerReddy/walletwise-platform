export type UUID = string;
export type IsoDateTime = string;
export type YearMonth = string;
export type DecimalValue = string | number;

export type Role = 'USER' | 'ADMIN';
export type WalletType = 'CASH' | 'BANK' | 'SAVINGS' | 'CREDIT' | 'OTHER';
export type CategoryType = 'INCOME' | 'EXPENSE';
export type TransactionType =
  | 'OPENING_BALANCE'
  | 'INCOME'
  | 'EXPENSE'
  | 'TRANSFER_IN'
  | 'TRANSFER_OUT'
  | 'ADJUSTMENT';
export type TransferStatus = 'PENDING' | 'COMPLETED' | 'FAILED';
export type NotificationType = 'BUDGET_APPROACHING' | 'BUDGET_REACHED' | 'BUDGET_EXCEEDED';

export interface User {
  id: UUID;
  displayName: string;
  email: string;
  role: Role;
  preferredCurrency: string;
  enabled: boolean;
}

export interface AuthResponse {
  accessToken: string;
  tokenType: 'Bearer';
  expiresAt: IsoDateTime;
  user: User;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface ProblemDetails {
  type?: string;
  title: string;
  status: number;
  detail: string;
  instance?: string;
  timestamp?: IsoDateTime;
  correlationId?: string;
  validationErrors?: Record<string, string | string[]>;
}

export interface Wallet {
  id: UUID;
  name: string;
  type: WalletType;
  currency: string;
  balance: DecimalValue;
  archived: boolean;
  createdAt: IsoDateTime;
  updatedAt: IsoDateTime;
}

export interface Category {
  id: UUID;
  name: string;
  type: CategoryType;
}

export interface WalletTransaction {
  id: UUID;
  walletId: UUID;
  walletName: string;
  currency: string;
  type: TransactionType;
  direction: 'CREDIT' | 'DEBIT';
  amount: DecimalValue;
  categoryId?: UUID | null;
  categoryName?: string | null;
  description?: string | null;
  occurredAt: IsoDateTime;
  transferId?: UUID | null;
  balanceAfter: DecimalValue;
  createdAt: IsoDateTime;
}

export interface Transfer {
  id: UUID;
  sourceWalletId: UUID;
  destinationWalletId: UUID;
  amount: DecimalValue;
  currency: string;
  status: TransferStatus;
  note?: string | null;
  createdAt: IsoDateTime;
  completedAt?: IsoDateTime | null;
}

export interface Budget {
  id: UUID;
  categoryId: UUID;
  categoryName: string;
  month: YearMonth;
  currency: string;
  limitAmount: DecimalValue;
  alertThresholdPercent: number;
  spentAmount: DecimalValue;
  remainingAmount: DecimalValue;
  utilizationPercent: number;
  createdAt: IsoDateTime;
  updatedAt: IsoDateTime;
}

export interface Notification {
  id: UUID;
  type: NotificationType;
  title: string;
  message: string;
  read: boolean;
  relatedResourceId?: UUID | null;
  createdAt: IsoDateTime;
}

export interface CategoryAggregate {
  categoryId?: UUID | null;
  categoryName: string;
  amount: DecimalValue;
  percentage?: number;
}

export interface TrendPoint {
  date: string;
  amount: DecimalValue;
}

export interface MonthlyAnalytics {
  month: YearMonth;
  currency: string;
  totalIncome: DecimalValue;
  totalExpense: DecimalValue;
  netCashFlow: DecimalValue;
  openingBalance: DecimalValue;
  closingBalance: DecimalValue;
  transactionCount: number;
  previousMonthComparisonPercent: number | null;
  expenseByCategory: CategoryAggregate[];
  incomeByCategory: CategoryAggregate[];
  spendingTrend: TrendPoint[];
  budgetUtilization: Budget[];
}

export interface AdminUser extends User {
  createdAt: IsoDateTime;
  updatedAt: IsoDateTime;
}

export interface AuditLog {
  id: UUID;
  actorUserId?: UUID | null;
  action: string;
  resourceType: string;
  resourceId?: UUID | null;
  outcome: 'SUCCESS' | 'FAILURE';
  correlationId?: string | null;
  occurredAt: IsoDateTime;
  clientIp?: string | null;
  userAgent?: string | null;
  metadataJson?: string | null;
}

export interface TransactionFilters {
  walletId?: string;
  type?: TransactionType | '';
  categoryId?: string;
  startDate?: string;
  endDate?: string;
  minAmount?: string;
  maxAmount?: string;
  search?: string;
  page: number;
  size: number;
  sort: string;
}

export interface AuditFilters {
  actorId?: string;
  action?: string;
  resourceType?: string;
  outcome?: string;
  startDate?: string;
  endDate?: string;
  page: number;
  size: number;
}

export interface WalletPayload {
  name: string;
  type: WalletType;
  currency: string;
  openingBalance: string;
}

export interface LedgerPayload {
  walletId: UUID;
  amount: string;
  categoryId: UUID;
  description?: string;
  occurredAt: IsoDateTime;
}

export interface AdjustmentPayload {
  walletId: UUID;
  amount: string;
  direction: 'CREDIT' | 'DEBIT';
  categoryId?: UUID;
  description: string;
  occurredAt: IsoDateTime;
}

export interface TransferPayload {
  sourceWalletId: UUID;
  destinationWalletId: UUID;
  amount: string;
  note?: string;
}

export interface BudgetPayload {
  categoryId: UUID;
  month: YearMonth;
  limitAmount: string;
  alertThresholdPercent: number;
}
