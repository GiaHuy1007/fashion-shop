create table vouchers (
    id bigint generated always as identity,
    code varchar(50) not null,
    type varchar(20) not null,
    value numeric(14, 2) not null,
    max_discount numeric(14, 2),
    min_order_value numeric(14, 2) not null default 0,
    usage_limit int not null,
    used_count int not null default 0,
    per_user_limit int not null default 1,
    start_at timestamptz not null,
    end_at timestamptz not null,
    active boolean not null default true,
    version bigint not null default 0,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),

    constraint pk_vouchers primary key (id),
    constraint uq_vouchers_code unique (code),
    constraint chk_vouchers_type check ( type in ('PERCENT', 'FIXED', 'FREESHIP') ),
    constraint chk_vouchers_value check ( value > 0 ),
    constraint chk_vouchers_min_order_value check ( min_order_value >= 0 ),
    constraint chk_vouchers_usage_limit check ( usage_limit > 0 ),
    constraint chk_vouchers_used_count check ( used_count between 0 and usage_limit ),
    constraint chk_vouchers_per_user_limit check ( per_user_limit > 0 ),
    constraint chk_vouchers_valid_date_range check ( start_at < end_at )
);

create table orders (
    id bigint generated always as identity,
    order_code varchar(20) not null,
    user_id bigint not null,
    status varchar(30) not null,
    payment_method varchar(20) not null,
    payment_status varchar(20) not null default 'UNPAID',
    subtotal numeric(14, 2) not null,
    discount_amount numeric(14, 2) not null default 0,
    shipping_fee numeric(14, 2) not null default 0,
    total_amount numeric(14, 2) not null,
    currency char(3) not null default 'VND',
    voucher_id bigint,
    voucher_code varchar(50),
    shipping_address jsonb not null,
    note varchar(500),
    idempotency_key varchar(64) not null,
    expires_at timestamptz,
    paid_at timestamptz,
    cancelled_at timestamptz,
    cancel_reason varchar(255),
    version bigint not null default 0,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),

    constraint pk_orders primary key (id),
    constraint fk_orders_user_id foreign key (user_id) references users(id) on delete restrict,
    constraint fk_orders_voucher_id foreign key (voucher_id) references vouchers(id) on delete restrict,
    constraint uq_orders_order_code unique (order_code),
    constraint uq_orders_user_id_idempotency_key unique (user_id, idempotency_key),
    constraint chk_orders_status check ( status in (
        'PENDING_PAYMENT', 'PENDING_CONFIRMATION', 'PAID', 'PROCESSING',
        'SHIPPING', 'DELIVERED', 'COMPLETED', 'CANCELLED', 'EXPIRED',
        'RETURN_REQUESTED', 'RETURNED'
    ) ),
    constraint chk_orders_payment_method check ( payment_method in ('COD', 'VNPAY', 'MOMO', 'STRIPE') ),
    constraint chk_orders_subtotal check ( subtotal >= 0 ),
    constraint chk_orders_discount_amount check ( discount_amount >= 0 ),
    constraint chk_orders_shipping_fee check ( shipping_fee >= 0 ),
    constraint chk_orders_total_amount check ( total_amount >= 0 )
);
create index idx_orders_user on orders(user_id, created_at desc);
create index idx_orders_pending on orders(expires_at) where status = 'PENDING_PAYMENT';

create table voucher_usages (
    id bigint generated always as identity,
    voucher_id bigint not null,
    user_id bigint not null,
    order_id bigint not null,
    status varchar(20) not null default 'APPLIED',
    created_at timestamptz not null default now(),

    constraint pk_voucher_usages primary key (id),
    constraint fk_voucher_usages_voucher_id foreign key (voucher_id) references vouchers(id) on delete restrict,
    constraint fk_voucher_usages_user_id foreign key (user_id) references users(id) on delete restrict,
    constraint fk_voucher_usages_order_id foreign key (order_id) references orders(id) on delete restrict,
    constraint uq_voucher_usages_voucher_id_order_id unique (voucher_id, order_id),
    constraint chk_voucher_usages_status check ( status in ('APPLIED', 'REVERTED') )
);
create index idx_voucher_usages_voucher_user on voucher_usages(voucher_id, user_id) where status = 'APPLIED';

create table order_items (
    id bigint generated always as identity,
    order_id bigint not null,
    variant_id bigint not null,
    product_id bigint not null,
    sku varchar(64) not null,
    product_name varchar(255) not null,
    color_name varchar(50) not null,
    size_code varchar(10) not null,
    image_key varchar(512),
    unit_price numeric(14, 2) not null,
    quantity int not null,
    line_total numeric(14, 2) not null,
    reviewed boolean not null default false,

    constraint pk_order_items primary key (id),
    constraint fk_order_items_order_id foreign key (order_id) references orders(id) on delete cascade,
    constraint chk_order_items_unit_price check ( unit_price >= 0 ),
    constraint chk_order_items_quantity check ( quantity > 0 ),
    constraint chk_order_items_line_total check ( line_total >= 0 )
);
create index idx_order_items_order_id on order_items(order_id);

create table order_status_histories (
    id bigint generated always as identity,
    order_id bigint not null,
    from_status varchar(30),
    to_status varchar(30) not null,
    changed_by bigint,
    reason varchar(255),
    created_at timestamptz not null default now(),

    constraint pk_order_status_histories primary key (id),
    constraint fk_order_status_histories_order_id foreign key (order_id) references orders(id) on delete cascade
);
create index idx_order_status_histories_order_id on order_status_histories(order_id, created_at asc);

create table payments (
    id bigint generated always as identity,
    order_id bigint not null,
    provider varchar(20) not null,
    txn_ref varchar(64) not null,
    provider_txn_id varchar(128),
    amount numeric(14, 2) not null,
    currency char(3) not null default 'VND',
    status varchar(20) not null default 'PENDING',
    payment_url text,
    raw_response jsonb,
    failure_reason varchar(255),
    expires_at timestamptz,
    paid_at timestamptz,
    version bigint not null default 0,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),

    constraint pk_payments primary key (id),
    constraint fk_payments_order_id foreign key (order_id) references orders(id) on delete restrict,
    constraint uq_payments_txn_ref unique (txn_ref),
    constraint uq_payments_provider_provider_txn_id unique (provider, provider_txn_id),
    constraint chk_payments_provider check ( provider in ('COD', 'VNPAY', 'MOMO', 'STRIPE') ),
    constraint chk_payments_status check ( status in ('PENDING', 'SUCCESS', 'FAILED', 'EXPIRED', 'REFUNDED') ),
    constraint chk_payments_amount check ( amount >= 0 )
);
create unique index uq_payment_success_per_order on payments(order_id) where status = 'SUCCESS';
create index idx_payments_order_id on payments(order_id);

create table payment_webhook_events (
    id bigint generated always as identity,
    provider varchar(20) not null,
    event_id varchar(128) not null,
    payment_id bigint,
    signature_valid boolean not null,
    payload jsonb not null,
    process_status varchar(20) not null default 'RECEIVED',
    received_at timestamptz not null default now(),
    processed_at timestamptz,

    constraint pk_payment_webhook_events primary key (id),
    constraint fk_payment_webhook_events_payment_id foreign key (payment_id) references payments(id) on delete restrict,
    constraint uq_payment_webhook_events_provider_event_id unique (provider, event_id),
    constraint chk_payment_webhook_events_provider check ( provider in ('COD', 'VNPAY', 'MOMO', 'STRIPE') ),
    constraint chk_payment_webhook_events_process_status check ( process_status in ('RECEIVED', 'PROCESSED', 'IGNORED', 'FAILED') )
);
