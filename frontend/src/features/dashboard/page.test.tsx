import { screen } from '@testing-library/react';
import { HttpResponse, http } from 'msw';
import { renderWithProviders } from '../../test/render';
import { server } from '../../test/server';
import { DashboardPage } from './page';

test('dashboard renders authoritative monthly aggregates and activity', async () => {
  renderWithProviders(<DashboardPage />);

  expect(await screen.findByRole('heading', { name: 'Dashboard' })).toBeVisible();
  expect(screen.getByText('Closing balance')).toBeVisible();
  expect(screen.getByText('Weekly groceries')).toBeVisible();
  expect(screen.getByText('Budget progress')).toBeVisible();
  expect(screen.getByText('$3,459.75')).toBeVisible();
});

test('dashboard shows a useful RFC 7807 API failure', async () => {
  server.use(
    http.get('http://localhost/api/v1/analytics/monthly', () =>
      HttpResponse.json(
        {
          type: 'https://walletwise.app/problems/service-unavailable',
          title: 'Service unavailable',
          status: 503,
          detail: 'Analytics are temporarily unavailable.',
          correlationId: 'test-correlation-id'
        },
        { status: 503 }
      )
    )
  );

  renderWithProviders(<DashboardPage />);
  expect(await screen.findByRole('alert')).toHaveTextContent(
    'Analytics are temporarily unavailable.'
  );
  expect(screen.getByRole('alert')).toHaveTextContent('test-correlation-id');
});
