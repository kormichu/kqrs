package com.kormichu.kqrs.query

import com.kormichu.kqrs.logger.logger

interface QueryExecutor {
    fun <Q : Query<R>, R> execute(query: Q, handler: QueryHandler<Q, R>): R
}

class MiddlewareQueryExecutor(
    private val middlewares: List<QueryMiddleware>,
    private val delegate: QueryExecutor = DefaultQueryExecutor(),
) : QueryExecutor {
    override fun <Q : Query<R>, R> execute(query: Q, handler: QueryHandler<Q, R>): R {
        val context = QueryExecutionContext(query, handler)
        return executeAt(0, context) { delegate.execute(query, handler) }
    }

    private fun <Q : Query<R>, R> executeAt(
        index: Int,
        context: QueryExecutionContext<Q, R>,
        terminalStep: () -> R,
    ): R {
        if (index >= middlewares.size) {
            return terminalStep()
        }

        return middlewares[index].intercept(context) {
            executeAt(index + 1, context, terminalStep)
        }
    }
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
