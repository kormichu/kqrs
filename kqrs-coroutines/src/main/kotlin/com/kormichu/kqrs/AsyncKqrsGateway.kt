package com.kormichu.kqrs

import com.kormichu.kqrs.command.AsyncCommandBus
import com.kormichu.kqrs.query.AsyncQueryBus
import com.kormichu.kqrs.command.Command
import com.kormichu.kqrs.event.EventPublisher
import com.kormichu.kqrs.query.Query

interface AsyncKqrsGateway {
    suspend fun <C: Command<R>, R> dispatchAsync(command: C): R
    suspend fun <Q: Query<R>, R> queryAsync(query: Q): R
}

class DefaultKqrsGateway (
    private val asyncCommandBus: AsyncCommandBus,
    private val asyncQueryBus: AsyncQueryBus,
    eventPublisher: EventPublisher,
): BaseKqrsGateway(eventPublisher), AsyncKqrsGateway {
    override suspend fun <C: Command<R>, R> dispatchAsync(command: C): R =
        withCommandContext(command) {
            asyncCommandBus.executeAsync(command).also {
                logger.info(
                    "The async command {} with ID {} has been dispatched with result {}",
                    command,
                    command.commandId,
                    it
                )
            }
        }

    override suspend fun <Q: Query<R>, R> queryAsync(query: Q): R =
        withQueryContext(query) {
            asyncQueryBus.dispatchAsync(query).also {
                logger.info("The async query {} with ID {} has been executed", query, query.queryId)
                logger.debug("The result of async query {}", it)
            }
        }
}
