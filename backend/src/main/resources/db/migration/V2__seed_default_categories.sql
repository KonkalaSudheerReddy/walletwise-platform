insert into categories (id, name, normalized_name, type, active) values
('10000000-0000-0000-0000-000000000001', 'Salary', 'salary', 'INCOME', true),
('10000000-0000-0000-0000-000000000002', 'Freelance', 'freelance', 'INCOME', true),
('10000000-0000-0000-0000-000000000003', 'Interest', 'interest', 'INCOME', true),
('10000000-0000-0000-0000-000000000004', 'Gift', 'gift', 'INCOME', true),
('10000000-0000-0000-0000-000000000005', 'Other Income', 'other income', 'INCOME', true),
('20000000-0000-0000-0000-000000000001', 'Housing', 'housing', 'EXPENSE', true),
('20000000-0000-0000-0000-000000000002', 'Groceries', 'groceries', 'EXPENSE', true),
('20000000-0000-0000-0000-000000000003', 'Dining', 'dining', 'EXPENSE', true),
('20000000-0000-0000-0000-000000000004', 'Transport', 'transport', 'EXPENSE', true),
('20000000-0000-0000-0000-000000000005', 'Utilities', 'utilities', 'EXPENSE', true),
('20000000-0000-0000-0000-000000000006', 'Health', 'health', 'EXPENSE', true),
('20000000-0000-0000-0000-000000000007', 'Entertainment', 'entertainment', 'EXPENSE', true),
('20000000-0000-0000-0000-000000000008', 'Shopping', 'shopping', 'EXPENSE', true),
('20000000-0000-0000-0000-000000000009', 'Education', 'education', 'EXPENSE', true),
('20000000-0000-0000-0000-000000000010', 'Travel', 'travel', 'EXPENSE', true),
('20000000-0000-0000-0000-000000000011', 'Other Expense', 'other expense', 'EXPENSE', true)
on conflict (type, normalized_name) do nothing;
