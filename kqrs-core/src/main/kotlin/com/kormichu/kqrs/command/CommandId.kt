package com.kormichu.kqrs.command

import com.kormichu.kqrs.Id
import java.util.*

data class CommandId(override val value: UUID) : Id<UUID>(value) {
    override fun toString(): String {
        return value.toString()
    }
}
