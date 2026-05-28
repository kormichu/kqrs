package com.kormichu.kqrs.event

import com.kormichu.kqrs.Id
import java.util.*

data class EventId(override val value: UUID) : Id<UUID>(value) {
    override fun toString(): String {
        return value.toString()
    }
}
