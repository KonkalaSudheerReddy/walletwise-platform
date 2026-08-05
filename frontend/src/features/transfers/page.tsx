import { zodResolver } from '@hookform/resolvers/zod';
import { ArrowRight, CheckCircle2, RefreshCw, ShieldCheck } from 'lucide-react';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { toast } from 'sonner';
import { z } from 'zod';
import { useCreateTransfer, useTransfers, useWallets } from '../../api/queries';
import type { Transfer, TransferPayload } from '../../api/types';
import { Badge, Button, Card, EmptyState, ErrorState, LoadingState, Money, PageHeader, SelectField, TextField } from '../../components/ui';
import { formatDateTime, titleCase } from '../../lib/format';

const transferSchema = z
  .object({
    sourceWalletId: z.string().min(1, 'Choose a source wallet.'),
    destinationWalletId: z.string().min(1, 'Choose a destination wallet.'),
    amount: z.string().regex(/^\d{1,15}(\.\d{1,2})?$/, 'Enter an amount with up to 2 decimals.').refine((value) => Number(value) > 0, 'Amount must be greater than zero.'),
    note: z.string().trim().max(160, 'Keep the note under 160 characters.').optional()
  })
  .refine((value) => value.sourceWalletId !== value.destinationWalletId, { path: ['destinationWalletId'], message: 'Choose a different wallet.' });

type TransferFormValues = z.infer<typeof transferSchema>;
interface PendingTransfer { payload: TransferPayload; idempotencyKey: string }

