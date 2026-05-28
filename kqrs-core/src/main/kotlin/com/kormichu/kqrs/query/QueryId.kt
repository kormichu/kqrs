package com.kormichu.kqrs.query

import com.kormichu.kqrs.Id
import java.util.*

data class QueryId(override val value: UUID) : Id<UUID>(value) {
    override fun toString(): String {
        return value.toString()
    }
}
