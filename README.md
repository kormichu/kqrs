# KQRS

A Kotlin CQRS (Command Query Responsibility Segregation) library for Spring Boot applications.

## Features

- Command and Query handling with automatic routing to handlers
- Middleware pipeline for command/query execution (sync + coroutine async)
- Event system with aggregate events and lifecycle events
- Separate `EventPublisher` (Spring `ApplicationEventPublisher` bridge) and `EventBus` (direct handler dispatch)
- Synchronous, asynchronous (coroutines), and reactive (Reactor) execution modes
- IO-optimised async handler variants (`AsyncIOCommandHandler`, `AsyncIOQueryHandler`)
- Built-in transactional handler variants (`TransactionalCommandHandler`, `AsyncTransactionalCommandHandler`, etc.)
- Validation error handling with dedicated exception types and lifecycle events
- MDC context propagation for logging and tracing
- Optional Prometheus metrics integration
- Paginated query support via `SpringPaginatedListQuery`

## Modules

| Module (artifact) | Description | Key dependencies |
|--------------------|-------------|------------------|
| `kqrs-core` | Core domain types: commands, queries, events, handlers, aggregates, repositories, dispatchers | `kotlin-reflect`, `kotlinx-coroutines-core`, `kotlinx-coroutines-slf4j`, `uuid-creator`, `slf4j-api` |
| `kqrs-spring` | Spring integration: handler storages, event bridge, `SpringPaginatedListQuery`, `SpringTransactionalExecutor` | `kqrs-core`, `spring-context`, `spring-tx`, `spring-data-commons` |
| `kqrs-reactor` | Reactive gateway (`ReactorKqrsGateway`) for Mono/Flux results | `kqrs-core`, `reactor-core` |
| `kqrs-prometheus` | Prometheus metrics event handlers | `kqrs-core`, `micrometer-core` |
| `kqrs-spring-boot-autoconfigure` | Spring Boot auto-configuration for core beans | `kqrs-spring`, `spring-boot-autoconfigure` |
| `kqrs-spring-boot-reactor-autoconfigure` | Auto-configuration for `ReactorKqrsGateway` | `kqrs-spring-boot-autoconfigure`, `kqrs-reactor`, `spring-boot-autoconfigure` |
| `kqrs-spring-boot-prometheus-autoconfigure` | Auto-configuration for Prometheus metrics handlers | `kqrs-spring-boot-autoconfigure`, `kqrs-prometheus`, `spring-boot-autoconfigure` |

Published artifacts use the coordinate `com.kormichu:kqrs-<module>`, e.g. `com.kormichu:kqrs-core`.

### Module dependency graph

```
kqrs-core
├── kqrs-spring ──► kqrs-spring-boot-autoconfigure
│                       ├── kqrs-spring-boot-reactor-autoconfigure ◄── kqrs-reactor
│                       └── kqrs-spring-boot-prometheus-autoconfigure ◄── kqrs-prometheus
├── kqrs-reactor
└── kqrs-prometheus
```

### Gradle dependency

For a Spring Boot application, add the auto-configuration module (it transitively brings in `kqrs-spring` and `kqrs-core`):

```kotlin
// build.gradle.kts
dependencies {
    implementation("com.kormichu:kqrs-spring-boot-autoconfigure:${kqrsVersion}")

    // Optional — Reactor support
    implementation("com.kormichu:kqrs-spring-boot-reactor-autoconfigure:${kqrsVersion}")

    // Optional — Prometheus metrics
    implementation("com.kormichu:kqrs-spring-boot-prometheus-autoconfigure:${kqrsVersion}")
}
```

## Quick Start

### Configuration

```yaml
kqrs:
  event-bus:
    blocking-listener: false   # true = blocking event dispatch, false = coroutine dispatch
  metrics:
    prometheus:
      enabled: false           # enable Prometheus metrics when MeterRegistry is present
```

### Define a Command

```kotlin
import com.kormichu.kqrs.command.Command
import com.kormichu.kqrs.command.CommandHandler
import com.kormichu.kqrs.event.EventPublisher

data class CreateUserCommand(
    val name: String,
    val email: String
) : Command<UserId>()

@Component
class CreateUserCommandHandler(
    private val userRepository: UserRepository,
    private val eventPublisher: EventPublisher
) : CommandHandler<CreateUserCommand, UserId> {
    override fun handle(command: CreateUserCommand): UserId =
        userRepository.saveAndPublishEvents(
            User.create(name = command.name, email = command.email),
            eventPublisher
        )
}
```

