SET LOCAL lock_timeout = '10s';

UPDATE customers SET full_name = full_name
WHERE first_name IS NULL OR last_name IS NULL;

ALTER TABLE customers
    VALIDATE CONSTRAINT customers_first_name_present,
    VALIDATE CONSTRAINT customers_last_name_present,
    VALIDATE CONSTRAINT customers_name_length;

CREATE UNIQUE INDEX customers_split_contact_key
ON customers (first_name, last_name, address, phone) NULLS NOT DISTINCT;
