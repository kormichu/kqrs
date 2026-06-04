package com.kormichu.kqrs.command

import com.kormichu.kqrs.logHandlerExecution
import com.kormichu.kqrs.logger.logger

interface CommandExecutor {
    fun <C : Command<R>, R> execute(command: C, handler: CommandHandler<C, R>): R
}

class DefaultCommandExecutor : CommandExecutor {
    private val logger by logger()

    override fun <C : Command<R>, R> execute(command: C, handler: CommandHandler<C, R>): R {
        logHandlerExecution(logger, "command", command, command.commandId, handler)
        return handler.handle(command)
    }
}
