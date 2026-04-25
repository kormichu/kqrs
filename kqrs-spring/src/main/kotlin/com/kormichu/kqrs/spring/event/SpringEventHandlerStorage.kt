package com.kormichu.kqrs.spring.event

import com.kormichu.kqrs.event.Event
import com.kormichu.kqrs.event.EventHandler
import com.kormichu.kqrs.event.EventHandlerStorage
import com.kormichu.kqrs.spring.SpringBeansHelper
import org.springframework.context.ApplicationContext
import kotlin.reflect.KClass

internal class SpringEventHandlerStorage(
    applicationContext: ApplicationContext
) : EventHandlerStorage {
    private val handlersMap = SpringBeansHelper.getListHandlers(
        applicationContext = applicationContext,
        handlerClassName = EventHandler::class.java,
    )

    override fun <E : Event> getHandlers(eventClass: KClass<E>): List<EventHandler<E>> {
        @Suppress("UNCHECKED_CAST")
        return handlersMap[eventClass] as? List<EventHandler<E>> ?: emptyList()
    }
}
