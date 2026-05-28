package com.kormichu.kqrs.query

import com.kormichu.kqrs.logger.logger

interface QueryExecutor {
    fun <Q : Query<R>, R> execute(query: Q, handler: QueryHandler<Q, R>): R
}

class DefaultQueryExecutor : QueryExecutor {
    private val logger by logger()

    override fun <Q : Query<R>, R> execute(query: Q, handler: QueryHandler<Q, R>): R {
        logger.debug(
            "Executing query {} with ID: {} by handler {}",
            query::class.java.simpleName,
            query.queryId,
            handler::class.java.simpleName,
        )
        return handler.handle(query)
    }
}
