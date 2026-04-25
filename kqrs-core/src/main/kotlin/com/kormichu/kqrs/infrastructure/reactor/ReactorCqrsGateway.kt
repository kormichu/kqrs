package com.kormichu.kqrs.infrastructure.reactor

import org.springframework.stereotype.Component
import com.kormichu.kqrs.BaseKqrsGateway
import com.kormichu.kqrs.command.Command
import com.kormichu.kqrs.command.CommandBus
import com.kormichu.kqrs.event.EventPublisher
import com.kormichu.kqrs.query.Query
import com.kormichu.kqrs.query.QueryBus
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant

@Component
class ReactorCqrsGateway (
    private val commandBus: CommandBus,
    private val queryBus: QueryBus,
    eventPublisher: EventPublisher
): BaseKqrsGateway(eventPublisher) {
    fun <C: Command<Mono<R>>, R: Any> dispatch(command: C): Mono<R> =
        withCommandContext(command, false) {
            val startProcessingAt = Instant.now()
            commandBus.execute(command)
                .doOnSuccess {
                    logger.info(
                        "The mono command {} with ID {} has been dispatched with result {}",
                        command,
                        command.commandId,
                        it
                    )
                }
                .doOnError {
                    handleCommandError(command, startProcessingAt, it)
                }
                .doFinally {
                    runPostProcessingCommand(command, startProcessingAt)
                }
        }

    fun <C: Command<Flux<R>>, R: Any> dispatch(command: C): Flux<R> =
        withCommandContext(command, false) {
            val startProcessingAt = Instant.now()
            commandBus.execute(command)
                .doOnComplete {
                    logger.info(
                        "The flux command {} with ID {} has been dispatched",
                        command,
                        command.commandId
                    )
                }
                .doOnError {
                    handleCommandError(command, startProcessingAt, it)
                }
                .doFinally {
                    runPostProcessingCommand(command, startProcessingAt)
                }
        }

    fun <Q: Query<Mono<R>>, R: Any> query(query: Q): Mono<R> =
        withQueryContext(query, false) {
            val startProcessingAt = Instant.now()
            queryBus.dispatch(query)
                .doOnSuccess {
                    logger.info("The mono query {} with ID {} has been executed", query, query.queryId)
                    logger.debug("The result of mono query {}", it)
                }
                .doOnError {
                    handleQueryError(query, startProcessingAt, it)
                }
                .doFinally {
                    runPostProcessingQuery(query, startProcessingAt)
                }
        }

    fun <Q: Query<Flux<R>>, R: Any> query(query: Q): Flux<R> =
        withQueryContext(query, false) {
            val startProcessingAt = Instant.now()
            queryBus.dispatch(query)
                .doOnComplete {
                    logger.info("The flux query {} with ID {} has been executed", query, query.queryId)
                }
                .doOnError {
                    handleQueryError(query, startProcessingAt, it)
                }
                .doFinally {
                    runPostProcessingQuery(query, startProcessingAt)
                }
        }
}
