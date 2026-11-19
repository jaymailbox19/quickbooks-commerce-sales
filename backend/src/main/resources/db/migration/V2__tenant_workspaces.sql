-- CSFP tenant/workspace foundation.
-- Run only after taking a database backup. This migration preserves existing
-- catalog and sales rows by assigning them to the existing Demo Tenant.
-- Application-level authorization is a separate required implementation step.

CREATE TABLE IF NOT EXISTS tenants (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_tenants_name UNIQUE (name)
);

INSERT INTO tenants (name)
VALUES ('Demo Tenant')
ON CONFLICT (name) DO NOTHING;

CREATE TABLE IF NOT EXISTS app_users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(320) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_app_users_email UNIQUE (email)
);

CREATE TABLE IF NOT EXISTS tenant_memberships (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_membership_user_tenant UNIQUE (user_id, tenant_id),
    CONSTRAINT ck_membership_role CHECK (role IN ('OWNER', 'ADMIN', 'MANAGER', 'VIEWER'))
);

ALTER TABLE categories ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE products ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE sales ADD COLUMN IF NOT EXISTS tenant_id BIGINT;

-- Existing single-business prototype data is retained under Demo Tenant.
UPDATE categories
SET tenant_id = (SELECT id FROM tenants WHERE name = 'Demo Tenant')
WHERE tenant_id IS NULL;

UPDATE products p
SET tenant_id = c.tenant_id
FROM categories c
WHERE p.category_id = c.id
  AND p.tenant_id IS NULL;

UPDATE sales s
SET tenant_id = p.tenant_id
FROM products p
WHERE s.product_id = p.id
  AND s.tenant_id IS NULL;

-- Fail closed rather than silently assigning orphaned records to a tenant.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM categories WHERE tenant_id IS NULL) THEN
        RAISE EXCEPTION 'Tenant migration stopped: category rows remain without tenant_id';
    END IF;
    IF EXISTS (SELECT 1 FROM products WHERE tenant_id IS NULL) THEN
        RAISE EXCEPTION 'Tenant migration stopped: product rows remain without tenant_id';
    END IF;
    IF EXISTS (SELECT 1 FROM sales WHERE tenant_id IS NULL) THEN
        RAISE EXCEPTION 'Tenant migration stopped: sales rows remain without tenant_id';
    END IF;
END $$;

ALTER TABLE categories ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE products ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE sales ALTER COLUMN tenant_id SET NOT NULL;

-- Remove the old global category-name uniqueness rule, if present. Names are
-- unique within a business, not across all businesses.
DO $$
DECLARE constraint_name TEXT;
BEGIN
    FOR constraint_name IN
        SELECT con.conname
        FROM pg_constraint con
        WHERE con.conrelid = 'categories'::regclass
          AND con.contype = 'u'
          AND pg_get_constraintdef(con.oid) = 'UNIQUE (name)'
    LOOP
        EXECUTE format('ALTER TABLE categories DROP CONSTRAINT %I', constraint_name);
    END LOOP;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uk_categories_tenant_id_id
    ON categories (tenant_id, id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_categories_tenant_name
    ON categories (tenant_id, name);
CREATE UNIQUE INDEX IF NOT EXISTS uk_products_tenant_id_id
    ON products (tenant_id, id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_products_tenant_name
    ON products (tenant_id, name);

-- Composite foreign keys prevent a product from referencing another
-- business's category and a sale from referencing another business's product.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_products_tenant_category') THEN
        ALTER TABLE products
            ADD CONSTRAINT fk_products_tenant_category
            FOREIGN KEY (tenant_id, category_id)
            REFERENCES categories (tenant_id, id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_sales_tenant_product') THEN
        ALTER TABLE sales
            ADD CONSTRAINT fk_sales_tenant_product
            FOREIGN KEY (tenant_id, product_id)
            REFERENCES products (tenant_id, id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_categories_tenant ON categories (tenant_id);
CREATE INDEX IF NOT EXISTS idx_products_tenant ON products (tenant_id);
CREATE INDEX IF NOT EXISTS idx_sales_tenant_date ON sales (tenant_id, sale_date);
CREATE INDEX IF NOT EXISTS idx_memberships_tenant_user ON tenant_memberships (tenant_id, user_id);
