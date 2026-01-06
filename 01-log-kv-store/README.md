# Week 1 — Append-Only Log Key-Value Store

This module implements a minimal append-only log key-value store used for Week 1 exercises.

What’s included:

- `LogSegment.java` — append-only segment file (simple binary format)
- `StorageEngine.java` — in-memory index + append/get APIs
- `RecoveryManager.java` — replay log to rebuild index on startup
- `Demo.java` — small runnable demonstration

Quick run (from repository root):

```bash
# compile the module sources (simple javac demo)
javac -d out 01-log-kv-store/src/main/java/chand/ankit/kv/*.java

# run the demo
java -cp out chand.ankit.kv.Demo
```

Next steps:
- Add unit tests (JUnit)
- Integrate this module into the project build (Maven/Gradle)
- Iterate on file format (add CRC, timestamps, tombstones)

## Key–Value Stores: Overview & Key/Value Types

This section summarizes common key–value systems and how they treat keys and values. Use this as guidance when choosing serialization and API surface for the storage engine.

- **Redis** — in-memory store. Keys are binary-safe strings; values are either binary strings or higher-level server-side data types (lists, sets, hashes, sorted sets).
- **RocksDB / LevelDB** — embedded LSM stores. Keys and values are opaque byte arrays; client is responsible for serialization and ordering semantics.
- **LMDB / Berkeley DB** — embedded B-tree stores. Keys/values are binary blobs; some language bindings provide typed helpers.
- **DynamoDB (AWS)** — managed store with typed attributes. Keys have explicit types (String, Number, Binary); values (attributes) carry types and schemas.
- **Cassandra** — wide-column store with typed partition and clustering keys via CQL; column values are typed by table schema.
- **FoundationDB** — ordered key–value core (bytes) with higher-level typed layers built atop (Record Layer, directory layer).
- **etcd / Consul** — distributed config KV. Keys are strings; values are binary (often JSON/YAML text).
- **Aerospike** — keys and bin values are typed (int, string, list, map, blob) and enforced by the server.

How key/value types are typically defined

- **Binary-first (embedded stores):** Core API exposes `byte[]` (or ByteBuffer); the store does not interpret content. Ordering, compaction, and indexing assume byte semantics.
- **String-first (config stores):** Keys are UTF-8 strings; values often text (JSON/YAML) or binary blobs.
- **Server-enforced types:** Some DBs enforce attribute/column types server-side (DynamoDB, Cassandra, Aerospike). Clients must conform to the schema.
- **Value-structure APIs:** Systems like Redis provide first-class structured types (lists, sets) with semantics beyond raw bytes.
- **Layered approach:** Provide a byte-based core and build typed layers and serializers on top (recommended for flexibility).

Common serialization patterns

- **UTF-8 strings** for human-readable text.
- **JSON** for flexible, schema-less structures (easy but larger).
- **Protobuf / Avro / Thrift** for compact, versioned binary schemas.
- **Custom binary formats / zero-copy** (ByteBuffer) for high-performance scenarios.

Practical recommendations for this project

- Keep the core storage API `byte[]`-based for maximal flexibility and performance (we already expose `put(String, byte[])` and `get(String)`).
- Provide typed wrappers and a `Serializer<T>` interface (added in this module) for ergonomics; document encoding and null semantics clearly.
- Offer convenience helpers for common types (e.g., `StringSerializer`, `putString`/`getString`) while keeping the byte API as the canonical contract.
- For large values, consider streaming APIs or using file-backed storage to avoid buffering huge payloads in memory.
- When adding higher-level query or indexing features, define explicit typed schemas or use a compact binary schema (Protobuf) to avoid brittle JSON parsing under load.

If you want, I can add a `ProtobufSerializer` example or `putString`/`getString` convenience methods to `StorageEngine`.
