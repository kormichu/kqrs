package com.kormichu.kqrs.spring

import com.kormichu.kqrs.DefaultKqrsGateway
import com.kormichu.kqrs.KqrsGateway
import com.kormichu.kqrs.command.AsyncCommandBus
import com.kormichu.kqrs.command.CommandBus
import com.kormichu.kqrs.AsyncDispatchers
import com.kormichu.kqrs.DefaultAsyncDispatchers
import com.kormichu.kqrs.event.EventPublisher
import com.kormichu.kqrs.query.AsyncQueryBus
import com.kormichu.kqrs.query.QueryBus
import kotlinx.coroutines.Dispatchers
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@AutoConfiguration
@Configuration
@EnableConfigurationProperties(SpringKqrsProperties::class)
class SpringKqrsConfiguration {
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
}
