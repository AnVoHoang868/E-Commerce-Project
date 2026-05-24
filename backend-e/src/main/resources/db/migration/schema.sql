-- ============================================================
-- Schema generated from JPA entities
-- Database: PostgreSQL
-- ============================================================

CREATE EXTENSION IF NOT EXISTS unaccent;

-- =========================== AUTH ============================

CREATE TABLE IF NOT EXISTS tbl_accounts
(
    id         SERIAL PRIMARY KEY,
    username   VARCHAR(50)  NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    role       VARCHAR(10)  NOT NULL DEFAULT 'CUSTOMER',
    email      VARCHAR(100) NOT NULL UNIQUE,
    verify     BOOLEAN      NOT NULL DEFAULT FALSE,
    status     VARCHAR(10)  NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ           DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ           DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_accounts_role CHECK (role IN ('CUSTOMER', 'ADMIN', 'EMPLOYEE')),
    CONSTRAINT chk_accounts_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'BANNED'))
);

-- =========================== USERS ===========================

CREATE TABLE IF NOT EXISTS tbl_users
(
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    f_name     VARCHAR(255),
    l_name     VARCHAR(255),
    avatar     VARCHAR(255),
    acc_id     INT,
    created_at TIMESTAMPTZ      DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ      DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tbl_recivers
(
    id       SERIAL PRIMARY KEY,
    f_name   VARCHAR(100),
    l_name   VARCHAR(100),
    phone    VARCHAR(10)  NOT NULL,
    country  VARCHAR(20)  NOT NULL,
    province VARCHAR(20)  NOT NULL,
    district VARCHAR(20)  NOT NULL,
    street   VARCHAR(100) NOT NULL,
    detail   VARCHAR(255),


    user_id  UUID         NOT NULL,

    CONSTRAINT fk_receiver_user FOREIGN KEY (user_id) REFERENCES tbl_users (id)
);

-- ========================= PRODUCT ===========================

CREATE TABLE IF NOT EXISTS tbl_categories
(
    id          SERIAL PRIMARY KEY,
    name        VARCHAR(50) NOT NULL,
    code        VARCHAR(10) NOT NULL,
    img_url     VARCHAR(255),
    description VARCHAR(50),
    status      VARCHAR(20)          DEFAULT 'ACTIVE',
    parent_id   INT,
    is_leaf     BOOLEAN     NOT NULL DEFAULT TRUE,
    level       INT         NOT NULL DEFAULT 0,
    path        VARCHAR(255),
    version     BIGINT      NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ          DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ          DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_category_parent FOREIGN KEY (parent_id) REFERENCES tbl_categories (id) ON DELETE SET NULL,
    CONSTRAINT chk_category_status CHECK (status IN ('ACTIVE', 'HIDDEN'))
);

CREATE TABLE IF NOT EXISTS tbl_providers
(
    id          SERIAL PRIMARY KEY,
    name        VARCHAR(50) NOT NULL,
    code        VARCHAR(10) NOT NULL,
    description VARCHAR(255),
    email       VARCHAR(100),
    phone       VARCHAR(15),
    status      VARCHAR(20)          DEFAULT 'ACTIVE',
    logo        VARCHAR(255),
    version     BIGINT      NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ          DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ          DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_provider_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'FAMOUS'))
);

CREATE TABLE IF NOT EXISTS tbl_products
(
    id            SERIAL PRIMARY KEY,
    name          VARCHAR(50) NOT NULL,
    code          VARCHAR(30) NOT NULL,
    description   VARCHAR(255),
    quantity_sold INT                  DEFAULT 0,
    price         NUMERIC              DEFAULT 0,
    status        VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    rated         REAL,
    category_id   INT,
    provider_id   INT,
    version       BIGINT      NOT NULL DEFAULT 0,
    img           VARCHAR(255),
    video         VARCHAR(255),
    created_at    TIMESTAMPTZ          DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMPTZ          DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES tbl_categories (id),
    CONSTRAINT fk_product_provider FOREIGN KEY (provider_id) REFERENCES tbl_providers (id),
    CONSTRAINT chk_product_status CHECK (status IN ('ACTIVE', 'ON_SALE', 'OUT_OF_STOCK', 'BESTSELLER', 'DELETED'))
);

