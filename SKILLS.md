# hestion-cqrs Skills

This module is a Kotlin CQRS library for Spring Boot apps. It provides command/query routing, event handling, and optional Prometheus metrics with sync, coroutine, and Reactor execution styles.

## Core Concepts
- Commands mutate state and extend `Command<R>`; queries read state and extend `Query<R>`.
- Handlers are Spring beans: `CommandHandler`, `QueryHandler`, `AsyncCommandHandler`, `AsyncQueryHandler`.
- IO-optimised async variants: `AsyncIOCommandHandler` and `AsyncIOQueryHandler` run on `Dispatchers.IO` by default.
- Each async handler can optionally override a `dispatcher` property for a custom `CoroutineDispatcher`.
- Events implement `Event` or `AggregateEvent` and are dispatched via `EventBus` (direct) or published via `EventPublisher` (Spring bridge).
- `EventPublisher` publishes through Spring's `ApplicationEventPublisher`; `SpringEventListener` bridges those back to `EventBus.dispatch()`.
- Aggregates extend `AggregateRoot` and record domain events; repositories accept `EventPublisher` for publishing.
- IDs use type-safe wrappers over primitives with UUID v7 helpers (`Id.generateUuidV7()`, `Id.fromUuidString()`, `Id.fromUuid()`, `Id.fromString()`).
- All handler interfaces extend the common `Handler<O>` base which provides automatic type parameter resolution.
- `PaginatedListQuery` is available for queries with `Pageable` pagination.

## Validation Error Handling
- `ValidationCommandHandlerException` and `ValidationQueryHandlerException` distinguish validation errors from unexpected failures.
- Validation errors emit `ValidationFailedCommandEvent` / `ValidationFailedQueryEvent` and are logged at INFO level.
- Unexpected errors emit `ErrorProcessCommandEvent` / `ErrorProcessQueryEvent` and are logged at ERROR level.

## Spring Integration
- Auto-configured beans: `CqrsGateway`, `CommandBus` (`DefaultCommandBus`), `QueryBus` (`DefaultQueryBus`), `EventBus` (`DefaultEventBus`), `EventPublisher` (`SpringEventPublisher`), `CoroutineDispatchers`, executors, handler storages, `SpringEventListener`.
- `CqrsGateway` and `ReactorCqrsGateway` both extend `BaseCqrsGateway` which provides lifecycle event publishing and error handling.
- Handlers are discovered from the Spring `ApplicationContext` via `SpringBeansHelper`.
- `ReactorCqrsGateway` is available only when Reactor is on the classpath.

## Execution Modes
- Synchronous: `CommandHandler` / `QueryHandler` and `CqrsGateway.dispatch()` / `query()`.
- Coroutines: `AsyncCommandHandler` / `AsyncQueryHandler` and `CqrsGateway.dispatchAsync()` / `queryAsync()`.
- IO Coroutines: `AsyncIOCommandHandler` / `AsyncIOQueryHandler` run on `Dispatchers.IO` by default.
- Reactor: use `ReactorCqrsGateway` with `Mono`/`Flux` results (supports both `Mono` and `Flux` for commands and queries).

## Configuration and Observability
- Properties under `cqrs.*` (e.g., `cqrs.event-bus.blocking-listener`, `cqrs.metrics.prometheus.enabled`).
- Lifecycle events emitted for command/query: `StartProcess*Event`, `StopProcess*Event`, `ErrorProcess*Event`, `ValidationFailed*Event`.
- Optional Prometheus metrics via `MetricsCommandEventHandler` / `MetricsQueryEventHandler` marker interfaces, enabled when `cqrs.metrics.prometheus.enabled=true` and a `MeterRegistry` is present.
- Metrics: `cqrs_command_start`, `cqrs_command_stop`, `cqrs_command_error`, `cqrs_command_duration`, `cqrs_query_start`, `cqrs_query_stop`, `cqrs_query_error`, `cqrs_query_duration`.
- MDC context is propagated in coroutine dispatchers via `MDCContext()`.
- `NoOpEventHandler` is available for stubbing or disabling event handlers.

## Constraints and Expectations
- Exactly one handler per command/query type.
- Multiple handlers may subscribe to the same event type.
- Reactor support requires `reactor-core` on the classpath.
- Repositories use `EventPublisher` (not `EventBus`) for publishing aggregate events.

