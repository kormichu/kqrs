package com.kormichu.kqrs

import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.superclasses

interface Handler<O: Any> {
    val objectClass: KClass<O>
        get() {
            val typeArgument = findTypeArgument(this::class)
            @Suppress("UNCHECKED_CAST")
            return typeArgument?.classifier as? KClass<O> ?:
                error("Could not determine type parameter O")
        }

    fun getBaseHandlerClass(): KClass<Handler<O>>

    private fun findTypeArgument(kClass: KClass<*>): KType? {
        val directTypeArgument = kClass.supertypes.firstOrNull { supertype ->
            supertype.classifier == getBaseHandlerClass()
        }?.arguments?.firstOrNull()?.type

        if (directTypeArgument != null) {
            return directTypeArgument
        }

        return kClass.superclasses
            .filterNot { it == Any::class }
            .firstNotNullOfOrNull { findTypeArgument(it) }
    }
}
