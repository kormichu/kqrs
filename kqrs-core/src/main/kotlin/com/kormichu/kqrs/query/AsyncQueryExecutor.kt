package com.kormichu.kqrs.query

import com.kormichu.kqrs.AsyncDispatchers
import com.kormichu.kqrs.executeOnDispatcher
import com.kormichu.kqrs.logHandlerExecution
import com.kormichu.kqrs.logger.logger

interface AsyncQueryExecutor {
    suspend fun <Q : Query<R>, R> executeAsync(query: Q, handler: AsyncQueryHandler<Q, R>): R
}

class DefaultAsyncQueryExecutor(
    private val coroutineDispatchers: AsyncDispatchers
) : AsyncQueryExecutor {
    private val logger by logger()

    override suspend fun <Q : Query<R>, R> executeAsync(query: Q, handler: AsyncQueryHandler<Q, R>): R {
        logHandlerExecution(logger, "async query", query, query.queryId, handler)
        return executeOnDispatcher(
            handlerDispatcher = handler.dispatcher,
            isIoHandler = handler is AsyncIOQueryHandler<*, *>,
            executorContext = { coroutineDispatchers.queryExecutorContext(it) },
            ioExecutorContext = { coroutineDispatchers.queryIOExecutorContext() },
        ) {
            handler.handle(query)
        }
    }
}
