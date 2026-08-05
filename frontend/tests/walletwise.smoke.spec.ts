import { expect, test } from '@playwright/test';

test.describe.configure({ mode: 'serial' });

test('demo user completes the core WalletWise journey', async ({ page }) => {
  const runId = Date.now().toString();
  const incomeNote = `E2E income ${runId}`;
  const expenseNote = `E2E expense ${runId}`;

  await page.goto('/login', { waitUntil: 'domcontentloaded' });
  await expect(page.getByRole('heading', { name: 'Welcome back' })).toBeVisible();
  await page.getByRole('button', { name: 'Use demo account' }).click();
  await page.getByRole('button', { name: 'Sign in' }).click();
  await expect(page.getByRole('heading', { name: 'Dashboard' })).toBeVisible();

  await page.getByRole('link', { name: 'Transactions' }).click();
  await page.getByRole('button', { name: 'Add income' }).click();
  await page.getByLabel('Wallet').selectOption({ index: 1 });
  await page.getByLabel('Amount').fill('25.00');
  await page.getByLabel('Category').selectOption({ index: 1 });
  await page.getByLabel('Description').fill(incomeNote);
  await page.getByRole('button', { name: 'Record income' }).click();
  await expect(page.getByText(incomeNote)).toBeVisible();

  await page.getByRole('button', { name: 'Add expense' }).click();
  await page.getByLabel('Wallet').selectOption({ index: 1 });
  await page.getByLabel('Amount').fill('1.00');
  await page.getByLabel('Category').selectOption({ index: 1 });
  await page.getByLabel('Description').fill(expenseNote);
  await page.getByRole('button', { name: 'Record expense' }).click();
  await expect(page.getByText(expenseNote)).toBeVisible();

  await page.getByRole('link', { name: 'Transfer' }).click();
  await page.getByLabel('Source wallet').selectOption({ index: 1 });
  await page.getByLabel('Destination wallet').selectOption({ index: 1 });
  await page.getByLabel('Amount').fill('0.50');
  await page.getByLabel('Note (optional)').fill(`E2E transfer ${runId}`);
  await page.getByRole('button', { name: 'Review transfer' }).click();
  await page.getByRole('button', { name: 'Confirm transfer' }).click();
  await expect(page.getByRole('heading', { name: 'Transfer complete' })).toBeVisible();

  await page.getByRole('link', { name: 'Transactions' }).click();
  await page.getByLabel('Search descriptions').fill(expenseNote);
  await expect(page.getByText(expenseNote)).toBeVisible();

  await page.getByRole('link', { name: 'Analytics' }).click();
  await expect(page.getByRole('heading', { name: 'Analytics' })).toBeVisible();
  await expect(page.getByText('Income and expenses')).toBeVisible();

  await page.getByRole('button', { name: 'Log out' }).click();
  await expect(page.getByRole('heading', { name: 'Welcome back' })).toBeVisible();
});
