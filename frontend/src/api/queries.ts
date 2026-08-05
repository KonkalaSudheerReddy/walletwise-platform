import {
  keepPreviousData,
  useMutation,
  useQuery,
  useQueryClient,
  type QueryClient
} from '@tanstack/react-query';
import { apiRequest } from './client';
import type {
  AdjustmentPayload,
  AdminUser,
  AuditFilters,
  AuditLog,
  Budget,
  BudgetPayload,
  Category,
  CategoryType,
  LedgerPayload,
  MonthlyAnalytics,
  Notification,
  PageResponse,
  TransactionFilters,
  Transfer,
  TransferPayload,
  User,
  Wallet,
  WalletPayload,
  WalletTransaction,
  YearMonth
} from './types';

export const keys = {
  wallets: ['wallets'] as const,
  wallet: (id: string) => ['wallets', id] as const,
  categories: (type?: CategoryType) => ['categories', type ?? 'all'] as const,
  transactions: (filters: TransactionFilters) => ['transactions', filters] as const,
  transfers: (page: number) => ['transfers', page] as const,
  budgets: (month: YearMonth) => ['budgets', month] as const,
  analytics: (month: YearMonth) => ['analytics', month] as const,
  notifications: (page: number, unreadOnly: boolean) =>
    ['notifications', page, unreadOnly] as const,
  unreadCount: ['notifications', 'unread-count'] as const,
  adminUsers: (page: number, search: string) => ['admin', 'users', page, search] as const,
  auditLogs: (filters: AuditFilters) => ['admin', 'audit-logs', filters] as const
};

function queryString(values: Record<string, string | number | boolean | undefined>) {
  const params = new URLSearchParams();
  Object.entries(values).forEach(([key, value]) => {
    if (value !== undefined && value !== '') params.set(key, String(value));
  });
  const serialized = params.toString();
  return serialized ? `?${serialized}` : '';
}

function invalidateFinancialData(client: QueryClient) {
  void client.invalidateQueries({ queryKey: keys.wallets });
  void client.invalidateQueries({ queryKey: ['transactions'] });
  void client.invalidateQueries({ queryKey: ['transfers'] });
  void client.invalidateQueries({ queryKey: ['analytics'] });
  void client.invalidateQueries({ queryKey: ['budgets'] });
  void client.invalidateQueries({ queryKey: keys.unreadCount });
}

export function useWallets(includeArchived = false) {
  return useQuery({
    queryKey: [...keys.wallets, { includeArchived }],
    queryFn: () =>
      apiRequest<Wallet[]>(`/api/v1/wallets${queryString({ includeArchived })}`)
  });
}

export function useWallet(id: string | undefined) {
  return useQuery({
    queryKey: keys.wallet(id ?? ''),
    queryFn: async () =>
      (
        await apiRequest<{ wallet: Wallet; recentActivity: WalletTransaction[] }>(
          `/api/v1/wallets/${id}`
        )
      ).wallet,
    enabled: Boolean(id)
  });
}

export function useCreateWallet() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: (payload: WalletPayload) =>
      apiRequest<Wallet>('/api/v1/wallets', { method: 'POST', body: payload }),
    onSuccess: () => invalidateFinancialData(client)
  });
}

export function useSetWalletArchived() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: ({ id, archived }: { id: string; archived: boolean }) =>
      apiRequest<Wallet>(`/api/v1/wallets/${id}/${archived ? 'archive' : 'restore'}`, {
        method: 'POST'
      }),
    onSuccess: (_, variables) => {
      invalidateFinancialData(client);
      void client.invalidateQueries({ queryKey: keys.wallet(variables.id) });
    }
  });
}

export function useCategories(type?: CategoryType) {
  return useQuery({
    queryKey: keys.categories(type),
    queryFn: () =>
      apiRequest<Category[]>(`/api/v1/categories${queryString({ type })}`),
    staleTime: 5 * 60_000
  });
}

export function useTransactions(filters: TransactionFilters) {
  const [sort, direction = 'desc'] = filters.sort.split(',');
  const { search, sort: ignoredSort, ...parameters } = filters;
  void ignoredSort;
  return useQuery({
    queryKey: keys.transactions(filters),
    queryFn: () =>
      apiRequest<PageResponse<WalletTransaction>>(
        `/api/v1/transactions${queryString({
          ...parameters,
          description: search,
          sort,
          direction
        })}`
      ),
    placeholderData: keepPreviousData
  });
}

export function useCreateLedgerEntry(kind: 'income' | 'expense') {
  const client = useQueryClient();
  return useMutation({
    mutationFn: (payload: LedgerPayload) =>
      apiRequest<WalletTransaction>(`/api/v1/transactions/${kind}`, {
        method: 'POST',
        body: payload
      }),
    onSuccess: () => invalidateFinancialData(client)
  });
}

