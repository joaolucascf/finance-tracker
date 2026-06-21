CREATE TABLE imported_transaction (
    id                      BIGSERIAL    PRIMARY KEY,
    transaction_id          BIGINT,
    external_transaction_id VARCHAR(255) NOT NULL,
    account_id              BIGINT       NOT NULL,
    provider                VARCHAR(30)  NOT NULL,
    imported_at             TIMESTAMP    NOT NULL DEFAULT now(),

    CONSTRAINT fk_it_transaction
        FOREIGN KEY (transaction_id)
        REFERENCES transaction(id)
        ON DELETE SET NULL,

    CONSTRAINT fk_it_account
        FOREIGN KEY (account_id)
        REFERENCES financial_account(id)
        ON DELETE CASCADE,

    CONSTRAINT uq_it_provider_external
        UNIQUE (provider, external_transaction_id)
);
