package com.kormichu.kqrs.spring.event

import com.kormichu.kqrs.event.Event
import com.kormichu.kqrs.event.EventBus
import org.springframework.context.ApplicationEvent
import org.springframework.context.PayloadApplicationEvent
import org.springframework.context.event.SmartApplicationListener
import org.springframework.core.ResolvableType

class SpringEventListener(
    private val eventBus: EventBus
) : SmartApplicationListener {
    override fun supportsEventType(eventType: Class<out ApplicationEvent>): Boolean {
        if (PayloadApplicationEvent::class.java.isAssignableFrom(eventType)) {
            val resolved = ResolvableType.forClass(eventType)
                .`as`(PayloadApplicationEvent::class.java)
                .getGeneric(0)
                .resolve() ?: return false
            return Event::class.java.isAssignableFrom(resolved)
        }
        return false
    }

    override fun onApplicationEvent(event: ApplicationEvent) {
        val domainEvent = when (event) {
            is PayloadApplicationEvent<*> -> event.payload as? Event ?: return
            else -> return
        }
        eventBus.dispatch(domainEvent)
    }
}
