# KQRS Skills

This module is a Kotlin CQRS library for Spring Boot apps. It provides command/query routing, event handling, and optional Prometheus metrics with sync, coroutine, and Reactor execution styles.

## Modules
- **`kqrs-core`** — Core domain types (commands, queries, events, handlers, aggregates, repositories, dispatchers). Dependencies: `kotlin-reflect`, `kotlinx-coroutines-core`, `kotlinx-coroutines-slf4j`, `uuid-creator`, `slf4j-api`.
- **`kqrs-spring`** — Spring integration (handler storages, event bridge, `SpringPaginatedListQuery`, `SpringTransactionalExecutor`). Dependencies: `kqrs-core`, `spring-context`, `spring-tx`, `spring-data-commons`.
- **`kqrs-reactor`** — Reactive gateway (`ReactorKqrsGateway`) for Mono/Flux results. Dependencies: `kqrs-core`, `reactor-core`.
- **`kqrs-prometheus`** — Prometheus metrics event handlers. Dependencies: `kqrs-core`, `micrometer-core`.
- **`kqrs-spring-boot-autoconfigure`** — Auto-configuration for core beans. Dependencies: `kqrs-spring`, `spring-boot-autoconfigure`.
- **`kqrs-spring-boot-reactor-autoconfigure`** — Auto-configuration for `ReactorKqrsGateway` (conditional on Reactor classpath). Dependencies: `kqrs-spring-boot-autoconfigure`, `kqrs-reactor`.
- **`kqrs-spring-boot-prometheus-autoconfigure`** — Auto-configuration for Prometheus metrics (conditional on `MeterRegistry` + `kqrs.metrics.prometheus.enabled`). Dependencies: `kqrs-spring-boot-autoconfigure`, `kqrs-prometheus`.
- Published as `com.kormichu:kqrs-<module>`. For Spring Boot apps, depend on `kqrs-spring-boot-autoconfigure` (transitively includes `kqrs-spring` and `kqrs-core`).

## Core Concepts
- Commands mutate state and extend `Command<R>`; queries read state and extend `Query<R>`.
- Handlers are Spring beans: `CommandHandler`, `QueryHandler`, `AsyncCommandHandler`, `AsyncQueryHandler`.
- IO-optimised async variants: `AsyncIOCommandHandler` and `AsyncIOQueryHandler` run on `Dispatchers.IO` by default.
- Transactional handler variants: `TransactionalCommandHandler`, `TransactionalQueryHandler`, `AsyncTransactionalCommandHandler`, `AsyncTransactionalQueryHandler`, `AsyncIOTransactionalCommandHandler`, `AsyncIOTransactionalQueryHandler` — these use `TransactionalExecutor` for declarative transaction boundaries.
- Each async handler can optionally override a `dispatcher` property for a custom `CoroutineDispatcher`.
- Events implement `Event` or `AggregateEvent` and are dispatched via `EventBus` (direct) or published via `EventPublisher` (Spring bridge).
- `EventPublisher` publishes through Spring's `ApplicationEventPublisher`; `SpringEventListener` (a `SmartApplicationListener`) bridges those back to `EventBus.dispatch()`.
- Aggregates extend `AggregateRoot` and record domain events; repositories accept `EventPublisher` for publishing.
- IDs use type-safe wrappers over primitives with UUID v7 helpers (`Id.generateUuidV7()`, `Id.fromUuidString()`, `Id.fromUuid()`, `Id.fromString()`).
- All handler interfaces extend the common `Handler<O>` base which provides automatic type parameter resolution.
- `SpringPaginatedListQuery` (in `kqrs-spring`) is available for queries with Spring `Pageable` pagination.

## Validation Error Handling
- `ValidationCommandHandlerException` and `ValidationQueryHandlerException` distinguish validation errors from unexpected failures.
- Validation errors emit `ValidationFailedCommandEvent` / `ValidationFailedQueryEvent` and are logged at INFO level.
- Unexpected errors emit `ErrorProcessCommandEvent` / `ErrorProcessQueryEvent` and are logged at ERROR level.

## Spring Integration
- Auto-configured beans: `KqrsGateway` (`DefaultKqrsGateway`), `CommandBus` (`DefaultCommandBus`), `AsyncCommandBus` (`DefaultAsyncCommandBus`), `QueryBus` (`DefaultQueryBus`), `AsyncQueryBus` (`DefaultAsyncQueryBus`), `EventBus` (`DefaultEventBus`), `EventPublisher` (`SpringEventPublisher`), `AsyncDispatchers` (`DefaultAsyncDispatchers`), executors, handler storages, `SpringEventListener`, `TransactionalExecutor` (`SpringTransactionalExecutor`).
- `DefaultKqrsGateway` implements both `KqrsGateway` (sync) and `AsyncKqrsGateway` (coroutine) interfaces, extending `BaseKqrsGateway` which provides lifecycle event publishing and error handling.
- Handlers are discovered from the Spring `ApplicationContext` via `SpringBeansHelper`.
- `ReactorKqrsGateway` (`DefaultReactorKqrsGateway`) is available only when Reactor is on the classpath.

## Execution Modes
- Synchronous: `CommandHandler` / `QueryHandler` and `KqrsGateway.dispatch()` / `query()`.
- Coroutines: `AsyncCommandHandler` / `AsyncQueryHandler` and `AsyncKqrsGateway.dispatchAsync()` / `queryAsync()`.
- IO Coroutines: `AsyncIOCommandHandler` / `AsyncIOQueryHandler` run on `Dispatchers.IO` by default.
- Reactor: use `ReactorKqrsGateway` with `Mono`/`Flux` results (supports both `Mono` and `Flux` for commands and queries).

## Transaction Support
- `TransactionalExecutor` interface with `execute` and `executeReadOnly` methods.
- `SpringTransactionalExecutor` provides Spring `@Transactional` integration.
- `TransactionalCommandHandler` / `TransactionalQueryHandler` automatically wrap `handleInTransaction()` in a transaction.
- `AsyncTransactionalCommandHandler` / `AsyncTransactionalQueryHandler` and their IO variants do the same for async handlers.

## Configuration and Observability
- Properties under `kqrs.*` (e.g., `kqrs.event-bus.blocking-listener`, `kqrs.metrics.prometheus.enabled`).
- Lifecycle events emitted for command/query: `StartProcessCommandEvent`, `StopProcessCommandEvent`, `ErrorProcessCommandEvent`, `ValidationFailedCommandEvent`, and corresponding query events.
- Optional Prometheus metrics via `MetricsCommandEventHandler` / `MetricsQueryEventHandler` marker interfaces, enabled when `kqrs.metrics.prometheus.enabled=true` and a `MeterRegistry` is present.
- Metrics: `kqrs_command_start`, `kqrs_command_stop`, `kqrs_command_error`, `kqrs_command_duration`, `kqrs_query_start`, `kqrs_query_stop`, `kqrs_query_error`, `kqrs_query_duration`.
- MDC context is propagated in coroutine dispatchers via `MDCContext()`.
- `NoOpEventHandler` is available for stubbing or disabling event handlers.

## Constraints and Expectations
- Exactly one handler per command/query type.
- Multiple handlers may subscribe to the same event type.
- Reactor support requires `reactor-core` on the classpath.
- Repositories use `EventPublisher` (not `EventBus`) for publishing aggregate events.
