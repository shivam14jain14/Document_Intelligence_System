-- Enable pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

-- Metadata store for document chunks (Spring AI manages embeddings in its own vector_store table)
CREATE TABLE document_chunks (
    id          UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID      NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    chunk_index INT       NOT NULL,
    content     TEXT      NOT NULL,
    token_count INT,
    start_char  INT,
    end_char    INT,
    metadata    JSONB     DEFAULT '{}',
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_chunks_document_id ON document_chunks(document_id);
