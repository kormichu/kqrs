# KQRS - Kotlin CQRS Library

A Kotlin CQRS (Command Query Responsibility Segregation) library for Spring Boot applications.

## Identity

This library provides a complete CQRS implementation with:
- Command and Query handling with automatic routing to handlers
- Event system with aggregate events and lifecycle events
- Separate `EventPublisher` (Spring `ApplicationEventPublisher` bridge) and `EventBus` (direct handler dispatch)
- Synchronous, asynchronous (coroutines), and reactive (Reactor) execution modes
- IO-optimised async handler variants (`AsyncIOCommandHandler`, `AsyncIOQueryHandler`)
- Built-in transactional handler variants via `TransactionalExecutor` (`TransactionalCommandHandler`, `AsyncTransactionalCommandHandler`, etc.)
- Validation error handling with dedicated exception types and lifecycle events
- MDC context propagation for logging and tracing
- Optional Prometheus metrics integration
- Paginated query support via `SpringPaginatedListQuery`

## Conventions

- Commands modify state and extend `Command<R>` where `R` is the result type
- Queries read state and extend `Query<R>` where `R` is the result type
- Handlers are Spring beans automatically discovered and registered
- Events are dispatched directly via `EventBus` and published through Spring's `ApplicationEventPublisher` via `EventPublisher`
- Repositories accept `EventPublisher` (not `EventBus`) for publishing aggregate events
- IDs use UUID v7 by default for time-ordered identifiers
- Validation errors throw `ValidationCommandHandlerException` or `ValidationQueryHandlerException` and emit `ValidationFailed*Event` lifecycle events (logged at INFO level), while unexpected errors emit `ErrorProcess*Event` (logged at ERROR level)

## Modules

The library is split into seven Gradle modules. Published artifacts use the coordinate `com.kormichu:kqrs-<module>`.

| Module (artifact) | Description | Key dependencies |
|--------------------|-------------|------------------|
| `kqrs-core` | Core domain types: commands, queries, events, handlers, aggregates, repositories, dispatchers | `kotlin-reflect`, `kotlinx-coroutines-core`, `kotlinx-coroutines-slf4j`, `uuid-creator`, `slf4j-api` |
| `kqrs-spring` | Spring integration: handler storages, event bridge, `SpringPaginatedListQuery`, `SpringTransactionalExecutor` | `kqrs-core`, `spring-context`, `spring-tx`, `spring-data-commons` |
| `kqrs-reactor` | Reactive gateway (`ReactorKqrsGateway`) for Mono/Flux results | `kqrs-core`, `reactor-core` |
| `kqrs-prometheus` | Prometheus metrics event handlers | `kqrs-core`, `micrometer-core` |
| `kqrs-spring-boot-autoconfigure` | Spring Boot auto-configuration for core beans | `kqrs-spring`, `spring-boot-autoconfigure` |
| `kqrs-spring-boot-reactor-autoconfigure` | Auto-configuration for `ReactorKqrsGateway` (conditional on `reactor.core.publisher.Mono` on classpath) | `kqrs-spring-boot-autoconfigure`, `kqrs-reactor`, `spring-boot-autoconfigure` |
| `kqrs-spring-boot-prometheus-autoconfigure` | Auto-configuration for Prometheus metrics handlers (conditional on `MeterRegistry` bean + `kqrs.metrics.prometheus.enabled`) | `kqrs-spring-boot-autoconfigure`, `kqrs-prometheus`, `spring-boot-autoconfigure` |

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

## Instructions

### Configuration (YAML)

The library exposes Spring Boot configuration properties under the `kqrs` prefix.

```yaml
kqrs:
  event-bus:
    # When true, event handlers are invoked on the caller thread (blocking).
    # When false, event handlers run on the event bus dispatcher (non-blocking).
    blocking-listener: false
  metrics:
    prometheus:
      # Enables Prometheus metrics handlers when a MeterRegistry is present.
      enabled: true
```

### Defining IDs

IDs are type-safe wrappers around primitive values:

```kotlin
import com.kormichu.kqrs.Id
import java.util.UUID

data class UserId(override val value: UUID) : Id<UUID>(value)
data class OrderId(override val value: String) : Id<String>(value)

// Generate UUIDs
val userId: UserId = Id.generateUuidV7()  // Time-ordered UUID v7 (recommended)
val legacyId: UserId = Id.generateUuid()   // Random UUID v4

// Parse from strings
val fromString: UserId = Id.fromUuidString("550e8400-e29b-41d4-a716-446655440000")
val fromUuid: UserId = Id.fromUuid(someUuid)
val stringId: OrderId = Id.fromString("order-123")
```

