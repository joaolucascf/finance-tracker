CREATE TABLE category (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    is_default BOOLEAN NOT NULL,
    user_id BIGINT,

    CONSTRAINT fk_category_user
        FOREIGN KEY (user_id)
        REFERENCES app_user(id)
        ON DELETE CASCADE,

    CONSTRAINT category_consistency_check
    CHECK (
        (is_default = true AND user_id IS NULL)
        OR
        (is_default = false AND user_id IS NOT NULL)
    )
);

CREATE UNIQUE INDEX ux_category_user_name
ON category(user_id, name)
WHERE is_default = false;

INSERT INTO category (name, is_default, user_id) VALUES
('Alimentação', true, NULL),
('Lazer', true, NULL),
('Saúde', true, NULL),
('Educação', true, NULL),
('Despesas Domésticas', true, NULL);