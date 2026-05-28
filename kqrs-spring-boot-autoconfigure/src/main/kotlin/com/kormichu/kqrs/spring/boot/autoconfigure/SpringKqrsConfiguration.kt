package com.kormichu.kqrs.spring.boot.autoconfigure

import com.kormichu.kqrs.AsyncDispatchers
import com.kormichu.kqrs.DefaultAsyncDispatchers
import com.kormichu.kqrs.DefaultKqrsGateway
import com.kormichu.kqrs.KqrsGateway
import com.kormichu.kqrs.command.AsyncCommandBus
import com.kormichu.kqrs.command.AsyncCommandExecutor
import com.kormichu.kqrs.command.AsyncCommandHandlerStorage
import com.kormichu.kqrs.command.CommandBus
import com.kormichu.kqrs.command.CommandExecutor
import com.kormichu.kqrs.command.CommandHandlerStorage
import com.kormichu.kqrs.command.DefaultAsyncCommandBus
import com.kormichu.kqrs.command.DefaultAsyncCommandExecutor
import com.kormichu.kqrs.command.DefaultCommandBus
import com.kormichu.kqrs.command.DefaultCommandExecutor
import com.kormichu.kqrs.event.DefaultEventBus
import com.kormichu.kqrs.event.DefaultEventExecutor
import com.kormichu.kqrs.event.EventBus
import com.kormichu.kqrs.event.EventExecutor
import com.kormichu.kqrs.event.EventHandlerStorage
import com.kormichu.kqrs.event.EventPublisher
import com.kormichu.kqrs.query.AsyncQueryBus
import com.kormichu.kqrs.query.AsyncQueryExecutor
import com.kormichu.kqrs.query.AsyncQueryHandlerStorage
import com.kormichu.kqrs.query.DefaultAsyncQueryBus
import com.kormichu.kqrs.query.DefaultAsyncQueryExecutor
import com.kormichu.kqrs.query.DefaultQueryBus
import com.kormichu.kqrs.query.DefaultQueryExecutor
import com.kormichu.kqrs.query.QueryBus
import com.kormichu.kqrs.query.QueryExecutor
import com.kormichu.kqrs.query.QueryHandlerStorage
import com.kormichu.kqrs.spring.command.SpringAsyncCommandHandlerStorage
import com.kormichu.kqrs.spring.command.SpringCommandHandlerStorage
import com.kormichu.kqrs.spring.event.SpringEventHandlerStorage
import com.kormichu.kqrs.spring.event.SpringEventListener
import com.kormichu.kqrs.spring.event.SpringEventPublisher
import com.kormichu.kqrs.spring.query.SpringAsyncQueryHandlerStorage
import com.kormichu.kqrs.spring.query.SpringQueryHandlerStorage
import com.kormichu.kqrs.spring.transaction.SpringTransactionalExecutor
import com.kormichu.kqrs.transaction.TransactionalExecutor
import kotlinx.coroutines.Dispatchers
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@AutoConfiguration
@Configuration
@EnableConfigurationProperties(SpringKqrsProperties::class)
@Suppress("TooManyFunctions")
class SpringKqrsConfiguration {

    // region Gateway

    @Bean
    fun kqrsGateway(
        commandBus: CommandBus,
        asyncCommandBus: AsyncCommandBus,
        queryBus: QueryBus,
        asyncQueryBus: AsyncQueryBus,
        eventPublisher: EventPublisher,
    ): KqrsGateway = DefaultKqrsGateway(
        commandBus = commandBus,
        asyncCommandBus = asyncCommandBus,
        queryBus = queryBus,
        asyncQueryBus = asyncQueryBus,
        eventPublisher = eventPublisher,
    )

    @Bean
    @Suppress("InjectDispatcher")
    fun kqrsCoroutineBusDispatchers(): AsyncDispatchers =
        DefaultAsyncDispatchers(
            defaultCommandIODispatcher = Dispatchers.IO,
            defaultQueryIODispatcher = Dispatchers.IO,
            defaultEventDispatcher = Dispatchers.Default,
        )

    // endregion

    // region Commands

    @Bean
    @ConditionalOnMissingBean(CommandExecutor::class)
    fun kqrsCommandExecutor(): CommandExecutor = DefaultCommandExecutor()

    @Bean
    @ConditionalOnMissingBean(AsyncCommandExecutor::class)
    fun kqrsAsyncCommandExecutor(
        coroutineDispatchers: AsyncDispatchers
    ): AsyncCommandExecutor = DefaultAsyncCommandExecutor(coroutineDispatchers)

    @Bean
    @ConditionalOnMissingBean(CommandHandlerStorage::class)
    fun kqrsCommandHandlerStorage(
        applicationContext: ApplicationContext
    ): CommandHandlerStorage = SpringCommandHandlerStorage(applicationContext)