### Commands

Commands represent operations that change application state:

```kotlin
import com.kormichu.kqrs.command.Command
import com.kormichu.kqrs.command.CommandHandler
import com.kormichu.kqrs.event.EventPublisher
import com.kormichu.kqrs.event.EventTag

// Define a command with custom event tags for observability
data class CreateUserCommand(
    val name: String,
    val email: String
) : Command<UserId>() {
    override fun getEventTags(): List<EventTag> = listOf(
        EventTag("user.email", email)
    )
}

// Define the command handler as a Spring bean
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

// Execute via KqrsGateway
@RestController
@RequestMapping("/users")
class UserController(private val kqrsGateway: KqrsGateway) {
    @PostMapping
    fun createUser(@RequestBody request: CreateUserRequest): CreateUserResponse =
        kqrsGateway.dispatch(
            CreateUserCommand(name = request.name, email = request.email)
        ).let { CreateUserResponse(id = it.value) }
}
```

### Queries

Queries retrieve data without modifying state:

```kotlin
import com.kormichu.kqrs.query.Query
import com.kormichu.kqrs.query.QueryHandler

// Define read model
data class UserReadModel(val id: UserId, val name: String, val email: String)

// Define query
data class GetUserByIdQuery(val id: UserId) : Query<UserReadModel>()

// Define query handler
@Component
class GetUserByIdQueryHandler(
    private val userRepository: UserReadModelRepository
) : QueryHandler<GetUserByIdQuery, UserReadModel> {
    override fun handle(query: GetUserByIdQuery): UserReadModel =
        userRepository.findById(query.id)
            ?: throw UserNotFoundException(query.id)
}

// Execute via KqrsGateway
@GetMapping("/{id}")
fun getUserById(@PathVariable id: UUID): UserResponse =
    kqrsGateway.query(GetUserByIdQuery(UserId(id))).toResponse()
```

### Paginated Queries

For queries that return paginated results (requires `kqrs-spring`):

```kotlin
import com.kormichu.kqrs.spring.query.SpringPaginatedListQuery
import org.springframework.data.domain.Pageable

data class ListUsersQuery(
    override val pagination: Pageable
) : SpringPaginatedListQuery<Page<UserReadModel>>(pagination = pagination)
```

### Aggregates and Domain Events

Aggregates encapsulate domain logic and emit events:

```kotlin
import com.kormichu.kqrs.AggregateRoot
import com.kormichu.kqrs.event.AggregateEvent
import com.kormichu.kqrs.event.EventPublisher
import com.kormichu.kqrs.repository.Repository

// Define aggregate events
data class UserCreatedEvent(
    override val aggregateId: UserId,
    val name: String,
    val email: String
) : AggregateEvent(aggregateId = aggregateId)

data class UserEmailChangedEvent(
    override val aggregateId: UserId,
    val newEmail: String,
    override val commandId: CommandId? = null  // Optional link to triggering command
) : AggregateEvent(aggregateId = aggregateId, commandId = commandId)

// Define aggregate
data class User(
    override val id: UserId,
    val name: String,
    val email: String
) : AggregateRoot<UserId>() {

    fun changeEmail(newEmail: String, commandId: CommandId? = null): User {
        val updated = copy(email = newEmail)
        updated.record(UserEmailChangedEvent(id, newEmail, commandId))
        return updated
    }

    companion object {
        fun create(name: String, email: String): User {
            val user = User(Id.generateUuidV7(), name, email)
            user.record(UserCreatedEvent(user.id, name, email))
            return user
        }
    }
}

// Define repository interface
interface UserRepository : Repository<User, UserId>

// Events are automatically published when using saveAndPublishEvents
userRepository.saveAndPublishEvents(user, eventPublisher)
```

### Event Handlers

Handle events using the `EventHandler` interface:

```kotlin
import com.kormichu.kqrs.event.EventHandler
import com.kormichu.kqrs.event.EventPublisher

// Define an event handler as a Spring bean (supports suspend functions)
@Component
class UserCreatedEventHandler(
    private val emailService: EmailService
) : EventHandler<UserCreatedEvent> {
    override suspend fun handle(event: UserCreatedEvent) {
        emailService.sendWelcomeEmail(event.email)
    }
}

// Multiple handlers can subscribe to the same event
@Component
class UserCreatedAuditHandler(
    private val auditService: AuditService
) : EventHandler<UserCreatedEvent> {
    override suspend fun handle(event: UserCreatedEvent) {
        auditService.logUserCreation(event.aggregateId)
    }
}

// Publish events directly via EventPublisher
@Component
class NotificationService(
    private val eventPublisher: EventPublisher
) {
    fun notifyUserCreated(userId: UserId, name: String, email: String) {
        eventPublisher.publish(UserCreatedEvent(userId, name, email))
    }

    fun notifyMultipleEvents(events: List<Event>) {
        eventPublisher.publish(events)
    }
}

// NoOpEventHandler is available for stubbing or disabling a handler
class StubHandler : NoOpEventHandler<UserCreatedEvent>()
```

