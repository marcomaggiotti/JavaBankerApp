# JavaBanker — Kafka Streams banking platform (study project + interview-challenge kit)

A small but production-shaped banking backend built to **study** and to **solve job-interview
challenges** for roles asking for Java, Spring Boot, Kafka Streams, Docker/Podman, Kubernetes/OpenShift,
CI/CD (GitLab), GitOps (ArgoCD/Flux) and Clean Architecture.

It is the evolution of the original console app in [`Console/`](Console/) (deposit / withdraw / balance,
"non può prelevare perché è povero"): the same business rules, re-implemented as an event-driven system.

```
          POST /api/transactions                      GET /api/accounts/{id}/balance
                   │                                              ▲
         ┌─────────▼─────────┐                         ┌──────────┴──────────┐
         │  REST adapter     │                         │ Interactive Query   │
         │  (Spring MVC)     │                         │ (RocksDB store)     │
         └─────────┬─────────┘                         └──────────▲──────────┘
                   │ KafkaTemplate (acks=all, idempotent)         │
                   ▼                                              │
   bank.transactions.v1 ──► BalanceTopology ──accepted──► bank.balances.v1 (compacted)
             │                    │  └──rejected──► bank.transactions.rejected.v1
             │                    └── balances-store ─────────────┘
             └────────► FraudDetectionTopology (1-min windows) ──► bank.fraud-alerts.v1
             └────────► LargeTransactionAlertTopology ──────────► bank.large-transactions.v1
```

## Quick start

Everything runs with one command and **no tokens or credentials**:

```bash
docker compose up --build
```

This starts Kafka, the app (http://localhost:8080), Kafka UI (http://localhost:8081) and a load
generator that sends 200 random transactions once the app is ready. Send more with
`docker compose run --rm load-generator`; stop and wipe data with `docker compose down -v`.

| Goal | Command |
|---|---|
| Unit + topology tests (no broker, ~10 s) | `mvn test` |
| + end-to-end test (embedded KRaft Kafka) | `mvn verify` |
| Full local stack (Kafka, app, Kafka UI, traffic) | `docker compose up --build` (or `podman compose up --build`) |
| Generate traffic | `python3 scripts/load_generator.py --count 200` |
| Practise a coding exercise (starter + tests + solution) | `scripts/exercises.sh list` / `test ex03` |
| Scaffold a new interview challenge | `scripts/new-challenge.sh MyChallenge` |
| Render Kubernetes manifests | `kubectl kustomize k8s/overlays/dev` |

```bash
curl -s -XPOST localhost:8080/api/transactions -H 'Content-Type: application/json' \
  -d '{"accountId":"acc-1","type":"DEPOSIT","amount":100.00,"currency":"EUR"}'
```

```bash
curl -s localhost:8080/api/accounts/acc-1/balance
```

## Repository map (skill → where to look)

| Skill in the job spec | Where it is practised |
|---|---|
| Java 21, Clean Code | `src/main/java` — records, sealed-ish enums, always-valid domain objects |
| Spring Boot | `config/`, `adapter/in/web`, `@ConfigurationProperties`, actuator probes, ProblemDetail errors |
| Kafka Streams | `streams/topology` — Processor API + state store, windowing, suppress, branching, interactive queries, EOS |
| Testing | `TopologyTestDriver` tests, `@WebMvcTest`, ArchUnit rules, `@EmbeddedKafka` end-to-end `*IT` |
| Docker / Podman | `Dockerfile` (multi-stage, layered jar, non-root, OpenShift-friendly), `docker-compose.yml` |
| Kubernetes / OpenShift | `k8s/base` + `k8s/overlays/{dev,prod}` (Kustomize, StatefulSet, probes, PDB, security context) |
| CI/CD | `.gitlab-ci.yml` (primary), `.github/workflows/ci.yml` (alternative) — build, test, image; no custom tokens |
| GitOps | `gitops/argocd/*` (AppProject + Applications), `gitops/flux/*` (study material for a cluster setup) |
| Software architecture | Hexagonal layout, enforced by `ArchitectureTest`; see [docs/architecture.md](docs/architecture.md) |
| Python (optional) | `scripts/load_generator.py` |

## Documentation

- [docs/study-guide.md](docs/study-guide.md) — topic-by-topic study plan with interview questions & answers
- [docs/interview-playbook.md](docs/interview-playbook.md) — step-by-step method for take-home and live-coding challenges
- [docs/architecture.md](docs/architecture.md) — design decisions and trade-offs (ADR style)
- [exercises/README.md](exercises/README.md) — 6 hands-on coding exercises: statement, starter code, tests, reference solution
- [challenges/README.md](challenges/README.md) — catalogue of 10 practice challenges (3 solved)
