-- Initial single-business schema for clean installations.
-- Existing installations are baselined at version 1 and continue with V2.

CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    CONSTRAINT categories_name_key UNIQUE (name)
);

CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    category_id BIGINT NOT NULL,
    CONSTRAINT fk_products_category
        FOREIGN KEY (category_id) REFERENCES categories(id)
);

CREATE TABLE sales (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    sale_date DATE NOT NULL,
    quantity INTEGER NOT NULL,
    amount NUMERIC(15, 2) NOT NULL,
    CONSTRAINT uk_sale_product_date_amount
        UNIQUE (product_id, sale_date, amount),
    CONSTRAINT fk_sales_product
        FOREIGN KEY (product_id) REFERENCES products(id)
);

CREATE INDEX idx_sales_sale_date ON sales (sale_date);
