import { zodResolver } from '@hookform/resolvers/zod';
import {
  createColumnHelper,
  flexRender,
  getCoreRowModel,
  useReactTable
} from '@tanstack/react-table';
import { ArrowDownRight, ArrowUpRight, FilterX, Plus, RefreshCcw, Search } from 'lucide-react';
import { useEffect, useMemo, useState } from 'react';
import { useForm } from 'react-hook-form';
import { useSearchParams } from 'react-router-dom';
import { toast } from 'sonner';
import { z } from 'zod';
import {
  useCategories,
  useCreateAdjustment,
  useCreateLedgerEntry,
  useTransactions,
  useWallets
} from '../../api/queries';
import type {
  AdjustmentPayload,
  LedgerPayload,
  TransactionFilters,
  TransactionType,
  WalletTransaction
} from '../../api/types';
import {
  Badge,
  Button,
  Card,
  EmptyState,
  ErrorState,
  LoadingState,
  Modal,
  Money,
  PageHeader,
  Pagination,
  SelectField,
  TextField
} from '../../components/ui';
import { currentLocalDateTime, formatDateTime, titleCase, toIsoInstant } from '../../lib/format';

const ledgerSchema = z.object({
  walletId: z.string().min(1, 'Choose a wallet.'),
  amount: z
    .string()
    .regex(/^\d{1,15}(\.\d{1,2})?$/, 'Enter a positive amount with up to 2 decimals.')
    .refine((value) => Number(value) > 0, 'Amount must be greater than zero.'),
  categoryId: z.string().min(1, 'Choose a category.'),
  description: z.string().trim().max(160, 'Keep the note under 160 characters.').optional(),
  occurredAt: z.string().min(1, 'Choose when this occurred.')
});

type LedgerFormValues = z.infer<typeof ledgerSchema>;

function LedgerDialog({
  kind,
  open,
  onClose
}: {
  kind: 'income' | 'expense';
  open: boolean;
  onClose: () => void;
}) {
  const wallets = useWallets();
  const categories = useCategories(kind === 'income' ? 'INCOME' : 'EXPENSE');
  const mutation = useCreateLedgerEntry(kind);
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors }
  } = useForm<LedgerFormValues>({
    resolver: zodResolver(ledgerSchema),
    defaultValues: {
      walletId: '',
      amount: '',
      categoryId: '',
      description: '',
      occurredAt: currentLocalDateTime()
    }
  });

  const close = () => {
    reset();
    onClose();
  };
  const submit = (values: LedgerFormValues) => {
    const payload: LedgerPayload = { ...values, occurredAt: toIsoInstant(values.occurredAt) };
    mutation.mutate(payload, {
      onSuccess: () => {
        toast.success(kind === 'income' ? 'Income recorded' : 'Expense recorded');
        close();
      },
      onError: (error) => toast.error(error.message)
    });
  };

  return (
    <Modal
      open={open}
      onClose={close}
      title={kind === 'income' ? 'Add income' : 'Add expense'}
      description="Ledger entries cannot be deleted through the public API."
    >
      <form className="space-y-4" onSubmit={(event) => void handleSubmit(submit)(event)} noValidate>
        <SelectField label="Wallet" error={errors.walletId?.message} {...register('walletId')}>
          <option value="">Choose a wallet</option>
          {wallets.data
            ?.filter((wallet) => !wallet.archived)
            .map((wallet) => (
              <option key={wallet.id} value={wallet.id}>
                {wallet.name} ({wallet.currency})
              </option>
            ))}
        </SelectField>
        <TextField
          label="Amount"
          inputMode="decimal"
          placeholder="0.00"
          error={errors.amount?.message}
          {...register('amount')}
        />
        <SelectField
          label="Category"
          error={errors.categoryId?.message}
          {...register('categoryId')}
        >
          <option value="">Choose a category</option>
          {categories.data?.map((category) => (
            <option key={category.id} value={category.id}>
              {category.name}
            </option>
          ))}
        </SelectField>
        <TextField
          label="Description"
          placeholder={kind === 'income' ? 'Monthly salary' : 'Weekly groceries'}
          error={errors.description?.message}
          {...register('description')}
        />
        <TextField
          label="Occurred at"
          type="datetime-local"
          error={errors.occurredAt?.message}
          {...register('occurredAt')}
        />
        <div className="flex justify-end gap-2 pt-2">
          <Button type="button" variant="ghost" onClick={close}>
            Cancel
          </Button>
          <Button type="submit" disabled={mutation.isPending}>
            {mutation.isPending ? 'Saving...' : `Record ${kind}`}
          </Button>
        </div>
      </form>
    </Modal>
  );
}

