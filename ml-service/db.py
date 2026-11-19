import os

import psycopg2


def get_sales():
    """Load weekly sales grouped by tenant and category.

    Connection details are supplied through environment variables only.
    """
    connection = psycopg2.connect(
        host=os.environ["DB_HOST"],
        port=int(os.environ.get("DB_PORT", "5432")),
        dbname=os.environ["DB_NAME"],
        user=os.environ["DB_USER"],
        password=os.environ["DB_PASSWORD"],
        connect_timeout=int(os.environ.get("DB_CONNECT_TIMEOUT", "5")),
        sslmode=os.environ.get("DB_SSLMODE", "prefer"),
    )
    try:
        with connection.cursor() as cursor:
            cursor.execute(
                """
                SELECT s.tenant_id, c.name AS category,
                       DATE_TRUNC('week', s.sale_date) AS week,
                       SUM(s.amount) AS sales
                FROM sales s
                JOIN products p ON s.product_id = p.id
                JOIN categories c ON p.category_id = c.id
                GROUP BY s.tenant_id, c.name, DATE_TRUNC('week', s.sale_date)
                ORDER BY s.tenant_id, c.name, week
                """
            )
            return cursor.fetchall()
    finally:
        connection.close()
