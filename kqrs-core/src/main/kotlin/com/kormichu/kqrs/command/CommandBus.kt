package com.kormichu.kqrs.command

import kotlin.reflect.KClass

interface CommandBus {
    fun <C : Command<R>, R> dispatch(command: C): R
}

class DefaultCommandBus(
    private val commandExecutor: CommandExecutor,
    private val handlerStorage: CommandHandlerStorage,
) : CommandBus {
    @Suppress("UNCHECKED_CAST")
    override fun <C : Command<R>, R> dispatch(command: C): R {
        val handler = handlerStorage.getHandler(command::class)
            ?: throw UnsupportedCommandException(command)
        return commandExecutor.execute(command, handler as CommandHandler<C, R>)
    }
}

abstract class ValidationCommandHandlerException(message: String) : RuntimeException(message)

class UnsupportedCommandException(command: Command<*>) :
    Exception("The command %s is unsupported by any handler".format(command.javaClass))

interface CommandHandlerStorage {
    fun <C : Command<R>, R> getHandler(commandClass: KClass<C>): CommandHandler<C, R>?
}
