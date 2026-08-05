import { Navigate, createBrowserRouter } from 'react-router-dom';
import { AdminRoute, AppShell, ProtectedRoute } from './layout';
import { NotFoundPage, RouteErrorPage } from './route-pages';
import { LandingPage, LoginPage, RegisterPage } from '../features/auth/pages';
import { DashboardPage } from '../features/dashboard/page';
import { WalletDetailPage, WalletsPage } from '../features/wallets/pages';
import { TransactionsPage } from '../features/transactions/page';
import { TransferPage } from '../features/transfers/page';
import { BudgetsPage } from '../features/budgets/page';
import { AnalyticsPage } from '../features/analytics/page';
import { NotificationsPage } from '../features/notifications/page';
import { ProfilePage } from '../features/profile/page';
import { AdminUsersPage, AuditLogsPage } from '../features/admin/pages';

export const router = createBrowserRouter([
  { path: '/', element: <LandingPage />, errorElement: <RouteErrorPage /> },
  { path: '/login', element: <LoginPage />, errorElement: <RouteErrorPage /> },
  { path: '/register', element: <RegisterPage />, errorElement: <RouteErrorPage /> },
  {
    element: <ProtectedRoute />,
    errorElement: <RouteErrorPage />,
    children: [
      {
        element: <AppShell />,
        children: [
          { path: '/dashboard', element: <DashboardPage /> },
          { path: '/wallets', element: <WalletsPage /> },
          { path: '/wallets/:walletId', element: <WalletDetailPage /> },
          { path: '/transactions', element: <TransactionsPage /> },
          { path: '/transfer', element: <TransferPage /> },
          { path: '/budgets', element: <BudgetsPage /> },
          { path: '/analytics', element: <AnalyticsPage /> },
          { path: '/notifications', element: <NotificationsPage /> },
          { path: '/profile', element: <ProfilePage /> },
          {
            element: <AdminRoute />,
            children: [
              { path: '/admin', element: <Navigate to="/admin/users" replace /> },
              { path: '/admin/users', element: <AdminUsersPage /> },
              { path: '/admin/audit-logs', element: <AuditLogsPage /> }
            ]
          }
        ]
      }
    ]
  },
  { path: '*', element: <NotFoundPage /> }
]);
