-- Insert test customer
INSERT INTO customers (first_name, last_name, email, email_opt_in, created_at, updated_at)
VALUES ('John', 'Doe', 'john@example.com', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Insert test cart
INSERT INTO carts (customer_id, cart_status, total_amount, created_at, updated_at, reminder_sent)
VALUES (1, 'ABANDONED', 99.99, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false);

-- Insert test abandoned cart
INSERT INTO abandoned_carts (cart_id, customer_id, abandoned_at, reminder_sent, recovery_status)
VALUES (1, 1, CURRENT_TIMESTAMP, false, 'PENDING');
