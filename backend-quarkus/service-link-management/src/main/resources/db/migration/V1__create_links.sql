CREATE TABLE links (
    id UUID PRIMARY KEY,
    code VARCHAR(32) NOT NULL UNIQUE,
    target_url TEXT NOT NULL,
    title VARCHAR(200),
    owner_id VARCHAR(255),
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    disabled_reason VARCHAR(16),
    expires_at TIMESTAMPTZ,
    redirect_type SMALLINT NOT NULL DEFAULT 302,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT links_status_check CHECK (status IN ('ACTIVE', 'DISABLED', 'DELETED')),
    CONSTRAINT links_redirect_type_check CHECK (redirect_type IN (301, 302))
);

CREATE INDEX links_owner_created_idx ON links (owner_id, created_at DESC);
CREATE INDEX links_updated_idx ON links (updated_at);
