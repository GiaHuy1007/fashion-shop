create table categories (
    id bigint generated always as identity,
    parent_id bigint,
    name varchar(150) not null,
    slug varchar(180) not null,
    path varchar(255),
    sort_order int not null default 0,
    active boolean not null default true,
    version bigint not null default 0,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),

    constraint pk_categories primary key (id),
    constraint uk_categories_slug unique (slug),
    constraint fk_categories_id foreign key (parent_id) references categories(id)
);
create index idx_categories_parent_id on categories(parent_id);
create index idx_categories_path on categories(path);

create table brands (
    id bigint generated always as identity,
    name varchar(150) not null,
    slug varchar(180) not null,
    version bigint not null default 0,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),

    constraint pk_brands primary key (id),
    constraint uk_brands_slug unique (slug)
);

create table colors (
    id smallint generated always as identity,
    code varchar(20) not null,
    name varchar(50) not null,
    hex char(7) not null,

    constraint pk_colors primary key (id),
    constraint uk_colors_code unique (code)
);

create table sizes (
    id smallint generated always as identity,
    size_group varchar(20) not null,
    code varchar(10) not null,
    sort_order smallint not null default 0,

    constraint pk_sizes primary key (id),
    constraint uk_sizes_size_group_code unique (size_group, code),
    constraint ck_sizes_size_group check ( size_group in ('TOP', 'BOTTOM', 'SHOES', 'FREE'))
);

create table products (
    id bigint generated always as identity,
    category_id bigint not null,
    brand_id bigint,
    name varchar(255) not null,
    slug varchar(280) not null,
    description text,
    gender varchar(10) not null,
    material varchar(120),
    base_price numeric (14, 2) not null,
    min_price numeric (14, 2),
    max_price numeric (14, 2),
    attributes jsonb not null default '{}'::jsonb,
    size_chart jsonb,
    status varchar(20) not null default 'DRAFT',
    avg_rating numeric(2, 1) not null default 0,
    review_count int not null default 0,
    sold_count int not null default 0,
    version bigint not null default 0,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    deleted_at timestamptz,

    constraint pk_products primary key (id),
    constraint uk_products_slug unique (slug),
    constraint fk_products_category_id foreign key (category_id) references categories(id) on delete restrict,
    constraint fk_products_brand_id foreign key (brand_id) references brands(id),
    constraint ck_products_gender check ( gender in ('MEN', 'WOMEN', 'UNISEX', 'KIDS') ),
    constraint ck_products_status check ( status in ('DRAFT', 'ACTIVE', 'INACTIVE') ),
    constraint ck_products_base_price check ( base_price >= 0 )
);
create index idx_products_listing on products(category_id, status, created_at desc) where deleted_at is null;
create index idx_products_attributes on products using gin (attributes jsonb_path_ops);

create table product_colors (
    id bigint generated always as identity,
    product_id bigint not null,
    color_id smallint not null,
    sort_order int not null default 0,

    constraint pk_product_colors primary key (id),
    constraint uk_product_colors_product_id_color_id unique (product_id, product_id),
    constraint fk_product_colors_product_id foreign key (product_id) references products(id) on delete cascade,
    constraint fk_product_colors_color_id foreign key (color_id) references colors(id) on delete restrict
);

create table product_variants (
    id bigint generated always as identity,
    product_id bigint not null,
    product_color_id bigint not null,
    size_id smallint not null,
    sku varchar(64) not null,
    barcode varchar(32),
    price numeric(14, 2) not null,
    compare_at_price numeric(14, 2),
    weight_gram int,
    status varchar(20) not null default 'ACTIVE',
    version bigint not null default 0,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),

    constraint pk_product_variants primary key (id),
    constraint uk_product_variants_sku unique (sku),
    constraint uk_product_variants_barcode unique (barcode),
    constraint uk_product_variants_product_id_product_color_product_size unique (product_id, product_color_id, size_id),
    constraint fk_product_variants_product_id foreign key (product_id) references products(id) on delete restrict,
    constraint fk_product_variants_product_color_id foreign key (product_color_id) references product_colors(id) on delete restrict,
    constraint fk_product_variants_size_id foreign key (size_id) references sizes(id) on delete restrict,
    constraint ck_product_variants_price check ( price >= 0 ),
    constraint ck_product_variants_status check ( status in ('ACTIVE', 'INACTIVE') )
);
create index idx_product_variants on product_variants(product_id) where status ='ACTIVE';

create table product_images (
    id bigint generated always as identity,
    product_id bigint not null,
    product_color_id bigint,
    object_key varchar(512) not null,
    media_type varchar(10) not null default 'IMAGE',
    content_type varchar(50),
    size_bytes bigint,
    width int,
    height int,
    sort_order int not null default 0,
    is_primary boolean not null default false,
    status varchar(10) not null default 'PENDING',
    created_at timestamptz not null default now(),

    constraint pk_product_images primary key (id),
    constraint uk_product_images_object_key unique (object_key),
    constraint fk_product_images_product_id foreign key (product_id) references products(id) on delete cascade,
    constraint fk_product_images_product_color_id foreign key (product_color_id) references product_colors(id) on delete cascade,
    constraint ck_product_images_media_type check ( media_type in ('IMAGE', 'VIDEO') ),
    constraint ck_product_images_status check ( status in ('PENDING', 'ACTIVE') )
);
create unique index uq_product_images_primary_image_per_color on product_images(product_id, coalesce(product_color_id, 0)) where is_primary = true;