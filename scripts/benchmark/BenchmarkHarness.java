// Document Intelligence — 1,000-document benchmark & evaluation harness
// ---------------------------------------------------------------------
// Generates N synthetic documents, each hiding a UNIQUE "needle" fact (a random
// access code), uploads them concurrently under a per-run category, waits for
// ingestion to finish, then asks the RAG agent to recall each needle. Because we
// KNOW the ground-truth answer for every document, we measure REAL retrieval +
// answer accuracy — not just throughput.
//
// No external libraries. Java 11+ single-file launch:
//
//   java scripts/benchmark/BenchmarkHarness.java \
//        http://localhost:8081 admin@docint.com admin123 1000
//
// Args: <baseUrl> <email> <password> [numDocs=1000] [uploadThreads=8] [querySample=50]
//
// RECOMMENDED: run against your LOCAL backend (local Postgres), NOT the t2.micro.
// Each run uses a UNIQUE category + unique content, so re-runs are NOT deduped.
// Cost: ~N embeddings + ~querySample chat calls => well under $1 on the LLM APIs.

import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.regex.*;

public class BenchmarkHarness {

    static HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
    static String base, token;
    static final AtomicBoolean FIRST_UP_ERR = new AtomicBoolean(false);
    static boolean verbose;

    record Doc(String codename, String filename, String code, byte[] body) {}

