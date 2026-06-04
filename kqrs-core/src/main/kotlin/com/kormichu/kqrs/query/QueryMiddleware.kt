package com.kormichu.kqrs.query

import kotlin.time.Duration

data class QueryExecutionContext<Q : Query<R>, R>(
    val query: Q,
    val handler: QueryHandler<Q, R>,
)

fun interface QueryMiddleware {
    fun <Q : Query<R>, R> intercept(context: QueryExecutionContext<Q, R>, next: () -> R): R
}

fun interface AsyncQueryMiddleware {
    suspend fun <Q : Query<R>, R> intercept(
        context: AsyncQueryExecutionContext<Q, R>,
        next: suspend () -> R,
    ): R
}

data class AsyncQueryExecutionContext<Q : Query<R>, R>(
    val query: Q,
    val handler: AsyncQueryHandler<Q, R>,
)

class RetryQueryMiddleware(
    private val maxAttempts: Int = 3,
    private val delayBetweenAttempts: Duration = Duration.ZERO,
    private val shouldRetry: (Throwable) -> Boolean = { true },
) : QueryMiddleware {
    init {
        require(maxAttempts > 0) { "maxAttempts must be greater than zero" }
    }

    override fun <Q : Query<R>, R> intercept(context: QueryExecutionContext<Q, R>, next: () -> R): R {
        var attempt = 1
        while (true) {
            try {
                return next()
            } catch (exception: Throwable) {
                if (attempt >= maxAttempts || !shouldRetry(exception)) {
                    throw exception
                }
                if (delayBetweenAttempts.isPositive()) {
                    Thread.sleep(delayBetweenAttempts.inWholeMilliseconds)
                }
                attempt++
            }
        }
    }
}

class RetryAsyncQueryMiddleware(
    private val maxAttempts: Int = 3,
    private val delayBetweenAttempts: Duration = Duration.ZERO,
    private val shouldRetry: (Throwable) -> Boolean = { true },
) : AsyncQueryMiddleware {
    init {
        require(maxAttempts > 0) { "maxAttempts must be greater than zero" }
    }

    override suspend fun <Q : Query<R>, R> intercept(
        context: AsyncQueryExecutionContext<Q, R>,
        next: suspend () -> R,
    ): R {
        var attempt = 1
        while (true) {
            try {
                return next()
            } catch (exception: Throwable) {
                if (attempt >= maxAttempts || !shouldRetry(exception)) {
                    throw exception
                }
                if (delayBetweenAttempts.isPositive()) {
                    kotlinx.coroutines.delay(delayBetweenAttempts)
                }
                attempt++
            }
        }
    }
}

class TracingQueryMiddleware(
    private val onStart: (QueryExecutionContext<*, *>) -> Unit = {},
    private val onSuccess: (QueryExecutionContext<*, *>, Any?) -> Unit = { _, _ -> },
    private val onError: (QueryExecutionContext<*, *>, Throwable) -> Unit = { _, _ -> },
) : QueryMiddleware {
    override fun <Q : Query<R>, R> intercept(context: QueryExecutionContext<Q, R>, next: () -> R): R {
        onStart(context)
        return runCatching { next() }
            .onSuccess { onSuccess(context, it) }
            .onFailure { onError(context, it) }
            .getOrThrow()
    }
}

class TracingAsyncQueryMiddleware(
    private val onStart: (AsyncQueryExecutionContext<*, *>) -> Unit = {},
    private val onSuccess: (AsyncQueryExecutionContext<*, *>, Any?) -> Unit = { _, _ -> },
    private val onError: (AsyncQueryExecutionContext<*, *>, Throwable) -> Unit = { _, _ -> },
) : AsyncQueryMiddleware {
    override suspend fun <Q : Query<R>, R> intercept(
        context: AsyncQueryExecutionContext<Q, R>,
        next: suspend () -> R,
    ): R {
        onStart(context)
        return try {
            next().also { onSuccess(context, it) }
        } catch (exception: Throwable) {
            onError(context, exception)
            throw exception
        }
    }
}
