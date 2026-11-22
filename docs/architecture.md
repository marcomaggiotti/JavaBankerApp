# Architecture & design decisions

## Layers (Hexagonal / Ports & Adapters)

```
com.javabanker
├── domain/            pure Java: Transaction, AccountBalance (business rules), TransactionOutcome, FraudAlert
├── application/
│   ├── port/in/       use-case interfaces (what the app offers)
│   ├── port/out/      what the app needs (TransactionPublisher, BalanceRepository)
│   └── service/       use-case implementations — NO Spring annotations
├── adapter/
│   ├── in/web/        REST controller, DTOs, error mapping
│   └── out/           kafka/ (producer), streams/ (interactive query, health)
├── streams/
│   ├── support/       the challenge framework: TopologyContributor, SourceStreams, JsonSerdes, Topics
│   └── topology/      Kafka Streams sub-topologies
├── challenges/        one package per interview challenge
└── config/            Spring wiring (composition root)
```

Dependency rule: arrows point inwards. `ArchitectureTest` fails the build if `domain` imports Spring/Kafka
or if `application` imports adapters.

## Decisions

### ADR-1 — Business rules live in the domain, not in the topology
`AccountBalance.apply(tx)` returns a `TransactionOutcome`. The Kafka Streams processor only loads state,
calls the domain, stores the result and forwards. **Why:** rules are unit-testable in milliseconds and reusable.

### ADR-2 — Processor API instead of `aggregate()` for balances
An aggregation must always emit a new value; it can't reject an input and route it to another topic.
`process()` + explicit `KeyValueStore` lets us emit accepted *or* rejected outcomes, then `split()`.

### ADR-3 — Partitioning by `accountId`
The producer keys records by accountId → all events of an account are in one partition → strict ordering
and a single task owns that account's state. No repartition topic needed. If the key were wrong you'd add
`.repartition()` (or `selectKey` before a stateful DSL op).

### ADR-4 — Exactly-once (`processing.guarantee=exactly_once_v2`)
Read-process-write is atomic (consumer offsets + state changelog + output in one Kafka transaction).
Requires brokers ≥ 2.5 and a transaction state log; costs some latency (commit interval).
Note: EOS does **not** dedupe client retries of the REST call — see challenge 6 (idempotency key).

### ADR-5 — CQRS / eventual consistency
POST returns `202 Accepted` after the broker ack; the read side (state store) catches up asynchronously.
Clients needing read-your-writes can poll, or the API can return the offset and wait for it.

### ADR-6 — Interactive queries from local state
Fast and no extra database, but each instance only holds its partitions. Production: set
`application.server=<pod-dns>:8080`, call `queryMetadataForKey()` and proxy to the owning pod (the headless
Service in `k8s/base/service.yaml` provides stable pod DNS). Alternative: sink `bank.balances.v1` into a DB.

### ADR-7 — StatefulSet + PVC for the stream app
Restarted pods find their RocksDB state on the volume and only replay the changelog tail.
Standby replicas (`num.standby.replicas=1` in prod) make failover fast.

### ADR-8 — Runs without tokens; Docker Compose is the default runtime
`docker compose up --build` starts everything (Kafka, topics, app, Kafka UI, traffic generator) with no
credentials or tokens. CI only builds, tests and pushes the image with GitLab's predefined `CI_REGISTRY_*`
variables. The `k8s/` and `gitops/` manifests are study material for a cluster setup: there ArgoCD would
sync Git into the cluster, and promotion would be a reviewed merge request changing the image tag.

### ADR-9 — JSON wire format
Simple to read for study. In production prefer Avro/Protobuf + Schema Registry with compatibility rules
(BACKWARD for consumers-first upgrades). `FAIL_ON_UNKNOWN_PROPERTIES=false` already gives tolerant readers.

## Known limitations (good to mention proactively in an interview)
- Single currency per account; FX handled in challenge 4.
- No authentication — add Spring Security (OAuth2 resource server / JWT).
- Poison pills are logged and skipped (`LogAndContinueExceptionHandler`); a DLQ topic would be better.
- Topics are created by the app (`KafkaAdmin`); in regulated platforms topics are GitOps-managed (Strimzi `KafkaTopic`).
