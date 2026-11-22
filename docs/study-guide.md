# Study guide

A 4-week plan mapped to the job requirements. Each topic: **what to know**, **where it is in this repo**,
**exercise**, and **typical interview questions** (with short answers).

---

## 1. Java (21) & Clean Code — week 1
**Know:** records, enums with behaviour, `Optional`, streams, `var`, text blocks, pattern matching for
`switch`/`instanceof`, sealed interfaces, virtual threads (Java 21), immutability, `BigDecimal` for money,
`equals/hashCode` contracts, concurrency basics (`CompletableFuture`, `ExecutorService`).
Clean Code: meaningful names, small functions, SRP, no hidden side effects, tests as documentation,
SOLID, composition over inheritance.
**Repo:** `domain/*` (always-valid records, rule in `AccountBalance.apply`), compare with the legacy
`Console/src/Main.java` — a great refactoring kata: extract the rules, remove duplication, inject I/O.
**Exercise:** refactor `Console/` to reuse `AccountBalance`; make `TransactionOutcome` a sealed interface
(`Accepted | Rejected`) and use pattern-matching `switch`.

**Q:** Why `BigDecimal` not `double`? → binary floating point can't represent 0.1; use `compareTo`, not `equals` (scale!).
**Q:** Record limitations? → final, no extra instance fields, shallow immutability (copy mutable collections).
**Q:** Virtual threads? → cheap threads for blocking I/O; avoid pinning (`synchronized` around I/O before JDK 24), don't pool them.

## 2. Spring Boot — week 1
**Know:** auto-configuration & `@Conditional*`, profiles, `@ConfigurationProperties` (records), bean
lifecycle & scopes, constructor injection, `@Transactional` pitfalls (self-invocation, proxies), Spring MVC vs
WebFlux, validation, `ProblemDetail`, Actuator (health groups, probes, Micrometer), test slices
(`@WebMvcTest`, `@DataJpaTest`), `@MockitoBean`, graceful shutdown.
**Repo:** `config/*`, `adapter/in/web/*`, `application.yml`, `KafkaStreamsHealthIndicator`, `TransactionControllerTest`.
**Exercise:** add Spring Security with JWT; add a `prod` profile; expose a custom metric (rejected count).

**Q:** How does auto-configuration work? → `AutoConfiguration.imports` lists config classes guarded by
`@ConditionalOnClass/MissingBean`; your beans win (we override `ProducerFactory` and `KafkaTemplate`).
**Q:** Liveness vs readiness? → liveness = restart me; readiness = don't send traffic. Never put
external dependencies in liveness (cascade restarts).
**Q:** JEE equivalents? → CDI (`@Inject`), JAX-RS (`@Path`), JPA, Bean Validation, MicroProfile Health/Config; Quarkus/Open Liberty.

## 3. Kafka & Kafka Streams — weeks 2–3 (core of the role)
**Kafka:** topics, partitions, offsets, consumer groups & rebalancing (cooperative sticky), replication,
ISR, `acks=all` + `min.insync.replicas`, idempotent producer, transactions, compaction, retention, KRaft.
**Streams:** KStream / KTable / GlobalKTable duality; stateless vs stateful ops; repartition topics;
co-partitioning; state stores (RocksDB) + changelog topics; tasks & threads & scaling (max parallelism =
partitions); windowing (tumbling, hopping, sliding, session) & grace; stream time vs wall-clock time;
`suppress`; joins (stream-stream windowed, stream-table, table-table, FK join); Processor API & punctuators;
EOS v2; interactive queries; standby replicas; error handlers; testing with `TopologyTestDriver`.
**Repo:** `streams/topology/*`, `streams/support/*`, tests in `src/test/.../streams`, `BankerPlatformIT`.
**Exercise:** solve challenges 4, 6, 8 then 5, 9 from `challenges/README.md`.

**Q:** When does Kafka Streams repartition? → after a key-changing op (`selectKey`, `map`, `groupBy`)
followed by a stateful op or join. `mapValues` keeps the key → no repartition.
**Q:** How is state fault tolerant? → every store write goes to a compacted changelog topic; a new owner
restores from it (standby replicas keep a warm copy).
**Q:** Exactly-once? → `exactly_once_v2`: one producer per thread, transactional commit of outputs +
changelog + offsets. Only within Kafka; external side effects need idempotency.
**Q:** KTable vs GlobalKTable? → KTable is partitioned (needs co-partitioning, time-synchronised);
GlobalKTable is fully replicated on every instance (small reference data, no co-partitioning).
**Q:** What happens with late records? → accepted while `stream time ≤ window end + grace`, dropped after
(metric `dropped-records`).
**Q:** How do you scale? → add instances up to the partition count; more partitions requires a
re-partitioning plan (keys move!).