    // Real-word codenames embed distinctively (unlike opaque IDs like DOC-00042, which are
    // near-identical to each other under dense embeddings). 40 x 40 = 1600 unique combos.
    static final String[] ADJ = {"Velvet","Crimson","Silent","Golden","Iron","Azure","Rapid","Hidden","Lunar","Solar",
        "Amber","Cobalt","Ivory","Onyx","Scarlet","Emerald","Frost","Granite","Maple","Nimbus","Quartz","Raven","Sable",
        "Tundra","Vivid","Willow","Zephyr","Brisk","Coral","Dusk","Ember","Polar","Royal","Swift","Twilight","Umber",
        "Verdant","Crystal","Stormy","Noble"};
    static final String[] NOUN = {"Hammer","Falcon","River","Summit","Harbor","Lantern","Compass","Meadow","Canyon",
        "Beacon","Cipher","Anchor","Comet","Forge","Glacier","Horizon","Juniper","Kestrel","Lagoon","Marble","Needle",
        "Orchard","Pioneer","Quiver","Ridge","Sentinel","Talon","Vault","Warden","Yonder","Aurora","Bastion","Citadel",
        "Delta","Echo","Fathom","Grove","Helix","Inlet","Jetty"};
    // Per-doc unique facts so each document has a distinctive lexical fingerprint (like real docs),
    // instead of 1000 near-identical templated files.
    static final String[] CITIES = {"Helsinki","Lisbon","Denver","Osaka","Nairobi","Toronto","Munich","Bogota","Manila",
        "Dublin","Austin","Bergen","Cairo","Perth","Quito","Riga","Seville","Tallinn","Utrecht","Vienna","Warsaw","Zurich","Bristol","Geneva"};
    static final String[] LEADS = {"Mara Okafor","Diego Silva","Priya Nair","Lars Eriksen","Yuki Tanaka","Sofia Rossi",
        "Omar Haddad","Elena Petrova","Kwame Mensah","Ingrid Holm","Rafael Costa","Nadia Karim","Tomas Novak","Aisha Bello",
        "Henrik Dahl","Lucia Marin","Viktor Popov","Mei Lin","Pablo Reyes","Hanna Virtanen","Sara Cohen","Marco Bianchi","Leah Goldberg","Noah Frank"};
    static final String[] DOMAINS = {"regional logistics rollout","customer onboarding revamp","data warehouse migration",
        "mobile app relaunch","supplier compliance review","network security upgrade","billing platform overhaul",
        "analytics dashboard redesign","warehouse automation pilot","payments gateway integration","fraud detection retraining",
        "loyalty program launch","content moderation tooling","inventory forecasting model","identity federation rollout","edge caching deployment"};

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.out.println("Usage: java BenchmarkHarness.java <baseUrl> <email> <password> [numDocs] [uploadThreads] [querySample]");
            return;
        }
        base = args[0].replaceAll("/$", "");
        String email = args[1], password = args[2];
        int numDocs       = args.length > 3 ? Integer.parseInt(args[3]) : 1000;
        int uploadThreads = args.length > 4 ? Integer.parseInt(args[4]) : 8;
        int querySample   = args.length > 5 ? Integer.parseInt(args[5]) : 50;
        verbose = numDocs <= 20;

        String runId = Long.toString(System.currentTimeMillis(), 36).toUpperCase();
        String category = "Bench-" + runId;   // unique per run => no dedupe, run-scoped queries
        System.out.printf("== Benchmark run %s: %d docs, %d threads, %d queries (category=%s) ==%n",
                runId, numDocs, uploadThreads, querySample, category);

        // ---- 0. Generate corpus (semantically DISTINCT per doc, unique content per run) ----
        Random rnd = new Random();
        List<String> combos = new ArrayList<>();
        for (String a : ADJ) for (String n : NOUN) combos.add(a + " " + n);
        Collections.shuffle(combos, rnd);
        if (numDocs > combos.size()) { System.out.println("Max " + combos.size() + " distinct docs; capping."); numDocs = combos.size(); }
        List<Doc> corpus = new ArrayList<>(numDocs);
        for (int i = 0; i < numDocs; i++) {
            String codename = combos.get(i);                       // e.g. "Velvet Hammer"
            String filename = "Project-" + codename.replace(" ", "-") + ".txt";
            String code = randomCode(rnd);
            byte[] body = buildDocument(codename, code, rnd).getBytes(StandardCharsets.UTF_8);
            corpus.add(new Doc(codename, filename, code, body));
        }
        System.out.println("Generated " + corpus.size() + " documents.");

        // ---- 1. Login ----
        token = extract(post("/api/auth/login",
                "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"), "token");
        if (token == null) { System.out.println("Login failed — check credentials/backend."); return; }
        System.out.println("Logged in.");

        // ---- 2. Upload concurrently ----
        System.out.println("Uploading...");
        long upStart = System.nanoTime();
        ExecutorService pool = Executors.newFixedThreadPool(uploadThreads);
        AtomicInteger ok = new AtomicInteger(), fail = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();
        for (Doc d : corpus) {
            futures.add(pool.submit(() -> {
                try {
                    int sc = uploadFile(d.filename(), d.body(), category);
                    if (sc >= 200 && sc < 300) ok.incrementAndGet(); else fail.incrementAndGet();
                } catch (Exception e) { fail.incrementAndGet(); }
            }));
        }
        for (Future<?> f : futures) f.get();
        pool.shutdown();
        double uploadSecs = (System.nanoTime() - upStart) / 1e9;
        System.out.printf("Uploaded: %d ok, %d failed in %.1fs (%.1f docs/s accepted)%n",
                ok.get(), fail.get(), uploadSecs, ok.get() / uploadSecs);
        if (ok.get() == 0) { System.out.println("All uploads failed — aborting."); return; }

        // ---- 3. Wait for async ingestion (poll FRESH, run-scoped list endpoint) ----
        System.out.println("Waiting for ingestion (embedding) to complete...");
        long ingStart = System.nanoTime();
        int[] s; long chunks = 0, lastChunks = -1; int stall = 0;
        while (true) {
            Thread.sleep(4000);
            s = pollRun(category, numDocs);   // {indexed, failed, processing, chunks}
            chunks = s[3];
            System.out.printf("  indexed=%d processing=%d failed=%d chunks=%d%n", s[0], s[2], s[1], chunks);
            if (s[2] == 0 && (s[0] + s[1]) >= ok.get()) break;   // nothing processing & all accounted for
            // Stall guard: if no progress for ~32s while docs remain in PROCESSING, they're orphaned — stop waiting.
            if (chunks == lastChunks) {
                if (++stall >= 8) { System.out.println("  [stalled: " + s[2] + " docs stuck in PROCESSING — proceeding with indexed set]"); break; }
            } else stall = 0;
            lastChunks = chunks;
        }
        double ingestSecs = (System.nanoTime() - ingStart) / 1e9 + uploadSecs;
        int indexed = s[0];
        System.out.printf("Ingestion done: %d indexed, %d failed, %d chunks/vectors, ~%.0fs total (%.0f docs/min)%n",
                indexed, s[1], chunks, ingestSecs, indexed / (ingestSecs / 60.0));

        // ---- 4. Retrieval + answer accuracy on a random sample ----
        System.out.println("Querying " + querySample + " random needles...");
        Collections.shuffle(corpus, rnd);
        List<Double> latencies = new ArrayList<>();
        int answerHits = 0, retrievalHits = 0, asked = 0;
        for (int i = 0; i < Math.min(querySample, corpus.size()); i++) {
            Doc d = corpus.get(i);
            String session = extract(post("/api/chat/sessions", "{}"), "id");
            if (session == null) { if (verbose) System.out.println("  [session create failed]"); continue; }
            // Ask naturally (no "code only") so the model cites its source — the app builds
            // citations by parsing [Source: filename] tags out of the answer text.
            String q = "What is the access code for Project " + d.codename() + "?";
            long t0 = System.nanoTime();
            String resp = post("/api/chat/sessions/" + session + "/messages",
                    "{\"message\":" + jsonStr(q) + ",\"categoryFilter\":" + jsonStr(category) + "}");
            latencies.add((System.nanoTime() - t0) / 1e9);
            asked++;
            if (verbose && i == 0) System.out.println("  [raw first response] " + resp);
            String answer = extract(resp, "answer");
            // citation filename is "Project-Velvet-Hammer.txt"; answer prose says "Velvet Hammer" — match either
            boolean rHit = resp != null && (resp.contains(d.codename()) || resp.contains(d.codename().replace(" ", "-")));
            boolean aHit = answer != null && answer.contains(d.code());
            if (rHit) retrievalHits++;
            if (aHit) answerHits++;
            if (verbose) System.out.printf("  Q[%s] code=%s -> retrieval=%s answer=%s | %s%n",
                    d.codename(), d.code(), rHit, aHit, answer == null ? "(no answer)" : trim(answer));
        }
        Collections.sort(latencies);

        // ---- 5. Report ----
        System.out.println();
        System.out.println("==================== RESULTS ====================");
        System.out.printf("Run id ...................... %s%n", runId);
        System.out.printf("Documents indexed ........... %d  (failed %d)%n", indexed, s[1]);
        System.out.printf("Chunks / vectors created .... %d  (avg %.1f per doc)%n", chunks, chunks / (double) Math.max(1, indexed));
        System.out.printf("Upload throughput ........... %.1f docs/s%n", ok.get() / uploadSecs);
        System.out.printf("End-to-end ingestion ........ %.0fs  (%.0f docs/min)%n", ingestSecs, indexed / (ingestSecs / 60.0));
        System.out.printf("Queries asked ............... %d%n", asked);
        if (asked > 0) {
            System.out.printf("End-to-end accuracy (exact needle) . %.1f%%   <- headline: correct unique code%n", 100.0 * answerHits / asked);
            System.out.printf("Citation accuracy (right source cited) %.1f%%%n", 100.0 * retrievalHits / asked);
            System.out.printf("Query latency p50 / p95 / max ..... %.2fs / %.2fs / %.2fs%n",
                    pct(latencies, 50), pct(latencies, 95), latencies.get(latencies.size() - 1));
        }
        System.out.println("=================================================");
        System.out.println("Tip: these docs are in category '" + category + "' — delete them from the UI when done.");
    }

    // ---------- polling the fresh list endpoint ----------
    static int[] pollRun(String category, int numDocs) throws Exception {
        String enc = java.net.URLEncoder.encode(category, StandardCharsets.UTF_8);
        String resp = get("/api/documents?category=" + enc + "&size=" + Math.max(numDocs, 20));
        int indexed = count(resp, "\"status\":\"INDEXED\"");
        int failed  = count(resp, "\"status\":\"FAILED\"");
        int proc    = count(resp, "\"status\":\"PROCESSING\"") + count(resp, "\"status\":\"NEEDS_OCR\"");
        long chunks = 0;
        Matcher m = Pattern.compile("\"chunkCount\":(\\d+)").matcher(resp == null ? "" : resp);
        while (m.find()) chunks += Long.parseLong(m.group(1));
        return new int[]{indexed, failed, proc, (int) chunks};
    }

    // ---------- document generation ----------
    static String randomCode(Random r) {
        String a = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder s = new StringBuilder("AC-");
        for (int i = 0; i < 8; i++) s.append(a.charAt(r.nextInt(a.length())));
        return s.toString();
    }
    static String buildDocument(String codename, String code, Random r) {
        // Each doc carries a UNIQUE fingerprint (codename + city + lead + domain + budget + quarter),
        // so its embedding is separable from the other 999 — a fair retrieval benchmark.
        String city = CITIES[r.nextInt(CITIES.length)];
        String lead = LEADS[r.nextInt(LEADS.length)];
        String domain = DOMAINS[r.nextInt(DOMAINS.length)];
        int budget = 50 + r.nextInt(950);   // $k
        int q = 1 + r.nextInt(4);
        StringBuilder sb = new StringBuilder();
        sb.append("# Project ").append(codename).append(" — ").append(city).append(" office\n\n");
        sb.append("Project ").append(codename).append(" leads the ").append(domain).append(" out of the ")
          .append(city).append(" office, managed by ").append(lead).append(" with a Q").append(q)
          .append(" budget of $").append(budget).append("k.\n\n");
        int paras = 4 + r.nextInt(3);
        for (int p = 0; p < paras; p++) {
            if (p == paras / 2) {
                sb.append("CONFIDENTIAL: The access code for Project ").append(codename)
                  .append(" is ").append(code).append(". Keep this code secure.\n\n");
            }
            sb.append(lead).append(" reports that the ").append(domain).append(" for Project ").append(codename)
              .append(" in ").append(city).append(" is tracking to its Q").append(q)
              .append(" milestones, with risks reviewed weekly and the $").append(budget).append("k budget on plan.\n\n");
        }
        return sb.toString();
    }

    // ---------- HTTP helpers ----------
    static String post(String path, String json) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + path))
                .timeout(Duration.ofSeconds(240))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json));
        if (token != null) b.header("Authorization", "Bearer " + token);
        return http.send(b.build(), HttpResponse.BodyHandlers.ofString()).body();
    }
    static int uploadFile(String filename, byte[] content, String category) throws Exception {
        String boundary = "----b" + System.nanoTime();
        var out = new java.io.ByteArrayOutputStream();
        java.util.function.BiConsumer<String, byte[]> part = (header, data) -> {
            try { out.write(header.getBytes(StandardCharsets.UTF_8)); out.write(data); out.write("\r\n".getBytes()); }
            catch (Exception ignored) {}
        };
        String pre = "--" + boundary + "\r\n";
        part.accept(pre + "Content-Disposition: form-data; name=\"file\"; filename=\"" + filename + "\"\r\n"
                + "Content-Type: text/plain\r\n\r\n", content);
        part.accept(pre + "Content-Disposition: form-data; name=\"category\"\r\n\r\n", category.getBytes());
        part.accept(pre + "Content-Disposition: form-data; name=\"storageProvider\"\r\n\r\n", "LOCAL".getBytes());
        out.write(("--" + boundary + "--\r\n").getBytes());

        HttpRequest req = HttpRequest.newBuilder(URI.create(base + "/api/documents/upload"))
                .timeout(Duration.ofSeconds(120))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(out.toByteArray()))
                .build();
        HttpResponse<String> r = http.send(req, HttpResponse.BodyHandlers.ofString());
        if ((r.statusCode() < 200 || r.statusCode() >= 300) && FIRST_UP_ERR.compareAndSet(false, true))
            System.out.println("  [first upload failure] HTTP " + r.statusCode() + " -> " + trim(r.body()));
        return r.statusCode();
    }
    static String get(String path) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(base + path))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + token).GET().build();
        return http.send(req, HttpResponse.BodyHandlers.ofString()).body();
    }

    // ---------- tiny JSON / util helpers ----------
    static int count(String s, String needle) {
        if (s == null) return 0;
        int n = 0, i = 0;
        while ((i = s.indexOf(needle, i)) >= 0) { n++; i += needle.length(); }
        return n;
    }
    static String extract(String json, String key) {
        if (json == null) return null;
        Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"").matcher(json);
        if (!m.find()) return null;
        return m.group(1).replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\");
    }
    static String jsonStr(String s) { return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\""; }
    static String trim(String s) { s = s.replaceAll("\\s+", " ").trim(); return s.length() > 120 ? s.substring(0, 120) + "..." : s; }
    static double pct(List<Double> sorted, int p) {
        if (sorted.isEmpty()) return 0;
        int idx = (int) Math.ceil(p / 100.0 * sorted.size()) - 1;
        return sorted.get(Math.max(0, Math.min(idx, sorted.size() - 1)));
    }
}