### Validation Errors

The library distinguishes validation errors from unexpected errors in lifecycle events:

```kotlin
import com.kormichu.kqrs.command.ValidationCommandHandlerException
import com.kormichu.kqrs.query.ValidationQueryHandlerException

// Define a validation exception for commands
class UserAlreadyExistsException(email: String) :
    ValidationCommandHandlerException("User with email $email already exists")

// Define a validation exception for queries
class InvalidFilterException(filter: String) :
    ValidationQueryHandlerException("Invalid filter: $filter")

// In a handler, throw a validation exception
@Component
class CreateUserCommandHandler(
    private val userRepository: UserRepository,
    private val eventPublisher: EventPublisher
) : CommandHandler<CreateUserCommand, UserId> {
    override fun handle(command: CreateUserCommand): UserId {
        if (userRepository.existsByEmail(command.email)) {
            throw UserAlreadyExistsException(command.email)
        }
        return userRepository.saveAndPublishEvents(
            User.create(command.name, command.email),
            eventPublisher
        )
    }
}
```

When a `ValidationCommandHandlerException` is thrown, the gateway emits a `ValidationFailedCommandEvent` (logged at INFO level) instead of an `ErrorProcessCommandEvent` (logged at ERROR level). The same applies to `ValidationQueryHandlerException` and `ValidationFailedQueryEvent`.

### Transactional Handlers

The library provides built-in transactional handler interfaces that wrap handler execution in a transaction via `TransactionalExecutor`:

```kotlin
import com.kormichu.kqrs.command.TransactionalCommandHandler
import com.kormichu.kqrs.query.TransactionalQueryHandler
import com.kormichu.kqrs.transaction.TransactionalExecutor

// Synchronous transactional command handler
@Component
class CreateUserTransactionalHandler(
    override val transactionalExecutor: TransactionalExecutor,
    private val userRepository: UserRepository,
    private val eventPublisher: EventPublisher
) : TransactionalCommandHandler<CreateUserCommand, UserId> {
    override fun handleInTransaction(command: CreateUserCommand): UserId =
        userRepository.saveAndPublishEvents(
            User.create(command.name, command.email),
            eventPublisher
        )
}

// Synchronous transactional query handler (read-only transaction)
@Component
class GetUserTransactionalHandler(
    override val transactionalExecutor: TransactionalExecutor,
    private val repository: UserReadModelRepository
) : TransactionalQueryHandler<GetUserByIdQuery, UserReadModel> {
    override fun handleInTransaction(query: GetUserByIdQuery): UserReadModel =
        repository.findById(query.id) ?: throw UserNotFoundException(query.id)
}

// Async transactional command handler
@Component
class CreateUserAsyncTransactionalHandler(
    override val transactionalExecutor: TransactionalExecutor,
    private val userRepository: UserRepository,
    private val eventPublisher: EventPublisher
) : AsyncTransactionalCommandHandler<CreateUserCommand, UserId> {
    override fun handleInTransaction(command: CreateUserCommand): UserId =
        userRepository.saveAndPublishEvents(
            User.create(command.name, command.email),
            eventPublisher
        )
}

// Async IO transactional variants are also available:
// - AsyncIOTransactionalCommandHandler
// - AsyncIOTransactionalQueryHandler
```

The `TransactionalExecutor` interface defines:
- `execute(block)` — wraps in a read-write transaction
- `executeReadOnly(block)` — wraps in a read-only transaction

`SpringTransactionalExecutor` is the default implementation using Spring `@Transactional`.

### Saga Pattern

Use a long-running, stateful saga to coordinate multiple commands and compensate on failure. Model the saga as a Spring component that reacts to events, persists its state, and uses `KqrsGateway` to issue follow-up commands.

