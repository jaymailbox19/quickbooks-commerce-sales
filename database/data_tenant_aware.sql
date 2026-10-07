-- Categories
INSERT INTO categories (name) VALUES ('Electronics')
ON CONFLICT (name) DO NOTHING;
INSERT INTO categories (name) VALUES ('Clothing')
ON CONFLICT (name) DO NOTHING;
INSERT INTO categories (name) VALUES ('Home')
ON CONFLICT (name) DO NOTHING;

-- Products
INSERT INTO products (name, category_id)
SELECT 'Laptop', id
FROM categories
WHERE name = 'Electronics'
  AND NOT EXISTS (
      SELECT 1
      FROM products p
      WHERE p.name = 'Laptop'
        AND p.category_id = categories.id
  );

INSERT INTO products (name, category_id)
SELECT 'Monitor', id
FROM categories
WHERE name = 'Electronics'
  AND NOT EXISTS (
      SELECT 1
      FROM products p
      WHERE p.name = 'Monitor'
        AND p.category_id = categories.id
  );

INSERT INTO products (name, category_id)
SELECT 'Shirt', id
FROM categories
WHERE name = 'Clothing'
  AND NOT EXISTS (
      SELECT 1
      FROM products p
      WHERE p.name = 'Shirt'
        AND p.category_id = categories.id
  );

INSERT INTO products (name, category_id)
SELECT 'Shoes', id
FROM categories
WHERE name = 'Clothing'
  AND NOT EXISTS (
      SELECT 1
      FROM products p
      WHERE p.name = 'Shoes'
        AND p.category_id = categories.id
  );

INSERT INTO products (name, category_id)
SELECT 'Chair', id
FROM categories
WHERE name = 'Home'
  AND NOT EXISTS (
      SELECT 1
      FROM products p
      WHERE p.name = 'Chair'
        AND p.category_id = categories.id
  );

INSERT INTO products (name, category_id)
SELECT 'Desk', id
FROM categories
WHERE name = 'Home'
  AND NOT EXISTS (
      SELECT 1
      FROM products p
      WHERE p.name = 'Desk'
        AND p.category_id = categories.id
  );

-- ============================================================
-- Tenants
-- ============================================================

CREATE TABLE IF NOT EXISTS tenants (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO tenants (name)
VALUES ('Demo Tenant')
ON CONFLICT (name) DO NOTHING;

-- ============================================================
-- Add tenant_id to Sales
-- ============================================================

ALTER TABLE sales
ADD COLUMN IF NOT EXISTS tenant_id INTEGER;

-- Populate existing sales
UPDATE sales
SET tenant_id = (
    SELECT id FROM tenants WHERE name = 'Demo Tenant'
)
WHERE tenant_id IS NULL;

-- Add foreign key if it does not already exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_sales_tenant'
    ) THEN
        ALTER TABLE sales
        ADD CONSTRAINT fk_sales_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants(id);
    END IF;
END $$;

ALTER TABLE sales
ALTER COLUMN tenant_id SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_sales_tenant_id
ON sales(tenant_id);

-- Sales


-- Current Week

INSERT INTO sales (tenant_id, product_id, sale_date, quantity, amount)
SELECT t.id, id, DATE '2026-09-20', 5, 5000.00
FROM products
CROSS JOIN tenants t
WHERE name = 'Laptop'
  AND t.name = 'Demo Tenant'
  AND NOT EXISTS (
      SELECT 1 FROM sales s
      WHERE s.tenant_id = t.id
        AND s.product_id = products.id
        AND s.sale_date = DATE '2026-09-20'
        AND s.amount = 5000.00
  );

INSERT INTO sales (tenant_id, product_id, sale_date, quantity, amount)
SELECT t.id, id, DATE '2026-09-17', 8, 2400.00
FROM products
CROSS JOIN tenants t
WHERE name = 'Monitor'
  AND t.name = 'Demo Tenant'
  AND NOT EXISTS (
      SELECT 1 FROM sales s
      WHERE s.tenant_id = t.id
        AND s.product_id = products.id
        AND s.sale_date = DATE '2026-09-17'
        AND s.amount = 2400.00
  );

INSERT INTO sales (tenant_id, product_id, sale_date, quantity, amount)
SELECT t.id, id, DATE '2026-09-19', 20, 1000.00
FROM products
CROSS JOIN tenants t
WHERE name = 'Shirt'
  AND t.name = 'Demo Tenant'
  AND NOT EXISTS (
      SELECT 1 FROM sales s
      WHERE s.tenant_id = t.id
        AND s.product_id = products.id
        AND s.sale_date = DATE '2026-09-19'
        AND s.amount = 1000.00
  );

INSERT INTO sales (tenant_id, product_id, sale_date, quantity, amount)
SELECT t.id, id, DATE '2026-09-15', 10, 1500.00
FROM products
CROSS JOIN tenants t
WHERE name = 'Shoes'
  AND t.name = 'Demo Tenant'
  AND NOT EXISTS (
      SELECT 1 FROM sales s
      WHERE s.tenant_id = t.id
        AND s.product_id = products.id
        AND s.sale_date = DATE '2026-09-15'
        AND s.amount = 1500.00
  );

