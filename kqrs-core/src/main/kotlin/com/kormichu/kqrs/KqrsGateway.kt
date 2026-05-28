package com.kormichu.kqrs

import com.kormichu.kqrs.command.AsyncCommandBus
import com.kormichu.kqrs.command.Command
import com.kormichu.kqrs.command.CommandBus
import com.kormichu.kqrs.command.ErrorProcessCommandEvent
import com.kormichu.kqrs.command.StartProcessCommandEvent
import com.kormichu.kqrs.command.StopProcessCommandEvent
import com.kormichu.kqrs.command.ValidationCommandHandlerException
import com.kormichu.kqrs.command.ValidationFailedCommandEvent
import com.kormichu.kqrs.event.EventPublisher
import com.kormichu.kqrs.logger.logger
import com.kormichu.kqrs.query.AsyncQueryBus
import com.kormichu.kqrs.query.ErrorProcessQueryEvent
import com.kormichu.kqrs.query.Query
import com.kormichu.kqrs.query.QueryBus
import com.kormichu.kqrs.query.StartProcessQueryEvent
import com.kormichu.kqrs.query.StopProcessQueryEvent
import com.kormichu.kqrs.query.ValidationFailedQueryEvent
import com.kormichu.kqrs.query.ValidationQueryHandlerException
import java.time.Instant

interface KqrsGateway {
    fun <C : Command<R>, R> dispatch(command: C): R
    fun <Q : Query<R>, R> query(query: Q): R
}

interface AsyncKqrsGateway {
    suspend fun <C : Command<R>, R> dispatchAsync(command: C): R
    suspend fun <Q : Query<R>, R> queryAsync(query: Q): R
}

class DefaultKqrsGateway(
    private val commandBus: CommandBus,
    private val asyncCommandBus: AsyncCommandBus,
    private val queryBus: QueryBus,
    private val asyncQueryBus: AsyncQueryBus,
    eventPublisher: EventPublisher,
) : BaseKqrsGateway(eventPublisher), KqrsGateway, AsyncKqrsGateway {
    override fun <C : Command<R>, R> dispatch(command: C): R =
        withCommandContext(command) {
            commandBus.dispatch(command).also {
                logger.info(
                    "The command {} with ID {} has been dispatched with result {}",
                    command,
                    command.commandId,
                    it,
                )
            }
        }

    override fun <Q : Query<R>, R> query(query: Q): R =
        withQueryContext(query) {
            queryBus.dispatch(query).also {
                logger.info("The query {} with ID {} has been executed", query, query.queryId)
                logger.debug("The result of query {}", it)
            }
        }

    override suspend fun <C : Command<R>, R> dispatchAsync(command: C): R =
        withCommandContext(command) {
            asyncCommandBus.dispatchAsync(command).also {
                logger.info(
                    "The async command {} with ID {} has been dispatched with result {}",
                    command,
                    command.commandId,
                    it,
                )
            }
        }

    override suspend fun <Q : Query<R>, R> queryAsync(query: Q): R =
        withQueryContext(query) {
            asyncQueryBus.dispatchAsync(query).also {
                logger.info("The async query {} with ID {} has been executed", query, query.queryId)
                logger.debug("The result of async query {}", it)
            }
        }
}

open class BaseKqrsGateway(
    protected val eventPublisher: EventPublisher,
) {
    protected val logger by logger()

    protected inline fun <C : Command<R>, R> withCommandContext(
        command: C,
        runPostProcess: Boolean = true,
        block: () -> R
    ): R {
        logger.info("Trying to dispatch command {} with ID {}", command, command.commandId)
        val startProcessEvent = StartProcessCommandEvent.fromCommand(command)
        eventPublisher.publish(startProcessEvent)

        return runCatching { block() }
            .onFailure { handleCommandError(command, startProcessEvent.occurredOn, it) }
            .also {
                if (runPostProcess) {
                    runPostProcessingCommand(command, startProcessEvent.occurredOn)
                }
            }
            .getOrThrow()
    }

    protected inline fun <Q : Query<R>, R> withQueryContext(
        query: Q,
        runPostProcess: Boolean = true,
        block: () -> R
    ): R {
        logger.info("Trying to execute query {} with ID {}", query, query.queryId)
        val startProcessEvent = StartProcessQueryEvent.Companion.fromQuery(query)
        eventPublisher.publish(startProcessEvent)

        return runCatching { block() }
            .onFailure { handleQueryError(query, startProcessEvent.occurredOn, it) }
            .also {
                if (runPostProcess) {
                    runPostProcessingQuery(query, startProcessEvent.occurredOn)
                }
            }
            .getOrThrow()
    }

    protected fun runPostProcessingCommand(command: Command<*>, startProcessingAt: Instant) {
        eventPublisher.publish(StopProcessCommandEvent.fromCommand(command, startProcessingAt))
    }

    protected fun runPostProcessingQuery(query: Query<*>, startProcessingAt: Instant) {
        eventPublisher.publish(StopProcessQueryEvent.Companion.fromQuery(query, startProcessingAt))
    }

    protected fun handleCommandError(command: Command<*>, startProcessingAt: Instant, exception: Throwable) {
        when (exception) {
            is ValidationCommandHandlerException -> {
                logger.info(
                    "Validation error while processing command {} with ID {}",
                    command,
                    command.commandId,
                    exception,
                )
                eventPublisher.publish(ValidationFailedCommandEvent.fromCommand(command, startProcessingAt, exception))
            }

            else -> {
                logger.error("Error while processing command {} with ID {}", command, command.commandId, exception)
                eventPublisher.publish(ErrorProcessCommandEvent.fromCommand(command, startProcessingAt, exception))
            }
        }
    }

    protected fun handleQueryError(query: Query<*>, startProcessingAt: Instant, exception: Throwable) {
        when (exception) {
            is ValidationQueryHandlerException -> {
                logger.info("Validation error while processing query {} with ID {}", query, query.queryId, exception)
                eventPublisher.publish(
                    ValidationFailedQueryEvent.Companion.fromQuery(
                        query,
                        startProcessingAt,
                        exception,
                    ),
                )
            }

            else -> {
                logger.error("Error while processing query {} with {}", query, query.queryId, exception)
                eventPublisher.publish(ErrorProcessQueryEvent.Companion.fromQuery(query, startProcessingAt, exception))
            }
        }
    }
}