```kotlin
import com.kormichu.kqrs.event.EventHandler
import com.kormichu.kqrs.KqrsGateway

// Example saga state
enum class OrderSagaStatus { STARTED, PAYMENT_RESERVED, INVENTORY_RESERVED, COMPLETED, COMPENSATING, FAILED }

data class OrderSagaState(
    val orderId: OrderId,
    val status: OrderSagaStatus,
    val lastError: String? = null
)

interface OrderSagaRepository {
    fun find(orderId: OrderId): OrderSagaState?
    fun save(state: OrderSagaState)
}

// Saga handlers as EventHandler implementations
@Component
class OrderPlacedSagaHandler(
    private val kqrsGateway: KqrsGateway,
    private val sagaRepository: OrderSagaRepository
) : EventHandler<OrderPlacedEvent> {
    override suspend fun handle(event: OrderPlacedEvent) {
        sagaRepository.save(OrderSagaState(event.orderId, OrderSagaStatus.STARTED))
        kqrsGateway.dispatchAsync(ReservePaymentCommand(event.orderId))
    }
}

@Component
class PaymentReservedSagaHandler(
    private val kqrsGateway: KqrsGateway,
    private val sagaRepository: OrderSagaRepository
) : EventHandler<PaymentReservedEvent> {
    override suspend fun handle(event: PaymentReservedEvent) {
        sagaRepository.save(OrderSagaState(event.orderId, OrderSagaStatus.PAYMENT_RESERVED))
        kqrsGateway.dispatchAsync(ReserveInventoryCommand(event.orderId))
    }
}

@Component
class InventoryReservedSagaHandler(
    private val kqrsGateway: KqrsGateway,
    private val sagaRepository: OrderSagaRepository
) : EventHandler<InventoryReservedEvent> {
    override suspend fun handle(event: InventoryReservedEvent) {
        sagaRepository.save(OrderSagaState(event.orderId, OrderSagaStatus.INVENTORY_RESERVED))
        kqrsGateway.dispatchAsync(ConfirmOrderCommand(event.orderId))
    }
}

@Component
class OrderConfirmedSagaHandler(
    private val sagaRepository: OrderSagaRepository
) : EventHandler<OrderConfirmedEvent> {
    override suspend fun handle(event: OrderConfirmedEvent) {
        sagaRepository.save(OrderSagaState(event.orderId, OrderSagaStatus.COMPLETED))
    }
}

@Component
class ReservationFailedSagaHandler(
    private val kqrsGateway: KqrsGateway,
    private val sagaRepository: OrderSagaRepository
) : EventHandler<ReservationFailedEvent> {
    override suspend fun handle(event: ReservationFailedEvent) {
        sagaRepository.save(OrderSagaState(event.orderId, OrderSagaStatus.COMPENSATING, event.reason))
        kqrsGateway.dispatchAsync(ReleasePaymentCommand(event.orderId))
        kqrsGateway.dispatchAsync(ReleaseInventoryCommand(event.orderId))
        sagaRepository.save(OrderSagaState(event.orderId, OrderSagaStatus.FAILED, event.reason))
    }
}
```

Key practices:
- Persist saga state to handle retries and restarts.
- Make handlers idempotent; the same event may be delivered more than once.
- Use correlation IDs (e.g., commandId or aggregateId) to link commands, events, and saga state.
- Emit explicit failure events to trigger compensations.
- Keep saga decisions in one place; let commands modify aggregates and publish events.

### Command and Query Lifecycle Events

The library automatically emits lifecycle events for monitoring via `EventPublisher`:

```kotlin
import com.kormichu.kqrs.command.*
import com.kormichu.kqrs.query.*
import com.kormichu.kqrs.event.EventHandler

// Command lifecycle events:
// - StartProcessCommandEvent: emitted when command processing begins
// - StopProcessCommandEvent: emitted on successful completion (includes duration)
// - ErrorProcessCommandEvent: emitted on unexpected failure (includes exception, logged at ERROR)
// - ValidationFailedCommandEvent: emitted on validation failure (includes exception, logged at INFO)

// Query lifecycle events:
// - StartProcessQueryEvent: emitted when query processing begins
// - StopProcessQueryEvent: emitted on successful completion (includes duration)
// - ErrorProcessQueryEvent: emitted on unexpected failure (includes exception, logged at ERROR)
// - ValidationFailedQueryEvent: emitted on validation failure (includes exception, logged at INFO)

@Component
class CommandStartMetricsHandler : EventHandler<StartProcessCommandEvent<*>> {
    override suspend fun handle(event: StartProcessCommandEvent<*>) {
        logger.info("Command started: ${event.commandName}")
    }
}

@Component
class CommandStopMetricsHandler : EventHandler<StopProcessCommandEvent<*>> {
    override suspend fun handle(event: StopProcessCommandEvent<*>) {
        logger.info("Command completed: ${event.commandName} in ${event.duration}")
    }
}

@Component
class CommandErrorMetricsHandler : EventHandler<ErrorProcessCommandEvent<*>> {
    override suspend fun handle(event: ErrorProcessCommandEvent<*>) {
        logger.error("Command failed: ${event.commandName}", event.exception)
    }
}
```

