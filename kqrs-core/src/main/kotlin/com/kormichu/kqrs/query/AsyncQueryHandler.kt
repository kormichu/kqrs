package com.kormichu.kqrs.query

import com.kormichu.kqrs.Handler
import com.kormichu.kqrs.transaction.TransactionalExecutor
import kotlinx.coroutines.CoroutineDispatcher
import kotlin.reflect.KClass

interface AsyncQueryHandler<Q : Query<R>, R> : Handler<Q> {
    val dispatcher: CoroutineDispatcher?
        get() = null

    suspend fun handle(query: Q): R

    @Suppress("UNCHECKED_CAST")
    override fun getBaseHandlerClass(): KClass<Handler<Q>> =
        AsyncQueryHandler::class as KClass<Handler<Q>>
}

interface AsyncIOQueryHandler<Q : Query<R>, R> : AsyncQueryHandler<Q, R> {
    @Suppress("UNCHECKED_CAST")
    override fun getBaseHandlerClass(): KClass<Handler<Q>> =
        AsyncIOQueryHandler::class as KClass<Handler<Q>>
}

interface AsyncTransactionalQueryHandler<Q : Query<R>, R> : AsyncQueryHandler<Q, R> {
    val transactionalExecutor: TransactionalExecutor

    fun handleInTransaction(query: Q): R
    override suspend fun handle(query: Q): R =
        transactionalExecutor.executeReadOnly {
            handleInTransaction(query)
        }

    @Suppress("UNCHECKED_CAST")
    override fun getBaseHandlerClass(): KClass<Handler<Q>> =
        AsyncTransactionalQueryHandler::class as KClass<Handler<Q>>
}

interface AsyncIOTransactionalQueryHandler<Q : Query<R>, R> :
    AsyncTransactionalQueryHandler<Q, R>,
    AsyncIOQueryHandler<Q, R> {
    @Suppress("UNCHECKED_CAST")
    override fun getBaseHandlerClass(): KClass<Handler<Q>> =
        AsyncIOTransactionalQueryHandler::class as KClass<Handler<Q>>
}
