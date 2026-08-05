import {
  BarChart3,
  Bell,
  ChartNoAxesCombined,
  ChevronLeft,
  ChevronRight,
  CircleDollarSign,
  CreditCard,
  LayoutDashboard,
  ListFilter,
  LogOut,
  Moon,
  PiggyBank,
  SendHorizontal,
  ShieldCheck,
  Sun,
  UserRound
} from 'lucide-react';
import { useEffect, useState, type ReactNode } from 'react';
import { Link, NavLink, Navigate, Outlet, useLocation } from 'react-router-dom';
import { useUnreadCount } from '../api/queries';
import { Button, LoadingState, WalletWiseLogo } from '../components/ui';
import { initials } from '../lib/format';
import { cn } from '../lib/styles';
import { useAuth } from './auth-context';

const mainNavigation = [
  { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { to: '/wallets', label: 'Wallets', icon: CreditCard },
  { to: '/transactions', label: 'Transactions', icon: ListFilter },
  { to: '/transfer', label: 'Transfer', icon: SendHorizontal },
  { to: '/budgets', label: 'Budgets', icon: PiggyBank },
  { to: '/analytics', label: 'Analytics', icon: BarChart3 }
];

function useTheme() {
  const [dark, setDark] = useState(() => {
    const stored = localStorage.getItem('walletwise-theme');
    return stored ? stored === 'dark' : window.matchMedia('(prefers-color-scheme: dark)').matches;
  });

  useEffect(() => {
    document.documentElement.classList.toggle('dark', dark);
    localStorage.setItem('walletwise-theme', dark ? 'dark' : 'light');
  }, [dark]);

  return { dark, setDark };
}

function SidebarLink({
  to,
  label,
  icon: Icon,
  collapsed
}: {
  to: string;
  label: string;
  icon: typeof LayoutDashboard;
  collapsed: boolean;
}) {
  return (
    <NavLink
      to={to}
      title={collapsed ? label : undefined}
      className={({ isActive }) =>
        cn(
          'flex h-11 items-center gap-3 rounded-xl px-3 text-sm font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-teal-500',
          isActive
            ? 'bg-teal-50 text-teal-800 dark:bg-teal-950 dark:text-teal-200'
            : 'text-slate-600 hover:bg-slate-100 hover:text-slate-950 dark:text-slate-300 dark:hover:bg-slate-800 dark:hover:text-white',
          collapsed && 'justify-center px-0'
        )
      }
    >
      <Icon className="h-5 w-5 shrink-0" aria-hidden="true" />
      {!collapsed && <span>{label}</span>}
    </NavLink>
  );
}

export function ProtectedRoute() {
  const auth = useAuth();
  const location = useLocation();

  if (auth.status === 'restoring') {
    return (
      <main className="grid min-h-screen place-items-center bg-slate-50 p-6 dark:bg-slate-950">
        <div className="w-full max-w-sm">
          <LoadingState label="Restoring your secure session" />
        </div>
      </main>
    );
  }

  if (auth.status === 'anonymous') {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }
  return <Outlet />;
}

export function AdminRoute() {
  const { user } = useAuth();
  if (user?.role !== 'ADMIN') return <Navigate to="/dashboard" replace />;
  return <Outlet />;
}

export function AppShell() {
  const { user, logout } = useAuth();
  const { dark, setDark } = useTheme();
  const [collapsed, setCollapsed] = useState(
    () => localStorage.getItem('walletwise-sidebar') === 'collapsed'
  );
  const unread = useUnreadCount(Boolean(user));

  useEffect(() => {
    localStorage.setItem('walletwise-sidebar', collapsed ? 'collapsed' : 'expanded');
  }, [collapsed]);

  return (
    <div className="min-h-screen bg-slate-50 text-slate-950 dark:bg-slate-950 dark:text-white">
      <a
        href="#main-content"
        className="fixed left-4 top-4 z-[60] -translate-y-24 rounded-lg bg-teal-700 px-4 py-2 text-white transition-transform focus:translate-y-0"
      >
        Skip to content
      </a>

      <aside
        className={cn(
          'fixed inset-y-0 left-0 z-30 hidden border-r border-slate-200 bg-white transition-[width] dark:border-slate-800 dark:bg-slate-900 lg:flex lg:flex-col',
          collapsed ? 'w-20' : 'w-64'
        )}
      >
        <div className={cn('flex h-20 items-center px-5', collapsed && 'justify-center px-0')}>
          <Link to="/dashboard" aria-label="WalletWise dashboard">
            <WalletWiseLogo compact={collapsed} />
          </Link>
        </div>
        <nav className="flex-1 space-y-1 px-3" aria-label="Primary">
          {mainNavigation.map((item) => (
            <SidebarLink key={item.to} {...item} collapsed={collapsed} />
          ))}
          {user?.role === 'ADMIN' && (
            <SidebarLink
              to="/admin/users"
              label="Administration"
              icon={ShieldCheck}
              collapsed={collapsed}
            />
          )}
        </nav>
        <div className="border-t border-slate-200 p-3 dark:border-slate-800">
          <Button
            variant="ghost"
            className={cn('w-full', collapsed ? 'px-0' : 'justify-start')}
            onClick={() => setCollapsed((value) => !value)}
            aria-label={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}
          >
            {collapsed ? <ChevronRight className="h-5 w-5" /> : <ChevronLeft className="h-5 w-5" />}
            {!collapsed && 'Collapse'}
          </Button>
        </div>
      </aside>

      <div className={cn('transition-[padding]', collapsed ? 'lg:pl-20' : 'lg:pl-64')}>
        <header className="sticky top-0 z-20 flex h-16 items-center justify-between border-b border-slate-200 bg-white/95 px-4 backdrop-blur dark:border-slate-800 dark:bg-slate-900/95 sm:px-6 lg:h-20 lg:px-8">
          <Link to="/dashboard" className="lg:hidden" aria-label="WalletWise dashboard">
            <WalletWiseLogo />
          </Link>
          <div className="hidden lg:block">
            <p className="text-sm font-medium">Hello, {user?.displayName.split(' ')[0]}</p>
            <p className="text-xs text-slate-500">Here is your financial overview.</p>
          </div>
          <div className="flex items-center gap-1.5 sm:gap-2">
            <Button
              variant="ghost"
              size="sm"
              aria-label={dark ? 'Use light theme' : 'Use dark theme'}
              onClick={() => setDark(!dark)}
            >
              {dark ? <Sun className="h-5 w-5" /> : <Moon className="h-5 w-5" />}
            </Button>
            <Link
              to="/notifications"
              className="relative grid h-9 w-9 place-items-center rounded-xl text-slate-600 hover:bg-slate-100 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-teal-500 dark:text-slate-300 dark:hover:bg-slate-800"
              aria-label={`${unread.data?.unreadCount ?? 0} unread notifications`}
            >
              <Bell className="h-5 w-5" />
              {(unread.data?.unreadCount ?? 0) > 0 && (
                <span className="absolute right-0.5 top-0.5 grid min-h-4 min-w-4 place-items-center rounded-full bg-rose-600 px-1 text-[10px] font-bold leading-none text-white">
                  {Math.min(unread.data?.unreadCount ?? 0, 99)}
                </span>
              )}
            </Link>
            <Link
              to="/profile"
              className="ml-1 grid h-9 w-9 place-items-center rounded-full bg-teal-100 text-xs font-bold text-teal-800 ring-1 ring-teal-200 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-teal-500 dark:bg-teal-950 dark:text-teal-200"
              aria-label="Open profile"
            >
              {initials(user?.displayName ?? 'User')}
            </Link>
            <Button
              variant="ghost"
              size="sm"
              className="hidden sm:inline-flex"
              onClick={() => void logout()}
            >
              <LogOut className="h-4 w-4" /> Log out
            </Button>
          </div>
        </header>

        <main
          id="main-content"
          className="mx-auto w-full max-w-[1600px] p-4 pb-24 sm:p-6 lg:p-8 lg:pb-8"
        >
          <Outlet />
        </main>
      </div>

      <nav
        className="fixed inset-x-0 bottom-0 z-30 grid grid-cols-5 border-t border-slate-200 bg-white px-1 pb-[max(.25rem,env(safe-area-inset-bottom))] dark:border-slate-800 dark:bg-slate-900 lg:hidden"
        aria-label="Mobile navigation"
      >
        {[
          { to: '/dashboard', label: 'Home', icon: LayoutDashboard },
          { to: '/wallets', label: 'Wallets', icon: CreditCard },
          { to: '/transactions', label: 'Activity', icon: ChartNoAxesCombined },
          { to: '/budgets', label: 'Budgets', icon: CircleDollarSign },
          { to: '/profile', label: 'More', icon: UserRound }
        ].map(({ to, label, icon: Icon }) => (
          <NavLink
            key={to}
            to={to}
            className={({ isActive }) =>
              cn(
                'flex min-h-16 flex-col items-center justify-center gap-1 rounded-xl text-[11px] font-medium',
                isActive ? 'text-teal-700 dark:text-teal-300' : 'text-slate-500 dark:text-slate-400'
              )
            }
          >
            <Icon className="h-5 w-5" />
            {label}
          </NavLink>
        ))}
      </nav>
    </div>
  );
}

export function PublicLayout({ children }: { children: ReactNode }) {
  return (
    <div className="min-h-screen bg-slate-50 text-slate-950 dark:bg-slate-950 dark:text-white">
      <header className="mx-auto flex h-20 max-w-7xl items-center justify-between px-5 sm:px-8">
        <Link to="/" aria-label="WalletWise home">
          <WalletWiseLogo />
        </Link>
        <nav className="flex items-center gap-2" aria-label="Account">
          <Link
            to="/login"
            className="rounded-xl px-4 py-2 text-sm font-medium hover:bg-slate-100 dark:hover:bg-slate-800"
          >
            Log in
          </Link>
          <Link
            to="/register"
            className="rounded-xl bg-teal-700 px-4 py-2 text-sm font-medium text-white hover:bg-teal-800"
          >
            Create account
          </Link>
        </nav>
      </header>
      {children}
    </div>
  );
}
