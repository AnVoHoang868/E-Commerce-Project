jdbc:postgresql://aws-1-ap-southeast-2.pooler.supabase.com:6543/tmdt1acreate table tbl_accounts
(
    id         serial
        primary key,
    username   varchar(50)                                                    not null
        unique,
    password   varchar(255)                                                   not null,
    role       varchar(10)              default 'CUSTOMER'::character varying not null
        constraint chk_accounts_role
            check ((role)::text = ANY
                   ((ARRAY ['CUSTOMER'::character varying, 'ADMIN'::character varying, 'EMPLOYEE'::character varying])::text[])),
    email      varchar(100)                                                   not null
        unique
        constraint ukoshwg9cg1y475p5ppsip091ed
            unique,
    verify     boolean                  default false                         not null,
    status     varchar(10)              default 'ACTIVE'::character varying   not null
        constraint chk_accounts_status
            check ((status)::text = ANY
                   ((ARRAY ['ACTIVE'::character varying, 'INACTIVE'::character varying, 'BANNED'::character varying])::text[])),
    created_at timestamp with time zone default CURRENT_TIMESTAMP,
    updated_at timestamp with time zone default CURRENT_TIMESTAMP
);

alter table tbl_accounts
    owner to postgres;

create table tbl_users
(
    id         uuid                     default gen_random_uuid() not null
        primary key,
    f_name     varchar(255),
    l_name     varchar(255),
    avatar     varchar(255),
    acc_id     integer,
    created_at timestamp with time zone default CURRENT_TIMESTAMP,
    updated_at timestamp with time zone default CURRENT_TIMESTAMP
);

alter table tbl_users
    owner to postgres;

create table tbl_recivers
(
    id       serial
        primary key,
    f_name   varchar(100),
    l_name   varchar(100),
    phone    varchar(10)  not null,
    country  varchar(20)  not null,
    province varchar(20)  not null,
    district varchar(20)  not null,
    street   varchar(100) not null,
    detail   varchar(255),
    user_id  uuid         not null
        constraint fk_receiver_user
            references tbl_users
);

alter table tbl_recivers
    owner to postgres;

create table tbl_categories
(
    id          serial
        primary key,
    name        varchar(50)                           not null,
    code        varchar(10)                           not null,
    img_url     varchar(255),
    description varchar(50),
    status      varchar(20)              default 'ACTIVE'::character varying
        constraint chk_category_status
            check ((status)::text = ANY ((ARRAY ['ACTIVE'::character varying, 'HIDDEN'::character varying])::text[])),
    parent_id   integer
        constraint fk_category_parent
            references tbl_categories
            on delete set null,
    is_leaf     boolean                  default true not null,
    level       integer                  default 0    not null,
    path        varchar(255),
    version     bigint                   default 0    not null,
    created_at  timestamp with time zone default CURRENT_TIMESTAMP,
    updated_at  timestamp with time zone default CURRENT_TIMESTAMP
);

alter table tbl_categories
    owner to postgres;

create table tbl_providers
(
    id          serial
        primary key,
    name        varchar(50)                        not null,
    code        varchar(10)                        not null,
    description varchar(255),
    email       varchar(100),
    phone       varchar(15),
    status      varchar(20)              default 'ACTIVE'::character varying
        constraint chk_provider_status
            check ((status)::text = ANY
                   ((ARRAY ['ACTIVE'::character varying, 'INACTIVE'::character varying, 'FAMOUS'::character varying])::text[])),
    logo        varchar(255),
    version     bigint                   default 0 not null,
    created_at  timestamp with time zone default CURRENT_TIMESTAMP,
    updated_at  timestamp with time zone default CURRENT_TIMESTAMP
);

alter table tbl_providers
    owner to postgres;

create table tbl_products
(
    id            serial
        primary key,
    name          varchar(50)                                                  not null,
    code          varchar(30)                                                  not null,
    description   varchar(255),
    quantity_sold integer                  default 0,
    price         numeric(38, 2)           default 0,
    status        varchar(20)              default 'ACTIVE'::character varying not null
        constraint chk_product_status
            check ((status)::text = ANY
                   ((ARRAY ['ACTIVE'::character varying, 'ON_SALE'::character varying, 'OUT_OF_STOCK'::character varying, 'BESTSELLER'::character varying, 'DELETED'::character varying])::text[])),
    rated         real,
    category_id   integer
        constraint fk_product_category
            references tbl_categories,
    provider_id   integer
        constraint fk_product_provider
            references tbl_providers,
    version       bigint                   default 0                           not null,
    img           varchar(255),
    video         varchar(255),
    created_at    timestamp with time zone default CURRENT_TIMESTAMP,
    updated_at    timestamp with time zone default CURRENT_TIMESTAMP
);

