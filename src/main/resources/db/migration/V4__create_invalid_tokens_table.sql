CREATE TABLE invalid_tokens (
    id BIGSERIAL PRIMARY KEY,
    token_jti VARCHAR(255) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    invalidated_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_invalid_tokens_user_id ON invalid_tokens(user_id);
CREATE INDEX idx_invalid_tokens_expires_at ON invalid_tokens(expires_at);
