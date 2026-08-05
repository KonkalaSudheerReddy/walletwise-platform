create table app_users (
    id uuid primary key,
    display_name varchar(100) not null,
    email_normalized varchar(320) not null unique,
    password_hash varchar(100) not null,
    role varchar(20) not null check (role in ('USER', 'ADMIN')),
    enabled boolean not null,
    preferred_currency varchar(3) not null check (preferred_currency ~ '^[A-Z]{3}$'),
    created_at timestamptz not null,
    updated_at timestamptz not null,
    check (email_normalized = lower(email_normalized))
);

create table refresh_tokens (
    id uuid primary key,
    user_id uuid not null references app_users(id) on delete restrict,
    token_hash varchar(64) not null unique,
    family_id uuid not null,
    expires_at timestamptz not null,
    revoked_at timestamptz,
    replaced_by_id uuid,
    created_at timestamptz not null,
    constraint fk_refresh_replacement foreign key (replaced_by_id) references refresh_tokens(id) on delete set null
);

create index idx_refresh_user_active on refresh_tokens(user_id, expires_at) where revoked_at is null;
create index idx_refresh_family on refresh_tokens(family_id);

create table categories (
    id uuid primary key,
    name varchar(80) not null,
    normalized_name varchar(80) not null,
    type varchar(20) not null check (type in ('INCOME', 'EXPENSE')),
    active boolean not null default true,
    unique (type, normalized_name)
);

create table wallets (
    id uuid primary key,
    owner_id uuid not null references app_users(id) on delete restrict,
    name varchar(100) not null,
    wallet_type varchar(20) not null check (wallet_type in ('CASH', 'BANK', 'SAVINGS', 'CREDIT', 'OTHER')),
    currency varchar(3) not null check (currency ~ '^[A-Z]{3}$'),
    current_balance numeric(19,4) not null,
    archived boolean not null default false,
    version bigint not null default 0,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create index idx_wallet_owner_archived on wallets(owner_id, archived, created_at desc);

create table idempotency_records (
    id uuid primary key,
    owner_id uuid not null references app_users(id) on delete restrict,
    operation varchar(80) not null,
    idempotency_key varchar(128) not null,
    request_hash varchar(64) not null,
    status varchar(20) not null check (status in ('PROCESSING', 'COMPLETED')),
    response_status integer,
    response_resource_id uuid,
    created_at timestamptz not null,
    expires_at timestamptz not null,
    unique (owner_id, operation, idempotency_key),
    check ((status = 'PROCESSING' and response_status is null and response_resource_id is null)
        or (status = 'COMPLETED' and response_status is not null and response_resource_id is not null))
);

create index idx_idempotency_expiry on idempotency_records(expires_at);

create table transfers (
    id uuid primary key,
    owner_id uuid not null references app_users(id) on delete restrict,
    source_wallet_id uuid not null references wallets(id) on delete restrict,
    destination_wallet_id uuid not null references wallets(id) on delete restrict,
    amount numeric(19,4) not null check (amount > 0),
    currency varchar(3) not null check (currency ~ '^[A-Z]{3}$'),
    status varchar(20) not null check (status in ('PENDING', 'COMPLETED', 'FAILED')),
    note varchar(500),
    idempotency_key varchar(128) not null,
    created_at timestamptz not null,
    completed_at timestamptz,
    check (source_wallet_id <> destination_wallet_id)
);

create index idx_transfer_owner_created on transfers(owner_id, created_at desc);

create table ledger_entries (
    id uuid primary key,
    wallet_id uuid not null references wallets(id) on delete restrict,
    owner_id uuid not null references app_users(id) on delete restrict,
    type varchar(30) not null check (type in ('OPENING_BALANCE', 'INCOME', 'EXPENSE', 'TRANSFER_IN', 'TRANSFER_OUT', 'ADJUSTMENT')),
    direction varchar(10) not null check (direction in ('CREDIT', 'DEBIT')),
    amount numeric(19,4) not null check (amount > 0),
    category_id uuid references categories(id) on delete restrict,
    description varchar(500),
    occurred_at timestamptz not null,
    transfer_id uuid references transfers(id) on delete restrict,
    balance_after numeric(19,4) not null,
    created_at timestamptz not null,
    check ((type in ('INCOME', 'EXPENSE') and category_id is not null) or type not in ('INCOME', 'EXPENSE')),
    check ((type in ('TRANSFER_IN', 'TRANSFER_OUT') and transfer_id is not null) or type not in ('TRANSFER_IN', 'TRANSFER_OUT'))
);

create index idx_ledger_owner_occurred on ledger_entries(owner_id, occurred_at desc, id desc);
create index idx_ledger_wallet_occurred on ledger_entries(wallet_id, occurred_at desc);
create index idx_ledger_owner_type_date on ledger_entries(owner_id, type, occurred_at);
create index idx_ledger_owner_category_date on ledger_entries(owner_id, category_id, occurred_at);
create index idx_ledger_transfer on ledger_entries(transfer_id) where transfer_id is not null;

create table budgets (
    id uuid primary key,
    owner_id uuid not null references app_users(id) on delete restrict,
    category_id uuid not null references categories(id) on delete restrict,
    period_start date not null,
    limit_amount numeric(19,4) not null check (limit_amount > 0),
    alert_threshold_percent integer not null check (alert_threshold_percent between 1 and 99),
    created_at timestamptz not null,
    updated_at timestamptz not null,
    unique (owner_id, category_id, period_start),
    check (extract(day from period_start) = 1)
);

create index idx_budget_owner_period on budgets(owner_id, period_start desc);

create table notifications (
    id uuid primary key,
    owner_id uuid not null references app_users(id) on delete restrict,
    type varchar(40) not null check (type in ('BUDGET_APPROACHING', 'BUDGET_REACHED', 'BUDGET_EXCEEDED')),
    title varchar(160) not null,
    message varchar(500) not null,
    related_resource_id uuid,
    budget_id uuid references budgets(id) on delete restrict,
    read_at timestamptz,
    created_at timestamptz not null
);

create unique index uq_notification_budget_threshold on notifications(budget_id, type) where budget_id is not null;
create index idx_notification_owner_unread on notifications(owner_id, created_at desc) where read_at is null;
create index idx_notification_owner_created on notifications(owner_id, created_at desc);

create table audit_logs (
    id uuid primary key,
    actor_user_id uuid references app_users(id) on delete restrict,
    action varchar(80) not null,
    resource_type varchar(80) not null,
    resource_id uuid,
    outcome varchar(20) not null check (outcome in ('SUCCESS', 'FAILURE')),
    occurred_at timestamptz not null,
    correlation_id varchar(64),
    client_ip varchar(45),
    user_agent varchar(512),
    metadata_json text,
    check (metadata_json is null or jsonb_typeof(metadata_json::jsonb) is not null)
);

create index idx_audit_occurred on audit_logs(occurred_at desc);
create index idx_audit_actor_occurred on audit_logs(actor_user_id, occurred_at desc);
create index idx_audit_filters on audit_logs(action, resource_type, outcome, occurred_at desc);