### Asynchronous Usage (Coroutines)

For non-blocking operations using Kotlin coroutines:

```kotlin
import com.kormichu.kqrs.command.AsyncCommandHandler
import com.kormichu.kqrs.command.AsyncIOCommandHandler
import com.kormichu.kqrs.query.AsyncQueryHandler
import com.kormichu.kqrs.query.AsyncIOQueryHandler
import com.kormichu.kqrs.repository.AsyncRepository
import com.kormichu.kqrs.repository.AsyncReadModelRepository
import com.kormichu.kqrs.event.EventPublisher

// Async repository interfaces
interface AsyncUserRepository : AsyncRepository<User, UserId>
interface AsyncUserReadModelRepository : AsyncReadModelRepository<UserReadModel, UserId>

// Async command handler
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

// Async IO command handler — runs on Dispatchers.IO by default
@Component
class ImportUsersAsyncHandler(
    private val externalApi: ExternalApi,
    private val eventPublisher: EventPublisher
) : AsyncIOCommandHandler<ImportUsersCommand, Int> {
    override suspend fun handle(command: ImportUsersCommand): Int =
        externalApi.fetchAndImport()
}

// Async query handler
@Component
class GetUserByIdAsyncHandler(
    private val repository: AsyncUserReadModelRepository
) : AsyncQueryHandler<GetUserByIdQuery, UserReadModel> {
    override suspend fun handle(query: GetUserByIdQuery): UserReadModel =
        repository.findById(query.id) ?: throw UserNotFoundException(query.id)
}

// Async IO query handler — runs on Dispatchers.IO by default
@Component
class SearchUsersAsyncHandler(
    private val searchClient: SearchClient
) : AsyncIOQueryHandler<SearchUsersQuery, List<UserReadModel>> {
    override suspend fun handle(query: SearchUsersQuery): List<UserReadModel> =
        searchClient.search(query.term)
}

// Custom dispatcher per handler (overrides the default)
@Component
class CustomDispatcherHandler : AsyncCommandHandler<MyCommand, MyResult> {
    override val dispatcher: CoroutineDispatcher = Dispatchers.IO

    override suspend fun handle(command: MyCommand): MyResult = TODO()
}

// Execute in controller using suspend functions
@RestController
@RequestMapping("/users")
class UserController(private val kqrsGateway: KqrsGateway) {
    @PostMapping
    suspend fun createUser(@RequestBody request: CreateUserRequest): CreateUserResponse =
        kqrsGateway.dispatchAsync(
            CreateUserCommand(request.name, request.email)
        ).let { CreateUserResponse(id = it.value) }

    @GetMapping("/{id}")
    suspend fun getUserById(@PathVariable id: UUID): UserResponse =
        kqrsGateway.queryAsync(GetUserByIdQuery(UserId(id))).toResponse()
}
```

### Transactional Command Handling with Async Handlers

When an `AsyncCommandHandler` needs transactional semantics without using `AsyncTransactionalCommandHandler`, you can extract the transactional logic into a separate Spring bean with a regular (non-suspend) function annotated with `@Transactional`. The handler then calls that bean inside a coroutine context switch.

```kotlin
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import com.kormichu.kqrs.command.AsyncCommandHandler
import com.kormichu.kqrs.AsyncDispatchers
import com.kormichu.kqrs.event.EventPublisher

// The async handler delegates to a transactional executor inside an IO context
@Component
class CreateCategoryCommandHandler(
    private val executor: CreateCategoryTransactionalExecutor,
    private val dispatchers: AsyncDispatchers
) : AsyncCommandHandler<CreateCategoryCommand, StoreCategoryId> {
    override suspend fun handle(command: CreateCategoryCommand): StoreCategoryId =
        withContext(dispatchers.commandIOExecutorContext()) {
            executor.saveCategory(command)
        }
}

// A separate Spring bean with a regular function so @Transactional proxying works
@Component
class CreateCategoryTransactionalExecutor(
    private val categoryRepository: CategoryRepository,
    private val eventPublisher: EventPublisher
) {
    @Transactional
    fun saveCategory(command: CreateCategoryCommand): StoreCategoryId =
        categoryRepository.saveAndPublishEvents(
            Category.create(
                id = command.id,
                name = command.name
            ),
            eventPublisher
        )
}
```

