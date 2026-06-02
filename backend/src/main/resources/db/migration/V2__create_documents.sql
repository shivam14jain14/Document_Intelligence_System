CREATE TABLE documents (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(500) NOT NULL,
    category        VARCHAR(100) NOT NULL,
    source          VARCHAR(50)  NOT NULL,          -- LOCAL, S3, AZURE_BLOB, SHAREPOINT
    source_path     TEXT         NOT NULL,           -- storage key / URL
    file_type       VARCHAR(20)  NOT NULL,           -- PDF, DOCX, XLSX, PPTX, TXT
    file_size_bytes BIGINT,
    file_hash       VARCHAR(64)  UNIQUE,             -- SHA-256, idempotency guard
    status          VARCHAR(20)  NOT NULL DEFAULT 'PROCESSING', -- PROCESSING, INDEXED, FAILED, NEEDS_OCR
    error_message   TEXT,
    page_count      INT,
    chunk_count     INT,
    uploaded_by     UUID         REFERENCES users(id) ON DELETE SET NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_documents_category  ON documents(category);
CREATE INDEX idx_documents_status    ON documents(status);
CREATE INDEX idx_documents_uploaded_by ON documents(uploaded_by);
CREATE INDEX idx_documents_created_at ON documents(created_at DESC);
