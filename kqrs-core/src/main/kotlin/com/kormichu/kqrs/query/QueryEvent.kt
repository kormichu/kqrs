package com.kormichu.kqrs.query

import com.kormichu.kqrs.event.Event
import com.kormichu.kqrs.event.EventTag
import java.time.Duration
import java.time.Instant

sealed class QueryEvent<Q: QueryName> (
    open val queryName: Q,
    override val eventTags: List<EventTag>
): Event()

data class StartProcessQueryEvent<Q: QueryName>(
    override val queryName: Q,
    override val eventTags: List<EventTag>
): QueryEvent<Q>(queryName, eventTags) {
    companion object {
        fun fromQuery(
            query: Query<*>
        ): StartProcessQueryEvent<*> {
            return StartProcessQueryEvent(
                queryName = query.queryName,
                eventTags = query.getEventTags()
            )
        }
    }
}

data class StopProcessQueryEvent<Q: QueryName>(
    override val queryName: Q,
    override val eventTags: List<EventTag>,
    val startProcessingAt: Instant
): QueryEvent<Q>(queryName, eventTags) {
    val duration: Duration = Duration.between(startProcessingAt, occurredOn)

    companion object {
        fun fromQuery(
            query: Query<*>,
            startProcessingAt: Instant
        ): StopProcessQueryEvent<*> {
            return StopProcessQueryEvent(
                queryName = query.queryName,
                startProcessingAt = startProcessingAt,
                eventTags = query.getEventTags()
            )
        }
    }
}

data class ErrorProcessQueryEvent<Q: QueryName>(
    override val queryName: Q,
    override val eventTags: List<EventTag>,
    val startProcessingAt: Instant,
    val exception: Throwable
): QueryEvent<Q>(queryName, eventTags) {
    val duration: Duration = Duration.between(startProcessingAt, occurredOn)

    companion object {
        fun fromQuery(
            query: Query<*>,
            startProcessingAt: Instant,
            exception: Throwable
        ): ErrorProcessQueryEvent<*> {
            return ErrorProcessQueryEvent(
                queryName = query.queryName,
                startProcessingAt = startProcessingAt,
                exception = exception,
                eventTags = query.getEventTags()
            )
        }
    }
}

data class ValidationFailedQueryEvent<Q: QueryName>(
    override val queryName: Q,
    override val eventTags: List<EventTag>,
    val startProcessingAt: Instant,
    val exception: ValidationQueryHandlerException
): QueryEvent<Q>(queryName, eventTags) {
    val duration: Duration = Duration.between(startProcessingAt, occurredOn)

    companion object {
        fun fromQuery(
            query: Query<*>,
            startProcessingAt: Instant,
            exception: ValidationQueryHandlerException
        ): ValidationFailedQueryEvent<*> {
            return ValidationFailedQueryEvent(
                queryName = query.queryName,
                startProcessingAt = startProcessingAt,
                exception = exception,
                eventTags = query.getEventTags()
            )
        }
    }
}
