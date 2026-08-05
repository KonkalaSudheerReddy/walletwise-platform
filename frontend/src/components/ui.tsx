import { AlertCircle, ChevronLeft, ChevronRight, LoaderCircle, X } from 'lucide-react';
import {
  useEffect,
  forwardRef,
  useId,
  useRef,
  type ButtonHTMLAttributes,
  type InputHTMLAttributes,
  type ReactNode,
  type SelectHTMLAttributes
} from 'react';
import type { ApiError } from '../api/client';
import type { DecimalValue } from '../api/types';
import { formatMoney } from '../lib/format';
import { cn } from '../lib/styles';

export function WalletWiseLogo({ compact = false }: { compact?: boolean }) {
  return (
    <span className="inline-flex items-center gap-2.5 font-semibold tracking-tight text-slate-950 dark:text-white">
      <svg
        aria-hidden="true"
        className="h-9 w-9 shrink-0"
        viewBox="0 0 40 40"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
      >
        <rect width="40" height="40" rx="12" fill="currentColor" className="text-teal-700" />
        <path
          d="M10 12.5 14.8 28h3.6l2.25-7.45L23 28h3.7L31 12.5h-3.65l-2.7 10.1-2.3-7.45H19l-2.25 7.45-2.85-10.1H10Z"
          fill="white"
        />
      </svg>
      {!compact && <span className="text-lg">WalletWise</span>}
    </span>
  );
}

export const Button = forwardRef<
  HTMLButtonElement,
  ButtonHTMLAttributes<HTMLButtonElement> & {
    variant?: 'primary' | 'secondary' | 'ghost' | 'danger';
    size?: 'sm' | 'md' | 'lg';
  }
>(function Button({ className, variant = 'primary', size = 'md', ...props }, ref) {
  return (
    <button
      ref={ref}
      className={cn(
        'inline-flex items-center justify-center gap-2 rounded-xl font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-teal-500 focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-55 dark:focus-visible:ring-offset-slate-950',
        variant === 'primary' && 'bg-teal-700 text-white hover:bg-teal-800',
        variant === 'secondary' &&
          'border border-slate-300 bg-white text-slate-800 hover:bg-slate-50 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-100 dark:hover:bg-slate-800',
        variant === 'ghost' &&
          'text-slate-600 hover:bg-slate-100 hover:text-slate-950 dark:text-slate-300 dark:hover:bg-slate-800 dark:hover:text-white',
        variant === 'danger' && 'bg-rose-700 text-white hover:bg-rose-800',
        size === 'sm' && 'h-9 px-3 text-sm',
        size === 'md' && 'h-11 px-4 text-sm',
        size === 'lg' && 'h-12 px-5',
        className
      )}
      {...props}
    />
  );
});

export function Card({ className, children }: { className?: string; children: ReactNode }) {
  return (
    <section
      className={cn(
        'rounded-2xl border border-slate-200 bg-white shadow-sm dark:border-slate-800 dark:bg-slate-900',
        className
      )}
    >
      {children}
    </section>
  );
}

interface FieldProps {
  label: string;
  error?: string;
  hint?: string;
}

export function TextField({
  label,
  error,
  hint,
  className,
  id,
  ...props
}: FieldProps & InputHTMLAttributes<HTMLInputElement>) {
  const generatedId = useId();
  const fieldId = id ?? generatedId;
  const helpId = `${fieldId}-help`;
  return (
    <div className="grid gap-1.5">
      <label
        className="text-sm font-medium text-slate-700 dark:text-slate-200"
        htmlFor={fieldId}
      >
        {label}
      </label>
      <input
        id={fieldId}
        aria-invalid={Boolean(error)}
        aria-describedby={error || hint ? helpId : undefined}
        className={cn(
          'h-11 w-full rounded-xl border border-slate-300 bg-white px-3.5 text-slate-950 outline-none transition placeholder:text-slate-400 focus:border-teal-600 focus:ring-2 focus:ring-teal-100 dark:border-slate-700 dark:bg-slate-950 dark:text-white dark:focus:border-teal-500 dark:focus:ring-teal-950',
          error && 'border-rose-500 focus:border-rose-500 focus:ring-rose-100',
          className
        )}
        {...props}
      />
      {(error || hint) && (
        <span
          id={helpId}
          className={cn('text-xs font-normal', error ? 'text-rose-600' : 'text-slate-500')}
        >
          {error ?? hint}
        </span>
      )}
    </div>
  );
}