export function useCreateAdjustment() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: (payload: AdjustmentPayload) =>
      apiRequest<WalletTransaction>('/api/v1/transactions/adjustment', {
        method: 'POST',
        body: payload
      }),
    onSuccess: () => invalidateFinancialData(client)
  });
}

export function useTransfers(page = 0) {
  return useQuery({
    queryKey: keys.transfers(page),
    queryFn: () =>
      apiRequest<PageResponse<Transfer>>(`/api/v1/transfers${queryString({ page, size: 20 })}`),
    placeholderData: keepPreviousData
  });
}

export function useCreateTransfer() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: ({ payload, idempotencyKey }: { payload: TransferPayload; idempotencyKey: string }) =>
      apiRequest<Transfer>('/api/v1/transfers', {
        method: 'POST',
        headers: { 'Idempotency-Key': idempotencyKey },
        body: payload
      }),
    onSuccess: () => invalidateFinancialData(client)
  });
}

export function useBudgets(month: YearMonth) {
  return useQuery({
    queryKey: keys.budgets(month),
    queryFn: () => apiRequest<Budget[]>(`/api/v1/budgets${queryString({ month })}`)
  });
}

export function useCreateBudget() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: (payload: BudgetPayload) =>
      apiRequest<Budget>('/api/v1/budgets', { method: 'POST', body: payload }),
    onSuccess: () => {
      void client.invalidateQueries({ queryKey: ['budgets'] });
      void client.invalidateQueries({ queryKey: ['analytics'] });
    }
  });
}

export function useUpdateBudget() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: Partial<BudgetPayload> }) =>
      apiRequest<Budget>(`/api/v1/budgets/${id}`, { method: 'PATCH', body: payload }),
    onSuccess: () => {
      void client.invalidateQueries({ queryKey: ['budgets'] });
      void client.invalidateQueries({ queryKey: ['analytics'] });
    }
  });
}

export function useDeleteBudget() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => apiRequest<void>(`/api/v1/budgets/${id}`, { method: 'DELETE' }),
    onSuccess: () => {
      void client.invalidateQueries({ queryKey: ['budgets'] });
      void client.invalidateQueries({ queryKey: ['analytics'] });
    }
  });
}

export function useMonthlyAnalytics(month: YearMonth) {
  return useQuery({
    queryKey: keys.analytics(month),
    queryFn: () =>
      apiRequest<MonthlyAnalytics>(`/api/v1/analytics/monthly${queryString({ month })}`),
    staleTime: 60_000
  });
}

export function useNotifications(page: number, unreadOnly: boolean) {
  return useQuery({
    queryKey: keys.notifications(page, unreadOnly),
    queryFn: () =>
      apiRequest<PageResponse<Notification>>(
        `/api/v1/notifications${queryString({ page, size: 20, unread: unreadOnly || undefined })}`
      ),
    placeholderData: keepPreviousData
  });
}

export function useUnreadCount(enabled = true) {
  return useQuery({
    queryKey: keys.unreadCount,
    queryFn: () => apiRequest<{ unreadCount: number }>('/api/v1/notifications/unread-count'),
    enabled,
    refetchInterval: () => (document.visibilityState === 'visible' ? 30_000 : false)
  });
}

export function useMarkNotificationRead() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: (id: string) =>
      apiRequest<Notification>(`/api/v1/notifications/${id}/read`, { method: 'PATCH' }),
    onSuccess: () => {
      void client.invalidateQueries({ queryKey: ['notifications'] });
    }
  });
}

export function useMarkAllNotificationsRead() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: () => apiRequest<void>('/api/v1/notifications/read-all', { method: 'PATCH' }),
    onSuccess: () => void client.invalidateQueries({ queryKey: ['notifications'] })
  });
}

export function useAdminUsers(page: number, search: string) {
  return useQuery({
    queryKey: keys.adminUsers(page, search),
    queryFn: () =>
      apiRequest<PageResponse<AdminUser>>(
        `/api/v1/admin/users${queryString({ page, size: 20, search })}`
      ),
    placeholderData: keepPreviousData
  });
}

export function useSetUserEnabled() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: ({ id, enabled }: { id: string; enabled: boolean }) =>
      apiRequest<User>(`/api/v1/admin/users/${id}/status`, {
        method: 'PATCH',
        body: { enabled }
      }),
    onSuccess: () => void client.invalidateQueries({ queryKey: ['admin', 'users'] })
  });
}

export function useAuditLogs(filters: AuditFilters) {
  return useQuery({
    queryKey: keys.auditLogs(filters),
    queryFn: () =>
      apiRequest<PageResponse<AuditLog>>(
        `/api/v1/admin/audit-logs${queryString({ ...filters })}`
      ),
    placeholderData: keepPreviousData
  });
}