Key practices:
- Never annotate a `suspend` function with `@Transactional` — Spring's proxy cannot intercept it correctly.
- Extract the transactional work into a dedicated `*TransactionalExecutor` Spring component with a regular (blocking) function.
- Use `withContext(dispatchers.commandIOExecutorContext())` (or `queryIOExecutorContext()`) to bridge from the coroutine world to the blocking transactional call on an IO dispatcher.
- The transactional executor can use `Repository.saveAndPublishEvents(aggregate, eventPublisher)` so that domain events are published after the entity is persisted within the same transaction boundary.
- The same pattern applies to queries: extract a `*TransactionalExecutor` for `AsyncQueryHandler` when read transactions are needed.
- Alternatively, use the built-in `AsyncTransactionalCommandHandler` / `AsyncIOTransactionalCommandHandler` interfaces which handle this automatically via `TransactionalExecutor`.

### Reactor Usage

For reactive streams using Project Reactor (optional dependency):

```kotlin
import com.kormichu.kqrs.reactor.ReactorKqrsGateway
import reactor.core.publisher.Mono
import reactor.core.publisher.Flux

// Define reactive command returning Mono
data class CreateUserCommand(
    val name: String,
    val email: String
) : Command<Mono<UserId>>()

@Component
class CreateUserReactiveHandler(
    private val userRepository: ReactiveUserRepository,
    private val eventPublisher: EventPublisher
) : CommandHandler<CreateUserCommand, Mono<UserId>> {
    override fun handle(command: CreateUserCommand): Mono<UserId> =
        userRepository.save(User.create(command.name, command.email))
            .map { it.id }
}

// Define reactive query returning Mono
data class GetUserByIdQuery(val id: UserId) : Query<Mono<UserReadModel>>()

@Component
class GetUserByIdReactiveHandler(
    private val repository: ReactiveUserReadModelRepository
) : QueryHandler<GetUserByIdQuery, Mono<UserReadModel>> {
    override fun handle(query: GetUserByIdQuery): Mono<UserReadModel> =
        repository.findById(query.id)
            .switchIfEmpty(Mono.error(UserNotFoundException(query.id)))
}

// Use ReactorKqrsGateway for Mono/Flux results
@RestController
@RequestMapping("/users")
class UserController(private val kqrsGateway: ReactorKqrsGateway) {
    @PostMapping
    fun createUser(@RequestBody request: CreateUserRequest): Mono<CreateUserResponse> =
        kqrsGateway.dispatch(
            CreateUserCommand(request.name, request.email)
        ).map { CreateUserResponse(id = it.value) }

    @GetMapping("/{id}")
    fun getUserById(@PathVariable id: UUID): Mono<UserResponse> =
        kqrsGateway.query(GetUserByIdQuery(UserId(id)))
            .map { it.toResponse() }

    // Flux is also supported for streaming results
    data class GetAllUsersQuery(val limit: Int) : Query<Flux<UserReadModel>>()
}
```

### Custom Command/Query Names

Override automatic naming:

```kotlin
import com.kormichu.kqrs.command.CommandName
import com.kormichu.kqrs.query.QueryName

class CreateUserCommandName : CommandName("user.create")
class GetUserQueryName : QueryName("user.get-by-id")

data class CreateUserCommand(
    val name: String,
    val email: String
) : Command<UserId>(commandName = CreateUserCommandName())

data class GetUserByIdQuery(
    val id: UserId
) : Query<UserReadModel>(queryName = GetUserQueryName())
```

## Spring Integration

The library provides seamless Spring Boot integration through auto-configuration.

### Auto-Configuration

The library automatically configures the following beans when included as a dependency:

| Bean | Description |
|------|-------------|
| `KqrsGateway` | Main entry point for dispatching commands and queries (`DefaultKqrsGateway` implements both `KqrsGateway` and `AsyncKqrsGateway`) |
| `CommandBus` | Routes commands to synchronous handlers (`DefaultCommandBus`) |
| `AsyncCommandBus` | Routes commands to async handlers (`DefaultAsyncCommandBus`) |
| `QueryBus` | Routes queries to synchronous handlers (`DefaultQueryBus`) |
| `AsyncQueryBus` | Routes queries to async handlers (`DefaultAsyncQueryBus`) |
| `EventBus` | Dispatches events directly to `EventHandler` beans (`DefaultEventBus`) |
| `EventPublisher` | Publishes events via Spring's `ApplicationEventPublisher` (`SpringEventPublisher`) |
| `AsyncDispatchers` | Provides coroutine dispatchers for async command, query, and event handling (`DefaultAsyncDispatchers`) |
| `CommandExecutor` / `AsyncCommandExecutor` | Execute commands via sync / coroutine handlers |
| `QueryExecutor` / `AsyncQueryExecutor` | Execute queries via sync / coroutine handlers |
| `EventExecutor` | Executes event handlers (blocking or non-blocking based on config) |
| `SpringEventListener` | Bridges Spring events to `EventBus.dispatch()` (implements `SmartApplicationListener`) |
| `TransactionalExecutor` | Transaction wrapper (`SpringTransactionalExecutor`) |
| `ReactorKqrsGateway` | Reactive gateway (only when `reactor-core` is on classpath) |

