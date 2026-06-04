package com.kormichu.kqrs.query

import com.kormichu.kqrs.AsyncDispatchers
import com.kormichu.kqrs.logger.logger
import kotlinx.coroutines.withContext

interface AsyncQueryExecutor {
    suspend fun <Q : Query<R>, R> executeAsync(query: Q, handler: AsyncQueryHandler<Q, R>): R
}

class MiddlewareAsyncQueryExecutor(
    private val middlewares: List<AsyncQueryMiddleware>,
    private val delegate: AsyncQueryExecutor,
) : AsyncQueryExecutor {
    override suspend fun <Q : Query<R>, R> executeAsync(query: Q, handler: AsyncQueryHandler<Q, R>): R {
        val context = AsyncQueryExecutionContext(query, handler)
        return executeAt(0, context) { delegate.executeAsync(query, handler) }
    }

    private suspend fun <Q : Query<R>, R> executeAt(
        index: Int,
        context: AsyncQueryExecutionContext<Q, R>,
        terminalStep: suspend () -> R,
    ): R {
        if (index >= middlewares.size) {
            return terminalStep()
        }

        return middlewares[index].intercept(context) {
            executeAt(index + 1, context, terminalStep)
        }
    }
}

class DefaultAsyncQueryExecutor(
    private val coroutineDispatchers: AsyncDispatchers
) : AsyncQueryExecutor {
    private val logger by logger()

    override suspend fun <Q : Query<R>, R> executeAsync(query: Q, handler: AsyncQueryHandler<Q, R>): R {
        logger.debug(
            "Executing async query {} with ID: {} by handler {}",
            query::class.java.simpleName,
            query.queryId,
            handler::class.java.simpleName,
        )
        return handler.dispatcher?.let {
            withContext(coroutineDispatchers.queryExecutorContext(it)) {
                handler.handle(query)
            }
        } ?: when (handler) {
            is AsyncIOQueryHandler<*, *> -> withContext(coroutineDispatchers.queryIOExecutorContext()) {
                handler.handle(query)
            }

            else -> handler.handle(query)
        }
    }
}
