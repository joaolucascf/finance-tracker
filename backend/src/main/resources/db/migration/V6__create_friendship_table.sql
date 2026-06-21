CREATE TABLE friendship (
    id           BIGSERIAL    PRIMARY KEY,
    requester_id BIGINT       NOT NULL,
    addressee_id BIGINT       NOT NULL,
    status       VARCHAR(20)  NOT NULL,
    created_at   TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at   TIMESTAMP,

    CONSTRAINT fk_friendship_requester
        FOREIGN KEY (requester_id)
        REFERENCES app_user (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_friendship_addressee
        FOREIGN KEY (addressee_id)
        REFERENCES app_user (id)
        ON DELETE CASCADE,

    CONSTRAINT chk_friendship_not_self
        CHECK (requester_id <> addressee_id),

    CONSTRAINT uq_friendship_pair
        UNIQUE (requester_id, addressee_id)
);

CREATE INDEX ix_friendship_requester ON friendship (requester_id);
CREATE INDEX ix_friendship_addressee ON friendship (addressee_id);
