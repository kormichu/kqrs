package com.kormichu.kqrs.spring.reactor

import com.kormichu.kqrs.DefaultReactorKqrsGateway
import com.kormichu.kqrs.ReactorKqrsGateway
import com.kormichu.kqrs.command.CommandBus
import com.kormichu.kqrs.command.CommandExecutor
import com.kormichu.kqrs.event.EventPublisher
import com.kormichu.kqrs.query.QueryBus
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@AutoConfiguration
@Configuration
class ReactorKqrsConfiguration {
    @Bean
    @ConditionalOnMissingBean(CommandExecutor::class)
    @ConditionalOnClass(name = ["reactor.core.publisher.Mono"])
    fun reactorKqrsGateway(
        commandBus: CommandBus,
        queryBus: QueryBus,
        eventPublisher: EventPublisher
    ): ReactorKqrsGateway = DefaultReactorKqrsGateway(
        commandBus = commandBus,
        queryBus = queryBus,
        eventPublisher = eventPublisher
    )
}