alter table tbl_products
    owner to postgres;

create table tbl_sizes
(
    id         serial
        primary key,
    size       varchar(255),
    weight     double precision,
    height     double precision,
    created_at timestamp with time zone default CURRENT_TIMESTAMP,
    updated_at timestamp with time zone default CURRENT_TIMESTAMP
);

alter table tbl_sizes
    owner to postgres;

create table tbl_items
(
    product_id integer           not null
        constraint fk_item_product
            references tbl_products
            on delete cascade,
    size_id    integer           not null
        constraint fk_item_size
            references tbl_sizes
            on delete cascade,
    quantity   integer default 0 not null,
    status     varchar(255)
        constraint chk_item_status
            check ((status)::text = ANY
                   ((ARRAY ['AVAILABLE'::character varying, 'OUT_OF_STOCK'::character varying, 'DISCONTINUED'::character varying])::text[])),
    primary key (product_id, size_id)
);

alter table tbl_items
    owner to postgres;

create table tbl_comments
(
    product_id integer not null
        constraint fk_comment_product
            references tbl_products,
    user_id    uuid    not null,
    content    text    not null,
    rating     real,
    created_at timestamp with time zone default CURRENT_TIMESTAMP,
    updated_at timestamp with time zone default CURRENT_TIMESTAMP,
    primary key (product_id, user_id)
);

alter table tbl_comments
    owner to postgres;

create table tbl_cart_items
(
    user_id    uuid    not null
        constraint fk_cart_user
            references tbl_users,
    product_id integer not null
        constraint fk_cart_product
            references tbl_products,
    size_id    integer not null
        constraint fk_cart_size
            references tbl_sizes,
    quantity   integer not null,
    created_at timestamp with time zone default CURRENT_TIMESTAMP,
    updated_at timestamp with time zone default CURRENT_TIMESTAMP,
    primary key (user_id, product_id, size_id)
);

alter table tbl_cart_items
    owner to postgres;

create table tbl_orders
(
    id               serial
        primary key,
    code             varchar(20)                                                   not null
        unique,
    status           varchar(20)              default 'PENDING'::character varying not null
        constraint chk_order_status
            check ((status)::text = ANY
                   ((ARRAY ['UNPAID'::character varying, 'PAID'::character varying, 'PENDING'::character varying, 'CONFIRMED'::character varying, 'SHIPPING'::character varying, 'DELIVERED'::character varying, 'COMPLETED'::character varying, 'CANCELLED'::character varying, 'RETURNED'::character varying])::text[])),
    voucher_code     varchar(30),
    total_price      numeric(38, 2)                                                not null,
    voucher_discount numeric(38, 2),
    final_price      numeric(38, 2)                                                not null,
    note             varchar(500),
    version          bigint                   default 0                            not null,
    user_id          uuid                                                          not null,
    receiver_id      integer                                                       not null,
    created_at       timestamp with time zone default CURRENT_TIMESTAMP,
    updated_at       timestamp with time zone default CURRENT_TIMESTAMP,
    payment_type     varchar(255)
        constraint tbl_orders_payment_type_check
            check ((payment_type)::text = ANY
                   ((ARRAY ['PAYMENT_UPON_DELIVER'::character varying, 'ONLINE'::character varying])::text[]))
);

alter table tbl_orders
    owner to postgres;

create table tbl_order_items
(
    order_id       integer        not null
        constraint fk_oi_order
            references tbl_orders,
    product_id     integer        not null,
    size_id        integer        not null,
    quantity       integer        not null,
    total_price    numeric(38, 2) not null,
    final_price    numeric(38, 2) not null,
    original_price numeric(38, 2) not null,
    primary key (order_id, product_id, size_id)
);

alter table tbl_order_items
    owner to postgres;

create table tbl_promotions
(
    id       serial
        primary key,
    value    integer                                                      not null,
    start_at timestamp with time zone default CURRENT_TIMESTAMP,
    end_at   timestamp with time zone,
    priority integer                  default 1,
    status   varchar(20)              default 'SCHEDULED'::character varying
        constraint chk_promo_status
            check ((status)::text = ANY
                   ((ARRAY ['SCHEDULED'::character varying, 'ACTIVE'::character varying, 'ENDED'::character varying, 'DELETED'::character varying])::text[])),
    scope    varchar(20)              default 'GLOBAL'::character varying not null
        constraint chk_promo_scope
            check ((scope)::text = ANY
                   ((ARRAY ['GLOBAL'::character varying, 'PRODUCT'::character varying, 'CATEGORY'::character varying, 'PROVIDER'::character varying])::text[])),
    version  bigint                   default 0                           not null
);

