create table search_outbox (
    id bigint generated always as identity,
    aggregate_type varchar(20) not null,
    aggregate_id bigint not null,
    event_type varchar(40) not null,
    status varchar(12) not null default 'PENDING',
    attempts int not null default 0,
    next_retry_at timestamptz not null default now(),
    locked_at timestamptz,
    last_error varchar(500),
    created_at timestamptz not null default now(),

    constraint pk_search_outbox primary key (id),
    constraint chk_search_outbox_aggregate_type check ( aggregate_type in ('PRODUCT', 'CATEGORY', 'BRAND') ),
    constraint chk_search_outbox_status check ( status in ('PENDING', 'PROCESSING', 'DONE', 'DEAD') ),
    constraint chk_search_outbox_attempts check ( attempts >= 0 )
);
create index idx_search_outbox_pending on search_outbox(next_retry_at, id) where status = 'PENDING';

create extension if not exists pg_trgm;
create extension if not exists unaccent;

create or replace function f_unaccent(text) returns text
    language sql immutable parallel safe as $$ select public.unaccent('public.unaccent', $1) $$;

create index idx_products_name_trgm on products using gin (f_unaccent(lower(name)) gin_trgm_ops)
    where deleted_at is null;
