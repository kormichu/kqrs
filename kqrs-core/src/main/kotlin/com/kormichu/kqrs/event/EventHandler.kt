package com.kormichu.kqrs.event

import com.kormichu.kqrs.Handler
import kotlin.reflect.KClass

interface EventHandler<E : Event>: Handler<E> {
    suspend fun handle(event: E)

    @Suppress("UNCHECKED_CAST")
    override fun getBaseHandlerClass(): KClass<Handler<E>> =
        EventHandler::class as KClass<Handler<E>>
}

open class NoOpEventHandler<E: Event> : EventHandler<E> {
    override suspend fun handle(event: E) {
        // no-op
    }
}
