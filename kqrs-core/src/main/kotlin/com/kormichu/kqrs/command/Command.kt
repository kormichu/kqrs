package com.kormichu.kqrs.command

import com.kormichu.kqrs.Id
import com.kormichu.kqrs.event.EventTag
import com.kormichu.kqrs.helper.ClassParameterizedHelper.getClassParameter

open class Command<R>(
    commandName: CommandName? = null,
    open val commandId: CommandId = Id.generateUuidV7(),
) {
    open val commandName: CommandName = commandName ?: CommandName.of(this::class)
    val resultClass: Class<R> get() = getClassParameter(this)

    open fun getEventTags(): List<EventTag> = emptyList()
}