const adjustmentSchema = z.object({
  walletId: z.string().min(1, 'Choose a wallet.'),
  amount: z
    .string()
    .regex(/^\d{1,15}(\.\d{1,2})?$/, 'Enter a positive amount with up to 2 decimals.')
    .refine((value) => Number(value) > 0, 'Amount must be greater than zero.'),
  direction: z.enum(['CREDIT', 'DEBIT']),
  categoryId: z.string().optional(),
  description: z
    .string()
    .trim()
    .min(3, 'Explain why this correction is needed.')
    .max(500, 'Keep the reason under 500 characters.'),
  occurredAt: z.string().min(1, 'Choose when this occurred.')
});

type AdjustmentFormValues = z.infer<typeof adjustmentSchema>;

function AdjustmentDialog({ open, onClose }: { open: boolean; onClose: () => void }) {
  const wallets = useWallets();
  const categories = useCategories();
  const mutation = useCreateAdjustment();
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors }
  } = useForm<AdjustmentFormValues>({
    resolver: zodResolver(adjustmentSchema),
    defaultValues: {
      walletId: '',
      amount: '',
      direction: 'CREDIT',
      categoryId: '',
      description: '',
      occurredAt: currentLocalDateTime()
    }
  });

  const close = () => {
    reset();
    onClose();
  };
  const submit = (values: AdjustmentFormValues) => {
    const payload: AdjustmentPayload = {
      ...values,
      categoryId: values.categoryId || undefined,
      occurredAt: toIsoInstant(values.occurredAt)
    };
    mutation.mutate(payload, {
      onSuccess: () => {
        toast.success('Adjustment recorded');
        close();
      },
      onError: (error) => toast.error(error.message)
    });
  };

  return (
    <Modal
      open={open}
      onClose={close}
      title="Record an adjustment"
      description="Use corrections only to reconcile a known balance difference. The immutable ledger and audit trail retain the reason."
    >
      <form className="space-y-4" onSubmit={(event) => void handleSubmit(submit)(event)} noValidate>
        <SelectField label="Wallet" error={errors.walletId?.message} {...register('walletId')}>
          <option value="">Choose a wallet</option>
          {wallets.data
            ?.filter((wallet) => !wallet.archived)
            .map((wallet) => (
              <option key={wallet.id} value={wallet.id}>
                {wallet.name} ({wallet.currency})
              </option>
            ))}
        </SelectField>
        <TextField
          label="Amount"
          inputMode="decimal"
          placeholder="0.00"
          error={errors.amount?.message}
          {...register('amount')}
        />
        <SelectField label="Direction" error={errors.direction?.message} {...register('direction')}>
          <option value="CREDIT">Credit - increase balance</option>
          <option value="DEBIT">Debit - decrease balance</option>
        </SelectField>
        <SelectField
          label="Category (optional)"
          error={errors.categoryId?.message}
          {...register('categoryId')}
        >
          <option value="">No category</option>
          {categories.data?.map((category) => (
            <option key={category.id} value={category.id}>
              {category.name} ({category.type.toLowerCase()})
            </option>
          ))}
        </SelectField>
        <TextField
          label="Correction reason"
          placeholder="Explain the reconciliation source"
          error={errors.description?.message}
          {...register('description')}
        />
        <TextField
          label="Occurred at"
          type="datetime-local"
          error={errors.occurredAt?.message}
          {...register('occurredAt')}
        />
        <div className="flex justify-end gap-2 pt-2">
          <Button type="button" variant="ghost" onClick={close}>
            Cancel
          </Button>
          <Button type="submit" disabled={mutation.isPending}>
            {mutation.isPending ? 'Saving...' : 'Record adjustment'}
          </Button>
        </div>
      </form>
    </Modal>
  );
}

function transactionTone(entry: WalletTransaction) {
  return entry.direction === 'CREDIT' ? 'success' : 'danger';
}

const columnHelper = createColumnHelper<WalletTransaction>();

