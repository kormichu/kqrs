package com.kormichu.kqrs.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.slf4j.MDCContext
import com.kormichu.kqrs.logger.logger
import kotlin.coroutines.CoroutineContext

interface CoroutineDispatchers {
    val defaultCommandIODispatcher: CoroutineDispatcher
    val defaultQueryIODispatcher: CoroutineDispatcher
    val defaultEventDispatcher: CoroutineDispatcher

    fun commandExecutorContext(
        commandDispatcher: CoroutineDispatcher
    ): CoroutineContext

    fun commandIOExecutorContext(
        commandDispatcher: CoroutineDispatcher? = null
    ): CoroutineContext = defaultCommandIODispatcher

    fun queryExecutorContext(
        queryDispatcher: CoroutineDispatcher
    ): CoroutineContext

    fun queryIOExecutorContext(
        queryDispatcher: CoroutineDispatcher? = null
    ): CoroutineContext = defaultQueryIODispatcher

    fun eventExecutorContext(
        eventDispatcher: CoroutineDispatcher? = null
    ): CoroutineContext = defaultEventDispatcher
}

open class DefaultCoroutineDispatchers(
    override val defaultCommandIODispatcher: CoroutineDispatcher,
    override val defaultQueryIODispatcher: CoroutineDispatcher,
    override val defaultEventDispatcher: CoroutineDispatcher
): CoroutineDispatchers {
    private val logger by logger()

    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        logger.error("Uncaught exception in coroutine context", exception)
    }

    override fun commandExecutorContext(
        commandDispatcher: CoroutineDispatcher
    ) = commandDispatcher +
        MDCContext() +
        exceptionHandler

    override fun commandIOExecutorContext(
        commandDispatcher: CoroutineDispatcher?
    ) = commandExecutorContext(
        commandDispatcher ?: defaultCommandIODispatcher
    )

    override fun queryExecutorContext(
        queryDispatcher: CoroutineDispatcher
    ) = queryDispatcher +
        MDCContext() +
        exceptionHandler

    override fun queryIOExecutorContext(
        queryDispatcher: CoroutineDispatcher?
    ) = queryExecutorContext(
        queryDispatcher ?: defaultQueryIODispatcher
    )

    override fun eventExecutorContext(
        eventDispatcher: CoroutineDispatcher?
    ) = (eventDispatcher ?: defaultEventDispatcher) +
        MDCContext() +
        exceptionHandler
}
