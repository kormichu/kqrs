package com.kormichu.kqrs.infrastructure.reactor

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import com.kormichu.kqrs.command.CommandBus
import com.kormichu.kqrs.event.EventPublisher
import com.kormichu.kqrs.query.QueryBus

@Configuration
class ReactorCqrsConfiguration {
    @Bean
    @ConditionalOnClass(name = ["reactor.core.publisher.Mono"])
    fun reactorCqrsGateway(
        commandBus: CommandBus,
        queryBus: QueryBus,
        eventPublisher: EventPublisher
    ): ReactorCqrsGateway = ReactorCqrsGateway(
        commandBus = commandBus,
        queryBus = queryBus,
        eventPublisher = eventPublisher
    )
}
