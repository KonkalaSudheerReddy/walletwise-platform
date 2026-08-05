import { zodResolver } from '@hookform/resolvers/zod';
import {
  ArrowRight,
  BarChart3,
  Check,
  Eye,
  EyeOff,
  LockKeyhole,
  PiggyBank,
  RefreshCw,
  SendHorizontal,
  ShieldCheck,
  WalletCards
} from 'lucide-react';
import { useState, type ReactNode } from 'react';
import { useForm } from 'react-hook-form';
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom';
import { z } from 'zod';
import { ApiError } from '../../api/client';
import { Button, Card, TextField } from '../../components/ui';
import { PublicLayout } from '../../app/layout';
import { useAuth } from '../../app/auth-context';

const loginSchema = z.object({
  email: z.string().trim().email('Enter a valid email address.'),
  password: z.string().min(1, 'Enter your password.').max(64, 'Password must be 64 characters or fewer.')
});

const registerSchema = z
  .object({
    displayName: z.string().trim().min(2, 'Enter at least 2 characters.').max(80),
    email: z.string().trim().email('Enter a valid email address.'),
    password: z
      .string()
      .min(12, 'Use at least 12 characters.')
      .max(64, 'Use 64 characters or fewer.')
      .regex(/[A-Z]/, 'Include an uppercase letter.')
      .regex(/[a-z]/, 'Include a lowercase letter.')
      .regex(/[0-9]/, 'Include a number.')
      .regex(/[^A-Za-z0-9]/, 'Include a special character.'),
    confirmPassword: z.string(),
    preferredCurrency: z.string().length(3)
  })
  .refine((values) => values.password === values.confirmPassword, {
    path: ['confirmPassword'],
    message: 'Passwords do not match.'
  });

type LoginValues = z.infer<typeof loginSchema>;
type RegisterValues = z.infer<typeof registerSchema>;

function AuthenticationCard({
  title,
  description,
  children,
  footer
}: {
  title: string;
  description: string;
  children: ReactNode;
  footer: ReactNode;
}) {
  return (
    <PublicLayout>
      <main className="mx-auto grid min-h-[calc(100vh-5rem)] max-w-7xl items-center gap-10 px-5 py-10 sm:px-8 lg:grid-cols-[1fr_460px]">
        <section className="hidden max-w-xl lg:block">
          <span className="inline-flex items-center gap-2 rounded-full bg-teal-100 px-3 py-1 text-sm font-medium text-teal-800 dark:bg-teal-950 dark:text-teal-200">
            <ShieldCheck className="h-4 w-4" /> Privacy-minded session design
          </span>
          <h1 className="mt-6 text-5xl font-semibold tracking-tight text-slate-950 dark:text-white">
            Clarity for every wallet and every month.
          </h1>
          <p className="mt-5 text-lg leading-8 text-slate-600 dark:text-slate-300">
            Track virtual balances, build better budget habits, and understand where your money goes.
          </p>
          <ul className="mt-8 space-y-3 text-sm text-slate-600 dark:text-slate-300">
            {['Short-lived access tokens stay in memory', 'Atomic, idempotent wallet transfers', 'Clear monthly spending analytics'].map(
              (item) => (
                <li key={item} className="flex items-center gap-3">
                  <span className="grid h-6 w-6 place-items-center rounded-full bg-emerald-100 text-emerald-700 dark:bg-emerald-950 dark:text-emerald-300">
                    <Check className="h-4 w-4" />
                  </span>
                  {item}
                </li>
              )
            )}
          </ul>
        </section>
        <Card className="mx-auto w-full max-w-md p-6 sm:p-8">
          <h1 className="text-2xl font-semibold tracking-tight">{title}</h1>
          <p className="mt-2 text-sm text-slate-500 dark:text-slate-400">{description}</p>
          <div className="mt-7">{children}</div>
          <p className="mt-6 text-center text-sm text-slate-500">{footer}</p>
        </Card>
      </main>
    </PublicLayout>
  );
}

