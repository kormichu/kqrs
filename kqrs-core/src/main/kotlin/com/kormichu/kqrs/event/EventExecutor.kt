package com.kormichu.kqrs.event

import com.kormichu.kqrs.coroutines.CoroutineDispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import com.kormichu.kqrs.logger.logger
import kotlin.getValue

interface EventExecutor {
    fun <E : Event> execute(event: E, handler: EventHandler<E>)
}

class DefaultEventExecutor(
    private val coroutineDispatchers: CoroutineDispatchers,
    private val blockingListener: Boolean
): EventExecutor {
    private val logger by logger()

    override fun <E : Event> execute(event: E, handler: EventHandler<E>) {
        if (blockingListener) {
            runBlocking {
                runCatching {
                    logger.debug(
                        "Handling event {} with ID: {} by handler {} in blocking way",
                        event::class.java.simpleName,
                        event.eventId,
                        handler::class.java.simpleName
                    )
                    handler.handle(event)
                }
                    .onFailure { logEventHandlingFailure(event, it) }
            }
        } else {
            launchEventDispatch {
                runCatching {
                    logger.debug(
                        "Handling event {} with ID: {} by handler {} in non-blocking way",
                        event::class.java.simpleName,
                        event.eventId,
                        handler::class.java.simpleName
                    )
                    handler.handle(event)
                }
                    .onFailure { logEventHandlingFailure(event, it) }
            }
        }
    }

    private fun launchEventDispatch(block: suspend CoroutineScope.() -> Unit) {
        CoroutineScope(
            coroutineDispatchers.eventExecutorContext()
        ).launch(block = block)
    }

    private fun logEventHandlingFailure(event: Event, throwable: Throwable) {
        logger.error(
            "Failed to handle event ${event::class.java.simpleName} with ID: ${event.eventId}",
            throwable
        )
    }
}
