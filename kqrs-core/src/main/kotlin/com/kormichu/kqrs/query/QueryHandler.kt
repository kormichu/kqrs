package com.kormichu.kqrs.query

import com.kormichu.kqrs.Handler
import com.kormichu.kqrs.transaction.TransactionalExecutor
import kotlin.reflect.KClass

interface QueryHandler<Q : Query<R>, R>: Handler<Q> {
    fun handle(query: Q): R

    @Suppress("UNCHECKED_CAST")
    override fun getBaseHandlerClass(): KClass<Handler<Q>> =
        QueryHandler::class as KClass<Handler<Q>>
}

interface TransactionalQueryHandler<Q : Query<R>, R>: QueryHandler<Q, R> {
    val transactionalExecutor: TransactionalExecutor

    fun handleInTransaction(query: Q): R

    override fun handle(query: Q): R =
        transactionalExecutor.executeReadOnly {
            handleInTransaction(query)
        }

    @Suppress("UNCHECKED_CAST")
    override fun getBaseHandlerClass(): KClass<Handler<Q>> =
        TransactionalQueryHandler::class as KClass<Handler<Q>>
}

