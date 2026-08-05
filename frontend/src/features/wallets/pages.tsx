import { zodResolver } from '@hookform/resolvers/zod';
import { Archive, ArrowLeft, Plus, RotateCcw, WalletCards } from 'lucide-react';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { toast } from 'sonner';
import { z } from 'zod';
import {
  useCreateWallet,
  useSetWalletArchived,
  useTransactions,
  useWallet,
  useWallets
} from '../../api/queries';
import type { TransactionFilters, Wallet, WalletPayload, WalletType } from '../../api/types';
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
  SelectField,
  TextField
} from '../../components/ui';
import { formatDate, titleCase } from '../../lib/format';

const walletSchema = z.object({
  name: z.string().trim().min(2, 'Use at least 2 characters.').max(60),
  type: z.enum(['CASH', 'BANK', 'SAVINGS', 'CREDIT', 'OTHER']),
  currency: z.string().length(3, 'Choose a currency.'),
  openingBalance: z
    .string()
    .regex(/^\d{1,15}(\.\d{1,2})?$/, 'Enter a positive amount with up to 2 decimals.')
});

function WalletForm({ onComplete }: { onComplete: () => void }) {
  const createWallet = useCreateWallet();
  const {
    register,
    handleSubmit,
    formState: { errors }
  } = useForm<WalletPayload>({
    resolver: zodResolver(walletSchema),
    defaultValues: { name: '', type: 'BANK', currency: 'USD', openingBalance: '0.00' }
  });

  const submit = (payload: WalletPayload) => {
    createWallet.mutate(payload, {
      onSuccess: (wallet) => {
        toast.success(`${wallet.name} was created`);
        onComplete();
      },
      onError: (error) => toast.error(error.message)
    });
  };

  return (
    <form className="space-y-4" onSubmit={(event) => void handleSubmit(submit)(event)} noValidate>
      <TextField
        label="Wallet name"
        placeholder="Daily spending"
        error={errors.name?.message}
        {...register('name')}
      />
      <SelectField label="Wallet type" error={errors.type?.message} {...register('type')}>
        {(['CASH', 'BANK', 'SAVINGS', 'CREDIT', 'OTHER'] satisfies WalletType[]).map((type) => (
          <option key={type} value={type}>
            {titleCase(type)}
          </option>
        ))}
      </SelectField>
      <SelectField label="Currency" error={errors.currency?.message} {...register('currency')}>
        <option value="USD">USD - US Dollar</option>
        <option value="INR">INR - Indian Rupee</option>
        <option value="EUR">EUR - Euro</option>
        <option value="GBP">GBP - British Pound</option>
      </SelectField>
      <TextField
        label="Opening balance"
        inputMode="decimal"
        hint="Creates an opening-balance ledger entry."
        error={errors.openingBalance?.message}
        {...register('openingBalance')}
      />
      <div className="flex justify-end gap-2 pt-2">
        <Button type="button" variant="ghost" onClick={onComplete}>
          Cancel
        </Button>
        <Button type="submit" disabled={createWallet.isPending}>
          {createWallet.isPending ? 'Creating...' : 'Create wallet'}
        </Button>
      </div>
    </form>
  );
}

function WalletCard({ wallet }: { wallet: Wallet }) {
  return (
    <Link
      to={`/wallets/${wallet.id}`}
      className="group rounded-2xl border border-slate-200 bg-white p-5 shadow-sm transition hover:-translate-y-0.5 hover:border-teal-300 hover:shadow-md dark:border-slate-800 dark:bg-slate-900 dark:hover:border-teal-800"
    >
      <div className="flex items-start justify-between gap-3">
        <span className="grid h-11 w-11 place-items-center rounded-2xl bg-teal-50 text-teal-700 dark:bg-teal-950 dark:text-teal-200">
          <WalletCards className="h-5 w-5" />
        </span>
        <div className="flex gap-2">
          {wallet.archived && <Badge tone="warning">Archived</Badge>}
          <Badge>{titleCase(wallet.type)}</Badge>
        </div>
      </div>
      <p className="mt-5 font-medium text-slate-700 dark:text-slate-200">{wallet.name}</p>
      <Money
        value={wallet.balance}
        currency={wallet.currency}
        className="mt-1 block text-2xl font-semibold tracking-tight"
      />
      <p className="mt-4 text-xs text-slate-500">Updated {formatDate(wallet.updatedAt)}</p>
    </Link>
  );
}

export function WalletsPage() {
  const [showCreate, setShowCreate] = useState(false);
  const [includeArchived, setIncludeArchived] = useState(false);
  const wallets = useWallets(includeArchived);

  return (
    <div className="space-y-7">
      <PageHeader
        title="Wallets"
        description="Balances remain separate by currency; WalletWise does not perform foreign exchange."
        actions={
          <Button onClick={() => setShowCreate(true)}>
            <Plus className="h-4 w-4" /> Create wallet
          </Button>
        }
      />
      <label className="inline-flex items-center gap-2 text-sm text-slate-600 dark:text-slate-300">
        <input
          type="checkbox"
          className="h-4 w-4 rounded border-slate-300 accent-teal-700"
          checked={includeArchived}
          onChange={(event) => setIncludeArchived(event.target.checked)}
        />{' '}
        Show archived wallets
      </label>
      {wallets.isPending ? (
        <LoadingState label="Loading wallets" />
      ) : wallets.error ? (
        <ErrorState error={wallets.error} />
      ) : wallets.data?.length ? (
        <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3" aria-label="Wallet list">
          {wallets.data.map((wallet) => (
            <WalletCard key={wallet.id} wallet={wallet} />
          ))}
        </section>
      ) : (
        <EmptyState
          title="No wallets yet"
          description="Create a wallet to record an opening balance and begin tracking activity."
          action={
            <Button onClick={() => setShowCreate(true)}>
              <Plus className="h-4 w-4" /> Create first wallet
            </Button>
          }
        />
      )}
      <Modal
        open={showCreate}
        onClose={() => setShowCreate(false)}
        title="Create a wallet"
        description="Opening balances are written to the immutable ledger."
      >
        <WalletForm onComplete={() => setShowCreate(false)} />
      </Modal>
    </div>
  );
}