export function LoginPage() {
  const auth = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [showPassword, setShowPassword] = useState(false);
  const [formError, setFormError] = useState<string>();
  const {
    register,
    handleSubmit,
    setValue,
    formState: { errors, isSubmitting }
  } = useForm<LoginValues>({ resolver: zodResolver(loginSchema), defaultValues: { email: '', password: '' } });

  if (auth.status === 'authenticated') return <Navigate to="/dashboard" replace />;

  const submit = async (values: LoginValues) => {
    setFormError(undefined);
    try {
      await auth.login(values);
      const from = (location.state as { from?: string } | null)?.from;
      navigate(from && from.startsWith('/') ? from : '/dashboard', { replace: true });
    } catch (error) {
      setFormError(error instanceof ApiError ? error.problem.detail : 'Unable to sign in. Please try again.');
    }
  };

  return (
    <AuthenticationCard
      title="Welcome back"
      description="Sign in to your WalletWise workspace."
      footer={<>New to WalletWise? <Link className="font-medium text-teal-700 hover:underline" to="/register">Create an account</Link></>}
    >
      <form className="space-y-4" onSubmit={(event) => void handleSubmit(submit)(event)} noValidate>
        {formError && <div className="rounded-xl border border-rose-200 bg-rose-50 p-3 text-sm text-rose-800" role="alert">{formError}</div>}
        <TextField label="Email address" type="email" autoComplete="email" placeholder="you@example.com" error={errors.email?.message} {...register('email')} />
        <div className="relative">
          <TextField label="Password" type={showPassword ? 'text' : 'password'} autoComplete="current-password" error={errors.password?.message} className="pr-11" {...register('password')} />
          <button type="button" className="absolute right-2 top-[31px] grid h-9 w-9 place-items-center rounded-lg text-slate-500 hover:bg-slate-100" onClick={() => setShowPassword((value) => !value)} aria-label={showPassword ? 'Hide password' : 'Show password'}>
            {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
          </button>
        </div>
        <Button className="w-full" size="lg" disabled={isSubmitting} type="submit">
          {isSubmitting ? <RefreshCw className="h-4 w-4 animate-spin" /> : <LockKeyhole className="h-4 w-4" />}
          Sign in
        </Button>
        <button
          type="button"
          className="w-full rounded-xl border border-dashed border-teal-300 bg-teal-50 p-3 text-sm font-medium text-teal-800 hover:bg-teal-100 dark:border-teal-800 dark:bg-teal-950 dark:text-teal-200"
          onClick={() => {
            setValue('email', 'demo@walletwise.app', { shouldValidate: true });
            setValue('password', 'Demo@12345', { shouldValidate: true });
          }}
        >
          Use demo account
        </button>
        <p className="text-center text-xs text-slate-500">Shared synthetic data may be reset periodically.</p>
      </form>
    </AuthenticationCard>
  );
}

export function RegisterPage() {
  const auth = useAuth();
  const navigate = useNavigate();
  const [showPassword, setShowPassword] = useState(false);
  const [formError, setFormError] = useState<string>();
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting }
  } = useForm<RegisterValues>({
    resolver: zodResolver(registerSchema),
    defaultValues: { displayName: '', email: '', password: '', confirmPassword: '', preferredCurrency: 'USD' }
  });

  if (auth.status === 'authenticated') return <Navigate to="/dashboard" replace />;

  const submit = async (values: RegisterValues) => {
    setFormError(undefined);
    try {
      await auth.register({
        displayName: values.displayName,
        email: values.email,
        password: values.password,
        preferredCurrency: values.preferredCurrency
      });
      navigate('/dashboard', { replace: true });
    } catch (error) {
      setFormError(error instanceof ApiError ? error.problem.detail : 'Unable to create your account.');
    }
  };

  return (
    <AuthenticationCard
      title="Create your account"
      description="Start with secure, synthetic financial tracking."
      footer={<>Already registered? <Link className="font-medium text-teal-700 hover:underline" to="/login">Sign in</Link></>}
    >
      <form className="space-y-4" onSubmit={(event) => void handleSubmit(submit)(event)} noValidate>
        {formError && <div className="rounded-xl border border-rose-200 bg-rose-50 p-3 text-sm text-rose-800" role="alert">{formError}</div>}
        <TextField label="Display name" autoComplete="name" error={errors.displayName?.message} {...register('displayName')} />
        <TextField label="Email address" type="email" autoComplete="email" error={errors.email?.message} {...register('email')} />
        <div className="relative">
          <TextField label="Password" type={showPassword ? 'text' : 'password'} autoComplete="new-password" error={errors.password?.message} className="pr-11" {...register('password')} />
          <button type="button" className="absolute right-2 top-[31px] grid h-9 w-9 place-items-center rounded-lg text-slate-500 hover:bg-slate-100" onClick={() => setShowPassword((value) => !value)} aria-label={showPassword ? 'Hide password' : 'Show password'}>
            {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
          </button>
        </div>
        <TextField label="Confirm password" type={showPassword ? 'text' : 'password'} autoComplete="new-password" error={errors.confirmPassword?.message} {...register('confirmPassword')} />
        <label className="grid gap-1.5 text-sm font-medium text-slate-700 dark:text-slate-200">
          Preferred currency
          <select className="h-11 rounded-xl border border-slate-300 bg-white px-3.5 dark:border-slate-700 dark:bg-slate-950" {...register('preferredCurrency')}>
            <option value="USD">USD - US Dollar</option>
            <option value="INR">INR - Indian Rupee</option>
            <option value="EUR">EUR - Euro</option>
            <option value="GBP">GBP - British Pound</option>
          </select>
        </label>
        <Button className="w-full" size="lg" disabled={isSubmitting} type="submit">
          {isSubmitting && <RefreshCw className="h-4 w-4 animate-spin" />}
          Create account
        </Button>
      </form>
    </AuthenticationCard>
  );
}

