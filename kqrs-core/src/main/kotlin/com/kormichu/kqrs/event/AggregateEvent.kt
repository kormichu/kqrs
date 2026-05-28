package com.kormichu.kqrs.event

import com.kormichu.kqrs.Id
import com.kormichu.kqrs.command.CommandId
import java.time.Instant

open class AggregateEvent(
    open val aggregateId: Id<*>,
    open val commandId: CommandId? = null,
    override val eventId: EventId = Id.Companion.generateUuidV7(),
    override val occurredOn: Instant = Instant.now(),
) : Event(eventId, occurredOn)