export function WalletDetailPage() {
  const { walletId } = useParams();
  const navigate = useNavigate();
  const wallet = useWallet(walletId);
  const archive = useSetWalletArchived();
  const [confirming, setConfirming] = useState(false);
  const filters: TransactionFilters = { walletId, page: 0, size: 10, sort: 'occurredAt,desc' };
  const transactions = useTransactions(filters);

  if (wallet.isPending) return <LoadingState label="Loading wallet" />;
  if (wallet.error) return <ErrorState error={wallet.error} title="Unable to load this wallet" />;
  if (!wallet.data)
    return (
      <EmptyState
        title="Wallet not found"
        description="It may have been removed or is not available to your account."
      />
    );
  const data = wallet.data;

  const setArchived = () => {
    archive.mutate(
      { id: data.id, archived: !data.archived },
      {
        onSuccess: () => {
          toast.success(data.archived ? 'Wallet restored' : 'Wallet archived');
          setConfirming(false);
        },
        onError: (error) => toast.error(error.message)
      }
    );
  };

  return (
    <div className="space-y-7">
      <button
        className="inline-flex items-center gap-2 text-sm font-medium text-slate-500 hover:text-slate-950 dark:hover:text-white"
        onClick={() => navigate(-1)}
      >
        <ArrowLeft className="h-4 w-4" /> Back to wallets
      </button>
      <PageHeader
        title={data.name}
        description={`${titleCase(data.type)} wallet / ${data.currency}`}
        actions={
          <Button
            variant={data.archived ? 'secondary' : 'danger'}
            onClick={() => setConfirming(true)}
          >
            {data.archived ? <RotateCcw className="h-4 w-4" /> : <Archive className="h-4 w-4" />}
            {data.archived ? 'Restore' : 'Archive'}
          </Button>
        }
      />
      <section className="grid gap-4 sm:grid-cols-3">
        <Card className="p-5 sm:col-span-2">
          <p className="text-sm text-slate-500">Current balance</p>
          <Money
            value={data.balance}
            currency={data.currency}
            className="mt-2 block text-4xl font-semibold tracking-tight"
          />
          <p className="mt-4 text-xs text-slate-500">Updated {formatDate(data.updatedAt)}</p>
        </Card>
        <Card className="p-5">
          <p className="text-sm text-slate-500">Status</p>
          <div className="mt-3">
            <Badge tone={data.archived ? 'warning' : 'success'}>
              {data.archived ? 'Archived' : 'Active'}
            </Badge>
          </div>
          <p className="mt-4 text-xs leading-5 text-slate-500">
            Archived wallets remain available for historical reporting.
          </p>
        </Card>
      </section>
      <Card className="overflow-hidden">
        <div className="flex items-center justify-between border-b border-slate-200 p-5 dark:border-slate-800">
          <div>
            <h2 className="font-semibold">Recent activity</h2>
            <p className="text-sm text-slate-500">Latest entries for this wallet</p>
          </div>
          <Link
            to={`/transactions?walletId=${data.id}`}
            className="text-sm font-medium text-teal-700 hover:underline"
          >
            View all
          </Link>
        </div>
        {transactions.isPending ? (
          <div className="p-5">
            <LoadingState />
          </div>
        ) : transactions.error ? (
          <div className="p-5">
            <ErrorState error={transactions.error} />
          </div>
        ) : !transactions.data?.content.length ? (
          <div className="p-5">
            <EmptyState title="No activity" description="This wallet has no ledger entries yet." />
          </div>
        ) : (
          <ul className="divide-y divide-slate-100 dark:divide-slate-800">
            {transactions.data.content.map((entry) => (
              <li key={entry.id} className="flex items-center justify-between gap-4 px-5 py-4">
                <div>
                  <p className="text-sm font-medium">
                    {entry.description || entry.categoryName || titleCase(entry.type)}
                  </p>
                  <p className="mt-1 text-xs text-slate-500">
                    {formatDate(entry.occurredAt)} / {titleCase(entry.type)}
                  </p>
                </div>
                <Money
                  value={entry.amount}
                  currency={data.currency}
                  className={
                    entry.direction === 'CREDIT'
                      ? 'font-semibold text-emerald-700'
                      : 'font-semibold text-rose-700'
                  }
                />
              </li>
            ))}
          </ul>
        )}
      </Card>
      <Modal
        open={confirming}
        onClose={() => setConfirming(false)}
        title={data.archived ? 'Restore this wallet?' : 'Archive this wallet?'}
        description={
          data.archived
            ? 'It will be available for new activity again.'
            : 'Its history remains visible, but it cannot receive new activity.'
        }
      >
        <div className="flex justify-end gap-2">
          <Button variant="ghost" onClick={() => setConfirming(false)}>
            Cancel
          </Button>
          <Button
            variant={data.archived ? 'primary' : 'danger'}
            disabled={archive.isPending}
            onClick={setArchived}
          >
            {archive.isPending ? 'Saving...' : data.archived ? 'Restore wallet' : 'Archive wallet'}
          </Button>
        </div>
      </Modal>
    </div>
  );
}
