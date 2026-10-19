CREATE TABLE outbox_event (
    event_id UUID PRIMARY KEY,
    operation_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP NULL,
    attempts INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_outbox_event_pending
    ON outbox_event (created_at)
    WHERE published_at IS NULL;