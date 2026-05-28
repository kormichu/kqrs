package com.kormichu.kqrs.event

import com.kormichu.kqrs.Id
import java.time.Instant

open class Event(
    open val eventId: EventId = Id.Companion.generateUuidV7(),
    open val occurredOn: Instant = Instant.now(),
    open val eventTags: List<EventTag> = emptyList()
)
