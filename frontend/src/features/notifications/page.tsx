import { Bell, BellRing, CheckCheck } from 'lucide-react';
import { useState } from 'react';
import { toast } from 'sonner';
import {
  useMarkAllNotificationsRead,
  useMarkNotificationRead,
  useNotifications
} from '../../api/queries';
import type { Notification } from '../../api/types';
import {
  Badge,
  Button,
  Card,
  EmptyState,
  ErrorState,
  LoadingState,
  PageHeader,
  Pagination
} from '../../components/ui';
import { formatDateTime } from '../../lib/format';
import { cn } from '../../lib/styles';

function NotificationRow({
  notification,
  onRead
}: {
  notification: Notification;
  onRead: (id: string) => void;
}) {
  const tone =
    notification.type === 'BUDGET_EXCEEDED'
      ? 'danger'
      : notification.type === 'BUDGET_REACHED'
        ? 'warning'
        : 'info';
  return (
    <li
      className={cn(
        'flex gap-4 p-5 sm:p-6',
        !notification.read && 'bg-teal-50/50 dark:bg-teal-950/20'
      )}
    >
      <span
        className={cn(
          'grid h-10 w-10 shrink-0 place-items-center rounded-xl',
          !notification.read
            ? 'bg-teal-100 text-teal-700 dark:bg-teal-950 dark:text-teal-200'
            : 'bg-slate-100 text-slate-500 dark:bg-slate-800'
        )}
      >
        {notification.read ? <Bell className="h-5 w-5" /> : <BellRing className="h-5 w-5" />}
      </span>
      <div className="min-w-0 flex-1">
        <div className="flex flex-wrap items-center gap-2">
          <h2 className="font-semibold">{notification.title}</h2>
          <Badge tone={tone}>
            {notification.type === 'BUDGET_EXCEEDED'
              ? 'Over budget'
              : notification.type === 'BUDGET_REACHED'
                ? 'Limit reached'
                : 'Approaching limit'}
          </Badge>
          {!notification.read && (
            <span className="h-2 w-2 rounded-full bg-teal-600" aria-label="Unread" />
          )}
        </div>
        <p className="mt-2 text-sm leading-6 text-slate-600 dark:text-slate-300">
          {notification.message}
        </p>
        <p className="mt-2 text-xs text-slate-500">{formatDateTime(notification.createdAt)}</p>
      </div>
      {!notification.read && (
        <Button
          variant="ghost"
          size="sm"
          className="shrink-0"
          onClick={() => onRead(notification.id)}
        >
          <CheckCheck className="h-4 w-4" />
          <span className="hidden sm:inline">Mark read</span>
        </Button>
      )}
    </li>
  );
}

export function NotificationsPage() {
  const [page, setPage] = useState(0);
  const [unreadOnly, setUnreadOnly] = useState(false);
  const notifications = useNotifications(page, unreadOnly);
  const markRead = useMarkNotificationRead();
  const markAll = useMarkAllNotificationsRead();

  return (
    <div className="space-y-7">
      <PageHeader
        title="Notifications"
        description="Budget alerts are durable and generated without duplicate thresholds."
        actions={
          <Button
            variant="secondary"
            disabled={markAll.isPending || !notifications.data?.content.some((item) => !item.read)}
            onClick={() =>
              markAll.mutate(undefined, {
                onSuccess: () => toast.success('All notifications marked as read'),
                onError: (error) => toast.error(error.message)
              })
            }
          >
            <CheckCheck className="h-4 w-4" /> Mark all read
          </Button>
        }
      />
      <label className="inline-flex items-center gap-2 text-sm text-slate-600 dark:text-slate-300">
        <input
          type="checkbox"
          className="h-4 w-4 rounded accent-teal-700"
          checked={unreadOnly}
          onChange={(event) => {
            setUnreadOnly(event.target.checked);
            setPage(0);
          }}
        />{' '}
        Show unread only
      </label>
      {notifications.isPending ? (
        <LoadingState label="Loading notifications" />
      ) : notifications.error ? (
        <ErrorState error={notifications.error} />
      ) : !notifications.data?.content.length ? (
        <EmptyState
          title={unreadOnly ? 'You are all caught up' : 'No notifications yet'}
          description={
            unreadOnly
              ? 'There are no unread budget alerts.'
              : 'Budget threshold alerts will appear here.'
          }
        />
      ) : (
        <>
          <Card className="overflow-hidden">
            <ul className="divide-y divide-slate-100 dark:divide-slate-800">
              {notifications.data.content.map((notification) => (
                <NotificationRow
                  key={notification.id}
                  notification={notification}
                  onRead={(id) =>
                    markRead.mutate(id, { onError: (error) => toast.error(error.message) })
                  }
                />
              ))}
            </ul>
          </Card>
          <Pagination
            page={notifications.data.page}
            totalPages={notifications.data.totalPages}
            onPageChange={setPage}
          />
        </>
      )}
    </div>
  );
}
