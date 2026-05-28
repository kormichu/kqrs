package com.kormichu.kqrs.command

import com.kormichu.kqrs.Handler
import com.kormichu.kqrs.transaction.TransactionalExecutor
import kotlinx.coroutines.CoroutineDispatcher
import kotlin.reflect.KClass

interface AsyncCommandHandler<C : Command<R>, R> : Handler<C> {
    val dispatcher: CoroutineDispatcher?
        get() = null

    suspend fun handle(command: C): R

    @Suppress("UNCHECKED_CAST")
    override fun getBaseHandlerClass(): KClass<Handler<C>> =
        AsyncCommandHandler::class as KClass<Handler<C>>
}

interface AsyncIOCommandHandler<C : Command<R>, R> : AsyncCommandHandler<C, R> {
    @Suppress("UNCHECKED_CAST")
    override fun getBaseHandlerClass(): KClass<Handler<C>> =
        AsyncIOCommandHandler::class as KClass<Handler<C>>
}

interface AsyncTransactionalCommandHandler<C : Command<R>, R> : AsyncCommandHandler<C, R> {
    val transactionalExecutor: TransactionalExecutor

    fun handleInTransaction(command: C): R
    override suspend fun handle(command: C): R =
        transactionalExecutor.execute {
            handleInTransaction(command)
        }

    @Suppress("UNCHECKED_CAST")
    override fun getBaseHandlerClass(): KClass<Handler<C>> =
        AsyncTransactionalCommandHandler::class as KClass<Handler<C>>
}

interface AsyncIOTransactionalCommandHandler<C : Command<R>, R> :
    AsyncTransactionalCommandHandler<C, R>,
    AsyncIOCommandHandler<C, R> {
    @Suppress("UNCHECKED_CAST")
    override fun getBaseHandlerClass(): KClass<Handler<C>> =
        AsyncIOTransactionalCommandHandler::class as KClass<Handler<C>>
}

