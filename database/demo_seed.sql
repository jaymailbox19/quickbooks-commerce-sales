-- Optional demonstration data for the Demo Tenant.
-- Run manually only after the application migrations have completed:
--   PGHOST="$DB_HOST" PGPORT="$DB_PORT" PGDATABASE="$DB_NAME" \
--   PGUSER="$DB_USER" PGPASSWORD="$DB_PASSWORD" \
--   psql -v ON_ERROR_STOP=1 -f database/demo_seed.sql
-- This file is deliberately not loaded automatically by Spring Boot.
BEGIN;

INSERT INTO categories (tenant_id, name)
SELECT t.id, category.name
FROM tenants t
CROSS JOIN (VALUES
    ('Electronics'),
    ('Clothing'),
    ('Home')
) AS category(name)
WHERE t.name = 'Demo Tenant'
ON CONFLICT (tenant_id, name) DO NOTHING;

INSERT INTO products (tenant_id, name, category_id)
SELECT t.id, product.name, c.id
FROM tenants t
JOIN (VALUES
    ('Laptop', 'Electronics'),
    ('Monitor', 'Electronics'),
    ('Shirt', 'Clothing'),
    ('Shoes', 'Clothing'),
    ('Chair', 'Home'),
    ('Desk', 'Home')
) AS product(name, category_name) ON TRUE
JOIN categories c
  ON c.tenant_id = t.id
 AND c.name = product.category_name
WHERE t.name = 'Demo Tenant'
ON CONFLICT (tenant_id, name) DO NOTHING;

INSERT INTO sales (tenant_id, product_id, sale_date, quantity, amount)
SELECT t.id, p.id, sample.sale_date, sample.quantity, sample.amount
FROM tenants t
JOIN (VALUES
    ('Laptop', DATE '2026-09-20', 5, 5000.00),
    ('Monitor', DATE '2026-09-17', 8, 2400.00),
    ('Shirt', DATE '2026-09-19', 20, 1000.00),
    ('Shoes', DATE '2026-09-15', 10, 1500.00),
    ('Chair', DATE '2026-09-18', 12, 1800.00),
    ('Desk', DATE '2026-09-16', 6, 2100.00),
    ('Laptop', DATE '2026-08-04', 10, 10000.00),
    ('Shirt', DATE '2026-08-05', 20, 6000.00),
    ('Laptop', DATE '2026-08-11', 12, 12000.00),
    ('Shirt', DATE '2026-08-12', 25, 7500.00),
    ('Laptop', DATE '2026-08-18', 8, 8000.00),
    ('Shirt', DATE '2026-08-19', 30, 9000.00),
    ('Laptop', DATE '2026-08-25', 15, 15000.00),
    ('Shirt', DATE '2026-08-26', 22, 6600.00),
    ('Laptop', DATE '2026-09-01', 14, 14000.00),
    ('Shirt', DATE '2026-09-02', 28, 8400.00),
    ('Chair', DATE '2026-09-03', 10, 1500.00),
    ('Laptop', DATE '2026-09-08', 16, 16000.00),
    ('Shirt', DATE '2026-09-09', 30, 9000.00),
    ('Chair', DATE '2026-09-10', 12, 1800.00),
    ('Laptop', DATE '2026-09-15', 18, 18000.00),
    ('Shirt', DATE '2026-09-16', 32, 9600.00),
    ('Chair', DATE '2026-09-17', 14, 2100.00)
) AS sample(product_name, sale_date, quantity, amount) ON TRUE
JOIN products p
  ON p.tenant_id = t.id
 AND p.name = sample.product_name
WHERE t.name = 'Demo Tenant'
ON CONFLICT ON CONSTRAINT uk_sale_product_date_amount DO NOTHING;

COMMIT;