INSERT INTO sales (tenant_id, product_id, sale_date, quantity, amount)
SELECT t.id, id, DATE '2026-09-18', 12, 1800.00
FROM products
CROSS JOIN tenants t
WHERE name = 'Chair'
  AND t.name = 'Demo Tenant'
  AND NOT EXISTS (
      SELECT 1 FROM sales s
      WHERE s.tenant_id = t.id
        AND s.product_id = products.id
        AND s.sale_date = DATE '2026-09-18'
        AND s.amount = 1800.00
  );

INSERT INTO sales (tenant_id, product_id, sale_date, quantity, amount)
SELECT t.id, id, DATE '2026-09-16', 6, 2100.00
FROM products
CROSS JOIN tenants t
WHERE name = 'Desk'
  AND t.name = 'Demo Tenant'
  AND NOT EXISTS (
      SELECT 1 FROM sales s
      WHERE s.tenant_id = t.id
        AND s.product_id = products.id
        AND s.sale_date = DATE '2026-09-16'
        AND s.amount = 2100.00
  );


-- Historical Week 1

INSERT INTO sales (tenant_id, product_id, sale_date, quantity, amount)
SELECT t.id, id, DATE '2026-08-04', 10, 10000.00
FROM products
CROSS JOIN tenants t
WHERE name = 'Laptop'
  AND t.name = 'Demo Tenant'
  AND NOT EXISTS (
      SELECT 1 FROM sales s
      WHERE s.tenant_id = t.id
        AND s.product_id = products.id
        AND s.sale_date = DATE '2026-08-04'
        AND s.amount = 10000.00
  );

INSERT INTO sales (tenant_id, product_id, sale_date, quantity, amount)
SELECT t.id, id, DATE '2026-08-05', 20, 6000.00
FROM products
CROSS JOIN tenants t
WHERE name = 'Shirt'
  AND t.name = 'Demo Tenant'
  AND NOT EXISTS (
      SELECT 1 FROM sales s
      WHERE s.tenant_id = t.id
        AND s.product_id = products.id
        AND s.sale_date = DATE '2026-08-05'
        AND s.amount = 6000.00
  );


-- Historical Week 2

INSERT INTO sales (tenant_id, product_id, sale_date, quantity, amount)
SELECT t.id, id, DATE '2026-08-11', 12, 12000.00
FROM products
CROSS JOIN tenants t
WHERE name = 'Laptop'
  AND t.name = 'Demo Tenant'
  AND NOT EXISTS (
      SELECT 1 FROM sales s
      WHERE s.tenant_id = t.id
        AND s.product_id = products.id
        AND s.sale_date = DATE '2026-08-11'
        AND s.amount = 12000.00
  );

INSERT INTO sales (tenant_id, product_id, sale_date, quantity, amount)
SELECT t.id, id, DATE '2026-08-12', 25, 7500.00
FROM products
CROSS JOIN tenants t
WHERE name = 'Shirt'
  AND t.name = 'Demo Tenant'
  AND NOT EXISTS (
      SELECT 1 FROM sales s
      WHERE s.tenant_id = t.id
        AND s.product_id = products.id
        AND s.sale_date = DATE '2026-08-12'
        AND s.amount = 7500.00
  );


-- Historical Week 3

INSERT INTO sales (tenant_id, product_id, sale_date, quantity, amount)
SELECT t.id, id, DATE '2026-08-18', 8, 8000.00
FROM products
CROSS JOIN tenants t
WHERE name = 'Laptop'
  AND t.name = 'Demo Tenant'
  AND NOT EXISTS (
      SELECT 1 FROM sales s
      WHERE s.tenant_id = t.id
        AND s.product_id = products.id
        AND s.sale_date = DATE '2026-08-18'
        AND s.amount = 8000.00
  );

INSERT INTO sales (tenant_id, product_id, sale_date, quantity, amount)
SELECT t.id, id, DATE '2026-08-19', 30, 9000.00
FROM products
CROSS JOIN tenants t
WHERE name = 'Shirt'
  AND t.name = 'Demo Tenant'
  AND NOT EXISTS (
      SELECT 1 FROM sales s
      WHERE s.tenant_id = t.id
        AND s.product_id = products.id
        AND s.sale_date = DATE '2026-08-19'
        AND s.amount = 9000.00
  );


-- Historical Week 4

INSERT INTO sales (tenant_id, product_id, sale_date, quantity, amount)
SELECT t.id, id, DATE '2026-08-25', 15, 15000.00
FROM products
CROSS JOIN tenants t
WHERE name = 'Laptop'
  AND t.name = 'Demo Tenant'
  AND NOT EXISTS (
      SELECT 1 FROM sales s
      WHERE s.tenant_id = t.id
        AND s.product_id = products.id
        AND s.sale_date = DATE '2026-08-25'
        AND s.amount = 15000.00
  );