export function SelectField({
  label,
  error,
  hint,
  id,
  children,
  ...props
}: FieldProps & SelectHTMLAttributes<HTMLSelectElement>) {
  const generatedId = useId();
  const fieldId = id ?? generatedId;
  const helpId = `${fieldId}-help`;
  return (
    <div className="grid gap-1.5">
      <label
        className="text-sm font-medium text-slate-700 dark:text-slate-200"
        htmlFor={fieldId}
      >
        {label}
      </label>
      <select
        id={fieldId}
        aria-invalid={Boolean(error)}
        aria-describedby={error || hint ? helpId : undefined}
        className="h-11 w-full rounded-xl border border-slate-300 bg-white px-3.5 text-slate-950 outline-none focus:border-teal-600 focus:ring-2 focus:ring-teal-100 dark:border-slate-700 dark:bg-slate-950 dark:text-white"
        {...props}
      >
        {children}
      </select>
      {(error || hint) && (
        <span
          id={helpId}
          className={cn('text-xs font-normal', error ? 'text-rose-600' : 'text-slate-500')}
        >
          {error ?? hint}
        </span>
      )}
    </div>
  );
}

export function PageHeader({
  title,
  description,
  actions
}: {
  title: string;
  description?: string;
  actions?: ReactNode;
}) {
  return (
    <header className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight text-slate-950 dark:text-white sm:text-3xl">
          {title}
        </h1>
        {description && (
          <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">{description}</p>
        )}
      </div>
      {actions && <div className="flex flex-wrap items-center gap-2">{actions}</div>}
    </header>
  );
}

export function Money({
  value,
  currency,
  className
}: {
  value: DecimalValue;
  currency: string;
  className?: string;
}) {
  return <span className={cn('tabular-nums', className)}>{formatMoney(value, currency)}</span>;
}

export function Badge({
  children,
  tone = 'neutral'
}: {
  children: ReactNode;
  tone?: 'neutral' | 'success' | 'warning' | 'danger' | 'info';
}) {
  return (
    <span
      className={cn(
        'inline-flex items-center rounded-full px-2.5 py-1 text-xs font-semibold',
        tone === 'neutral' && 'bg-slate-100 text-slate-700 dark:bg-slate-800 dark:text-slate-200',
        tone === 'success' &&
          'bg-emerald-100 text-emerald-800 dark:bg-emerald-950 dark:text-emerald-200',
        tone === 'warning' && 'bg-amber-100 text-amber-800 dark:bg-amber-950 dark:text-amber-200',
        tone === 'danger' && 'bg-rose-100 text-rose-800 dark:bg-rose-950 dark:text-rose-200',
        tone === 'info' && 'bg-sky-100 text-sky-800 dark:bg-sky-950 dark:text-sky-200'
      )}
    >
      {children}
    </span>
  );
}

export function Progress({ value, label }: { value: number; label: string }) {
  const clamped = Math.max(0, Math.min(value, 100));
  return (
    <div>
      <div className="mb-1.5 flex justify-between text-xs text-slate-500 dark:text-slate-400">
        <span>{label}</span>
        <span className="tabular-nums">{Math.round(value)}%</span>
      </div>
      <div
        className="h-2 overflow-hidden rounded-full bg-slate-100 dark:bg-slate-800"
        role="progressbar"
        aria-label={label}
        aria-valuemin={0}
        aria-valuemax={100}
        aria-valuenow={Math.round(clamped)}
      >
        <div
          className={cn(
            'h-full rounded-full transition-[width]',
            value >= 100 ? 'bg-rose-600' : value >= 80 ? 'bg-amber-500' : 'bg-teal-600'
          )}
          style={{ width: `${clamped}%` }}
        />
      </div>
    </div>
  );
}

export function LoadingState({ label = 'Loading' }: { label?: string }) {
  return (
    <div className="grid min-h-48 place-items-center rounded-2xl border border-slate-200 bg-white p-8 dark:border-slate-800 dark:bg-slate-900">
      <div className="flex items-center gap-2 text-sm text-slate-500" role="status">
        <LoaderCircle className="h-5 w-5 animate-spin" aria-hidden="true" />
        {label}...
      </div>
    </div>
  );
}

