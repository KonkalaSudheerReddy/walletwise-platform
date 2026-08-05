import { ArrowDownRight, ArrowRight, ArrowUpRight, CreditCard, PiggyBank, SendHorizontal, WalletCards } from 'lucide-react';
import { Link } from 'react-router-dom';
import { useBudgets, useMonthlyAnalytics, useTransactions, useWallets } from '../../api/queries';
import type { TransactionFilters } from '../../api/types';
import { CategoryDonutChart, SpendingTrendChart } from '../../components/charts';
import { Badge, Card, EmptyState, ErrorState, LoadingState, Money, PageHeader, Progress } from '../../components/ui';
import { currentMonth, formatDate, titleCase } from '../../lib/format';

const recentFilters: TransactionFilters = {
  page: 0,
  size: 6,
  sort: 'occurredAt,desc'
};

export function DashboardPage() {
  const month = currentMonth();
  const analytics = useMonthlyAnalytics(month);
  const wallets = useWallets();
  const recent = useTransactions(recentFilters);
  const budgets = useBudgets(month);

  if (analytics.isPending || wallets.isPending || recent.isPending || budgets.isPending) {
    return <LoadingState label="Preparing your dashboard" />;
  }
  const firstError = analytics.error ?? wallets.error ?? recent.error ?? budgets.error;
  if (firstError) return <ErrorState error={firstError} title="Your dashboard is temporarily unavailable" />;
  if (!analytics.data) return <EmptyState title="Nothing to analyze yet" description="Create a wallet and record your first income to begin." />;

  const data = analytics.data;
  return (
    <div className="space-y-7">
      <PageHeader
        title="Dashboard"
        description={`A clear view of ${month} activity in ${data.currency}.`}
        actions={
          <>
            <Link to="/transactions?add=expense" className="inline-flex h-11 items-center gap-2 rounded-xl border border-slate-300 bg-white px-4 text-sm font-medium hover:bg-slate-50 dark:border-slate-700 dark:bg-slate-900">
              <ArrowDownRight className="h-4 w-4 text-rose-600" /> Add expense
            </Link>
            <Link to="/transfer" className="inline-flex h-11 items-center gap-2 rounded-xl bg-teal-700 px-4 text-sm font-medium text-white hover:bg-teal-800">
              <SendHorizontal className="h-4 w-4" /> Transfer
            </Link>
          </>
        }
      />

      <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4" aria-label="Monthly summary">
        {[
          { label: 'Closing balance', value: data.closingBalance, icon: WalletCards, tone: 'text-teal-700 bg-teal-50 dark:bg-teal-950' },
          { label: 'Income', value: data.totalIncome, icon: ArrowUpRight, tone: 'text-emerald-700 bg-emerald-50 dark:bg-emerald-950' },
          { label: 'Expenses', value: data.totalExpense, icon: ArrowDownRight, tone: 'text-rose-700 bg-rose-50 dark:bg-rose-950' },
          { label: 'Net cash flow', value: data.netCashFlow, icon: CreditCard, tone: 'text-sky-700 bg-sky-50 dark:bg-sky-950' }
        ].map(({ label, value, icon: Icon, tone }) => (
          <Card key={label} className="p-5">
            <div className="flex items-start justify-between gap-3">
              <div><p className="text-sm text-slate-500">{label}</p><Money className="mt-2 block text-2xl font-semibold tracking-tight" value={value} currency={data.currency} /></div>
              <span className={`grid h-10 w-10 place-items-center rounded-xl ${tone}`}><Icon className="h-5 w-5" /></span>
            </div>
          </Card>
        ))}
      </section>

      <section className="grid gap-5 xl:grid-cols-[1.5fr_1fr]">
        <Card className="min-w-0 p-5 sm:p-6">
          <div className="flex items-center justify-between"><div><h2 className="font-semibold">Spending trend</h2><p className="text-sm text-slate-500">Daily expenses this month</p></div><Badge tone="info">{data.transactionCount} entries</Badge></div>
          {data.spendingTrend.length ? <SpendingTrendChart data={data.spendingTrend} currency={data.currency} /> : <EmptyState title="No expense trend yet" description="Expense activity will appear here." />}
        </Card>
        <Card className="min-w-0 p-5 sm:p-6">
          <div><h2 className="font-semibold">Expense mix</h2><p className="text-sm text-slate-500">Where this month went</p></div>
          {data.expenseByCategory.length ? <CategoryDonutChart data={data.expenseByCategory} currency={data.currency} /> : <EmptyState title="No categories yet" description="Add an expense to build a category breakdown." />}
        </Card>
      </section>

      <section className="grid gap-5 xl:grid-cols-3">
        <Card className="p-5 sm:p-6 xl:col-span-2">
          <div className="flex items-center justify-between"><div><h2 className="font-semibold">Recent activity</h2><p className="text-sm text-slate-500">Your latest ledger entries</p></div><Link className="inline-flex items-center gap-1 text-sm font-medium text-teal-700 hover:underline" to="/transactions">View all <ArrowRight className="h-4 w-4" /></Link></div>
          {!recent.data?.content.length ? <div className="mt-5"><EmptyState title="No activity yet" description="Income, expenses, and transfers will appear here." /></div> : <ul className="mt-4 divide-y divide-slate-100 dark:divide-slate-800">{recent.data.content.map((transaction) => <li key={transaction.id} className="flex items-center justify-between gap-4 py-3"><div className="min-w-0"><p className="truncate text-sm font-medium">{transaction.description || transaction.categoryName || titleCase(transaction.type)}</p><p className="text-xs text-slate-500">{transaction.walletName} / {formatDate(transaction.occurredAt)}</p></div><Money className={transaction.direction === 'CREDIT' ? 'font-semibold text-emerald-700' : 'font-semibold text-rose-700'} value={transaction.amount} currency={transaction.currency} /></li>)}</ul>}
        </Card>

        <Card className="p-5 sm:p-6">
          <div className="flex items-center justify-between"><div><h2 className="font-semibold">Budget progress</h2><p className="text-sm text-slate-500">Current month</p></div><PiggyBank className="h-5 w-5 text-teal-700" /></div>
          {!budgets.data?.length ? <div className="mt-5"><EmptyState title="No budgets yet" description="Create category limits to stay on track." action={<Link className="text-sm font-medium text-teal-700 hover:underline" to="/budgets">Create a budget</Link>} /></div> : <div className="mt-5 space-y-5">{budgets.data.slice(0, 4).map((budget) => <div key={budget.id}><div className="mb-2 flex justify-between gap-3 text-sm"><span className="font-medium">{budget.categoryName}</span><span className="text-slate-500"><Money value={budget.spentAmount} currency={data.currency} /> / <Money value={budget.limitAmount} currency={data.currency} /></span></div><Progress value={budget.utilizationPercent} label={`${budget.categoryName} budget used`} /></div>)}</div>}
        </Card>
      </section>

      <Card className="p-5 sm:p-6">
        <div className="flex items-center justify-between"><div><h2 className="font-semibold">Your wallets</h2><p className="text-sm text-slate-500">Balances stay separate by currency.</p></div><Link className="text-sm font-medium text-teal-700 hover:underline" to="/wallets">Manage wallets</Link></div>
        {!wallets.data?.length ? <div className="mt-5"><EmptyState title="Create your first wallet" description="Opening balances are recorded as immutable ledger entries." /></div> : <div className="mt-5 grid gap-3 sm:grid-cols-2 xl:grid-cols-3">{wallets.data.slice(0, 3).map((wallet) => <Link key={wallet.id} to={`/wallets/${wallet.id}`} className="rounded-2xl border border-slate-200 p-4 transition hover:border-teal-300 hover:bg-teal-50/40 dark:border-slate-800 dark:hover:border-teal-800"><div className="flex items-center justify-between"><p className="font-medium">{wallet.name}</p><Badge>{titleCase(wallet.type)}</Badge></div><Money className="mt-3 block text-xl font-semibold" value={wallet.balance} currency={wallet.currency} /></Link>)}</div>}
      </Card>
    </div>
  );
}

