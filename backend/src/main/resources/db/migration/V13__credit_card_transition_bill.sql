-- Transition (open-cycle) bills live in credit_card_bill with no provider bill yet:
-- external_bill_id NULL, status OPEN, due_date = previous due + 1 month (or NULL for a
-- brand-new card). Real (closed) Pluggy bills always carry external_bill_id.
ALTER TABLE credit_card_bill
    ALTER COLUMN external_bill_id DROP NOT NULL,
    ALTER COLUMN due_date         DROP NOT NULL,
    ALTER COLUMN bill_sequence    DROP NOT NULL;

-- At most one open transition bill per account (the current cycle).
CREATE UNIQUE INDEX uq_ccb_open_transition
    ON credit_card_bill (account_id)
    WHERE external_bill_id IS NULL;

-- provider_billed is gone: orphan purchases keep bill_id NULL and the transition bill
-- aggregates the EXPENSE ones; closed bills come straight from the provider.
ALTER TABLE imported_transaction
    DROP COLUMN provider_billed;
