create table roles (
    id smallint generated always as identity,
    name varchar(30) not null,

    constraint pk_roles primary key (id),
    constraint uq_roles_name unique (name)
);

create table users (
    id bigint generated always as identity,
    email varchar(255) not null,
    phone varchar(20) not null,
    password_hash varchar(255) not null,
    full_name varchar(150) not null,
    avatar_key varchar(512),
    status varchar(20) not null default 'ACTIVE',
    email_verified boolean not null default false,
    version bigint not null default 0,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    deleted_at timestamptz,

    constraint pk_users primary key (id),
    constraint uq_email unique (email),
    constraint uq_phone unique (phone),
    constraint ck_status check ( status in ('ACTIVE', 'LOCKED') )
);

create table user_roles (
    user_id bigint not null,
    role_id smallint not null,

    constraint pk_user_roles primary key (user_id, role_id),
    constraint fk_user_roles_user foreign key (user_id) references users(id) on delete cascade,
    constraint fk_user_roles_role foreign key (role_id) references roles(id) on delete cascade
);
create index idx_user_roles_role_id on user_roles (role_id);

create table addresses (
    id bigint generated always as identity,
    user_id bigint not null,
    receiver_name varchar(150) not null,
    phone varchar(20) not null,
    province_code varchar(20) not null,
    ward_code varchar(20) not null,
    street varchar(255) not null,
    is_default boolean not null default false,
    version bigint not null default 0,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),

    constraint pk_addresses primary key (id),
    constraint fk_addresses_user foreign key (user_id) references users(id)
);
create index idx_address_user_id on addresses (user_id);
create unique index uq_addresses_default_per_user on addresses (user_id) where is_default = true;