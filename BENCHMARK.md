# Benchmark — 1,000-Document Ingestion & Retrieval

Reproducible load + quality benchmark for the Document Intelligence Platform's agentic RAG pipeline,
run locally against the same Spring Boot backend that ships to production.

Harness: [`scripts/benchmark/BenchmarkHarness.java`](scripts/benchmark/BenchmarkHarness.java)
(single-file Java, no dependencies).

## Method — ground-truth "needle" test

Most "I tested it on N docs" claims are unverifiable. This benchmark makes correctness **exactly measurable**:

1. Generate **N documents**, each about a unique real-word codename (e.g. *Project Velvet Hammer*) with a
   distinctive fingerprint (city, lead, domain, budget, quarter) so documents are lexically separable — like real ones.
2. Hide a **unique random access code** in each document (the "needle").
3. Upload all N concurrently through the real `/api/documents/upload` endpoint.
4. Wait for async ingestion (parse → chunk → embed → index in pgvector) to finish, polling the
   **non-cached** document list (the stats endpoint is `@Cacheable`).
5. Ask the RAG agent for a random sample of needles and check, per query:
   - **Retrieval accuracy** — was the correct source document cited?
   - **End-to-end accuracy** — did the answer contain the exact unique code?

Because each code exists in exactly one document, a correct answer **proves** correct retrieval + grounded generation.

## Results (1,000 documents, 50-query sample)

| Metric | Result |
|---|---|
| Documents ingested | **1,000** (0 failed) |
| Vectors created | 1,000 |
| Ingestion throughput | **~1,076 docs/min** (1,000 indexed in ~56s) |
| Concurrent upload (8 threads) | **1,000 accepted, 0 failed** |
| **Retrieval accuracy (correct source cited)** | **100%** (50/50) |
| End-to-end exact-answer accuracy | **80%** (40/50) |
| Query latency (p50 / p95 / max) | **7.84s / 10.48s / 13.77s** |

> **Retrieval is 100%** — the correct source document is found and cited on every query. The 20% end-to-end gap
> is the LLM layer: every needle is labeled `CONFIDENTIAL`, so the model sometimes declines to repeat the code it
> correctly retrieved. RAG search quality and answer behavior are therefore measured separately.

Environment: local dev profile (Caffeine cache), local PostgreSQL 18 + pgvector (exact search, `index-type: NONE`),
OpenAI `text-embedding-3-small`, Claude for agentic tool-calling. Latency is dominated by LLM generation, not vector search.

## Engineering finding — async-executor saturation → backpressure

Load-testing the ingestion path surfaced a real concurrency bug:

- **Symptom:** under burst upload, exactly **60 ok / 40 failed** for 100 concurrent uploads (HTTP 500); 40 documents
  stuck permanently in `PROCESSING`.
- **Root cause:** the ingestion `ThreadPoolTaskExecutor` (core 5, max 10, queue 50) saturates at
  `maxPool + queueCapacity = 60` in-flight tasks; the default `AbortPolicy` then throws `TaskRejectedException`.
  Because the upload commits the document row *before* submitting the async task, rejected tasks orphaned the rows.
  The **deterministic** 60/40 split (independent of thread count) was the tell — a fixed queue depth, not contention.
- **Fix:** `executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy())` — when saturated, the
  submitting thread runs the task itself, applying **backpressure**. After the fix: **1,000 / 0** uploads, no orphans.

## Scaling notes (what changes beyond 1,000 docs)

- **Retrieval:** exact search (`index-type: NONE`) is accurate to tens of thousands of vectors; switch to **HNSW**
  beyond that. Dense retrieval also degrades on near-duplicate documents keyed by opaque IDs — production systems add
  **hybrid (vector + keyword/BM25) search** and tune `topK`.
- **Ingestion:** already async + batched embeddings (batch 20); scale the executor and parallelize embedding calls,
  or move to an event-driven worker (SQS/Kafka) with retry/DLQ for independent scaling.
- **Resilience:** add a startup reaper to mark interrupted `PROCESSING` documents as `FAILED`.

## Reproduce

```bash
# backend running locally on :8081 (dev profile, local Postgres + pgvector)
java scripts/benchmark/BenchmarkHarness.java http://localhost:8081 admin@docint.com admin123 1000 8 50
# args: <baseUrl> <email> <password> [numDocs=1000] [uploadThreads=8] [querySample=50]
```