### Handler Discovery

Handlers are automatically discovered via Spring's `ApplicationContext`. Any Spring bean implementing the following interfaces will be registered:

```kotlin
// Command handlers
interface CommandHandler<C : Command<R>, R>                          // Synchronous
interface TransactionalCommandHandler<C : Command<R>, R>             // Synchronous, transactional
interface AsyncCommandHandler<C : Command<R>, R>                     // Coroutine-based
interface AsyncIOCommandHandler<C : Command<R>, R>                   // Coroutine-based, IO dispatcher
interface AsyncTransactionalCommandHandler<C : Command<R>, R>        // Coroutine-based, transactional
interface AsyncIOTransactionalCommandHandler<C : Command<R>, R>      // Coroutine-based, IO dispatcher, transactional

// Query handlers
interface QueryHandler<Q : Query<R>, R>                              // Synchronous
interface TransactionalQueryHandler<Q : Query<R>, R>                 // Synchronous, transactional
interface AsyncQueryHandler<Q : Query<R>, R>                         // Coroutine-based
interface AsyncIOQueryHandler<Q : Query<R>, R>                       // Coroutine-based, IO dispatcher
interface AsyncTransactionalQueryHandler<Q : Query<R>, R>            // Coroutine-based, transactional
interface AsyncIOTransactionalQueryHandler<Q : Query<R>, R>          // Coroutine-based, IO dispatcher, transactional

// Event handlers
interface EventHandler<E : Event>                                    // Coroutine-based (always async)
```

All handler interfaces extend the common `Handler<O>` interface which provides automatic type parameter resolution via `objectClass`.

### EventBus and EventPublisher Architecture

The library separates event dispatching into two distinct mechanisms:

1. **`EventBus` (direct dispatch)**: Routes events to all registered `EventHandler` beans for that event type via `eventBus.dispatch(event)`. Used internally by `SpringEventListener`.

2. **`EventPublisher` (Spring bridge)**: Publishes events through Spring's `ApplicationEventPublisher` via `eventPublisher.publish(event)`. The `SpringEventListener` then bridges these to `EventBus.dispatch()`.

```kotlin
// EventPublisher interface — publishes through Spring's ApplicationEventPublisher
interface EventPublisher {
    fun publish(events: List<Event>)
    fun publish(event: Event) = publish(listOf(event))
}

// EventBus interface — dispatches directly to EventHandler beans
interface EventBus {
    fun <E : Event> dispatch(event: E)
}

// Flow: EventPublisher.publish() → Spring ApplicationEventPublisher
//       → SpringEventListener (SmartApplicationListener) → EventBus.dispatch()
//       → EventHandler.handle()

// DefaultEventBus dispatches to all matching EventHandler beans
class DefaultEventBus(
    private val executor: EventExecutor,
    private val handlerStorage: EventHandlerStorage
) : EventBus

// SpringEventPublisher bridges to Spring's event system
class SpringEventPublisher(
    private val eventPublisher: ApplicationEventPublisher
) : EventPublisher

// SpringEventListener bridges Spring events back to EventBus
class SpringEventListener(private val eventBus: EventBus) : SmartApplicationListener {
    // Filters for PayloadApplicationEvent<Event> and dispatches to EventBus
}
```

### Multiple Event Handlers

Unlike command/query handlers (which require exactly one handler per type), multiple `EventHandler` implementations can subscribe to the same event type:

```kotlin
// All handlers will be invoked when UserCreatedEvent is published
@Component
class SendWelcomeEmailHandler : EventHandler<UserCreatedEvent> {
    override suspend fun handle(event: UserCreatedEvent) { /* ... */ }
}

@Component
class UpdateUserStatisticsHandler : EventHandler<UserCreatedEvent> {
    override suspend fun handle(event: UserCreatedEvent) { /* ... */ }
}

@Component
class AuditUserCreationHandler : EventHandler<UserCreatedEvent> {
    override suspend fun handle(event: UserCreatedEvent) { /* ... */ }
}
```

### Configuration Properties

