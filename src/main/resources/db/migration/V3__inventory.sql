create table invetories (
    variant_id bigint not null,
    on_hand int not null default 0,
    reserved int not null default 0,
    low_stock_threshold int not null default 0,
    version bigint not null default 0,
    updated_at timestamptz not null default now(),

    constraint pk_invetories primary key (variant_id),
    constraint fk_invetories_variant_id foreign key (variant_id) references product_variants(id) on delete restrict,
    constraint ck_invetories_on_hand check ( on_hand >= 0 ),
    constraint ck_invetories_reserved check ( reserved >= 0 ),
    constraint ck_invetories_reserved_le_on_hand check ( reserved <= on_hand )
);

create table inventory_reservations (
    id bigint generated always as identity,
    order_id bigint not null,
    variant_id bigint not null,
    quantity int not null,
    status varchar(20) not null default 'RESERVED',
    expiries_at timestamptz not null,
    created_at timestamptz not null default now(),

    constraint pk_inventory_reservations primary key (id),
    constraint fk_inventory_reservations_variant_id foreign key (variant_id) references product_variants(id) on delete restrict ,
    constraint uk_inventory_reservations_order_id_variant_id unique (order_id, variant_id),
    constraint ck_inventory_reservations_quantity check ( quantity > 0 ),
    constraint ck_inventory_reservations_status check ( status in ('RESERVED', 'COMMITTED', 'RELEASED') )
);
create index idx_inventory_reservations_variant_id on inventory_reservations(variant_id);
create index idx_inventory_reservations_expiries_at on inventory_reservations(expiries_at) where status = 'RESERVED';

create table inventory_transactions (
    id bigint generated always as identity,
    variant_id bigint not null,
    type varchar(20) not null,
    delta int not null,
    ref_type varchar(30),
    ref_id bigint,
    created_at timestamptz not null default now(),

    constraint pk_inventory_transactions primary key (id),
    constraint fk_inventory_transactions_variant_id foreign key (variant_id) references product_variants(id) on delete restrict,
    constraint ck_inventory_transactions_type check ( type in ('IMPORT', 'SALE', 'RELEASE', 'RETURN', 'ADJUST') )
);

create index idx_inventory_transactions_varivant_id on inventory_transactions(variant_id, created_at desc);
create index idx_inventory_transactions_ref on inventory_transactions(ref_type, ref_id);