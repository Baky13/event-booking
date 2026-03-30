CREATE TABLE events (
    id              BIGSERIAL       PRIMARY KEY,
    title           VARCHAR(200)    NOT NULL,
    description     TEXT,
    event_date      TIMESTAMP       NOT NULL,
    location        VARCHAR(300)    NOT NULL,
    max_seats       INTEGER         NOT NULL,
    available_seats INTEGER         NOT NULL,
    organizer_id    BIGINT          NOT NULL REFERENCES users(id),
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE TABLE bookings (
    id               BIGSERIAL    PRIMARY KEY,
    event_id         BIGINT       NOT NULL REFERENCES events(id),
    user_id          BIGINT       NOT NULL REFERENCES users(id),
    status           VARCHAR(20)  NOT NULL,
    waitlist_position INTEGER,
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
    cancelled_at     TIMESTAMP
);

CREATE UNIQUE INDEX idx_booking_unique_active
    ON bookings (event_id, user_id)
    WHERE status != 'CANCELLED';
