package com.kormichu.kqrs.query

import com.kormichu.kqrs.logHandlerExecution
import com.kormichu.kqrs.logger.logger

interface QueryExecutor {
    fun <Q : Query<R>, R> execute(query: Q, handler: QueryHandler<Q, R>): R
}

class DefaultQueryExecutor : QueryExecutor {
    private val logger by logger()

    override fun <Q : Query<R>, R> execute(query: Q, handler: QueryHandler<Q, R>): R {
        logHandlerExecution(logger, "query", query, query.queryId, handler)
        return handler.handle(query)
    }
}
