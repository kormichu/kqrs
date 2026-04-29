package com.kormichu.kqrs.spring.event

import com.kormichu.kqrs.AsyncDispatchers
import com.kormichu.kqrs.event.DefaultEventBus
import com.kormichu.kqrs.event.DefaultEventExecutor
import com.kormichu.kqrs.event.EventBus
import com.kormichu.kqrs.event.EventExecutor
import com.kormichu.kqrs.event.EventHandlerStorage
import com.kormichu.kqrs.spring.SpringKqrsProperties
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
class SpringKqrsEventConfiguration {
    @Bean
    @ConditionalOnMissingBean(EventHandlerStorage::class)
    fun kqrsEventHandlerStorage(
        applicationContext: ApplicationContext
    ): EventHandlerStorage = SpringEventHandlerStorage(applicationContext)

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
    @ConditionalOnMissingBean(EventExecutor::class)
    fun kqrsEventExecutor(
        coroutineDispatchers: AsyncDispatchers,
        cqrsProperties: SpringKqrsProperties,
    ): EventExecutor = DefaultEventExecutor(
        coroutineDispatchers,
        blockingListener = cqrsProperties.eventBus.blockingListener,
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
}
