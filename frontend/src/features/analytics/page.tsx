import { ArrowDownRight, ArrowUpRight, BarChart3, CalendarRange } from 'lucide-react';
import { useState } from 'react';
import { useMonthlyAnalytics } from '../../api/queries';
import type { MonthlyAnalytics } from '../../api/types';
import {
  CategoryDonutChart,
  IncomeExpenseChart,
  SpendingTrendChart
} from '../../components/charts';
import {
  Badge,
  Card,
  EmptyState,
  ErrorState,
  LoadingState,
  Money,
  PageHeader,
  Progress,
  TextField
} from '../../components/ui';
import { currentMonth } from '../../lib/format';

export function AnalyticsPage() {
  const [month, setMonth] = useState(currentMonth());
  const analytics = useMonthlyAnalytics(month);

  return (
    <div className="space-y-7">
      <PageHeader
        title="Analytics"
        description="Monthly aggregates are calculated by the database and scoped to your account."
        actions={
          <TextField
            label="Analysis month"
            type="month"
            value={month}
            onChange={(event) => setMonth(event.target.value)}
          />
        }
      />
      {analytics.isPending ? (
        <LoadingState label="Calculating monthly analytics" />
      ) : analytics.error ? (
        <ErrorState error={analytics.error} />
      ) : !analytics.data ? (
        <EmptyState
          title="No analytics available"
          description="Record some wallet activity for this month."
        />
      ) : (
        <AnalyticsContent data={analytics.data} />
      )}
    </div>
  );
}

