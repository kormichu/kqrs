package com.kormichu.kqrs.command

import com.kormichu.kqrs.AsyncDispatchers
import com.kormichu.kqrs.logger.logger
import kotlinx.coroutines.withContext

interface AsyncCommandExecutor {
    suspend fun <C : Command<R>, R> executeAsync(command: C, handler: AsyncCommandHandler<C, R>): R
}

class MiddlewareAsyncCommandExecutor(
    private val middlewares: List<AsyncCommandMiddleware>,
    private val delegate: AsyncCommandExecutor,
) : AsyncCommandExecutor {
    override suspend fun <C : Command<R>, R> executeAsync(command: C, handler: AsyncCommandHandler<C, R>): R {
        val context = AsyncCommandExecutionContext(command, handler)
        return executeAt(0, context) { delegate.executeAsync(command, handler) }
    }

    private suspend fun <C : Command<R>, R> executeAt(
        index: Int,
        context: AsyncCommandExecutionContext<C, R>,
        terminalStep: suspend () -> R,
    ): R {
        if (index >= middlewares.size) {
            return terminalStep()
        }

        return middlewares[index].intercept(context) {
            executeAt(index + 1, context, terminalStep)
        }
    }
}

class DefaultAsyncCommandExecutor(
    private val coroutineDispatchers: AsyncDispatchers
) : AsyncCommandExecutor {
    private val logger by logger()

    override suspend fun <C : Command<R>, R> executeAsync(command: C, handler: AsyncCommandHandler<C, R>): R {
        logger.debug(
            "Executing async command {} with ID: {} by handler {}",
            command::class.java.simpleName,
            command.commandId,
            handler::class.java.simpleName,
        )
        return handler.dispatcher?.let {
            withContext(coroutineDispatchers.commandExecutorContext(it)) {
                handler.handle(command)
            }
        } ?: when (handler) {
            is AsyncIOCommandHandler<*, *> -> withContext(coroutineDispatchers.commandIOExecutorContext()) {
                handler.handle(command)
            }

            else -> handler.handle(command)
        }
    }
}
