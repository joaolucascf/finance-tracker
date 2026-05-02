CREATE TABLE transaction (
    id BIGSERIAL PRIMARY KEY,

    amount NUMERIC(15,2) NOT NULL,
    type VARCHAR(20) NOT NULL,
    description VARCHAR(60),
    date DATE NOT NULL,

    user_id BIGINT NOT NULL,
    category_id BIGINT,

    CONSTRAINT fk_transaction_user
        FOREIGN KEY (user_id)
        REFERENCES app_user(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_transaction_category
        FOREIGN KEY (category_id)
        REFERENCES category(id)
        ON DELETE SET NULL
);