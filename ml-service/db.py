import psycopg2


def get_sales():

    connection = psycopg2.connect(
        host="localhost",
        port=5432,
        database="quickbooks",
        user="quickbooks",
        password="quickbooks"
    )

    cursor = connection.cursor()

    cursor.execute("""
        SELECT
    s.tenant_id,
    c.name AS category,
    DATE_TRUNC('week', s.sale_date) AS week,
    SUM(s.amount) AS sales
    FROM sales s
    JOIN products p ON s.product_id = p.id
    JOIN categories c ON p.category_id = c.id
    GROUP BY
        s.tenant_id,
        c.name,
        DATE_TRUNC('week', s.sale_date)
    ORDER BY
        s.tenant_id,
        c.name,
        week;
    """)

    rows = cursor.fetchall()

    cursor.close()
    connection.close()

    return rows