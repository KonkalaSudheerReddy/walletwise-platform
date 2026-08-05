import { HttpResponse, http } from 'msw';
import { authResponse } from '../test/handlers';
import { server } from '../test/server';
import { apiRequest } from './client';

test('concurrent 401 responses share one refresh and retry once', async () => {
  let refreshCalls = 0;
  server.use(
    http.get('http://localhost/api/v1/protected/:id', ({ request, params }) => {
      if (request.headers.get('Authorization') === 'Bearer test-access-token') {
        return HttpResponse.json({ id: params.id });
      }
      return HttpResponse.json({ title: 'Unauthorized', status: 401, detail: 'Expired' }, { status: 401 });
    }),
    http.post('http://localhost/api/v1/auth/refresh', () => {
      refreshCalls += 1;
      return HttpResponse.json(authResponse);
    })
  );

  const [first, second] = await Promise.all([
    apiRequest<{ id: string }>('/api/v1/protected/one'),
    apiRequest<{ id: string }>('/api/v1/protected/two')
  ]);

  expect(first.id).toBe('one');
  expect(second.id).toBe('two');
  expect(refreshCalls).toBe(1);
});
