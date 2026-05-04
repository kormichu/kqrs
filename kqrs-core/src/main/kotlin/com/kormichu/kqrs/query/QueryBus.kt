package com.kormichu.kqrs.query

import kotlin.reflect.KClass

interface QueryBus {
    fun <Q : Query<R>, R> dispatch(query: Q): R
}

open class DefaultQueryBus(
    private val queryExecutor: QueryExecutor,
    private val handlerStorage: QueryHandlerStorage,
) : QueryBus {
    @Suppress("UNCHECKED_CAST")
    override fun <Q : Query<R>, R> dispatch(query: Q): R {
        val handler = handlerStorage.getHandler(query::class)
            ?: throw UnsupportedQueryException(query)
        return queryExecutor.execute(query, handler as QueryHandler<Q, R>)
    }
}

abstract class ValidationQueryHandlerException(message: String) : RuntimeException(message)

class UnsupportedQueryException(query: Query<*>) :
    Exception("The query %s is unsupported by any handler".format(query.javaClass))

interface QueryHandlerStorage {
    fun <Q : Query<R>, R> getHandler(queryClass: KClass<Q>): QueryHandler<Q, R>?
}
