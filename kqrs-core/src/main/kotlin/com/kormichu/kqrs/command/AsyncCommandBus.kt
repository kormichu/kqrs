package com.kormichu.kqrs.command

import kotlin.reflect.KClass

interface AsyncCommandBus {
    suspend fun <C : Command<R>, R> dispatchAsync(command: C): R
}

class DefaultAsyncCommandBus(
    private val asyncCommandExecutor: AsyncCommandExecutor,
    private val asyncHandlerStorage: AsyncCommandHandlerStorage
) : AsyncCommandBus {
    @Suppress("UNCHECKED_CAST")
    override suspend fun <C : Command<R>, R> dispatchAsync(command: C): R {
        val handler = asyncHandlerStorage.getHandler(command::class)
            ?: throw UnsupportedAsyncCommandException(command)
        return asyncCommandExecutor.executeAsync(command, handler as AsyncCommandHandler<C, R>)
    }
}

class UnsupportedAsyncCommandException(command: Command<*>) :
    Exception("The async command %s is unsupported by any handler".format(command.javaClass))

interface AsyncCommandHandlerStorage {
    fun <C : Command<R>, R> getHandler(commandClass: KClass<C>): AsyncCommandHandler<C, R>?
}
