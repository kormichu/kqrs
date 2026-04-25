package com.kormichu.kqrs

import com.github.f4b6a3.uuid.UuidCreator
import java.util.UUID

open class Id<V>(open val value: V) {
    companion object {
        inline fun <reified T : Id<UUID>> generateUuid(): T {
            return T::class.constructors.first().call(UUID.randomUUID())
        }

        inline fun <reified T : Id<UUID>> generateUuidV7(): T {
            return T::class.constructors.first().call(UuidCreator.getTimeOrderedEpoch())
        }

        inline fun <reified T : Id<String>> fromString(id: String): T {
            return T::class.constructors.first().call(id)
        }

        inline fun <reified T : Id<UUID>> fromUuidString(id: String): T {
            return T::class.constructors.first().call(UUID.fromString(id))
        }

        inline fun <reified T : Id<UUID>> fromUuid(id: UUID): T {
            return T::class.constructors.first().call(id)
        }
    }
}
