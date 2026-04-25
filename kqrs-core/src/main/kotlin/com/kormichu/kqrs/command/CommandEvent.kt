package com.kormichu.kqrs.command

import com.kormichu.kqrs.event.Event
import com.kormichu.kqrs.event.EventTag
import java.time.Duration
import java.time.Instant

sealed class CommandEvent<C: CommandName> (
    open val commandName: C,
    override val eventTags: List<EventTag>
): Event()

data class StartProcessCommandEvent<C: CommandName> (
    override val commandName: C,
    override val eventTags: List<EventTag>
): CommandEvent<C>(commandName, eventTags) {
    companion object {
        fun fromCommand(
            command: Command<*>
        ): StartProcessCommandEvent<*> {
            return StartProcessCommandEvent(
                commandName = command.commandName,
                eventTags = command.getEventTags()
            )
        }
    }
}

data class StopProcessCommandEvent<C: CommandName> (
    override val commandName: C,
    override val eventTags: List<EventTag>,
    val startProcessingAt: Instant,
): CommandEvent<C>(commandName, eventTags) {
    val duration: Duration = Duration.between(startProcessingAt, occurredOn)

    companion object {
        fun fromCommand(
            command: Command<*>,
            startProcessingAt: Instant
        ): StopProcessCommandEvent<*> {
            return StopProcessCommandEvent(
                commandName = command.commandName,
                startProcessingAt = startProcessingAt,
                eventTags = command.getEventTags()
            )
        }
    }
}

data class ErrorProcessCommandEvent<C: CommandName> (
    override val commandName: C,
    override val eventTags: List<EventTag>,
    val startProcessingAt: Instant,
    val exception: Throwable
): CommandEvent<C>(commandName, eventTags) {
    val duration: Duration = Duration.between(startProcessingAt, occurredOn)

    companion object {
        fun fromCommand(
            command: Command<*>,
            startProcessingAt: Instant,
            exception: Throwable
        ): ErrorProcessCommandEvent<*> {
            return ErrorProcessCommandEvent(
                commandName = command.commandName,
                startProcessingAt = startProcessingAt,
                eventTags = command.getEventTags(),
                exception = exception
            )
        }
    }
}

data class ValidationFailedCommandEvent<C: CommandName>(
    override val commandName: C,
    override val eventTags: List<EventTag>,
    val startProcessingAt: Instant,
    val exception: ValidationCommandHandlerException
): CommandEvent<C>(commandName, eventTags) {
    val duration: Duration = Duration.between(startProcessingAt, occurredOn)

    companion object {
        fun fromCommand(
            command: Command<*>,
            startProcessingAt: Instant,
            exception: ValidationCommandHandlerException
        ): ValidationFailedCommandEvent<*> {
            return ValidationFailedCommandEvent(
                commandName = command.commandName,
                startProcessingAt = startProcessingAt,
                exception = exception,
                eventTags = command.getEventTags()
            )
        }
    }
}
