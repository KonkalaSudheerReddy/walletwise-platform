import { render, screen } from '@testing-library/react';
import { SelectField, TextField } from './ui';

test('form fields expose concise accessible names', () => {
  render(
    <>
      <SelectField label="Wallet" defaultValue="">
        <option value="">Choose a wallet</option>
        <option value="checking">Everyday Checking</option>
      </SelectField>
      <TextField label="Amount" hint="Enter a positive value." />
    </>
  );

  expect(screen.getByRole('combobox', { name: 'Wallet' })).toBeVisible();
  expect(screen.getByRole('textbox', { name: 'Amount' })).toBeVisible();
});
