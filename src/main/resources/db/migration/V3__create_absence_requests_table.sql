-- Flyway V3 migration: create absence_requests table

CREATE TABLE IF NOT EXISTS absence_requests (
    id SERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    reason TEXT,
    type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    approver_id BIGINT,
    rejection_reason TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (approver_id) REFERENCES users(id)
);

CREATE INDEX idx_absence_user ON absence_requests(user_id);
CREATE INDEX idx_absence_status ON absence_requests(status);
CREATE INDEX idx_absence_dates ON absence_requests(start_date, end_date);

