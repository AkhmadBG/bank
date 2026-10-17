ALTER TABLE notifications
    ADD COLUMN IF NOT EXISTS event_id UUID;

CREATE UNIQUE INDEX IF NOT EXISTS uk_notifications_event_id
    ON notifications (event_id);