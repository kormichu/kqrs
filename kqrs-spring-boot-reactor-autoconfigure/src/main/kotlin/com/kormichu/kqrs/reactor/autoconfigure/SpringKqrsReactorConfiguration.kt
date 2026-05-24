package com.kormichu.kqrs.reactor.autoconfigure

import com.kormichu.kqrs.command.CommandBus
import com.kormichu.kqrs.event.EventPublisher
import com.kormichu.kqrs.query.QueryBus
import com.kormichu.kqrs.spring.boot.autoconfigure.SpringKqrsProperties
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@AutoConfiguration
@Configuration
@EnableConfigurationProperties(SpringKqrsProperties::class)
@Suppress("TooManyFunctions")
class SpringKqrsReactorConfiguration {
    @Configuration
    @ConditionalOnClass(name = ["reactor.core.publisher.Mono"])
    class ReactorConfiguration {
        @Bean
        @ConditionalOnMissingBean(name = ["reactorKqrsGateway"])
        fun reactorKqrsGateway(
            commandBus: CommandBus,
            queryBus: QueryBus,
            eventPublisher: EventPublisher
        ): Any {
            val gatewayClass = Class.forName("com.kormichu.kqrs.reactor.DefaultReactorKqrsGateway")
            return gatewayClass.getDeclaredConstructor(
                CommandBus::class.java,
                QueryBus::class.java,
                EventPublisher::class.java
            ).newInstance(commandBus, queryBus, eventPublisher)
        }
    }
}
