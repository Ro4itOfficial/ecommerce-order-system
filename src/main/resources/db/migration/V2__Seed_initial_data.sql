-- V2__Seed_initial_data.sql
-- Seed initial data for Order Processing System

-- Insert default admin user (password: Admin123!)
INSERT INTO users (user_id, username, email, password, first_name, last_name, email_verified, enabled)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'admin',
    'admin@ecommerce.com',
    '$2a$10$YQkFqaWzOHpHGPHKFHBpXO4Wxf2dHs0x1x0Z2NhHhV7.kFGzR0Kie',
    'System',
    'Administrator',
    true,
    true
);

-- Assign admin roles
INSERT INTO user_roles (user_id, role)
VALUES
    ('00000000-0000-0000-0000-000000000001', 'ADMIN'),
    ('00000000-0000-0000-0000-000000000001', 'USER');

-- Insert test user (password: User123!)
INSERT INTO users (user_id, username, email, password, first_name, last_name, email_verified, enabled)
VALUES (
    '00000000-0000-0000-0000-000000000002',
    'testuser',
    'test@example.com',
    '$2a$10$LqLwKaNweKVfHPYBCPhRZ.xhGWH0cGj5nYqFR8vhX0VcH9x9.p3W2',
    'Test',
    'User',
    true,
    true
);

-- Assign user role
INSERT INTO user_roles (user_id, role)
VALUES ('00000000-0000-0000-0000-000000000002', 'USER');

-- Insert sample orders for testing
INSERT INTO orders (order_id, customer_id, customer_email, customer_name, status, total_amount, currency, shipping_address, payment_method)
VALUES
(
    '10000000-0000-0000-0000-000000000001',
    'CUST001',
    'john.doe@example.com',
    'John Doe',
    'PENDING',
    1999.98,
    'USD',
    '123 Main St, New York, NY 10001',
    'CREDIT_CARD'
),
(
    '10000000-0000-0000-0000-000000000002',
    'CUST001',
    'john.doe@example.com',
    'John Doe',
    'PROCESSING',
    698.94,
    'USD',
    '123 Main St, New York, NY 10001',
    'PAYPAL'
),
(
    '10000000-0000-0000-0000-000000000003',
    'CUST002',
    'jane.smith@example.com',
    'Jane Smith',
    'SHIPPED',
    349.50,
    'USD',
    '456 Oak Ave, Los Angeles, CA 90001',
    'DEBIT_CARD'
);

-- Insert sample order items with discount_amount and tax_amount
INSERT INTO order_items (item_id, order_id, product_id, product_name, product_description, quantity, unit_price, discount_amount, tax_amount, subtotal)
VALUES
(
    '20000000-0000-0000-0000-000000000001',
    '10000000-0000-0000-0000-000000000001',
    'PROD001',
    'Laptop',
    'High-performance laptop with 16GB RAM',
    2,
    999.99,
    0.00,
    0.00,
    1999.98
),
(
    '20000000-0000-0000-0000-000000000002',
    '10000000-0000-0000-0000-000000000002',
    'PROD002',
    'Wireless Mouse',
    'Ergonomic wireless mouse',
    1,
    49.99,
    5.00,
    3.00,
    47.99
),
(
    '20000000-0000-0000-0000-000000000003',
    '10000000-0000-0000-0000-000000000002',
    'PROD003',
    'Mechanical Keyboard',
    'RGB mechanical keyboard',
    1,
    149.99,
    10.00,
    9.50,
    149.49
),
(
    '20000000-0000-0000-0000-000000000004',
    '10000000-0000-0000-0000-000000000002',
    'PROD004',
    'Monitor Stand',
    'Adjustable monitor stand',
    2,
    200.00,
    0.00,
    1.46,
    401.46
),
(
    '20000000-0000-0000-0000-000000000005',
    '10000000-0000-0000-0000-000000000003',
    'PROD005',
    'Headphones',
    'Noise-canceling wireless headphones',
    1,
    349.50,
    0.00,
    0.00,
    349.50
);

-- Update order shipped_at for shipped order
UPDATE orders
SET shipped_at = CURRENT_TIMESTAMP - INTERVAL '1 day',
    tracking_number = 'TRK123456789'
WHERE order_id = '10000000-0000-0000-0000-000000000003';

-- Update order processed_at for processing order
UPDATE orders
SET processed_at = CURRENT_TIMESTAMP - INTERVAL '2 hours'
WHERE order_id = '10000000-0000-0000-0000-000000000002';