    @Bean
    @ConditionalOnMissingBean(AsyncCommandHandlerStorage::class)
    fun kqrsAsyncCommandHandlerStorage(
        applicationContext: ApplicationContext
    ): AsyncCommandHandlerStorage = SpringAsyncCommandHandlerStorage(applicationContext)

    @Bean
    @ConditionalOnMissingBean(CommandBus::class)
    fun kqrsCommandBus(
        commandExecutor: CommandExecutor,
        handlerStorage: CommandHandlerStorage
    ): CommandBus = DefaultCommandBus(
        commandExecutor = commandExecutor,
        handlerStorage = handlerStorage,
    )

    @Bean
    @ConditionalOnMissingBean(AsyncCommandBus::class)
    fun kqrsAsyncCommandBus(
        asyncCommandExecutor: AsyncCommandExecutor,
        asyncHandlerStorage: AsyncCommandHandlerStorage
    ): AsyncCommandBus = DefaultAsyncCommandBus(
        asyncCommandExecutor = asyncCommandExecutor,
        asyncHandlerStorage = asyncHandlerStorage,
    )

    // endregion

    // region Queries

    @Bean
    @ConditionalOnMissingBean(QueryExecutor::class)
    fun kqrsQueryExecutor(): QueryExecutor = DefaultQueryExecutor()

    @Bean
    @ConditionalOnMissingBean(AsyncQueryExecutor::class)
    fun kqrsAsyncQueryExecutor(
        coroutineDispatchers: AsyncDispatchers
    ): AsyncQueryExecutor = DefaultAsyncQueryExecutor(coroutineDispatchers)

    @Bean
    @ConditionalOnMissingBean(QueryHandlerStorage::class)
    fun kqrsQueryHandlerStorage(
        applicationContext: ApplicationContext
    ): QueryHandlerStorage = SpringQueryHandlerStorage(applicationContext)

    @Bean
    @ConditionalOnMissingBean(AsyncQueryHandlerStorage::class)
    fun kqrsAsyncQueryHandlerStorage(
        applicationContext: ApplicationContext
    ): AsyncQueryHandlerStorage = SpringAsyncQueryHandlerStorage(applicationContext)

    @Bean
    @ConditionalOnMissingBean(QueryBus::class)
    fun kqrsQueryBus(
        queryExecutor: QueryExecutor,
        handlerStorage: QueryHandlerStorage
    ): QueryBus = DefaultQueryBus(
        queryExecutor = queryExecutor,
        handlerStorage = handlerStorage,
    )

    @Bean
    @ConditionalOnMissingBean(AsyncQueryBus::class)
    fun kqrsAsyncQueryBus(
        asyncQueryExecutor: AsyncQueryExecutor,
        asyncHandlerStorage: AsyncQueryHandlerStorage
    ): AsyncQueryBus = DefaultAsyncQueryBus(
        asyncQueryExecutor = asyncQueryExecutor,
        asyncHandlerStorage = asyncHandlerStorage,
    )

    // endregion

    // region Events

    @Bean
    @ConditionalOnMissingBean(EventHandlerStorage::class)
    fun kqrsEventHandlerStorage(
        applicationContext: ApplicationContext
    ): EventHandlerStorage = SpringEventHandlerStorage(applicationContext)

    @Bean
    @ConditionalOnMissingBean(EventExecutor::class)
    fun kqrsEventExecutor(
        coroutineDispatchers: AsyncDispatchers,
        cqrsProperties: SpringKqrsProperties,
    ): EventExecutor = DefaultEventExecutor(
        coroutineDispatchers,
        blockingListener = cqrsProperties.eventBus.blockingListener,
    )

    @Bean
    @ConditionalOnMissingBean(EventBus::class)
    fun kqrsEventBus(
        executor: EventExecutor,
        handlerStorage: EventHandlerStorage
    ): EventBus = DefaultEventBus(
        executor = executor,
        handlerStorage = handlerStorage,
    )

    @Bean
    @ConditionalOnMissingBean(SpringEventListener::class)
    fun kqrsEventListener(
        eventBus: EventBus
    ): SpringEventListener = SpringEventListener(
        eventBus = eventBus,
    )

    @Bean
    @ConditionalOnMissingBean(SpringEventPublisher::class)
    fun kqrsEventPublisher(
        eventPublisher: ApplicationEventPublisher
    ): SpringEventPublisher = SpringEventPublisher(
        eventPublisher = eventPublisher,
    )

    // endregion

    // region Transaction

    @Bean
    @ConditionalOnMissingBean(TransactionalExecutor::class)
    fun kqrsTransactionalExecutor(): TransactionalExecutor = SpringTransactionalExecutor()

    // endregion
}
