import { zodResolver } from '@hookform/resolvers/zod';
import { Pencil, Plus, Trash2 } from 'lucide-react';
import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { toast } from 'sonner';
import { z } from 'zod';
import { useBudgets, useCategories, useCreateBudget, useDeleteBudget, useUpdateBudget } from '../../api/queries';
import type { Budget, BudgetPayload } from '../../api/types';
import { Badge, Button, Card, EmptyState, ErrorState, LoadingState, Modal, Money, PageHeader, Progress, SelectField, TextField } from '../../components/ui';
import { currentMonth } from '../../lib/format';

const budgetSchema = z.object({
  categoryId: z.string().min(1, 'Choose an expense category.'),
  month: z.string().regex(/^\d{4}-(0[1-9]|1[0-2])$/, 'Choose a valid month.'),
  limitAmount: z.string().regex(/^\d{1,15}(\.\d{1,2})?$/, 'Enter an amount with up to 2 decimals.').refine((value) => Number(value) > 0, 'Limit must be greater than zero.'),
  alertThresholdPercent: z.number().int().min(1).max(99)
});

function BudgetForm({ month, budget, onComplete }: { month: string; budget?: Budget; onComplete: () => void }) {
  const categories = useCategories('EXPENSE');
  const create = useCreateBudget();
  const update = useUpdateBudget();
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors }
  } = useForm<BudgetPayload>({
    resolver: zodResolver(budgetSchema),
    defaultValues: {
      categoryId: budget?.categoryId ?? '',
      month: budget?.month ?? month,
      limitAmount: budget ? String(budget.limitAmount) : '',
      alertThresholdPercent: budget?.alertThresholdPercent ?? 80
    }
  });

  useEffect(() => {
    reset({ categoryId: budget?.categoryId ?? '', month: budget?.month ?? month, limitAmount: budget ? String(budget.limitAmount) : '', alertThresholdPercent: budget?.alertThresholdPercent ?? 80 });
  }, [budget, month, reset]);

  const submit = (payload: BudgetPayload) => {
    const options = {
      onSuccess: () => { toast.success(budget ? 'Budget updated' : 'Budget created'); onComplete(); },
      onError: (error: Error) => toast.error(error.message)
    };
    if (budget) update.mutate({ id: budget.id, payload: { limitAmount: payload.limitAmount, alertThresholdPercent: payload.alertThresholdPercent } }, options);
    else create.mutate(payload, options);
  };

  return (
    <form className="space-y-4" onSubmit={(event) => void handleSubmit(submit)(event)} noValidate>
      <SelectField label="Expense category" disabled={Boolean(budget)} error={errors.categoryId?.message} {...register('categoryId')}><option value="">Choose a category</option>{categories.data?.map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}</SelectField>
      <TextField label="Budget month" type="month" disabled={Boolean(budget)} error={errors.month?.message} {...register('month')} />
      <TextField label="Limit amount" inputMode="decimal" placeholder="500.00" error={errors.limitAmount?.message} {...register('limitAmount')} />
      <TextField label="Alert threshold (%)" type="number" min={1} max={99} error={errors.alertThresholdPercent?.message} {...register('alertThresholdPercent', { valueAsNumber: true })} />
      <div className="flex justify-end gap-2 pt-2"><Button type="button" variant="ghost" onClick={onComplete}>Cancel</Button><Button type="submit" disabled={create.isPending || update.isPending}>{create.isPending || update.isPending ? 'Saving...' : budget ? 'Save changes' : 'Create budget'}</Button></div>
    </form>
  );
}

export function BudgetsPage() {
  const [month, setMonth] = useState(currentMonth());
  const [editing, setEditing] = useState<Budget | 'new'>();
  const [deleting, setDeleting] = useState<Budget>();
  const budgets = useBudgets(month);
  const remove = useDeleteBudget();

  const confirmDelete = () => {
    if (!deleting) return;
    remove.mutate(deleting.id, {
      onSuccess: () => { toast.success('Budget deleted'); setDeleting(undefined); },
      onError: (error) => toast.error(error.message)
    });
  };

  return (
    <div className="space-y-7">
      <PageHeader title="Budgets" description="Set monthly category limits and see progress before spending becomes a surprise." actions={<><TextField label="Budget month" type="month" value={month} onChange={(event) => setMonth(event.target.value)} /><Button onClick={() => setEditing('new')}><Plus className="h-4 w-4" /> Create budget</Button></>} />
      {budgets.isPending ? <LoadingState label="Loading budgets" /> : budgets.error ? <ErrorState error={budgets.error} /> : !budgets.data?.length ? <EmptyState title={`No budgets for ${month}`} description="Add an expense category limit to begin tracking progress." action={<Button onClick={() => setEditing('new')}><Plus className="h-4 w-4" /> Add a budget</Button>} /> : <section className="grid gap-4 md:grid-cols-2 xl:grid-cols-3" aria-label="Monthly budgets">{budgets.data.map((budget) => {
        const tone = budget.utilizationPercent > 100 ? 'danger' : budget.utilizationPercent >= budget.alertThresholdPercent ? 'warning' : 'success';
        return <Card key={budget.id} className="p-5"><div className="flex items-start justify-between gap-3"><div><p className="font-semibold">{budget.categoryName}</p><p className="mt-1 text-xs text-slate-500">Alert at {budget.alertThresholdPercent}%</p></div><Badge tone={tone}>{budget.utilizationPercent > 100 ? 'Over budget' : budget.utilizationPercent >= budget.alertThresholdPercent ? 'Approaching limit' : 'On track'}</Badge></div><div className="mt-6"><Progress value={budget.utilizationPercent} label={`${budget.categoryName} utilization`} /></div><div className="mt-5 grid grid-cols-2 gap-3 rounded-xl bg-slate-50 p-4 text-sm dark:bg-slate-950"><div><p className="text-xs text-slate-500">Spent</p><Money className="mt-1 block font-semibold" value={budget.spentAmount} currency={budget.currency} /></div><div><p className="text-xs text-slate-500">Remaining</p><Money className="mt-1 block font-semibold" value={budget.remainingAmount} currency={budget.currency} /></div></div><div className="mt-4 flex justify-end gap-1"><Button variant="ghost" size="sm" onClick={() => setEditing(budget)}><Pencil className="h-4 w-4" /> Edit</Button><Button variant="ghost" size="sm" className="text-rose-700" onClick={() => setDeleting(budget)}><Trash2 className="h-4 w-4" /> Delete</Button></div></Card>;
      })}</section>}
      <Modal open={Boolean(editing)} onClose={() => setEditing(undefined)} title={editing === 'new' ? 'Create budget' : 'Edit budget'} description="Budget usage updates after relevant expense activity."><BudgetForm month={month} budget={editing === 'new' ? undefined : editing} onComplete={() => setEditing(undefined)} /></Modal>
      <Modal open={Boolean(deleting)} onClose={() => setDeleting(undefined)} title="Delete this budget?" description="Only future or unused budgets can be safely deleted."><div className="flex justify-end gap-2"><Button variant="ghost" onClick={() => setDeleting(undefined)}>Cancel</Button><Button variant="danger" disabled={remove.isPending} onClick={confirmDelete}>{remove.isPending ? 'Deleting...' : 'Delete budget'}</Button></div></Modal>
    </div>
  );
}
