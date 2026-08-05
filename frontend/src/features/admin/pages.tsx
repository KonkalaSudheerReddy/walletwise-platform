import { FileClock, Search, UsersRound } from 'lucide-react';
import { useState } from 'react';
import { NavLink } from 'react-router-dom';
import { toast } from 'sonner';
import { useAdminUsers, useAuditLogs, useSetUserEnabled } from '../../api/queries';
import type { AuditFilters } from '../../api/types';
import { Badge, Button, Card, EmptyState, ErrorState, LoadingState, PageHeader, Pagination, SelectField, TextField } from '../../components/ui';
import { formatDateTime } from '../../lib/format';
import { cn } from '../../lib/styles';

function AdminTabs() {
  return (
    <nav className="inline-flex rounded-xl border border-slate-200 bg-white p-1 dark:border-slate-800 dark:bg-slate-900" aria-label="Administration sections">
      {[{ to: '/admin/users', label: 'Users', icon: UsersRound }, { to: '/admin/audit-logs', label: 'Audit logs', icon: FileClock }].map(({ to, label, icon: Icon }) => <NavLink key={to} to={to} className={({ isActive }) => cn('inline-flex h-9 items-center gap-2 rounded-lg px-3 text-sm font-medium', isActive ? 'bg-teal-50 text-teal-800 dark:bg-teal-950 dark:text-teal-200' : 'text-slate-500 hover:text-slate-900 dark:hover:text-white')}><Icon className="h-4 w-4" />{label}</NavLink>)}
    </nav>
  );
}

export function AdminUsersPage() {
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const users = useAdminUsers(page, search);
  const status = useSetUserEnabled();

  return (
    <div className="space-y-7">
      <PageHeader title="Administration" description="Manage account access without exposing authentication secrets." actions={<AdminTabs />} />
      <Card className="p-4"><div className="relative max-w-md"><Search className="absolute left-3 top-3.5 h-4 w-4 text-slate-400" /><input aria-label="Search users" className="h-11 w-full rounded-xl border border-slate-300 bg-white pl-10 pr-3 text-sm dark:border-slate-700 dark:bg-slate-950" value={search} onChange={(event) => { setSearch(event.target.value); setPage(0); }} placeholder="Search by name or email" /></div></Card>
      {users.isPending ? <LoadingState label="Loading users" /> : users.error ? <ErrorState error={users.error} /> : !users.data?.content.length ? <EmptyState title="No users found" description="Try a different search." /> : <><Card className="overflow-hidden"><div className="overflow-x-auto"><table className="w-full min-w-[720px] text-left text-sm"><thead className="border-b border-slate-200 bg-slate-50 text-xs uppercase tracking-wide text-slate-500 dark:border-slate-800 dark:bg-slate-950"><tr><th className="px-5 py-3">User</th><th className="px-5 py-3">Role</th><th className="px-5 py-3">Currency</th><th className="px-5 py-3">Status</th><th className="px-5 py-3 text-right">Action</th></tr></thead><tbody className="divide-y divide-slate-100 dark:divide-slate-800">{users.data.content.map((user) => <tr key={user.id}><td className="px-5 py-4"><p className="font-medium">{user.displayName}</p><p className="text-xs text-slate-500">{user.email}</p></td><td className="px-5 py-4"><Badge tone={user.role === 'ADMIN' ? 'info' : 'neutral'}>{user.role}</Badge></td><td className="px-5 py-4">{user.preferredCurrency}</td><td className="px-5 py-4"><Badge tone={user.enabled ? 'success' : 'danger'}>{user.enabled ? 'Enabled' : 'Disabled'}</Badge></td><td className="px-5 py-4 text-right"><Button variant={user.enabled ? 'danger' : 'secondary'} size="sm" disabled={status.isPending} onClick={() => status.mutate({ id: user.id, enabled: !user.enabled }, { onSuccess: () => toast.success(user.enabled ? 'Account disabled' : 'Account enabled'), onError: (error) => toast.error(error.message) })}>{user.enabled ? 'Disable' : 'Enable'}</Button></td></tr>)}</tbody></table></div></Card><Pagination page={users.data.page} totalPages={users.data.totalPages} onPageChange={setPage} /></>}
    </div>
  );
}

export function AuditLogsPage() {
  const [filters, setFilters] = useState<AuditFilters>({ page: 0, size: 20 });
  const logs = useAuditLogs(filters);
  const update = (name: keyof AuditFilters, value: string | number) => setFilters((current) => ({ ...current, [name]: value || undefined, page: name === 'page' ? Number(value) : 0 }));

  return (
    <div className="space-y-7">
      <PageHeader title="Administration" description="Append-only security and business event history." actions={<AdminTabs />} />
      <Card className="p-4 sm:p-5"><div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-5"><TextField label="Actor user ID" value={filters.actorId ?? ''} onChange={(event) => update('actorId', event.target.value)} placeholder="UUID" /><TextField label="Action" value={filters.action ?? ''} onChange={(event) => update('action', event.target.value)} placeholder="TRANSFER_COMPLETED" /><TextField label="Resource type" value={filters.resourceType ?? ''} onChange={(event) => update('resourceType', event.target.value)} /><SelectField label="Outcome" value={filters.outcome ?? ''} onChange={(event) => update('outcome', event.target.value)}><option value="">All outcomes</option><option value="SUCCESS">Success</option><option value="FAILURE">Failure</option></SelectField><div className="grid grid-cols-2 gap-2"><TextField label="From" type="date" value={filters.startDate ?? ''} onChange={(event) => update('startDate', event.target.value)} /><TextField label="To" type="date" value={filters.endDate ?? ''} onChange={(event) => update('endDate', event.target.value)} /></div></div></Card>
      {logs.isPending ? <LoadingState label="Loading audit logs" /> : logs.error ? <ErrorState error={logs.error} /> : !logs.data?.content.length ? <EmptyState title="No audit events found" description="Adjust the filters or perform an audited action." /> : <><Card className="overflow-hidden"><div className="overflow-x-auto"><table className="w-full min-w-[900px] text-left text-sm"><thead className="border-b border-slate-200 bg-slate-50 text-xs uppercase tracking-wide text-slate-500 dark:border-slate-800 dark:bg-slate-950"><tr><th className="px-5 py-3">Time</th><th className="px-5 py-3">Actor</th><th className="px-5 py-3">Action</th><th className="px-5 py-3">Resource</th><th className="px-5 py-3">Outcome</th><th className="px-5 py-3">Correlation ID</th></tr></thead><tbody className="divide-y divide-slate-100 dark:divide-slate-800">{logs.data.content.map((log) => <tr key={log.id}><td className="whitespace-nowrap px-5 py-4">{formatDateTime(log.occurredAt)}</td><td className="px-5 py-4">{log.actorUserId || 'System'}</td><td className="px-5 py-4 font-medium">{log.action}</td><td className="px-5 py-4">{log.resourceType}{log.resourceId && <p className="max-w-40 truncate text-xs text-slate-500">{log.resourceId}</p>}</td><td className="px-5 py-4"><Badge tone={log.outcome === 'SUCCESS' ? 'success' : 'danger'}>{log.outcome}</Badge></td><td className="px-5 py-4 font-mono text-xs">{log.correlationId ? `${log.correlationId.slice(0, 12)}...` : 'System'}</td></tr>)}</tbody></table></div></Card><Pagination page={logs.data.page} totalPages={logs.data.totalPages} onPageChange={(page) => update('page', page)} /></>}
    </div>
  );
}

