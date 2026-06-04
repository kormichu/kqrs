package com.kormichu.kqrs.command

import com.kormichu.kqrs.AsyncDispatchers
import com.kormichu.kqrs.executeOnDispatcher
import com.kormichu.kqrs.logHandlerExecution
import com.kormichu.kqrs.logger.logger

interface AsyncCommandExecutor {
    suspend fun <C : Command<R>, R> executeAsync(command: C, handler: AsyncCommandHandler<C, R>): R
}

class DefaultAsyncCommandExecutor(
    private val coroutineDispatchers: AsyncDispatchers
) : AsyncCommandExecutor {
    private val logger by logger()

    override suspend fun <C : Command<R>, R> executeAsync(command: C, handler: AsyncCommandHandler<C, R>): R {
        logHandlerExecution(logger, "async command", command, command.commandId, handler)
        return executeOnDispatcher(
            handlerDispatcher = handler.dispatcher,
            isIoHandler = handler is AsyncIOCommandHandler<*, *>,
            executorContext = { coroutineDispatchers.commandExecutorContext(it) },
            ioExecutorContext = { coroutineDispatchers.commandIOExecutorContext() },
        ) {
            handler.handle(command)
        }
    }
}
