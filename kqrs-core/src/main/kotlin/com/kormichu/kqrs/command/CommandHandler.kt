package com.kormichu.kqrs.command

import com.kormichu.kqrs.Handler
import com.kormichu.kqrs.transaction.TransactionalExecutor
import kotlin.reflect.KClass

interface CommandHandler<C : Command<R>, R> : Handler<C> {
    fun handle(command: C): R

    @Suppress("UNCHECKED_CAST")
    override fun getBaseHandlerClass(): KClass<Handler<C>> =
        CommandHandler::class as KClass<Handler<C>>
}

interface TransactionalCommandHandler<C : Command<R>, R> : CommandHandler<C, R> {
    val transactionalExecutor: TransactionalExecutor

    fun handleInTransaction(command: C): R

    override fun handle(command: C): R =
        transactionalExecutor.execute {
            handleInTransaction(command)
        }

    @Suppress("UNCHECKED_CAST")
    override fun getBaseHandlerClass(): KClass<Handler<C>> =
        TransactionalCommandHandler::class as KClass<Handler<C>>
}

