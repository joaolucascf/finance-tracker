CREATE TABLE financial_connection (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    institution_name VARCHAR(100) NOT NULL,
    provider    VARCHAR(30)  NOT NULL,
    external_item_id VARCHAR(255) NOT NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT now(),

    CONSTRAINT fk_fc_user
        FOREIGN KEY (user_id)
        REFERENCES app_user(id)
        ON DELETE CASCADE
);
