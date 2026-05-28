package com.kormichu.kqrs.spring.command

import com.kormichu.kqrs.command.AsyncCommandHandler
import com.kormichu.kqrs.command.AsyncCommandHandlerStorage
import com.kormichu.kqrs.command.Command
import com.kormichu.kqrs.command.CommandHandler
import com.kormichu.kqrs.command.CommandHandlerStorage
import com.kormichu.kqrs.spring.SpringBeansHelper
import org.springframework.context.ApplicationContext
import kotlin.reflect.KClass

class SpringCommandHandlerStorage(
    applicationContext: ApplicationContext
) : CommandHandlerStorage {
    private val handlersMap = SpringBeansHelper.getHandlers(
        applicationContext = applicationContext,
        handlerClassName = CommandHandler::class.java,
    )

    @Suppress("UNCHECKED_CAST")
    override fun <C : Command<R>, R> getHandler(commandClass: KClass<C>): CommandHandler<C, R>? {
        return when (val handler = handlersMap[commandClass]) {
            is CommandHandler<*, *> -> handler as CommandHandler<C, R>
            else -> null
        }
    }
}

class SpringAsyncCommandHandlerStorage(
    applicationContext: ApplicationContext
) : AsyncCommandHandlerStorage {
    private val handlersMap = SpringBeansHelper.getHandlers(
        applicationContext = applicationContext,
        handlerClassName = AsyncCommandHandler::class.java,
    )

    @Suppress("UNCHECKED_CAST")
    override fun <C : Command<R>, R> getHandler(commandClass: KClass<C>): AsyncCommandHandler<C, R>? {
        return when (val handler = handlersMap[commandClass]) {
            is AsyncCommandHandler<*, *> -> handler as AsyncCommandHandler<C, R>
            else -> null
        }
    }
}