alter table tbl_promotions
    owner to postgres;

create table tbl_promotion_category
(
    category_id  integer not null,
    promotion_id integer not null
        constraint fk_pc_promotion
            references tbl_promotions
            on delete cascade,
    primary key (category_id, promotion_id)
);

alter table tbl_promotion_category
    owner to postgres;

create table tbl_promotion_product
(
    product_id   integer not null,
    promotion_id integer not null
        constraint fk_pp_promotion
            references tbl_promotions,
    primary key (product_id, promotion_id)
);

alter table tbl_promotion_product
    owner to postgres;

create table tbl_vouchers
(
    id               serial
        primary key,
    code             varchar(30)                                                  not null
        unique,
    "discountType"   varchar(20)                                                  not null
        constraint chk_voucher_discount_type
            check (("discountType")::text = ANY
                   ((ARRAY ['PERCENT'::character varying, 'FIXED'::character varying])::text[])),
    "voucherType"    varchar(20)                                                  not null
        constraint chk_voucher_type
            check (("voucherType")::text = ANY
                   ((ARRAY ['NEWBIE'::character varying, 'GLOBAL'::character varying])::text[])),
    status           varchar(20)              default 'ACTIVE'::character varying not null
        constraint chk_voucher_status
            check ((status)::text = ANY
                   ((ARRAY ['ACTIVE'::character varying, 'INACTIVE'::character varying, 'COMMING_SOON'::character varying])::text[])),
    value            integer                                                      not null,
    min_order_amount numeric(38, 2)           default 0                           not null,
    start_at         timestamp with time zone,
    end_at           timestamp with time zone,
    version          bigint                   default 0                           not null,
    created_at       timestamp with time zone default CURRENT_TIMESTAMP,
    updated_at       timestamp with time zone default CURRENT_TIMESTAMP,
    discount_type    varchar(20)                                                  not null
        constraint tbl_vouchers_discount_type_check
            check ((discount_type)::text = ANY
                   ((ARRAY ['PERCENT'::character varying, 'FIXED'::character varying])::text[])),
    voucher_type     varchar(20)                                                  not null
        constraint tbl_vouchers_voucher_type_check
            check ((voucher_type)::text = ANY
                   ((ARRAY ['NEWBIE'::character varying, 'GLOBAL'::character varying])::text[]))
);

alter table tbl_vouchers
    owner to postgres;

create table tbl_user_vouchers
(
    voucher_id      integer                                            not null
        constraint fk_uv_voucher
            references tbl_vouchers,
    user_id         uuid                                               not null,
    status          varchar(20) default 'AVAILABLE'::character varying not null
        constraint chk_uv_status
            check ((status)::text = ANY
                   ((ARRAY ['AVAILABLE'::character varying, 'USED'::character varying, 'EXPIRED'::character varying])::text[])),
    end_at          timestamp with time zone,
    min_price_apply numeric(38, 2),
    version         bigint      default 0                              not null,
    primary key (voucher_id, user_id),
    constraint uk_user_voucher
        unique (user_id, voucher_id)
);

alter table tbl_user_vouchers
    owner to postgres;

create table tbl_notifications
(
    id          serial
        primary key,
    code        varchar(30)
        unique,
    sender_id   uuid,
    receiver_id uuid,
    type        varchar(20)           not null,
    title       varchar(100),
    message     text,
    created_at  timestamp(6) with time zone,
    updated_at  timestamp(6) with time zone,
    for_admin   boolean default true  not null,
    is_delete   boolean default false not null,
    readed      boolean default false not null
);

alter table tbl_notifications
    owner to postgres;

create function unaccent(regdictionary, text) returns text
    stable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function unaccent(regdictionary, text) owner to supabase_admin;

create function unaccent(text) returns text
    stable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function unaccent(text) owner to supabase_admin;

create function unaccent_init(internal) returns internal
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function unaccent_init(internal) owner to supabase_admin;

create function unaccent_lexize(internal, internal, internal, internal) returns internal
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function unaccent_lexize(internal, internal, internal, internal) owner to supabase_admin;

