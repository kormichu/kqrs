package com.kormichu.kqrs.query

import kotlin.reflect.KClass

interface AsyncQueryBus {
    suspend fun <Q : Query<R>, R> dispatchAsync(query: Q): R
}

open class DefaultAsyncQueryBus(
    private val asyncQueryExecutor: AsyncQueryExecutor,
    private val asyncHandlerStorage: AsyncQueryHandlerStorage
) : AsyncQueryBus {
    @Suppress("UNCHECKED_CAST")
    override suspend fun <Q : Query<R>, R> dispatchAsync(query: Q): R {
        val handler = asyncHandlerStorage.getHandler(query::class)
            ?: throw UnsupportedAsyncQueryException(query)
        return asyncQueryExecutor.executeAsync(query, handler as AsyncQueryHandler<Q, R>)
    }
}

class UnsupportedAsyncQueryException(query: Query<*>) :
    Exception("The async query %s is unsupported by any handler".format(query.javaClass))

interface AsyncQueryHandlerStorage {
    fun <Q : Query<R>, R> getHandler(queryClass: KClass<Q>): AsyncQueryHandler<Q, R>?
}
