# DDIA Java Labs 🚀

Hands-on implementations inspired by *Designing Data-Intensive Applications* (Martin Kleppmann).

- **Language:** Java
- **Focus:** storage engines, replication, partitioning, streaming, transactions
- **Goal:** learn data-intensive systems by implementing core concepts from first principles

---

## Motivation

Reading DDIA provides strong theory. This repository helps build real intuition by implementing simplified systems that highlight design tradeoffs (not production-ready code).

## What You'll Build (Overview)

- Log-structured storage (append-only KV store)
- LSM trees and compaction
- Replication (leader–follower)
- Partitioning and consistent hashing
- Stream logs (mini Kafka)
- Transactions, WAL, and recovery

## Roadmap (8 Weeks)

1. Append-only KV store
2. LSM tree + compaction
3. Replication
4. Partitioning
5. Streaming log
6. Transactions & recovery

## Repository Structure

```
ddia-java/
├── 01-log-kv-store/
├── 02-lsm-tree/
├── 03-replication/
├── 04-partitioning/
├── 05-streaming-log/
├── 06-transactions/
└── common/
```

Each module is self-contained, independently buildable, and documented with design decisions and tradeoffs.

---

## 8-Week Implementation Plan (High Level)

### Week 1 — Append-Only Log Key-Value Store

- Topics: log-structured storage, storage & retrieval, WAL, crash recovery
- Build: append-only log file, in-memory index (`Map<String, Long>`), `put`/`get` APIs, log replay for recovery
- Java focus: `RandomAccessFile`, `FileChannel`, `ByteBuffer`
- Deliverables:
  - `01-log-kv-store/`
    - `LogSegment.java`
    - `StorageEngine.java`
    - `RecoveryManager.java`
    - `README.md`

### Week 2 — Log Segments & Compaction

- Topics: storage internals, compaction tradeoffs
- Build: fixed-size log segments, background compaction, obsolete-entry removal
- Deliverables: `SegmentManager.java`, `CompactionWorker.java`

### Week 3 — LSM Tree (MemTable + SSTables)

- Topics: LSM trees, SSTables, read/write amplification
- Build: `MemTable` (ConcurrentSkipListMap), flush to immutable SSTables, sparse index
- Java focus: `MappedByteBuffer`, immutable file formats
- Deliverables: `MemTable.java`, `SSTableWriter.java`, `SSTableReader.java`, `LSMEngine.java`

### Week 4 — Compaction Strategies

- Topics: size-tiered vs leveled compaction, tombstones
- Build: simple size-tiered and leveled compaction implementations
- Deliverables: `CompactionStrategy.java`, `SizeTieredCompactor.java`, `LeveledCompactor.java`

### Week 5 — Replication (Leader–Follower)

- Topics: replication, consistency models, failure handling
- Build: leader + followers, replication log, ACK-based commit
- Java focus: `ExecutorService`, networking (TCP/HTTP), serialization

### Week 6 — Partitioning & Consistent Hashing

- Topics: sharding, load balancing, virtual nodes, rebalancing
- Deliverables: `HashRing.java`, `PartitionManager.java`

### Week 7 — Streaming Log (Mini Kafka)

- Topics: stream processing, topic logs, consumer groups, offsets
- Build: `TopicLog`, `Producer`, `Consumer`, `OffsetManager`

### Week 8 — Transactions & Recovery

- Topics: transactions, WAL, atomic batches, idempotency
- Build: `WriteAheadLog`, `TransactionManager`, `RecoveryManager`

---

## Tooling

- Build: Maven or Gradle
- Testing: JUnit
- Benchmarking: JMH
- Logging: SLF4J
- Profiling: Java Flight Recorder, VisualVM

## Outcomes

By completing this project you will be able to:

- Explain LSM trees from first principles
- Reason about replication and consistency tradeoffs
- Design partitioned, fault-tolerant systems
- Demonstrate implementation depth beyond theoretical knowledge

## Quick Start

Build and run using Maven:

```bash
mvn -DskipTests package
```

Or with Gradle:

```bash
./gradlew build
```

Run tests with:

```bash
mvn test
```

## Contributing

Contributions are welcome. Open an issue or submit a PR with clear motivation and tests where applicable.

## Disclaimer

This project prioritizes learning and clarity over production readiness. Many optimizations and edge cases are intentionally omitted.

---

If you've read DDIA — this repository helps you prove it by building the systems yourself.