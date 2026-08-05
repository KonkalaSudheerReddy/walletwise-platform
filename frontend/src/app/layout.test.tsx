import { screen } from '@testing-library/react';
import { HttpResponse, http } from 'msw';
import { Route, Routes } from 'react-router-dom';
import { AuthProvider } from './auth';
import { ProtectedRoute } from './layout';
import { authResponse } from '../test/handlers';
import { renderWithProviders } from '../test/render';
import { server } from '../test/server';

test('protected routes wait for refresh and then render authenticated content', async () => {
  server.use(
    http.post('http://localhost/api/v1/auth/refresh', () => HttpResponse.json(authResponse))
  );

  renderWithProviders(
    <AuthProvider>
      <Routes>
        <Route element={<ProtectedRoute />}>
          <Route path="/secure" element={<h1>Secure wallet content</h1>} />
        </Route>
        <Route path="/login" element={<h1>Login screen</h1>} />
      </Routes>
    </AuthProvider>,
    { route: '/secure' }
  );

  expect(screen.getByText(/Restoring your secure session/)).toBeVisible();
  expect(await screen.findByRole('heading', { name: 'Secure wallet content' })).toBeVisible();
  expect(screen.queryByRole('heading', { name: 'Login screen' })).not.toBeInTheDocument();
});
