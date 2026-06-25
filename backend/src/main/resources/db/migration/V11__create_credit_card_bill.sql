CREATE TABLE credit_card_bill (
    id               BIGSERIAL     PRIMARY KEY,
    account_id       BIGINT        NOT NULL,
    provider         VARCHAR(30)   NOT NULL,
    external_bill_id VARCHAR(255)  NOT NULL,
    due_date         DATE          NOT NULL,
    total_amount     NUMERIC(15,2) NOT NULL DEFAULT 0,
    status           VARCHAR(10)   NOT NULL DEFAULT 'OPEN',
    bill_sequence    INT           NOT NULL DEFAULT 0,
    custom_name      VARCHAR(60),
    created_at       TIMESTAMP     NOT NULL DEFAULT now(),
    updated_at       TIMESTAMP     NOT NULL DEFAULT now(),

    CONSTRAINT fk_ccb_account
        FOREIGN KEY (account_id)
        REFERENCES financial_account(id)
        ON DELETE CASCADE,

    CONSTRAINT uq_ccb_provider_external
        UNIQUE (provider, external_bill_id)
);

ALTER TABLE transaction
    ADD COLUMN bill_id            BIGINT,
    ADD COLUMN installment_number INT,
    ADD COLUMN total_installments INT,
    ADD CONSTRAINT fk_transaction_bill
        FOREIGN KEY (bill_id)
        REFERENCES credit_card_bill(id)
        ON DELETE SET NULL;

ALTER TABLE imported_transaction
    ADD COLUMN provider_billed BOOLEAN NOT NULL DEFAULT FALSE;