export function EmptyState({
  title,
  description,
  action
}: {
  title: string;
  description: string;
  action?: ReactNode;
}) {
  return (
    <div className="grid min-h-48 place-items-center rounded-2xl border border-dashed border-slate-300 bg-white p-8 text-center dark:border-slate-700 dark:bg-slate-900">
      <div className="max-w-sm">
        <h2 className="font-semibold text-slate-900 dark:text-white">{title}</h2>
        <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">{description}</p>
        {action && <div className="mt-4">{action}</div>}
      </div>
    </div>
  );
}

export function ErrorState({
  error,
  title = 'Unable to load this page'
}: {
  error: Error;
  title?: string;
}) {
  const maybeApiError = error as ApiError;
  const correlationId = maybeApiError.problem?.correlationId;
  return (
    <div
      className="rounded-2xl border border-rose-200 bg-rose-50 p-5 text-rose-950 dark:border-rose-900 dark:bg-rose-950/40 dark:text-rose-100"
      role="alert"
    >
      <div className="flex gap-3">
        <AlertCircle className="mt-0.5 h-5 w-5 shrink-0" aria-hidden="true" />
        <div>
          <h2 className="font-semibold">{title}</h2>
          <p className="mt-1 text-sm">{error.message}</p>
          {correlationId && <p className="mt-2 text-xs opacity-75">Reference: {correlationId}</p>}
        </div>
      </div>
    </div>
  );
}

export function Modal({
  open,
  onClose,
  title,
  description,
  children
}: {
  open: boolean;
  onClose: () => void;
  title: string;
  description?: string;
  children: ReactNode;
}) {
  const closeRef = useRef<HTMLButtonElement>(null);

  useEffect(() => {
    if (!open) return;
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onClose();
    };
    document.addEventListener('keydown', onKeyDown);
    closeRef.current?.focus();
    return () => document.removeEventListener('keydown', onKeyDown);
  }, [onClose, open]);

  if (!open) return null;
  return (
    <div
      className="fixed inset-0 z-50 grid place-items-end bg-slate-950/55 p-0 sm:place-items-center sm:p-4"
      onMouseDown={onClose}
    >
      <section
        role="dialog"
        aria-modal="true"
        aria-labelledby="modal-title"
        className="max-h-[92vh] w-full overflow-y-auto rounded-t-3xl bg-white p-5 shadow-2xl dark:bg-slate-900 sm:max-w-lg sm:rounded-2xl sm:p-6"
        onMouseDown={(event) => event.stopPropagation()}
      >
        <header className="flex items-start justify-between gap-4">
          <div>
            <h2 id="modal-title" className="text-xl font-semibold text-slate-950 dark:text-white">
              {title}
            </h2>
            {description && <p className="mt-1 text-sm text-slate-500">{description}</p>}
          </div>
          <Button
            ref={closeRef}
            variant="ghost"
            size="sm"
            aria-label="Close dialog"
            onClick={onClose}
          >
            <X className="h-5 w-5" />
          </Button>
        </header>
        <div className="mt-5">{children}</div>
      </section>
    </div>
  );
}

export function Pagination({
  page,
  totalPages,
  onPageChange
}: {
  page: number;
  totalPages: number;
  onPageChange: (page: number) => void;
}) {
  if (totalPages <= 1) return null;
  return (
    <nav className="flex items-center justify-between gap-4" aria-label="Pagination">
      <Button
        variant="secondary"
        size="sm"
        disabled={page <= 0}
        onClick={() => onPageChange(page - 1)}
      >
        <ChevronLeft className="h-4 w-4" /> Previous
      </Button>
      <span className="text-sm text-slate-500">
        Page <span className="font-medium text-slate-900 dark:text-white">{page + 1}</span> of{' '}
        {totalPages}
      </span>
      <Button
        variant="secondary"
        size="sm"
        disabled={page >= totalPages - 1}
        onClick={() => onPageChange(page + 1)}
      >
        Next <ChevronRight className="h-4 w-4" />
      </Button>
    </nav>
  );
}