INSERT INTO sales (tenant_id, product_id, sale_date, quantity, amount)
SELECT t.id, id, DATE '2026-08-26', 22, 6600.00
FROM products
CROSS JOIN tenants t
WHERE name = 'Shirt'
  AND t.name = 'Demo Tenant'
  AND NOT EXISTS (
      SELECT 1 FROM sales s
      WHERE s.tenant_id = t.id
        AND s.product_id = products.id
        AND s.sale_date = DATE '2026-08-26'
        AND s.amount = 6600.00
  );

  -- Historical Week 5

INSERT INTO sales (tenant_id, product_id, sale_date, quantity, amount)
SELECT t.id, id, DATE '2026-09-01', 14, 14000.00
FROM products
CROSS JOIN tenants t
WHERE name = 'Laptop'
  AND t.name = 'Demo Tenant'
  AND NOT EXISTS (
      SELECT 1 FROM sales s
      WHERE s.tenant_id = t.id
        AND s.product_id = products.id
        AND s.sale_date = DATE '2026-09-01'
        AND s.amount = 14000.00
  );

INSERT INTO sales (tenant_id, product_id, sale_date, quantity, amount)
SELECT t.id, id, DATE '2026-09-02', 28, 8400.00
FROM products
CROSS JOIN tenants t
WHERE name = 'Shirt'
  AND t.name = 'Demo Tenant'
  AND NOT EXISTS (
      SELECT 1 FROM sales s
      WHERE s.tenant_id = t.id
        AND s.product_id = products.id
        AND s.sale_date = DATE '2026-09-02'
        AND s.amount = 8400.00
  );

INSERT INTO sales (tenant_id, product_id, sale_date, quantity, amount)
SELECT t.id, id, DATE '2026-09-03', 10, 1500.00
FROM products
CROSS JOIN tenants t
WHERE name = 'Chair'
  AND t.name = 'Demo Tenant'
  AND NOT EXISTS (
      SELECT 1 FROM sales s
      WHERE s.tenant_id = t.id
        AND s.product_id = products.id
        AND s.sale_date = DATE '2026-09-03'
        AND s.amount = 1500.00
  );


-- Historical Week 6

INSERT INTO sales (tenant_id, product_id, sale_date, quantity, amount)
SELECT t.id, id, DATE '2026-09-08', 16, 16000.00
FROM products
CROSS JOIN tenants t
WHERE name = 'Laptop'
  AND t.name = 'Demo Tenant'
  AND NOT EXISTS (
      SELECT 1 FROM sales s
      WHERE s.tenant_id = t.id
        AND s.product_id = products.id
        AND s.sale_date = DATE '2026-09-08'
        AND s.amount = 16000.00
  );

INSERT INTO sales (tenant_id, product_id, sale_date, quantity, amount)
SELECT t.id, id, DATE '2026-09-09', 30, 9000.00
FROM products
CROSS JOIN tenants t
WHERE name = 'Shirt'
  AND t.name = 'Demo Tenant'
  AND NOT EXISTS (
      SELECT 1 FROM sales s
      WHERE s.tenant_id = t.id
        AND s.product_id = products.id
        AND s.sale_date = DATE '2026-09-09'
        AND s.amount = 9000.00
  );

INSERT INTO sales (tenant_id, product_id, sale_date, quantity, amount)
SELECT t.id, id, DATE '2026-09-10', 12, 1800.00
FROM products
CROSS JOIN tenants t
WHERE name = 'Chair'
  AND t.name = 'Demo Tenant'
  AND NOT EXISTS (
      SELECT 1 FROM sales s
      WHERE s.tenant_id = t.id
        AND s.product_id = products.id
        AND s.sale_date = DATE '2026-09-10'
        AND s.amount = 1800.00
  );


-- Historical Week 7

INSERT INTO sales (tenant_id, product_id, sale_date, quantity, amount)
SELECT t.id, id, DATE '2026-09-15', 18, 18000.00
FROM products
CROSS JOIN tenants t
WHERE name = 'Laptop'
  AND t.name = 'Demo Tenant'
  AND NOT EXISTS (
      SELECT 1 FROM sales s
      WHERE s.tenant_id = t.id
        AND s.product_id = products.id
        AND s.sale_date = DATE '2026-09-15'
        AND s.amount = 18000.00
  );

INSERT INTO sales (tenant_id, product_id, sale_date, quantity, amount)
SELECT t.id, id, DATE '2026-09-16', 32, 9600.00
FROM products
CROSS JOIN tenants t
WHERE name = 'Shirt'
  AND t.name = 'Demo Tenant'
  AND NOT EXISTS (
      SELECT 1 FROM sales s
      WHERE s.tenant_id = t.id
        AND s.product_id = products.id
        AND s.sale_date = DATE '2026-09-16'
        AND s.amount = 9600.00
  );

INSERT INTO sales (tenant_id, product_id, sale_date, quantity, amount)
SELECT t.id, id, DATE '2026-09-17', 14, 2100.00
FROM products
CROSS JOIN tenants t
WHERE name = 'Chair'
  AND t.name = 'Demo Tenant'
  AND NOT EXISTS (
      SELECT 1 FROM sales s
      WHERE s.tenant_id = t.id
        AND s.product_id = products.id
        AND s.sale_date = DATE '2026-09-17'
        AND s.amount = 2100.00
  );
