package com.kormichu.kqrs

import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.full.superclasses

interface Handler<O : Any> {
    val objectClass: KClass<O>
        get() {
            val typeArgument = findTypeArgument(this::class)
            @Suppress("UNCHECKED_CAST")
            return typeArgument?.classifier as? KClass<O> ?: error("Could not determine type parameter O")
        }

    fun getBaseHandlerClass(): KClass<Handler<O>>

    private fun findTypeArgument(kClass: KClass<*>): KType? {
        val directTypeArgument = kClass.supertypes.firstOrNull { supertype ->
            supertype.classifier == getBaseHandlerClass()
        }?.arguments?.firstOrNull()?.type

        // Only return the direct type argument if its classifier is a concrete class
        // (not a TypeParameter), to avoid returning unresolved generic type parameters
        // from intermediate generic interfaces
        if (directTypeArgument != null && directTypeArgument.classifier !is KTypeParameter) {
            return directTypeArgument
        }

        return kClass.superclasses
            .filterNot { it == Any::class }
            .firstNotNullOfOrNull { findTypeArgument(it) }
    }
}
