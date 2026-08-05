import { AlertTriangle, Home } from 'lucide-react';
import { Link, isRouteErrorResponse, useRouteError } from 'react-router-dom';
import { Button, Card, WalletWiseLogo } from '../components/ui';

export function RouteErrorPage() {
  const error = useRouteError();
  const status = isRouteErrorResponse(error) ? error.status : 500;
  const message = isRouteErrorResponse(error)
    ? error.statusText || 'The requested page is unavailable.'
    : error instanceof Error
      ? error.message
      : 'An unexpected page error occurred.';

  return (
    <main className="grid min-h-screen place-items-center bg-slate-50 p-5 dark:bg-slate-950">
      <Card className="w-full max-w-lg p-8 text-center">
        <div className="flex justify-center">
          <WalletWiseLogo />
        </div>
        <span className="mx-auto mt-8 grid h-14 w-14 place-items-center rounded-2xl bg-amber-100 text-amber-700 dark:bg-amber-950 dark:text-amber-200">
          <AlertTriangle className="h-7 w-7" />
        </span>
        <p className="mt-5 text-sm font-semibold text-teal-700">Error {status}</p>
        <h1 className="mt-2 text-2xl font-semibold">We could not open this page</h1>
        <p className="mt-2 text-sm text-slate-500">{message}</p>
        <Link to="/dashboard" className="mt-7 inline-block">
          <Button>
            <Home className="h-4 w-4" /> Return to dashboard
          </Button>
        </Link>
      </Card>
    </main>
  );
}

export function NotFoundPage() {
  return (
    <main className="grid min-h-screen place-items-center bg-slate-50 p-5 dark:bg-slate-950">
      <Card className="w-full max-w-lg p-8 text-center">
        <p className="text-sm font-semibold text-teal-700">404</p>
        <h1 className="mt-2 text-3xl font-semibold">Page not found</h1>
        <p className="mt-3 text-slate-500">
          The address may be incorrect or the page may have moved.
        </p>
        <Link to="/" className="mt-7 inline-block">
          <Button>
            <Home className="h-4 w-4" /> Go home
          </Button>
        </Link>
      </Card>
    </main>
  );
}
