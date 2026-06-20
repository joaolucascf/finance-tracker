CREATE TABLE user_profile (
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT        NOT NULL UNIQUE,
    nickname       VARCHAR(100),
    birth_date     DATE,
    monthly_income NUMERIC(15, 2),
    marital_status VARCHAR(20),
    photo          BYTEA,
    photo_type     VARCHAR(50),

    CONSTRAINT fk_user_profile_user
        FOREIGN KEY (user_id)
        REFERENCES app_user (id)
        ON DELETE CASCADE
);
