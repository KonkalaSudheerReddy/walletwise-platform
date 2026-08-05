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
  const transactionTable = page.getByRole('table');
  await page.getByRole('button', { name: 'Add income' }).click();
  const incomeDialog = page.getByRole('dialog', { name: 'Add income' });
  await incomeDialog.getByLabel('Wallet', { exact: true }).selectOption({ index: 1 });
  await incomeDialog.getByLabel('Amount', { exact: true }).fill('25.00');
  await incomeDialog.getByLabel('Category', { exact: true }).selectOption({ index: 1 });
  await incomeDialog.getByLabel('Description', { exact: true }).fill(incomeNote);
  await incomeDialog.getByRole('button', { name: 'Record income' }).click();
  await expect(transactionTable.getByText(incomeNote, { exact: true })).toBeVisible();

  await page.getByRole('button', { name: 'Add expense' }).click();
  const expenseDialog = page.getByRole('dialog', { name: 'Add expense' });
  await expenseDialog.getByLabel('Wallet', { exact: true }).selectOption({ index: 1 });
  await expenseDialog.getByLabel('Amount', { exact: true }).fill('1.00');
  await expenseDialog.getByLabel('Category', { exact: true }).selectOption({ index: 1 });
  await expenseDialog.getByLabel('Description', { exact: true }).fill(expenseNote);
  await expenseDialog.getByRole('button', { name: 'Record expense' }).click();
  await expect(transactionTable.getByText(expenseNote, { exact: true })).toBeVisible();

  await page.getByRole('link', { name: 'Transfer' }).click();
  await page.getByLabel('Source wallet', { exact: true }).selectOption({ index: 1 });
  await page.getByLabel('Destination wallet', { exact: true }).selectOption({ index: 1 });
  await page.getByLabel('Amount', { exact: true }).fill('0.50');
  await page.getByLabel('Note (optional)', { exact: true }).fill(`E2E transfer ${runId}`);
  await page.getByRole('button', { name: 'Review transfer' }).click();
  await page.getByRole('button', { name: 'Confirm transfer' }).click();
  await expect(page.getByRole('heading', { name: 'Transfer complete' })).toBeVisible();

  await page.getByRole('link', { name: 'Transactions' }).click();
  await page.getByLabel('Search descriptions').fill(expenseNote);
  await expect(page.getByRole('table').getByText(expenseNote, { exact: true })).toBeVisible();

  await page.getByRole('link', { name: 'Analytics' }).click();
  await expect(page.getByRole('heading', { name: 'Analytics' })).toBeVisible();
  await expect(page.getByText('Income and expenses')).toBeVisible();

  await page.getByRole('button', { name: 'Log out' }).click();
  await expect(page.getByRole('heading', { name: 'Welcome back' })).toBeVisible();
});