export function LandingPage() {
  const capabilities = [
    { icon: WalletCards, title: 'Every wallet in one view', description: 'Organize cash, savings, and virtual bank wallets without connecting to a real institution.' },
    { icon: SendHorizontal, title: 'Retry-safe transfers', description: 'Learn how idempotency and atomic ledger entries protect a transfer from duplicate processing.' },
    { icon: PiggyBank, title: 'Budgets that stay visible', description: 'Track category limits and receive useful alerts as spending approaches a threshold.' },
    { icon: BarChart3, title: 'Monthly financial clarity', description: 'Explore income, expenses, category mix, trends, and month-over-month context.' }
  ];

  return (
    <PublicLayout>
      <main>
        <section className="mx-auto grid max-w-7xl items-center gap-14 px-5 py-16 sm:px-8 lg:grid-cols-[1fr_1fr] lg:py-24">
          <div>
            <span className="inline-flex items-center gap-2 rounded-full bg-teal-100 px-3 py-1 text-sm font-medium text-teal-800 dark:bg-teal-950 dark:text-teal-200">
              <ShieldCheck className="h-4 w-4" /> Portfolio-quality financial tracking
            </span>
            <h1 className="mt-6 max-w-2xl text-5xl font-semibold tracking-[-0.045em] text-slate-950 dark:text-white sm:text-6xl">
              Make sense of your money, one wallet at a time.
            </h1>
            <p className="mt-6 max-w-xl text-lg leading-8 text-slate-600 dark:text-slate-300">
              WalletWise brings virtual wallets, immutable activity, monthly budgets, and useful analytics into one calm workspace.
            </p>
            <div className="mt-8 flex flex-wrap gap-3">
              <Link to="/register" className="inline-flex h-12 items-center gap-2 rounded-xl bg-teal-700 px-5 font-medium text-white hover:bg-teal-800">Create free account <ArrowRight className="h-4 w-4" /></Link>
              <Link to="/login" className="inline-flex h-12 items-center rounded-xl border border-slate-300 bg-white px-5 font-medium hover:bg-slate-50 dark:border-slate-700 dark:bg-slate-900">Explore the demo</Link>
            </div>
            <p className="mt-5 text-xs text-slate-500">Educational portfolio application. No real funds, bank connections, payments, or financial advice.</p>
          </div>

          <div className="relative mx-auto w-full max-w-xl" aria-label="WalletWise dashboard preview">
            <div className="absolute -inset-5 rounded-[2.5rem] bg-teal-100/70 blur-3xl dark:bg-teal-950/40" />
            <Card className="relative overflow-hidden p-5 sm:p-7">
              <div className="flex items-center justify-between">
                <div><p className="text-sm text-slate-500">Available in USD</p><p className="mt-1 text-3xl font-semibold">$12,480.50</p></div>
                <span className="grid h-11 w-11 place-items-center rounded-2xl bg-teal-100 text-teal-700"><BarChart3 className="h-5 w-5" /></span>
              </div>
              <div className="mt-7 grid grid-cols-2 gap-3">
                <div className="rounded-2xl bg-emerald-50 p-4 dark:bg-emerald-950/40"><p className="text-xs text-emerald-700 dark:text-emerald-300">Income this month</p><p className="mt-1 font-semibold">+$5,200.00</p></div>
                <div className="rounded-2xl bg-rose-50 p-4 dark:bg-rose-950/40"><p className="text-xs text-rose-700 dark:text-rose-300">Expenses</p><p className="mt-1 font-semibold">-$2,740.25</p></div>
              </div>
              <div className="mt-6 flex h-36 items-end gap-2" aria-hidden="true">
                {[32, 48, 38, 72, 51, 84, 63, 92, 58, 76, 66, 88].map((height, index) => <span key={index} className="flex-1 rounded-t-md bg-teal-600/80" style={{ height: `${height}%` }} />)}
              </div>
              <div className="mt-5 rounded-2xl border border-slate-200 p-4 dark:border-slate-800">
                <div className="flex items-center justify-between"><div><p className="font-medium">Groceries</p><p className="text-xs text-slate-500">Monthly budget</p></div><span className="text-sm font-semibold">72%</span></div>
                <div className="mt-3 h-2 rounded-full bg-slate-100 dark:bg-slate-800"><div className="h-full w-[72%] rounded-full bg-teal-600" /></div>
              </div>
            </Card>
          </div>
        </section>

        <section className="border-y border-slate-200 bg-white dark:border-slate-800 dark:bg-slate-900">
          <div className="mx-auto max-w-7xl px-5 py-20 sm:px-8">
            <div className="max-w-2xl"><p className="text-sm font-semibold uppercase tracking-widest text-teal-700">Designed for clarity</p><h2 className="mt-3 text-3xl font-semibold tracking-tight sm:text-4xl">Serious engineering behind a straightforward experience.</h2></div>
            <div className="mt-10 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
              {capabilities.map(({ icon: Icon, title, description }) => <article key={title} className="rounded-2xl border border-slate-200 p-5 dark:border-slate-800"><span className="grid h-10 w-10 place-items-center rounded-xl bg-teal-100 text-teal-700 dark:bg-teal-950 dark:text-teal-200"><Icon className="h-5 w-5" /></span><h3 className="mt-5 font-semibold">{title}</h3><p className="mt-2 text-sm leading-6 text-slate-500">{description}</p></article>)}
            </div>
          </div>
        </section>

        <footer className="mx-auto flex max-w-7xl flex-col gap-3 px-5 py-10 text-sm text-slate-500 sm:flex-row sm:items-center sm:justify-between sm:px-8"><p>(c) {new Date().getFullYear()} Konkala Sudheer Reddy</p><p>Built as an educational software portfolio project.</p></footer>
      </main>
    </PublicLayout>
  );
}