### Define a Query

```kotlin
import com.kormichu.kqrs.query.Query
import com.kormichu.kqrs.query.QueryHandler

data class GetUserByIdQuery(val id: UserId) : Query<UserReadModel>()

@Component
class GetUserByIdQueryHandler(
    private val repository: UserReadModelRepository
) : QueryHandler<GetUserByIdQuery, UserReadModel> {
    override fun handle(query: GetUserByIdQuery): UserReadModel =
        repository.findById(query.id) ?: throw UserNotFoundException(query.id)
}
```

### Dispatch via Gateway

```kotlin
import com.kormichu.kqrs.KqrsGateway

@RestController
@RequestMapping("/users")
class UserController(private val kqrsGateway: KqrsGateway) {
    @PostMapping
    fun create(@RequestBody req: CreateUserRequest) =
        kqrsGateway.dispatch(CreateUserCommand(req.name, req.email))
            .let { CreateUserResponse(it.value) }

    @GetMapping("/{id}")
    fun getById(@PathVariable id: UUID) =
        kqrsGateway.query(GetUserByIdQuery(UserId(id))).toResponse()
}
```

### Middleware (Tracing, Retry, etc.)

`CommandExecutor`, `QueryExecutor`, `AsyncCommandExecutor`, and `AsyncQueryExecutor` now support middleware pipelines.
When one or more middleware beans are registered, auto-configuration switches from default executors to middleware-aware executors.

```kotlin
@Configuration
class KqrsMiddlewareConfiguration {
    @Bean
    @Order(10)
    fun commandTracingMiddleware() = TracingCommandMiddleware(
        onStart = { ctx -> logger.info("start command {}", ctx.command.commandId) },
        onError = { ctx, ex -> logger.error("failed command {}", ctx.command.commandId, ex) },
    )

    @Bean
    @Order(20)
    fun queryRetryMiddleware() = RetryQueryMiddleware(
        maxAttempts = 3,
        shouldRetry = { it is java.io.IOException }
    )
}
```

Ordering is controlled by Spring bean ordering (`@Order` / `Ordered`).
Middleware can short-circuit execution (skip handler invocation) or wrap handler execution.

Built-in middleware:
- Tracing: `TracingCommandMiddleware`, `TracingQueryMiddleware`, async variants
- Retry: `RetryCommandMiddleware`, `RetryQueryMiddleware`, async variants

Retry safety guidance:
- Commands are often non-idempotent, so command retry defaults to `maxAttempts = 1` (disabled by default).
- Queries are usually read-only and default to `maxAttempts = 3`.
- For commands, enable retries only when the command is guaranteed idempotent.

Lifecycle event interaction:
- Middleware wraps handler execution only.
- Gateway lifecycle events (`StartProcess*`, `StopProcess*`, `ErrorProcess*`, `ValidationFailed*`) remain unchanged and continue to be published by gateway context handling.

### Async (Coroutines)

```kotlin
import com.kormichu.kqrs.command.AsyncCommandHandler

@Component
class CreateUserAsyncHandler(
    private val userRepository: AsyncUserRepository,
    private val eventPublisher: EventPublisher
) : AsyncCommandHandler<CreateUserCommand, UserId> {
    override suspend fun handle(command: CreateUserCommand): UserId =
        userRepository.saveAndPublishEvents(
            User.create(command.name, command.email),
            eventPublisher
        )
}

// In controller
suspend fun createUser(@RequestBody req: CreateUserRequest) =
    kqrsGateway.dispatchAsync(CreateUserCommand(req.name, req.email))
```

### Reactor

```kotlin
import com.kormichu.kqrs.reactor.ReactorKqrsGateway

@RestController
class UserController(private val gateway: ReactorKqrsGateway) {
    @PostMapping("/users")
    fun create(@RequestBody req: CreateUserRequest): Mono<CreateUserResponse> =
        gateway.dispatch(CreateUserCommand(req.name, req.email))
            .map { CreateUserResponse(it.value) }
}
```

For full documentation see [AGENTS.md](AGENTS.md).

## License

See [LICENSE](LICENSE).
