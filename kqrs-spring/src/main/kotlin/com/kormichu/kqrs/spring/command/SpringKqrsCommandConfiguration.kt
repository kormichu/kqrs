package com.kormichu.kqrs.spring.command

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
import com.kormichu.kqrs.coroutines.CoroutineDispatchers
import com.kormichu.kqrs.spring.SpringKqrsProperties
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@AutoConfiguration
@Configuration
@EnableConfigurationProperties(SpringKqrsProperties::class)
class SpringKqrsCommandConfiguration {
    @Bean
    @ConditionalOnMissingBean(CommandExecutor::class)
    fun kqrsCommandExecutor(): CommandExecutor = DefaultCommandExecutor()

    @Bean
    @ConditionalOnMissingBean(AsyncCommandExecutor::class)
    fun kqrsAsyncCommandExecutor(
        coroutineDispatchers: CoroutineDispatchers
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
}
