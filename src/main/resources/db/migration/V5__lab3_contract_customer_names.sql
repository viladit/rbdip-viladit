SET LOCAL lock_timeout = '10s';

LOCK TABLE customers IN ACCESS EXCLUSIVE MODE;

ALTER TABLE customers
    ALTER COLUMN first_name SET NOT NULL,
    ALTER COLUMN last_name SET NOT NULL;

DROP TRIGGER customers_sync_names ON customers;
DROP FUNCTION sync_customer_names();

ALTER TABLE customers
    DROP CONSTRAINT customers_first_name_present,
    DROP CONSTRAINT customers_last_name_present,
    DROP COLUMN full_name;
