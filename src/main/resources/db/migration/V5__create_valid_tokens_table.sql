CREATE TABLE valid_tokens (
    id BIGSERIAL PRIMARY KEY,
    token_jti VARCHAR(255) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    keycloak_subject VARCHAR(255) NOT NULL,
    issued_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_valid_tokens_user_id ON valid_tokens(user_id);
CREATE INDEX idx_valid_tokens_expires_at ON valid_tokens(expires_at);
CREATE INDEX idx_valid_tokens_jti ON valid_tokens(token_jti);
