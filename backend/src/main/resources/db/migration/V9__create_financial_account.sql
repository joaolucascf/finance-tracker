CREATE TABLE financial_account (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT          NOT NULL,
    connection_id       BIGINT          NOT NULL,
    external_account_id VARCHAR(255)    NOT NULL,
    name                VARCHAR(100)    NOT NULL,
    type                VARCHAR(30)     NOT NULL,
    current_balance     NUMERIC(15,2)   NOT NULL DEFAULT 0,
    currency            VARCHAR(10)     NOT NULL DEFAULT 'BRL',
    created_at          TIMESTAMP       NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP       NOT NULL DEFAULT now(),

    CONSTRAINT fk_fa_user
        FOREIGN KEY (user_id)
        REFERENCES app_user(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_fa_connection
        FOREIGN KEY (connection_id)
        REFERENCES financial_connection(id)
        ON DELETE CASCADE
);

ALTER TABLE transaction
    ADD CONSTRAINT fk_transaction_source_account
    FOREIGN KEY (source_account_id)
    REFERENCES financial_account(id)
    ON DELETE SET NULL;
