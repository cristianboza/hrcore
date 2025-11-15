-- Flyway V2 migration: create feedback table

CREATE TABLE IF NOT EXISTS feedback (
    id SERIAL PRIMARY KEY,
    from_user_id BIGINT NOT NULL,
    to_user_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    polished_content TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (from_user_id) REFERENCES users(id),
    FOREIGN KEY (to_user_id) REFERENCES users(id)
);

CREATE INDEX idx_feedback_to_user ON feedback(to_user_id);
CREATE INDEX idx_feedback_from_user ON feedback(from_user_id);
CREATE INDEX idx_feedback_status ON feedback(status);

