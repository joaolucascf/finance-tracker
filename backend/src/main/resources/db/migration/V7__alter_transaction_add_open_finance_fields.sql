ALTER TABLE transaction
    ADD COLUMN imported BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN source_account_id BIGINT;
