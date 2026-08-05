import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { AuthProvider } from '../../app/auth';
import { renderWithProviders } from '../../test/render';
import { LoginPage } from './pages';

test('login form reports accessible validation errors and fills demo credentials', async () => {
  const user = userEvent.setup();
  renderWithProviders(
    <AuthProvider>
      <LoginPage />
    </AuthProvider>,
    { route: '/login' }
  );

  await screen.findByRole('heading', { name: 'Welcome back' });
  await user.click(screen.getByRole('button', { name: 'Sign in' }));
  expect(await screen.findByText('Enter a valid email address.')).toBeVisible();
  expect(screen.getByText('Enter your password.')).toBeVisible();

  await user.click(screen.getByRole('button', { name: 'Use demo account' }));
  await waitFor(() => {
    expect(screen.getByLabelText('Email address')).toHaveValue('demo@walletwise.app');
    expect(screen.getByLabelText('Password')).toHaveValue('Demo@12345');
  });
});