export function TransactionsPage() {
  const [params, setParams] = useSearchParams();
  const [dialog, setDialog] = useState<'income' | 'expense' | 'adjustment' | null>(() => {
    const requested = params.get('add');
    return requested === 'income' || requested === 'expense' || requested === 'adjustment'
      ? requested
      : null;
  });
  const wallets = useWallets(true);
  const categories = useCategories();

  const filters: TransactionFilters = {
    walletId: params.get('walletId') ?? undefined,
    type: (params.get('type') as TransactionType | null) ?? '',
    categoryId: params.get('categoryId') ?? undefined,
    startDate: params.get('startDate') ?? undefined,
    endDate: params.get('endDate') ?? undefined,
    minAmount: params.get('minAmount') ?? undefined,
    maxAmount: params.get('maxAmount') ?? undefined,
    search: params.get('search') ?? undefined,
    page: Math.max(0, Number(params.get('page') ?? '0') || 0),
    size: 20,
    sort: params.get('sort') ?? 'occurredAt,desc'
  };
  const transactions = useTransactions(filters);

  useEffect(() => {
    if (params.has('add')) {
      const next = new URLSearchParams(params);
      next.delete('add');
      setParams(next, { replace: true });
    }
  }, [params, setParams]);

  const updateFilter = (name: string, value: string) => {
    const next = new URLSearchParams(params);
    if (value) next.set(name, value);
    else next.delete(name);
    if (name !== 'page') next.delete('page');
    setParams(next, { replace: true });
  };

  const columns = useMemo(
    () => [
      columnHelper.accessor('occurredAt', {
        header: 'Date',
        cell: (info) => formatDateTime(info.getValue())
      }),
      columnHelper.accessor('description', {
        header: 'Description',
        cell: (info) => (
          <div>
            <p className="font-medium">
              {info.getValue() ||
                info.row.original.categoryName ||
                titleCase(info.row.original.type)}
            </p>
            <p className="text-xs text-slate-500">{info.row.original.walletName}</p>
          </div>
        )
      }),
      columnHelper.accessor('type', {
        header: 'Type',
        cell: (info) => <Badge>{titleCase(info.getValue())}</Badge>
      }),
      columnHelper.accessor('categoryName', {
        header: 'Category',
        cell: (info) => info.getValue() || '-'
      }),
      columnHelper.accessor('amount', {
        header: 'Amount',
        cell: (info) => (
          <Money
            value={info.getValue()}
            currency={info.row.original.currency}
            className={
              info.row.original.direction === 'CREDIT'
                ? 'font-semibold text-emerald-700'
                : 'font-semibold text-rose-700'
            }
          />
        )
      })
    ],
    []
  );
  const table = useReactTable({
    data: transactions.data?.content ?? [],
    columns,
    getCoreRowModel: getCoreRowModel(),
    manualPagination: true,
    pageCount: transactions.data?.totalPages ?? 0
  });

  return (
    <div className="space-y-7">
      <PageHeader
        title="Transactions"
        description="Search and filter your immutable wallet ledger."
        actions={
          <>
            <Button variant="ghost" onClick={() => setDialog('adjustment')}>
              <RefreshCcw className="h-4 w-4" /> Add adjustment
            </Button>
            <Button variant="secondary" onClick={() => setDialog('income')}>
              <ArrowUpRight className="h-4 w-4 text-emerald-600" /> Add income
            </Button>
            <Button onClick={() => setDialog('expense')}>
              <ArrowDownRight className="h-4 w-4" /> Add expense
            </Button>
          </>
        }
      />

      <Card className="p-4 sm:p-5">
        <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
          <div className="relative">
            <Search className="absolute left-3 top-3.5 h-4 w-4 text-slate-400" />
            <input
              aria-label="Search descriptions"
              value={filters.search ?? ''}
              onChange={(event) => updateFilter('search', event.target.value)}
              placeholder="Search descriptions"
              className="h-11 w-full rounded-xl border border-slate-300 bg-white pl-10 pr-3 text-sm dark:border-slate-700 dark:bg-slate-950"
            />
          </div>
          <select
            aria-label="Filter by wallet"
            value={filters.walletId ?? ''}
            onChange={(event) => updateFilter('walletId', event.target.value)}
            className="h-11 rounded-xl border border-slate-300 bg-white px-3 text-sm dark:border-slate-700 dark:bg-slate-950"
          >
            <option value="">All wallets</option>
            {wallets.data?.map((wallet) => (
              <option key={wallet.id} value={wallet.id}>
                {wallet.name}
                {wallet.archived ? ' (archived)' : ''}
              </option>
            ))}
          </select>
          <select
            aria-label="Filter by type"
            value={filters.type ?? ''}
            onChange={(event) => updateFilter('type', event.target.value)}
            className="h-11 rounded-xl border border-slate-300 bg-white px-3 text-sm dark:border-slate-700 dark:bg-slate-950"
          >
            <option value="">All types</option>
            {(
              [
                'OPENING_BALANCE',
                'INCOME',
                'EXPENSE',
                'TRANSFER_IN',
                'TRANSFER_OUT',
                'ADJUSTMENT'
              ] satisfies TransactionType[]
            ).map((type) => (
              <option key={type} value={type}>
                {titleCase(type)}
              </option>
            ))}
          </select>
          <select
            aria-label="Filter by category"
            value={filters.categoryId ?? ''}
            onChange={(event) => updateFilter('categoryId', event.target.value)}
            className="h-11 rounded-xl border border-slate-300 bg-white px-3 text-sm dark:border-slate-700 dark:bg-slate-950"
          >
            <option value="">All categories</option>
            {categories.data?.map((category) => (
              <option key={category.id} value={category.id}>
                {category.name}
              </option>
            ))}
          </select>
          <TextField
            label="From date"
            type="date"
            value={filters.startDate ?? ''}
            onChange={(event) => updateFilter('startDate', event.target.value)}
          />
          <TextField
            label="To date"
            type="date"
            value={filters.endDate ?? ''}
            onChange={(event) => updateFilter('endDate', event.target.value)}
          />
          <TextField
            label="Minimum amount"
            inputMode="decimal"
            value={filters.minAmount ?? ''}
            onChange={(event) => updateFilter('minAmount', event.target.value)}
          />
          <TextField
            label="Maximum amount"
            inputMode="decimal"
            value={filters.maxAmount ?? ''}
            onChange={(event) => updateFilter('maxAmount', event.target.value)}
          />
        </div>
        <div className="mt-4 flex flex-wrap items-center justify-between gap-3">
          <select
            aria-label="Sort transactions"
            value={filters.sort}
            onChange={(event) => updateFilter('sort', event.target.value)}
            className="h-9 rounded-lg border border-slate-300 bg-white px-3 text-sm dark:border-slate-700 dark:bg-slate-950"
          >
            <option value="occurredAt,desc">Newest first</option>
            <option value="occurredAt,asc">Oldest first</option>
            <option value="amount,desc">Highest amount</option>
            <option value="amount,asc">Lowest amount</option>
          </select>
          <Button variant="ghost" size="sm" onClick={() => setParams({}, { replace: true })}>
            <FilterX className="h-4 w-4" /> Clear filters
          </Button>
        </div>
      </Card>

      {transactions.isPending ? (
        <LoadingState label="Loading transactions" />
      ) : transactions.error ? (
        <ErrorState error={transactions.error} />
      ) : !transactions.data?.content.length ? (
        <EmptyState
          title="No matching activity"
          description="Try clearing filters or record a new ledger entry."
          action={
            <Button onClick={() => setDialog('expense')}>
              <Plus className="h-4 w-4" /> Add expense
            </Button>
          }
        />
      ) : (
        <>
          <Card className="hidden overflow-hidden md:block">
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead className="border-b border-slate-200 bg-slate-50 text-xs uppercase tracking-wide text-slate-500 dark:border-slate-800 dark:bg-slate-950">
                  <tr>
                    {table.getHeaderGroups()[0]?.headers.map((header) => (
                      <th key={header.id} className="px-5 py-3 font-semibold">
                        {flexRender(header.column.columnDef.header, header.getContext())}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
                  {table.getRowModel().rows.map((row) => (
                    <tr key={row.id} className="hover:bg-slate-50 dark:hover:bg-slate-800/50">
                      {row.getVisibleCells().map((cell) => (
                        <td key={cell.id} className="px-5 py-4">
                          {flexRender(cell.column.columnDef.cell, cell.getContext())}
                        </td>
                      ))}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </Card>
          <section className="grid gap-3 md:hidden" aria-label="Transaction list">
            {transactions.data.content.map((entry) => (
              <Card key={entry.id} className="p-4">
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <p className="font-medium">
                      {entry.description || entry.categoryName || titleCase(entry.type)}
                    </p>
                    <p className="mt-1 text-xs text-slate-500">
                      {entry.walletName} / {formatDateTime(entry.occurredAt)}
                    </p>
                  </div>
                  <Money
                    value={entry.amount}
                    currency={entry.currency}
                    className={
                      entry.direction === 'CREDIT'
                        ? 'font-semibold text-emerald-700'
                        : 'font-semibold text-rose-700'
                    }
                  />
                </div>
                <div className="mt-3">
                  <Badge tone={transactionTone(entry)}>{titleCase(entry.type)}</Badge>
                </div>
              </Card>
            ))}
          </section>
          <Pagination
            page={transactions.data.page}
            totalPages={transactions.data.totalPages}
            onPageChange={(page) => updateFilter('page', String(page))}
          />
        </>
      )}
      <LedgerDialog kind="income" open={dialog === 'income'} onClose={() => setDialog(null)} />
      <LedgerDialog kind="expense" open={dialog === 'expense'} onClose={() => setDialog(null)} />
      <AdjustmentDialog open={dialog === 'adjustment'} onClose={() => setDialog(null)} />
    </div>
  );
}
