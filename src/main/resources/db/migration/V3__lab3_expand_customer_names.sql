SET LOCAL lock_timeout = '10s';

CREATE FUNCTION split_customer_name(full_name TEXT)
RETURNS TABLE (first_name TEXT, last_name TEXT)
LANGUAGE sql IMMUTABLE STRICT AS $$
    SELECT coalesce(parts[1], $1), coalesce(parts[2], '')
    FROM (SELECT regexp_match($1, '^( *[^ ]+) (.+)$') AS parts) AS matched;
$$;

ALTER TABLE customers
    ADD COLUMN first_name VARCHAR(255),
    ADD COLUMN last_name VARCHAR(255),
    ADD CONSTRAINT customers_first_name_present CHECK (first_name IS NOT NULL) NOT VALID,
    ADD CONSTRAINT customers_last_name_present CHECK (last_name IS NOT NULL) NOT VALID,
    ADD CONSTRAINT customers_name_length CHECK (
        length(first_name) + length(last_name) + CASE WHEN last_name = '' THEN 0 ELSE 1 END <= 255
    ) NOT VALID;

CREATE FUNCTION sync_customer_names() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE
    legacy_write BOOLEAN;
BEGIN
    IF TG_OP = 'INSERT' THEN
        legacy_write := NEW.full_name IS NOT NULL;
    ELSE
        legacy_write := NEW.full_name IS DISTINCT FROM OLD.full_name
            OR NEW.first_name IS NULL OR NEW.last_name IS NULL;
    END IF;

    IF legacy_write THEN
        SELECT first_name, last_name INTO NEW.first_name, NEW.last_name
        FROM split_customer_name(NEW.full_name);
    ELSIF NEW.last_name = '' THEN
        NEW.full_name := NEW.first_name;
    ELSE
        NEW.full_name := NEW.first_name || ' ' || NEW.last_name;
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER customers_sync_names
BEFORE INSERT OR UPDATE ON customers
FOR EACH ROW EXECUTE FUNCTION sync_customer_names();
