package com.kormichu.kqrs.spring.event

import com.kormichu.kqrs.event.Event
import com.kormichu.kqrs.event.EventPublisher
import org.springframework.context.ApplicationEventPublisher

class SpringEventPublisher(
    private val eventPublisher: ApplicationEventPublisher,
) : EventPublisher {
    override fun publish(events: List<Event>) {
        events.forEach { eventPublisher.publishEvent(it) }
    }
}
