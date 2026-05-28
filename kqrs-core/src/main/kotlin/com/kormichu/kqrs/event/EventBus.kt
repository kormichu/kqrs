package com.kormichu.kqrs.event

import kotlin.reflect.KClass

interface EventBus {
    fun <E : Event> dispatch(event: E)
}

class DefaultEventBus(
    private val executor: EventExecutor,
    private val handlerStorage: EventHandlerStorage
) : EventBus {
    @Suppress("UNCHECKED_CAST")
    override fun <E : Event> dispatch(event: E) {
        handlerStorage.getHandlers(event::class).forEach {
            executor.execute(event, it as EventHandler<Event>)
        }
    }
}

interface EventHandlerStorage {
    fun <E : Event> getHandlers(eventClass: KClass<E>): List<EventHandler<E>>
}
