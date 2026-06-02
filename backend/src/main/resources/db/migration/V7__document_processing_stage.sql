-- Fine-grained ingestion stage within the PROCESSING status
-- (PARSING, CHUNKING, EMBEDDING, DONE) — surfaced to the UI as a live progress popup.
ALTER TABLE documents ADD COLUMN processing_stage VARCHAR(30);
