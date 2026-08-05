import { HttpResponse, http } from 'msw';

const user = {
  id: 'user-1',
  displayName: 'Demo User',
  email: 'demo@walletwise.app',
  role: 'USER',
  preferredCurrency: 'USD',
  enabled: true
};

export const authResponse = {
  accessToken: 'test-access-token',
  tokenType: 'Bearer',
  expiresAt: '2030-01-01T00:00:00Z',
  user
};

export const handlers = [
  http.post('http://localhost/api/v1/auth/refresh', () =>
    HttpResponse.json({ title: 'Unauthorized', status: 401, detail: 'No session' }, { status: 401 })
  ),
  http.get('http://localhost/api/v1/wallets', () =>
    HttpResponse.json([
      {
        id: 'wallet-1',
        name: 'Everyday',
        type: 'BANK',
        currency: 'USD',
        balance: 1250.5,
        archived: false,
        createdAt: '2026-01-01T00:00:00Z',
        updatedAt: '2026-08-01T00:00:00Z'
      }
    ])
  ),
  http.get('http://localhost/api/v1/transactions', () =>
    HttpResponse.json({
      content: [
        {
          id: 'transaction-1',
          walletId: 'wallet-1',
          walletName: 'Everyday',
          currency: 'USD',
          type: 'EXPENSE',
          direction: 'DEBIT',
          amount: 42.25,
          categoryId: 'category-1',
          categoryName: 'Groceries',
          description: 'Weekly groceries',
          occurredAt: '2026-08-03T10:00:00Z',
          balanceAfter: 1250.5,
          createdAt: '2026-08-03T10:00:00Z'
        }
      ],
      page: 0,
      size: 6,
      totalElements: 1,
      totalPages: 1,
      first: true,
      last: true
    })
  ),
  http.get('http://localhost/api/v1/budgets', () =>
    HttpResponse.json([
      {
        id: 'budget-1',
        categoryId: 'category-1',
        categoryName: 'Groceries',
        month: '2026-08',
        currency: 'USD',
        limitAmount: 500,
        alertThresholdPercent: 80,
        spentAmount: 350,
        remainingAmount: 150,
        utilizationPercent: 70,
        createdAt: '2026-08-01T00:00:00Z',
        updatedAt: '2026-08-01T00:00:00Z'
      }
    ])
  ),
  http.get('http://localhost/api/v1/analytics/monthly', () =>
    HttpResponse.json({
      month: '2026-08',
      currency: 'USD',
      totalIncome: 5200,
      totalExpense: 2740.25,
      netCashFlow: 2459.75,
      openingBalance: 1000,
      closingBalance: 3459.75,
      transactionCount: 12,
      previousMonthComparisonPercent: -4.5,
      expenseByCategory: [
        { categoryId: 'category-1', categoryName: 'Groceries', amount: 350, percentage: 12.7 }
      ],
      incomeByCategory: [
        { categoryId: 'income-1', categoryName: 'Salary', amount: 5200, percentage: 100 }
      ],
      spendingTrend: [{ date: '2026-08-03', amount: 42.25 }],
      budgetUtilization: [
        {
          id: 'budget-1',
          categoryId: 'category-1',
          categoryName: 'Groceries',
          month: '2026-08',
          currency: 'USD',
          limitAmount: 500,
          alertThresholdPercent: 80,
          spentAmount: 350,
          remainingAmount: 150,
          utilizationPercent: 70,
          createdAt: '2026-08-01T00:00:00Z',
          updatedAt: '2026-08-01T00:00:00Z'
        }
      ]
    })
  ),
  http.get('http://localhost/api/v1/notifications/unread-count', () =>
    HttpResponse.json({ unreadCount: 2 })
  )
];
