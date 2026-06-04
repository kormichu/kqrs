package com.kormichu.kqrs.command

import com.kormichu.kqrs.logger.logger

interface CommandExecutor {
    fun <C : Command<R>, R> execute(command: C, handler: CommandHandler<C, R>): R
}

class MiddlewareCommandExecutor(
    private val middlewares: List<CommandMiddleware>,
    private val delegate: CommandExecutor = DefaultCommandExecutor(),
) : CommandExecutor {
    override fun <C : Command<R>, R> execute(command: C, handler: CommandHandler<C, R>): R {
        val context = CommandExecutionContext(command, handler)
        return executeAt(0, context) { delegate.execute(command, handler) }
    }

    private fun <C : Command<R>, R> executeAt(
        index: Int,
        context: CommandExecutionContext<C, R>,
        terminalStep: () -> R,
    ): R {
        if (index >= middlewares.size) {
            return terminalStep()
        }

        return middlewares[index].intercept(context) {
            executeAt(index + 1, context, terminalStep)
        }
    }
}

class DefaultCommandExecutor : CommandExecutor {
    private val logger by logger()

    override fun <C : Command<R>, R> execute(command: C, handler: CommandHandler<C, R>): R {
        logger.debug(
            "Executing command {} with ID: {} by handler {}",
            command::class.java.simpleName,
            command.commandId,
            handler::class.java.simpleName,
        )
        return handler.handle(command)
    }
}