export function TransferPage() {
  const wallets = useWallets();
  const history = useTransfers();
  const transfer = useCreateTransfer();
  const [pending, setPending] = useState<PendingTransfer>();
  const [result, setResult] = useState<Transfer>();
  const {
    register,
    handleSubmit,
    watch,
    reset,
    formState: { errors }
  } = useForm<TransferFormValues>({ resolver: zodResolver(transferSchema), defaultValues: { sourceWalletId: '', destinationWalletId: '', amount: '', note: '' } });

  const sourceWalletId = watch('sourceWalletId');
  const source = wallets.data?.find((wallet) => wallet.id === sourceWalletId);
  const destinationOptions = wallets.data?.filter((wallet) => !wallet.archived && wallet.id !== sourceWalletId && (!source || wallet.currency === source.currency));

  const prepare = (payload: TransferPayload) => {
    setResult(undefined);
    setPending({ payload, idempotencyKey: crypto.randomUUID() });
  };

  const submitPending = () => {
    if (!pending) return;
    transfer.mutate(pending, {
      onSuccess: (response) => {
        setResult(response);
        setPending(undefined);
        reset();
        toast.success('Transfer completed safely');
      },
      onError: (error) => toast.error(error.message, { description: 'Use "Retry safely" to send the exact same request key.' })
    });
  };

  if (wallets.isPending) return <LoadingState label="Loading wallets" />;
  if (wallets.error) return <ErrorState error={wallets.error} />;

  return (
    <div className="space-y-7">
      <PageHeader title="Transfer" description="Move virtual funds atomically between wallets in the same currency." />
      <div className="grid gap-5 xl:grid-cols-[1fr_420px]">
        <Card className="p-5 sm:p-7">
          <div className="mb-6 flex items-start gap-3 rounded-2xl bg-teal-50 p-4 text-teal-900 dark:bg-teal-950/60 dark:text-teal-100"><ShieldCheck className="mt-0.5 h-5 w-5 shrink-0" /><div><p className="text-sm font-semibold">Duplicate-safe by design</p><p className="mt-1 text-xs leading-5 opacity-80">WalletWise generates one idempotency key per confirmed request. An uncertain request can be safely retried without moving money twice.</p></div></div>
          {result ? <div className="grid min-h-80 place-items-center text-center"><div><span className="mx-auto grid h-16 w-16 place-items-center rounded-full bg-emerald-100 text-emerald-700 dark:bg-emerald-950 dark:text-emerald-200"><CheckCircle2 className="h-8 w-8" /></span><h2 className="mt-5 text-2xl font-semibold">Transfer complete</h2><Money className="mt-2 block text-xl font-semibold" value={result.amount} currency={result.currency} /><p className="mt-2 text-sm text-slate-500">Reference {result.id.slice(0, 8)}</p><Button className="mt-6" variant="secondary" onClick={() => setResult(undefined)}>Make another transfer</Button></div></div> : pending ? <section aria-labelledby="review-title"><h2 id="review-title" className="text-xl font-semibold">Review transfer</h2><p className="mt-1 text-sm text-slate-500">Confirm every detail before submitting.</p><div className="my-7 flex items-center gap-4"><div className="flex-1 rounded-2xl border border-slate-200 p-4 dark:border-slate-800"><p className="text-xs text-slate-500">From</p><p className="mt-1 font-semibold">{wallets.data?.find((wallet) => wallet.id === pending.payload.sourceWalletId)?.name}</p></div><ArrowRight className="h-5 w-5 shrink-0 text-teal-700" /><div className="flex-1 rounded-2xl border border-slate-200 p-4 dark:border-slate-800"><p className="text-xs text-slate-500">To</p><p className="mt-1 font-semibold">{wallets.data?.find((wallet) => wallet.id === pending.payload.destinationWalletId)?.name}</p></div></div><div className="rounded-2xl bg-slate-50 p-5 dark:bg-slate-950"><div className="flex justify-between gap-4"><span className="text-sm text-slate-500">Amount</span><Money className="text-lg font-semibold" value={pending.payload.amount} currency={source?.currency ?? 'USD'} /></div>{pending.payload.note && <div className="mt-4 flex justify-between gap-4 border-t border-slate-200 pt-4 text-sm dark:border-slate-800"><span className="text-slate-500">Note</span><span className="text-right">{pending.payload.note}</span></div>}<p className="mt-4 border-t border-slate-200 pt-4 text-xs text-slate-500 dark:border-slate-800">Request key ...{pending.idempotencyKey.slice(-8)}</p></div>{transfer.error && <div className="mt-5 rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-900" role="alert"><p className="font-semibold">The outcome needs confirmation</p><p className="mt-1">{transfer.error.message}</p></div>}<div className="mt-6 flex flex-wrap justify-end gap-2"><Button variant="ghost" disabled={transfer.isPending} onClick={() => { setPending(undefined); transfer.reset(); }}>Back to edit</Button><Button disabled={transfer.isPending} onClick={submitPending}>{transfer.isPending ? <><RefreshCw className="h-4 w-4 animate-spin" /> Sending...</> : transfer.isError ? <><RefreshCw className="h-4 w-4" /> Retry safely</> : 'Confirm transfer'}</Button></div></section> : <form className="space-y-5" onSubmit={(event) => void handleSubmit(prepare)(event)} noValidate><SelectField label="Source wallet" error={errors.sourceWalletId?.message} {...register('sourceWalletId')}><option value="">Choose a wallet</option>{wallets.data?.filter((wallet) => !wallet.archived).map((wallet) => <option key={wallet.id} value={wallet.id}>{wallet.name} / {wallet.currency}</option>)}</SelectField>{source && <div className="-mt-2 flex justify-between rounded-xl bg-slate-50 px-4 py-3 text-sm dark:bg-slate-950"><span className="text-slate-500">Available balance</span><Money value={source.balance} currency={source.currency} className="font-semibold" /></div>}<SelectField label="Destination wallet" error={errors.destinationWalletId?.message} hint={source ? `Only ${source.currency} wallets are available.` : 'Choose the source first.'} disabled={!source} {...register('destinationWalletId')}><option value="">Choose a wallet</option>{destinationOptions?.map((wallet) => <option key={wallet.id} value={wallet.id}>{wallet.name} / {wallet.currency}</option>)}</SelectField><TextField label="Amount" inputMode="decimal" placeholder="0.00" error={errors.amount?.message} {...register('amount')} /><TextField label="Note (optional)" placeholder="Move to savings" error={errors.note?.message} {...register('note')} /><Button className="w-full" size="lg" type="submit" disabled={(wallets.data?.length ?? 0) < 2}>Review transfer <ArrowRight className="h-4 w-4" /></Button>{(wallets.data?.length ?? 0) < 2 && <p className="text-center text-xs text-amber-700">Create at least two same-currency wallets to transfer.</p>}</form>}
        </Card>

        <Card className="self-start overflow-hidden"><div className="border-b border-slate-200 p-5 dark:border-slate-800"><h2 className="font-semibold">Recent transfers</h2><p className="text-sm text-slate-500">Latest transfer requests</p></div>{history.isPending ? <div className="p-5"><LoadingState /></div> : history.error ? <div className="p-5"><ErrorState error={history.error} /></div> : !history.data?.content.length ? <div className="p-5"><EmptyState title="No transfers yet" description="Completed transfers will appear here." /></div> : <ul className="divide-y divide-slate-100 dark:divide-slate-800">{history.data.content.slice(0, 8).map((item) => <li key={item.id} className="p-4"><div className="flex items-center justify-between gap-3"><Money value={item.amount} currency={item.currency} className="font-semibold" /><Badge tone={item.status === 'COMPLETED' ? 'success' : item.status === 'FAILED' ? 'danger' : 'warning'}>{titleCase(item.status)}</Badge></div><p className="mt-2 text-xs text-slate-500">{formatDateTime(item.createdAt)} / {item.note || 'No note'}</p></li>)}</ul>}</Card>
      </div>
    </div>
  );
}