### Optional: Apache Flink
| | Kafka Streams | Flink |
|---|---|---|
| Deployment | a library inside your app (any JVM, K8s Deployment/StatefulSet) | a cluster (JobManager/TaskManagers, or K8s operator) |
| Sources | Kafka only | Kafka, files, CDC, Kinesis, … |
| State | RocksDB + Kafka changelog | RocksDB + checkpoints to S3/HDFS (Chandy-Lamport snapshots) |
| Time | event time, grace | event time, watermarks |
| APIs | DSL, Processor API | DataStream, Table/SQL, `KeyedProcessFunction` |
| Choose when | microservice-sized, Kafka-centric | large-scale, multi-source, SQL analytics, batch+stream |

## 4. Containers: Docker / Podman — week 3
**Know:** image layers & caching, multi-stage builds, distroless/JRE base images, non-root users,
`ENTRYPOINT` vs `CMD`, JVM container awareness (`MaxRAMPercentage`), health checks, compose networks,
BuildKit cache mounts; Podman = daemonless, rootless, `podman generate kube`.
**Repo:** `Dockerfile`, `.dockerignore`, `docker-compose.yml`.
**Exercise:** `docker build` and compare size with/without layered jar; run with `podman` rootless; add
`docker scout` / `trivy` scan to CI.

**Q:** Why the `chgrp 0` in the Dockerfile? → OpenShift runs containers with a random UID in group 0;
group-writable dirs keep it working.
**Q:** Why layered jars? → dependencies rarely change, so pushes/pulls only move the small app layer.

## 5. Kubernetes / OpenShift & cloud — week 3
**Know:** Pod, Deployment vs StatefulSet, Service (ClusterIP/headless), Ingress/OpenShift Route,
ConfigMap/Secret, probes, requests/limits & QoS, HPA, PDB, rolling updates, namespaces, RBAC,
SecurityContext & SCC (OpenShift), operators (Strimzi for Kafka), Kustomize vs Helm.
Cloud: managed K8s (EKS/AKS/GKE), managed Kafka (MSK, Event Hubs Kafka API, Confluent Cloud), IAM / workload
identity, object storage, VPC networking.
**Repo:** `k8s/base`, `k8s/overlays/{dev,prod}`.
**Exercise:** run on kind/minikube with Strimzi; add an HPA on CPU; add an OpenShift `Route`.

**Q:** Why a StatefulSet here? → stable identity + PVC per pod keeps RocksDB state across restarts.
**Q:** How many replicas max? → useful up to number of input partitions (extra pods = idle standbys).

## 6. CI/CD (GitLab first) — week 4
**Know:** stages/jobs, `rules`, cache vs artifacts, JUnit reports, services (dind), runners & executors,
protected variables, environments, merge-request pipelines, `needs` (DAG), templates/`include`,
security scanning (SAST, dependency, container). GitHub Actions, Jenkins, AWS CodePipeline, Azure DevOps equivalents.
**Repo:** `.gitlab-ci.yml`, `.github/workflows/ci.yml`.
**Exercise:** add `needs:` to make a DAG; add a `trivy` job; add a merge-request-only pipeline.

## 7. GitOps (ArgoCD / Flux) — week 4
**Know:** principles — declarative, versioned & immutable, pulled automatically, continuously reconciled.
ArgoCD: Application, AppProject, sync policies (prune, selfHeal), sync waves/hooks, ApplicationSet,
app-of-apps. Flux: GitRepository, Kustomization, HelmRelease, image automation. Secrets: Sealed
Secrets / SOPS / External Secrets. Promotion between environments.
**Repo:** `gitops/argocd/*`, `gitops/flux/*`, `promote-dev` job.
**Exercise:** install ArgoCD in kind, apply `gitops/argocd/`, change the dev replica count via Git and watch it sync.

**Q:** Push vs pull deployment? → pull (GitOps) keeps cluster credentials inside the cluster, gives drift
detection and Git audit history; push (CI `kubectl apply`) is simpler but less secure.
**Q:** Rollback? → `git revert` the promotion commit; ArgoCD syncs back.

## 8. Software architecture — throughout
**Know:** layered vs hexagonal vs clean architecture, DDD tactical patterns (aggregate, value object,
domain event), CQRS, event sourcing, sagas, outbox, idempotency, consistency models, 12-factor app,
ADRs, C4 diagrams, observability (logs, metrics, traces).
**Repo:** [architecture.md](architecture.md), `ArchitectureTest`.

---

### Weekly checklist
- [ ] W1 Java + Spring: refactor `Console/`, add security, all tests green
- [ ] W2 Kafka Streams basics: challenges 3, 4, 8
- [ ] W3 Stateful streams + containers + K8s: challenges 5, 6, 9; run on kind
- [ ] W4 CI/CD + GitOps + mock interviews: ArgoCD in kind, 2 timed challenges
