package com.kormichu.kqrs

import com.kormichu.kqrs.event.Event

abstract class AggregateRoot<ID : Id<*>> {
    abstract val id: ID
    private val events: MutableList<Event> = mutableListOf()

    fun record(event: Event) {
        events.add(event)
    }

    fun pullEvents(): List<Event> {
        val currentEvents = events.toList()
        events.clear()
        return currentEvents
    }
}
