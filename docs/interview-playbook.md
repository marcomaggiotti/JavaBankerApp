# Interview challenge playbook

A repeatable method for **take-home assignments** (1–3 days) and **live coding** (45–90 min) on
streaming / Spring / cloud topics. Interviewers grade *how* you work as much as the result.

## 0. Before the interview
- `mvn verify` passes on your laptop, IDE indexed, Docker/Podman running.
- Re-read `streams/support/` and `TopologyTestSupport` so you can scaffold in < 2 minutes.
- Rehearse 2 challenges from [../challenges/README.md](../challenges/README.md) against a timer.

## 1. Clarify (5–10 % of the time)
Ask, then **write the answers down** (in the challenge's javadoc / README):

| Question | Why it matters |
|---|---|
| What is the key? Is input already partitioned by it? | repartitioning, ordering |
| Event time or processing time? Where is the timestamp? | windowing, `TimestampExtractor` |
| Late / out-of-order events? How late? | grace period |
| Output: every update or final result per window? | `suppress` / emit strategy |
| Duplicates possible? Delivery guarantee expected? | idempotency, EOS |
| Volume (msg/s), number of keys, retention | state size, partitions, scaling |
| Invalid records: drop, DLQ, fail? | error handling |
| What does "done" mean — tests, Docker, deploy manifests? | scope |

## 2. Model the domain first (pure Java)
- Records for events (`Transaction`), state (`AccountBalance`), results (`TransactionOutcome`).
- Put rules in domain methods; unit-test them without Kafka. It's the fastest feedback loop you have.

## 3. Test first with TopologyTestDriver
```bash
scripts/new-challenge.sh VelocityCheck
mvn test -Dtest=VelocityCheckTopologyTest
```
Write acceptance criteria as test names: `alertsOncePerWindowWhenThresholdReached`,
`lateEventWithinGraceIsCounted`, `tombstonesAreIgnored`… Then make them green one by one.
Remember: **stream time only advances with new records** — pipe a later "tick" record to close windows.

## 4. Implement the topology — pick the simplest primitive

| Need | Primitive |
|---|---|
| filter / transform one record | `filter`, `mapValues` (no repartition!), `flatMapValues` |
| route to several outputs | `split().branch()` |
| running total per key | `groupByKey().aggregate()` / `reduce()` |
| count in time buckets | `windowedBy(TimeWindows.ofSizeAndGrace(..))` |
| activity sessions | `SessionWindows` |
| enrich with reference data | `join(KTable)` (co-partitioned) or `join(GlobalKTable)` |
| correlate two event streams | `KStream-KStream join` + `JoinWindows` |
| custom state / reject / TTL / timers | `process()` + `StateStore` + `punctuate` |

Narrate trade-offs as you code: *"mapValues keeps the key so no repartition"*, *"GlobalKTable because FX
rates are small and we don't want to co-partition"*.

## 5. Make it run
- Register the contributor bean in `KafkaStreamsConfig`.
- `docker compose up --build`, send data with `scripts/load_generator.py`, watch topics in Kafka UI.
- If asked: Dockerfile, K8s manifests, pipeline — all exist in this repo to adapt.

## 6. Production-readiness checklist (mention even if you don't implement)
- [ ] Delivery semantics: EOS v2 / idempotent producer / idempotent consumers
- [ ] Error handling: deserialization handler, production handler, DLQ
- [ ] State: store sizing, changelog compaction, standby replicas, PVC
- [ ] Scaling: partitions = max parallelism; repartition topics; hot keys
- [ ] Observability: actuator health (stream state), Micrometer/Prometheus metrics, consumer lag
- [ ] Schema evolution: Avro/Protobuf + registry
- [ ] Security: TLS/SASL to Kafka, OAuth2 on the API, secrets via K8s Secrets/Vault
- [ ] Deployment: rolling update vs. state restore, `terminationGracePeriodSeconds`, PDB

## 7. Wrap up (last 10 %)
- Run the whole suite, commit with meaningful messages (small commits tell a story).
- README section: assumptions, how to run, what you'd do next with more time.

## Live-coding timeline (60 min)
| min | activity |
|---|---|
| 0–7 | clarify, write assumptions |
| 7–12 | domain records + first failing test |
| 12–45 | topology, test by test |
| 45–55 | edge cases (late, null, duplicate) |
| 55–60 | explain production concerns & next steps |
