package com.kormichu.kqrs

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.slf4j.Logger
import kotlin.coroutines.CoroutineContext

/**
 * Shared logic for the command and query handler execution flows.
 *
 * Both flows log the operation being executed and, in the asynchronous case, select the coroutine
 * context to run the handler on. These helpers keep that behaviour in a single place so the command
 * and query executors do not duplicate it.
 */

internal fun logHandlerExecution(
    logger: Logger,
    operationKind: String,
    operation: Any,
    operationId: Any,
    handler: Any,
) {
    logger.debug(
        "Executing {} {} with ID: {} by handler {}",
        operationKind,
        operation::class.java.simpleName,
        operationId,
        handler::class.java.simpleName,
    )
}

internal suspend fun <R> executeOnDispatcher(
    handlerDispatcher: CoroutineDispatcher?,
    isIoHandler: Boolean,
    executorContext: (CoroutineDispatcher) -> CoroutineContext,
    ioExecutorContext: () -> CoroutineContext,
    handle: suspend () -> R,
): R {
    return handlerDispatcher?.let {
        withContext(executorContext(it)) {
            handle()
        }
    } ?: if (isIoHandler) {
        withContext(ioExecutorContext()) {
            handle()
        }
    } else {
        handle()
    }
}