CREATE TABLE IF NOT EXISTS tbl_sizes
(
    id         SERIAL PRIMARY KEY,
    size       VARCHAR(255),
    weight     DOUBLE PRECISION,
    height     DOUBLE PRECISION,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tbl_items
(
    product_id INT NOT NULL,
    size_id    INT NOT NULL,
    quantity   INT NOT NULL DEFAULT 0,
    status     VARCHAR(255),

    PRIMARY KEY (product_id, size_id),
    CONSTRAINT fk_item_product FOREIGN KEY (product_id) REFERENCES tbl_products (id) ON DELETE CASCADE,
    CONSTRAINT fk_item_size FOREIGN KEY (size_id) REFERENCES tbl_sizes (id) ON DELETE CASCADE,
    CONSTRAINT chk_item_status CHECK (status IN ('AVAILABLE', 'OUT_OF_STOCK', 'DISCONTINUED'))
);

CREATE TABLE IF NOT EXISTS tbl_comments
(
    product_id INT  NOT NULL,
    user_id    UUID NOT NULL,
    content    TEXT NOT NULL,
    rating     REAL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (product_id, user_id),
    CONSTRAINT fk_comment_product FOREIGN KEY (product_id) REFERENCES tbl_products (id)
);

-- =========================== CART ============================

CREATE TABLE IF NOT EXISTS tbl_cart_items
(
    user_id    UUID NOT NULL,
    product_id INT  NOT NULL,
    size_id    INT  NOT NULL,
    quantity   INT  NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (user_id, product_id, size_id),
    CONSTRAINT fk_cart_user FOREIGN KEY (user_id) REFERENCES tbl_users (id),
    CONSTRAINT fk_cart_product FOREIGN KEY (product_id) REFERENCES tbl_products (id),
    CONSTRAINT fk_cart_size FOREIGN KEY (size_id) REFERENCES tbl_sizes (id)
);

-- ========================== ORDER ============================

CREATE TABLE IF NOT EXISTS tbl_orders
(
    id               SERIAL PRIMARY KEY,
    code             VARCHAR(20) NOT NULL UNIQUE,
    status           VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    voucher_code     VARCHAR(30),
    total_price      NUMERIC     NOT NULL,
    voucher_discount NUMERIC,
    final_price      NUMERIC     NOT NULL,
    note             VARCHAR(500),
    payment_type     VARCHAR(30),
    version          BIGINT      NOT NULL DEFAULT 0,
    user_id          UUID        NOT NULL,
    receiver_id      INT         NOT NULL,
    created_at       TIMESTAMPTZ          DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMPTZ          DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_order_status CHECK (status IN
                                       ('UNPAID', 'PAID', 'PENDING', 'CONFIRMED',
                                        'SHIPPING', 'DELIVERED', 'COMPLETED', 'CANCELLED', 'RETURNED')),
    CONSTRAINT chk_order_payment_type CHECK (payment_type IN
                                             ('PAYMENT_UPON_DELIVER', 'ONLINE'))
);

CREATE TABLE IF NOT EXISTS tbl_order_items
(
    order_id    INT     NOT NULL,
    product_id  INT     NOT NULL,
    size_id     INT     NOT NULL,
    quantity    INT     NOT NULL,
    original_price NUMERIC NOT NULL,
    final_price    NUMERIC NOT NULL,
    total_price NUMERIC NOT NULL,

    PRIMARY KEY (order_id, product_id, size_id),
    CONSTRAINT fk_oi_order FOREIGN KEY (order_id) REFERENCES tbl_orders (id)
);

-- ======================== PROMOTION ==========================

CREATE TABLE IF NOT EXISTS tbl_promotions
(
    id       SERIAL PRIMARY KEY,
    value    INT         NOT NULL,
    start_at TIMESTAMPTZ          DEFAULT CURRENT_TIMESTAMP,
    end_at   TIMESTAMPTZ,
    priority INT                  DEFAULT 1,
    status   VARCHAR(20)          DEFAULT 'SCHEDULED',
    scope    VARCHAR(20) NOT NULL DEFAULT 'GLOBAL',
    version  BIGINT      NOT NULL DEFAULT 0,

    CONSTRAINT chk_promo_status CHECK (status IN ('SCHEDULED', 'ACTIVE', 'ENDED', 'DELETED')),
    CONSTRAINT chk_promo_scope CHECK (scope IN ('GLOBAL', 'PRODUCT', 'CATEGORY', 'PROVIDER'))
);

CREATE TABLE IF NOT EXISTS tbl_promotion_category
(
    category_id  INT NOT NULL,
    promotion_id INT NOT NULL,

    PRIMARY KEY (category_id, promotion_id),
    CONSTRAINT fk_pc_promotion FOREIGN KEY (promotion_id) REFERENCES tbl_promotions (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS tbl_promotion_product
(
    product_id   INT NOT NULL,
    promotion_id INT NOT NULL,

    PRIMARY KEY (product_id, promotion_id),
    CONSTRAINT fk_pp_promotion FOREIGN KEY (promotion_id) REFERENCES tbl_promotions (id)
);

-- ========================= VOUCHER ===========================

CREATE TABLE IF NOT EXISTS tbl_vouchers
(
    id               SERIAL PRIMARY KEY,
    code             VARCHAR(30) NOT NULL UNIQUE,
    "discountType"   VARCHAR(20) NOT NULL,
    "voucherType"    VARCHAR(20) NOT NULL,
    status           VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    value            INT         NOT NULL,
    min_order_amount NUMERIC     NOT NULL DEFAULT 0,
    start_at         TIMESTAMPTZ,
    end_at           TIMESTAMPTZ,
    version          BIGINT      NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ          DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMPTZ          DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_voucher_discount_type CHECK ("discountType" IN ('PERCENT', 'FIXED')),
    CONSTRAINT chk_voucher_type CHECK ("voucherType" IN ('NEWBIE', 'GLOBAL')),
    CONSTRAINT chk_voucher_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'COMMING_SOON'))
);

CREATE TABLE IF NOT EXISTS tbl_user_vouchers
(
    voucher_id      INT         NOT NULL,
    user_id         UUID        NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    end_at          TIMESTAMPTZ,
    min_price_apply NUMERIC,
    version         BIGINT      NOT NULL DEFAULT 0,

    PRIMARY KEY (voucher_id, user_id),
    CONSTRAINT uk_user_voucher UNIQUE (user_id, voucher_id),
    CONSTRAINT fk_uv_voucher FOREIGN KEY (voucher_id) REFERENCES tbl_vouchers (id),
    CONSTRAINT chk_uv_status CHECK (status IN ('AVAILABLE', 'USED', 'EXPIRED'))
);

-- ======================= Notification ====================
create table if not exists tbl_notifications
(
    id          serial primary key,
    code        varchar(30) unique,
    sender_id   uuid,
    receiver_id uuid,
    type        varchar(20) not null,
    title       varchar(100),
    message     text
)
