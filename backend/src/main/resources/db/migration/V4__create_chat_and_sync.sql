CREATE TABLE chat_sessions (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID         REFERENCES users(id) ON DELETE CASCADE,
    title      VARCHAR(500) NOT NULL DEFAULT 'New Chat',
    created_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_chat_sessions_user_id ON chat_sessions(user_id);

CREATE TABLE chat_messages (
    id             UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id     UUID      NOT NULL REFERENCES chat_sessions(id) ON DELETE CASCADE,
    role           VARCHAR(20) NOT NULL,        -- USER, ASSISTANT
    content        TEXT      NOT NULL,
    source_chunks  JSONB     DEFAULT '[]',      -- chunks Claude used for this answer
    tool_calls     JSONB     DEFAULT '[]',      -- full audit: what tools Claude called
    tokens_used    INT,
    created_at     TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_chat_messages_session_id ON chat_messages(session_id);
CREATE INDEX idx_chat_messages_created_at ON chat_messages(created_at);

-- Sync jobs for external source connectors
CREATE TABLE sync_jobs (
    id                 UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    source_type        VARCHAR(50)  NOT NULL,   -- AZURE_BLOB, SHAREPOINT, LOCAL
    source_config      JSONB        NOT NULL DEFAULT '{}',
    category           VARCHAR(100) NOT NULL,
    status             VARCHAR(20)  NOT NULL DEFAULT 'PENDING', -- PENDING, RUNNING, COMPLETED, FAILED
    total_files        INT          DEFAULT 0,
    processed_files    INT          DEFAULT 0,
    failed_files       INT          DEFAULT 0,
    errors             JSONB        DEFAULT '[]',
    initiated_by       UUID         REFERENCES users(id) ON DELETE SET NULL,
    started_at         TIMESTAMP,
    completed_at       TIMESTAMP,
    created_at         TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_sync_jobs_status     ON sync_jobs(status);
CREATE INDEX idx_sync_jobs_created_at ON sync_jobs(created_at DESC);
