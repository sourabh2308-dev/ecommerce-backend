-- V6: Notifications, Support Tickets, and Loyalty Points

-- ── Notifications ──────────────────────────────────────
CREATE TABLE IF NOT EXISTS notifications (
    id           BIGSERIAL    PRIMARY KEY,
    uuid         VARCHAR(36)  NOT NULL UNIQUE,
    user_uuid    VARCHAR(36)  NOT NULL,
    type         VARCHAR(30)  NOT NULL,
    title        VARCHAR(255) NOT NULL,
    message      VARCHAR(1000),
    reference_id VARCHAR(100),
    is_read      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP    NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_notif_user      ON notifications(user_uuid);
CREATE INDEX IF NOT EXISTS idx_notif_user_read ON notifications(user_uuid, is_read);

-- ── Support Tickets ────────────────────────────────────
CREATE TABLE IF NOT EXISTS support_tickets (
    id                 BIGSERIAL    PRIMARY KEY,
    uuid               VARCHAR(36)  NOT NULL UNIQUE,
    user_uuid          VARCHAR(36)  NOT NULL,
    subject            VARCHAR(200) NOT NULL,
    description        VARCHAR(2000) NOT NULL,
    category           VARCHAR(30)  NOT NULL DEFAULT 'OTHER',
    status             VARCHAR(30)  NOT NULL DEFAULT 'OPEN',
    order_uuid         VARCHAR(36),
    assigned_admin_uuid VARCHAR(36),
    resolved_at        TIMESTAMP,
    created_at         TIMESTAMP,
    updated_at         TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ticket_user   ON support_tickets(user_uuid);
CREATE INDEX IF NOT EXISTS idx_ticket_status ON support_tickets(status);

-- ── Support Messages ───────────────────────────────────
CREATE TABLE IF NOT EXISTS support_messages (
    id          BIGSERIAL    PRIMARY KEY,
    ticket_id   BIGINT       NOT NULL REFERENCES support_tickets(id) ON DELETE CASCADE,
    sender_uuid VARCHAR(36)  NOT NULL,
    sender_role VARCHAR(20)  NOT NULL,
    content     VARCHAR(2000) NOT NULL,
    created_at  TIMESTAMP    NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_msg_ticket ON support_messages(ticket_id);

-- ── Loyalty Points ─────────────────────────────────────
CREATE TABLE IF NOT EXISTS loyalty_points (
    id            BIGSERIAL    PRIMARY KEY,
    user_uuid     VARCHAR(36)  NOT NULL,
    type          VARCHAR(30)  NOT NULL,
    points        INTEGER      NOT NULL,
    balance_after INTEGER      NOT NULL,
    reference_id  VARCHAR(100),
    description   VARCHAR(500),
    created_at    TIMESTAMP    NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_loyalty_user ON loyalty_points(user_uuid);
