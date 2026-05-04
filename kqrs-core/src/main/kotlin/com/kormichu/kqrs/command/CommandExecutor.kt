package com.kormichu.kqrs.command

import com.kormichu.kqrs.logger.logger

interface CommandExecutor {
    fun <C : Command<R>, R> execute(command: C, handler: CommandHandler<C, R>): R
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
