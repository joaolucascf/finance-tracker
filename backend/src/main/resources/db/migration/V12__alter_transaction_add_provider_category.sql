-- Raw provider category (e.g. Pluggy's "Investments"). Kept verbatim so we can both
-- hide investment movements from the main ledger today and build a dedicated
-- investments view later without re-syncing. NULL for manual transactions.
ALTER TABLE transaction
    ADD COLUMN provider_category VARCHAR(80);
