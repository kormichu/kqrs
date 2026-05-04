package com.kormichu.kqrs.query

import kotlin.reflect.KClass

open class QueryName(open val value: String) {
    override fun toString(): String = value

    companion object {
        fun of(clazz: KClass<out Query<*>>): QueryName {
            return DefaultQueryName(clazz.java.name)
        }
    }
}

data class DefaultQueryName(override val value: String) : QueryName(value)
