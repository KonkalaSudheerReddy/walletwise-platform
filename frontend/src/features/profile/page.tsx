import { BarChart3, Bell, LogOut, SendHorizontal, ShieldCheck, UserRound } from 'lucide-react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../app/auth-context';
import { Badge, Button, Card, PageHeader } from '../../components/ui';
import { initials } from '../../lib/format';

export function ProfilePage() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  if (!user) return null;

  const signOut = async () => {
    await logout();
    navigate('/login', { replace: true });
  };

  const moreLinks = [
    { to: '/transfer', label: 'Transfer money', icon: SendHorizontal },
    { to: '/analytics', label: 'Monthly analytics', icon: BarChart3 },
    { to: '/notifications', label: 'Notifications', icon: Bell },
    ...(user.role === 'ADMIN' ? [{ to: '/admin/users', label: 'Administration', icon: ShieldCheck }] : [])
  ];

  return (
    <div className="space-y-7">
      <PageHeader title="Profile" description="Your account identity and session controls." />
      <div className="grid gap-5 lg:grid-cols-[380px_1fr]">
        <Card className="p-6 text-center"><span className="mx-auto grid h-20 w-20 place-items-center rounded-full bg-teal-100 text-2xl font-bold text-teal-800 dark:bg-teal-950 dark:text-teal-200">{initials(user.displayName)}</span><h2 className="mt-5 text-xl font-semibold">{user.displayName}</h2><p className="mt-1 text-sm text-slate-500">{user.email}</p><div className="mt-4 flex justify-center gap-2"><Badge tone={user.enabled ? 'success' : 'danger'}>{user.enabled ? 'Active' : 'Disabled'}</Badge><Badge tone={user.role === 'ADMIN' ? 'info' : 'neutral'}>{user.role}</Badge></div></Card>
        <div className="space-y-5"><Card className="p-6"><div className="flex items-center gap-3"><span className="grid h-10 w-10 place-items-center rounded-xl bg-slate-100 text-slate-600 dark:bg-slate-800 dark:text-slate-200"><UserRound className="h-5 w-5" /></span><div><h2 className="font-semibold">Account details</h2><p className="text-sm text-slate-500">Used to personalize currency displays.</p></div></div><dl className="mt-6 divide-y divide-slate-100 text-sm dark:divide-slate-800"><div className="flex justify-between gap-4 py-4"><dt className="text-slate-500">Display name</dt><dd className="font-medium">{user.displayName}</dd></div><div className="flex justify-between gap-4 py-4"><dt className="text-slate-500">Email</dt><dd className="break-all text-right font-medium">{user.email}</dd></div><div className="flex justify-between gap-4 py-4"><dt className="text-slate-500">Preferred currency</dt><dd className="font-medium">{user.preferredCurrency}</dd></div></dl><p className="mt-4 text-xs leading-5 text-slate-500">Wallet currencies may differ. WalletWise never performs currency conversion.</p></Card>
          <Card className="p-6"><div className="flex items-center gap-3"><span className="grid h-10 w-10 place-items-center rounded-xl bg-teal-50 text-teal-700 dark:bg-teal-950 dark:text-teal-200"><ShieldCheck className="h-5 w-5" /></span><div><h2 className="font-semibold">Session security</h2><p className="text-sm text-slate-500">Your short-lived access token is kept only in memory.</p></div></div><Button variant="danger" className="mt-6" onClick={() => void signOut()}><LogOut className="h-4 w-4" /> Log out securely</Button></Card>
        </div>
      </div>
      <Card className="p-5 lg:hidden"><h2 className="font-semibold">More</h2><nav className="mt-3 grid gap-1" aria-label="Additional mobile navigation">{moreLinks.map(({ to, label, icon: Icon }) => <Link key={to} to={to} className="flex h-12 items-center gap-3 rounded-xl px-3 text-sm font-medium text-slate-700 hover:bg-slate-100 dark:text-slate-200 dark:hover:bg-slate-800"><Icon className="h-5 w-5 text-teal-700" />{label}</Link>)}</nav></Card>
    </div>
  );
}
