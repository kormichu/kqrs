package com.kormichu.kqrs.command

import kotlinx.coroutines.withContext
import com.kormichu.kqrs.AsyncDispatchers
import com.kormichu.kqrs.logger.logger
import kotlin.getValue

interface AsyncCommandExecutor {
    suspend fun <C : Command<R>, R> executeAsync(command: C, handler: AsyncCommandHandler<C, R>): R
}

class DefaultAsyncCommandExecutor(
    private val coroutineDispatchers: AsyncDispatchers
): AsyncCommandExecutor {
    private val logger by logger()

    override suspend fun <C : Command<R>, R> executeAsync(command: C, handler: AsyncCommandHandler<C, R>): R {
        logger.debug(
            "Executing async command {} with ID: {} by handler {}",
            command::class.java.simpleName,
            command.commandId,
            handler::class.java.simpleName
        )
        return handler.dispatcher?.let {
            withContext(coroutineDispatchers.commandExecutorContext(it)) {
                handler.handle(command)
            }
        } ?: when(handler) {
            is AsyncIOCommandHandler<*, *> -> withContext(coroutineDispatchers.commandIOExecutorContext()) {
                handler.handle(command)
            }

            else -> handler.handle(command)
        }
    }
}
