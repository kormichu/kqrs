package com.kormichu.kqrs.event

interface EventPublisher {
    fun publish(events: List<Event>)
    fun publish(event: Event) = publish(listOf(event))
}

