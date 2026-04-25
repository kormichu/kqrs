package com.kormichu.kqrs.repository

import com.kormichu.kqrs.AggregateRoot
import com.kormichu.kqrs.Id
import com.kormichu.kqrs.event.EventPublisher

interface Repository<T : AggregateRoot<ID>, ID : Id<*>> {
    fun findById(id: ID): T?
    fun existsById(id: ID): Boolean = findById(id) != null
    fun save(aggregate: T): ID
    fun saveAndPublishEvents(aggregate: T, eventPublisher: EventPublisher): ID {
        val id = save(aggregate)
        publishEvents(aggregate, eventPublisher)
        return id
    }
    fun publishEvents(aggregate: T, eventPublisher: EventPublisher) {
        val events = aggregate.pullEvents()
        if (events.isNotEmpty()) {
            eventPublisher.publish(events)
        }
    }
}
