package com.kormichu.kqrs.repository

import com.kormichu.kqrs.AggregateRoot
import com.kormichu.kqrs.Id
import com.kormichu.kqrs.event.EventPublisher

interface AsyncRepository<T : AggregateRoot<ID>, ID : Id<*>> {
    suspend fun findById(id: ID): T?
    suspend fun existsById(id: ID): Boolean = findById(id) != null
    suspend fun save(aggregate: T): ID
    suspend fun saveAndPublishEvents(aggregate: T, eventPublisher: EventPublisher): ID {
        val id = save(aggregate)
        publishEvents(aggregate, eventPublisher)
        return id
    }

    suspend fun publishEvents(aggregate: T, eventPublisher: EventPublisher) {
        val events = aggregate.pullEvents()
        if (events.isNotEmpty()) {
            eventPublisher.publish(events)
        }
    }
}
