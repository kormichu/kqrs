package com.kormichu.kqrs.query

import com.kormichu.kqrs.AsyncDispatchers
import kotlinx.coroutines.withContext
import com.kormichu.kqrs.logger.logger
import kotlin.getValue

interface AsyncQueryExecutor {
    suspend fun <Q : Query<R>, R> executeAsync(query: Q, handler: AsyncQueryHandler<Q, R>): R
}

class DefaultAsyncQueryExecutor(
    private val coroutineDispatchers: AsyncDispatchers
): AsyncQueryExecutor {
    private val logger by logger()

    override suspend fun <Q : Query<R>, R> executeAsync(query: Q, handler: AsyncQueryHandler<Q, R>): R {
        logger.debug(
            "Executing async query {} with ID: {} by handler {}",
            query::class.java.simpleName,
            query.queryId,
            handler::class.java.simpleName
        )
        return handler.dispatcher?.let {
            withContext(coroutineDispatchers.queryExecutorContext(it)) {
                handler.handle(query)
            }
        } ?: when(handler) {
            is AsyncIOQueryHandler<*, *> -> withContext(coroutineDispatchers.queryIOExecutorContext()) {
                handler.handle(query)
            }

            else -> handler.handle(query)
        }
    }
}
