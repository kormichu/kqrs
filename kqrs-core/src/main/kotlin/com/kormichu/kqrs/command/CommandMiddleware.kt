package com.kormichu.kqrs.command

import kotlin.time.Duration

data class CommandExecutionContext<C : Command<R>, R>(
    val command: C,
    val handler: CommandHandler<C, R>,
)

fun interface CommandMiddleware {
    fun <C : Command<R>, R> intercept(context: CommandExecutionContext<C, R>, next: () -> R): R
}

fun interface AsyncCommandMiddleware {
    suspend fun <C : Command<R>, R> intercept(
        context: AsyncCommandExecutionContext<C, R>,
        next: suspend () -> R,
    ): R
}

data class AsyncCommandExecutionContext<C : Command<R>, R>(
    val command: C,
    val handler: AsyncCommandHandler<C, R>,
)

class RetryCommandMiddleware(
    private val maxAttempts: Int = 1,
    private val delayBetweenAttempts: Duration = Duration.ZERO,
    private val shouldRetry: (Throwable) -> Boolean = { true },
) : CommandMiddleware {
    init {
        require(maxAttempts > 0) { "maxAttempts must be greater than zero" }
    }

    override fun <C : Command<R>, R> intercept(context: CommandExecutionContext<C, R>, next: () -> R): R {
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

class RetryAsyncCommandMiddleware(
    private val maxAttempts: Int = 1,
    private val delayBetweenAttempts: Duration = Duration.ZERO,
    private val shouldRetry: (Throwable) -> Boolean = { true },
) : AsyncCommandMiddleware {
    init {
        require(maxAttempts > 0) { "maxAttempts must be greater than zero" }
    }

    override suspend fun <C : Command<R>, R> intercept(
        context: AsyncCommandExecutionContext<C, R>,
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

class TracingCommandMiddleware(
    private val onStart: (CommandExecutionContext<*, *>) -> Unit = {},
    private val onSuccess: (CommandExecutionContext<*, *>, Any?) -> Unit = { _, _ -> },
    private val onError: (CommandExecutionContext<*, *>, Throwable) -> Unit = { _, _ -> },
) : CommandMiddleware {
    override fun <C : Command<R>, R> intercept(context: CommandExecutionContext<C, R>, next: () -> R): R {
        onStart(context)
        return runCatching { next() }
            .onSuccess { onSuccess(context, it) }
            .onFailure { onError(context, it) }
            .getOrThrow()
    }
}

class TracingAsyncCommandMiddleware(
    private val onStart: (AsyncCommandExecutionContext<*, *>) -> Unit = {},
    private val onSuccess: (AsyncCommandExecutionContext<*, *>, Any?) -> Unit = { _, _ -> },
    private val onError: (AsyncCommandExecutionContext<*, *>, Throwable) -> Unit = { _, _ -> },
) : AsyncCommandMiddleware {
    override suspend fun <C : Command<R>, R> intercept(
        context: AsyncCommandExecutionContext<C, R>,
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