function AnalyticsContent({ data }: { data: MonthlyAnalytics }) {
  const comparison = data.previousMonthComparisonPercent;
  return (
    <>
      <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4" aria-label="Analytics summary">
        <Card className="p-5">
          <p className="text-sm text-slate-500">Income</p>
          <Money
            className="mt-2 block text-2xl font-semibold text-emerald-700"
            value={data.totalIncome}
            currency={data.currency}
          />
          <p className="mt-3 flex items-center gap-1 text-xs text-slate-500">
            <ArrowUpRight className="h-4 w-4" /> Money added this month
          </p>
        </Card>
        <Card className="p-5">
          <p className="text-sm text-slate-500">Expenses</p>
          <Money
            className="mt-2 block text-2xl font-semibold text-rose-700"
            value={data.totalExpense}
            currency={data.currency}
          />
          <p className="mt-3 flex items-center gap-1 text-xs text-slate-500">
            <ArrowDownRight className="h-4 w-4" /> Money spent this month
          </p>
        </Card>
        <Card className="p-5">
          <p className="text-sm text-slate-500">Net cash flow</p>
          <Money
            className="mt-2 block text-2xl font-semibold"
            value={data.netCashFlow}
            currency={data.currency}
          />
          <p className="mt-3 text-xs text-slate-500">Income minus expenses</p>
        </Card>
        <Card className="p-5">
          <p className="text-sm text-slate-500">Previous month</p>
          <div className="mt-2 flex items-center gap-2 text-2xl font-semibold">
            {comparison === null ? '-' : `${Math.abs(comparison).toFixed(1)}%`}{' '}
            {comparison !== null && (
              <Badge tone={comparison <= 0 ? 'success' : 'warning'}>
                {comparison <= 0 ? 'lower spend' : 'higher spend'}
              </Badge>
            )}
          </div>
          <p className="mt-3 text-xs text-slate-500">Expense comparison</p>
        </Card>
      </section>

      <section className="grid gap-5 xl:grid-cols-2">
        <Card className="min-w-0 p-5 sm:p-6">
          <div>
            <h2 className="font-semibold">Income and expenses</h2>
            <p className="text-sm text-slate-500">Side-by-side comparison</p>
          </div>
          <IncomeExpenseChart
            income={data.totalIncome}
            expense={data.totalExpense}
            currency={data.currency}
          />
        </Card>
        <Card className="min-w-0 p-5 sm:p-6">
          <div>
            <h2 className="font-semibold">Expense categories</h2>
            <p className="text-sm text-slate-500">Category share for the month</p>
          </div>
          {data.expenseByCategory.length ? (
            <CategoryDonutChart data={data.expenseByCategory} currency={data.currency} />
          ) : (
            <EmptyState
              title="No expense categories"
              description="No categorized expenses exist for this month."
            />
          )}
        </Card>
      </section>

      <Card className="min-w-0 p-5 sm:p-6">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="font-semibold">Spending over time</h2>
            <p className="text-sm text-slate-500">Daily expense totals</p>
          </div>
          <CalendarRange className="h-5 w-5 text-teal-700" />
        </div>
        {data.spendingTrend.length ? (
          <SpendingTrendChart data={data.spendingTrend} currency={data.currency} />
        ) : (
          <EmptyState
            title="No trend to display"
            description="Daily totals appear after expenses are recorded."
          />
        )}
      </Card>

      <section className="grid gap-5 xl:grid-cols-[1fr_1.25fr]">
        <Card className="p-5 sm:p-6">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="font-semibold">Balance movement</h2>
              <p className="text-sm text-slate-500">Opening to closing balance</p>
            </div>
            <BarChart3 className="h-5 w-5 text-teal-700" />
          </div>
          <div className="mt-7 flex items-center gap-4">
            <div className="flex-1 rounded-2xl bg-slate-50 p-4 dark:bg-slate-950">
              <p className="text-xs text-slate-500">Opening</p>
              <Money
                className="mt-2 block text-xl font-semibold"
                value={data.openingBalance}
                currency={data.currency}
              />
            </div>
            <span aria-hidden="true" className="text-slate-400">
              to
            </span>
            <div className="flex-1 rounded-2xl bg-teal-50 p-4 dark:bg-teal-950">
              <p className="text-xs text-slate-500">Closing</p>
              <Money
                className="mt-2 block text-xl font-semibold"
                value={data.closingBalance}
                currency={data.currency}
              />
            </div>
          </div>
          <p className="mt-5 text-sm text-slate-500">
            {data.transactionCount} ledger entries were included.
          </p>
        </Card>
        <Card className="p-5 sm:p-6">
          <div>
            <h2 className="font-semibold">Budget utilization</h2>
            <p className="text-sm text-slate-500">Category limits for this month</p>
          </div>
          {data.budgetUtilization.length ? (
            <div className="mt-6 space-y-5">
              {data.budgetUtilization.map((budget) => (
                <div key={budget.id}>
                  <div className="mb-2 flex justify-between gap-3 text-sm">
                    <span className="font-medium">{budget.categoryName}</span>
                    <span className="text-slate-500">
                      <Money value={budget.spentAmount} currency={budget.currency} /> of{' '}
                      <Money value={budget.limitAmount} currency={budget.currency} />
                    </span>
                  </div>
                  <Progress
                    value={budget.utilizationPercent}
                    label={`${budget.categoryName} budget utilization`}
                  />
                </div>
              ))}
            </div>
          ) : (
            <div className="mt-5">
              <EmptyState
                title="No budgets for this month"
                description="Create a budget to compare spending with your plan."
              />
            </div>
          )}
        </Card>
      </section>

      {data.incomeByCategory.length > 0 && (
        <Card className="overflow-hidden">
          <div className="border-b border-slate-200 p-5 dark:border-slate-800">
            <h2 className="font-semibold">Income sources</h2>
          </div>
          <div className="divide-y divide-slate-100 dark:divide-slate-800">
            {data.incomeByCategory.map((category) => (
              <div
                key={`${category.categoryId ?? 'none'}-${category.categoryName}`}
                className="flex items-center justify-between gap-4 px-5 py-4"
              >
                <span className="text-sm font-medium">{category.categoryName}</span>
                <Money
                  value={category.amount}
                  currency={data.currency}
                  className="font-semibold text-emerald-700"
                />
              </div>
            ))}
          </div>
        </Card>
      )}
    </>
  );
}