```kotlin
// SpringKqrsProperties
@ConfigurationProperties("kqrs")
data class SpringKqrsProperties(
    val eventBus: SpringKqrsEventBusProperties,
    val metrics: SpringKqrsMetricsProperties
)

data class SpringKqrsEventBusProperties(
    val blockingListener: Boolean  // default: false
)

data class SpringKqrsMetricsProperties(
    val prometheus: SpringKqrsMetricsPrometheusProperties
)

data class SpringKqrsMetricsPrometheusProperties(
    val enabled: Boolean  // default: false
)
```

### Prometheus Metrics Integration

When `kqrs.metrics.prometheus.enabled=true` and a `MeterRegistry` bean is present:

```kotlin
// Automatically registered beans via PrometheusMetricsCommandConfiguration
// and PrometheusMetricsQueryConfiguration:
// - PrometheusMetricsStartCommandEventHandler: records command start counts
// - PrometheusMetricsStopCommandEventHandler: records command stop counts and durations
// - PrometheusMetricsErrorCommandEventHandler: records command error counts
// - PrometheusMetricsStartQueryEventHandler: records query start counts
// - PrometheusMetricsStopQueryEventHandler: records query stop counts and durations
// - PrometheusMetricsErrorQueryEventHandler: records query error counts

// These implement MetricsCommandEventHandler / MetricsQueryEventHandler
// marker interfaces for handler type resolution.

// Metrics recorded:
// - kqrs_command_start, kqrs_command_stop, kqrs_command_error, kqrs_command_duration
// - kqrs_query_start, kqrs_query_stop, kqrs_query_error, kqrs_query_duration
// - Event tags are included as metric labels plus "command" or "query"
```

### Custom Beans

You can override any auto-configured bean:

```kotlin
@Configuration
class CustomKqrsConfiguration {
    // Override the default AsyncDispatchers
    @Bean
    fun kqrsCoroutineBusDispatchers(): AsyncDispatchers =
        DefaultAsyncDispatchers(
            defaultCommandIODispatcher = Dispatchers.IO,
            defaultQueryIODispatcher = Dispatchers.IO,
            defaultEventDispatcher = Dispatchers.IO  // Use IO dispatcher for events
        )
}
```

## Examples

### Complete User Management Example

```kotlin
// 1. Define domain types
data class UserId(override val value: UUID) : Id<UUID>(value)

data class User(
    override val id: UserId,
    val name: String,
    val email: String
) : AggregateRoot<UserId>() {
    companion object {
        fun create(name: String, email: String): User {
            val user = User(Id.generateUuidV7(), name, email)
            user.record(UserCreatedEvent(user.id, name, email))
            return user
        }
    }
}

// 2. Define events
data class UserCreatedEvent(
    override val aggregateId: UserId,
    val name: String,
    val email: String
) : AggregateEvent(aggregateId = aggregateId)

// 3. Define command and handler
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
            User.create(command.name, command.email),
            eventPublisher
        )
}

// 4. Define query and handler
data class UserReadModel(val id: UserId, val name: String, val email: String)
data class GetUserByIdQuery(val id: UserId) : Query<UserReadModel>()

@Component
class GetUserByIdQueryHandler(
    private val repository: UserReadModelRepository
) : QueryHandler<GetUserByIdQuery, UserReadModel> {
    override fun handle(query: GetUserByIdQuery): UserReadModel =
        repository.findById(query.id) ?: throw UserNotFoundException(query.id)
}

// 5. Define event handlers
@Component
class UserCreatedNotificationHandler(
    private val emailService: EmailService
) : EventHandler<UserCreatedEvent> {
    override suspend fun handle(event: UserCreatedEvent) {
        emailService.sendWelcomeEmail(event.email)
    }
}

// 6. REST Controller
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

## Restrictions

- Handlers must be Spring beans (annotated with `@Component` or registered via `@Bean`)
- Each command/query type must have exactly one handler
- Multiple event handlers can subscribe to the same event type
- Reactor support requires `reactor-core` on the classpath
- `ReactorKqrsGateway` is only registered when Reactor is available
- Repositories accept `EventPublisher` (not `EventBus`) for publishing aggregate events

## Debugging

- Enable DEBUG logging for `com.kormichu.kqrs` to see handler routing and execution details
- Use lifecycle events (`StartProcess*`, `StopProcess*`, `ErrorProcess*`, `ValidationFailed*`) for monitoring
- Validation errors (`ValidationCommandHandlerException`, `ValidationQueryHandlerException`) are logged at INFO level; unexpected errors at ERROR level
- Enable metrics via `kqrs.metrics.prometheus.enabled=true` for Prometheus integration

