package com.kormichu.kqrs.command

import kotlin.reflect.KClass

open class CommandName(open val value: String) {
    override fun toString(): String = value

    companion object {
        fun of(clazz: KClass<out Command<*>>): CommandName {
            return DefaultCommandName(clazz.java.name)
        }
    }
}

data class DefaultCommandName(override val value: String) : CommandName(value)
