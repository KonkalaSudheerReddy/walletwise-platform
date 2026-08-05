import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { HttpResponse, http } from 'msw';
import { renderWithProviders } from '../../test/render';
import { server } from '../../test/server';
import { TransferPage } from './page';

test('an uncertain transfer retry reuses the same idempotency key and body', async () => {
  const keys: string[] = [];
  const bodies: string[] = [];
  let attempts = 0;
  server.use(
    http.get('http://localhost/api/v1/wallets', () =>
      HttpResponse.json([
        { id: 'source', name: 'Everyday', type: 'BANK', currency: 'USD', balance: 500, archived: false, createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z' },
        { id: 'destination', name: 'Savings', type: 'SAVINGS', currency: 'USD', balance: 100, archived: false, createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z' }
      ])
    ),
    http.get('http://localhost/api/v1/transfers', () =>
      HttpResponse.json({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0, first: true, last: true })
    ),
    http.post('http://localhost/api/v1/transfers', async ({ request }) => {
      attempts += 1;
      keys.push(request.headers.get('Idempotency-Key') ?? '');
      bodies.push(await request.text());
      if (attempts === 1) {
        return HttpResponse.json({ title: 'Gateway timeout', status: 503, detail: 'Transfer outcome is not yet known.' }, { status: 503 });
      }
      return HttpResponse.json({ id: 'transfer-1', sourceWalletId: 'source', destinationWalletId: 'destination', amount: 10, currency: 'USD', status: 'COMPLETED', note: 'Save', createdAt: '2026-08-01T00:00:00Z', completedAt: '2026-08-01T00:00:01Z' });
    })
  );

  const user = userEvent.setup();
  renderWithProviders(<TransferPage />);
  await screen.findByRole('heading', { name: 'Transfer' });
  await user.selectOptions(screen.getByLabelText('Source wallet'), 'source');
  await user.selectOptions(screen.getByLabelText('Destination wallet'), 'destination');
  await user.type(screen.getByLabelText('Amount'), '10.00');
  await user.type(screen.getByLabelText('Note (optional)'), 'Save');
  await user.click(screen.getByRole('button', { name: 'Review transfer' }));
  await user.click(screen.getByRole('button', { name: 'Confirm transfer' }));
  await screen.findByRole('button', { name: 'Retry safely' });
  await user.click(screen.getByRole('button', { name: 'Retry safely' }));

  expect(await screen.findByRole('heading', { name: 'Transfer complete' })).toBeVisible();
  await waitFor(() => expect(keys).toHaveLength(2));
  expect(keys[0]).toBeTruthy();
  expect(keys[1]).toBe(keys[0]);
  expect(bodies[1]).toBe(bodies[0]);
});
