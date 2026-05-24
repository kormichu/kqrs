package com.kormichu.kqrs.reactor

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.kormichu.kqrs.command.Command
import com.kormichu.kqrs.command.CommandBus
import com.kormichu.kqrs.command.CommandEvent
import com.kormichu.kqrs.command.CommandName
import com.kormichu.kqrs.command.ErrorProcessCommandEvent
import com.kormichu.kqrs.command.StartProcessCommandEvent
import com.kormichu.kqrs.command.StopProcessCommandEvent
import com.kormichu.kqrs.command.ValidationCommandHandlerException
import com.kormichu.kqrs.command.ValidationFailedCommandEvent
import com.kormichu.kqrs.event.EventPublisher
import com.kormichu.kqrs.event.EventTag
import com.kormichu.kqrs.query.ErrorProcessQueryEvent
import com.kormichu.kqrs.query.Query
import com.kormichu.kqrs.query.QueryBus
import com.kormichu.kqrs.query.QueryEvent
import com.kormichu.kqrs.query.QueryName
import com.kormichu.kqrs.query.StartProcessQueryEvent
import com.kormichu.kqrs.query.StopProcessQueryEvent
import com.kormichu.kqrs.query.ValidationFailedQueryEvent
import com.kormichu.kqrs.query.ValidationQueryHandlerException
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

class DefaultReactorKqrsGatewayTest {
    private val commandBus = mockk<CommandBus>()
    private val queryBus = mockk<QueryBus>()
    private val eventPublisher = mockk<EventPublisher>()
    private lateinit var gateway: ReactorKqrsGateway

    @BeforeEach
    fun setUp() {
        gateway = DefaultReactorKqrsGateway(
            commandBus = commandBus,
            queryBus = queryBus,
            eventPublisher = eventPublisher
        )
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `should dispatch command reactively with Mono`() {
        // given
        val command = TestReactorCommandMono("test")
        val expectedResult = TestReactorCommandResult("reactive-success")
        every { commandBus.dispatch(command) } returns Mono.just(expectedResult)

        val eventsSlot = mutableListOf<CommandEvent<*>>()
        every { eventPublisher.publish(capture(eventsSlot)) } just runs

        // when
        val resultMono = gateway.dispatch(command)

        // then
        StepVerifier.create(resultMono)
            .expectNext(expectedResult)
            .verifyComplete()

        verify(exactly = 1) { commandBus.dispatch(command) }
        verify(exactly = 1) { eventPublisher.publish(ofType(StartProcessCommandEvent::class)) }
        verify(exactly = 1) { eventPublisher.publish(ofType(StopProcessCommandEvent::class)) }
        verify(exactly = 0) { eventPublisher.publish(ofType(ErrorProcessCommandEvent::class)) }

        val startEvent = eventsSlot.first { it is StartProcessCommandEvent } as StartProcessCommandEvent
        val stopEvent = eventsSlot.first { it is StopProcessCommandEvent } as StopProcessCommandEvent

        assertThat(startEvent.commandName).isEqualTo(command.commandName)
        assertThat(startEvent.eventTags).isEqualTo(command.getEventTags())
        assertThat(stopEvent.commandName).isEqualTo(command.commandName)
        assertThat(stopEvent.eventTags).isEqualTo(command.getEventTags())
    }

    @Test
    fun `should handle error when reactive command dispatch fails`() {
        // given
        val command = TestReactorCommandMono("test")
        val exception = RuntimeException("Reactive command failed")
        every { commandBus.dispatch(command) } returns Mono.error(exception)

        val eventsSlot = mutableListOf<CommandEvent<*>>()
        every { eventPublisher.publish(capture(eventsSlot)) } just runs

        // when
        val resultMono = gateway.dispatch(command)

        // then
        StepVerifier.create(resultMono)
            .expectErrorMatches { it is RuntimeException && it.message == "Reactive command failed" }
            .verify()

        verify(exactly = 1) { eventPublisher.publish(ofType(StartProcessCommandEvent::class)) }
        verify(exactly = 1) { eventPublisher.publish(ofType(StopProcessCommandEvent::class)) }
        verify(exactly = 1) { eventPublisher.publish(ofType(ErrorProcessCommandEvent::class)) }

        val startEvent = eventsSlot.first { it is StartProcessCommandEvent } as StartProcessCommandEvent
        val stopEvent = eventsSlot.first { it is StopProcessCommandEvent } as StopProcessCommandEvent
        val failEvent = eventsSlot.first { it is ErrorProcessCommandEvent } as ErrorProcessCommandEvent

        assertThat(startEvent.commandName).isEqualTo(command.commandName)
        assertThat(startEvent.eventTags).isEqualTo(command.getEventTags())
        assertThat(stopEvent.commandName).isEqualTo(command.commandName)
        assertThat(stopEvent.eventTags).isEqualTo(command.getEventTags())
        assertThat(failEvent.commandName).isEqualTo(command.commandName)
        assertThat(failEvent.eventTags).isEqualTo(command.getEventTags())
    }

    @Test
    fun `should dispatch query reactively with Mono`() {
        // given
        val query = TestReactorQueryMono("id123")
        val expectedResult = TestReactorQueryResult("reactive-result")
        every { queryBus.dispatch(query) } returns Mono.just(expectedResult)

        val eventsSlot = mutableListOf<QueryEvent<*>>()
        every { eventPublisher.publish(capture(eventsSlot)) } just runs

        // when
        val resultMono = gateway.query(query)

        // then
        StepVerifier.create(resultMono)
            .expectNext(expectedResult)
            .verifyComplete()
        verify(exactly = 1) { queryBus.dispatch(query) }
        verify(exactly = 1) { eventPublisher.publish(ofType(StartProcessQueryEvent::class)) }
        verify(exactly = 1) { eventPublisher.publish(ofType(StopProcessQueryEvent::class)) }
        verify(exactly = 0) { eventPublisher.publish(ofType(ErrorProcessQueryEvent::class)) }

        val startEvent = eventsSlot.first { it is StartProcessQueryEvent } as StartProcessQueryEvent
        val stopEvent = eventsSlot.first { it is StopProcessQueryEvent } as StopProcessQueryEvent

        assertThat(startEvent.queryName).isEqualTo(query.queryName)
        assertThat(startEvent.eventTags).isEqualTo(query.getEventTags())
        assertThat(stopEvent.queryName).isEqualTo(query.queryName)
        assertThat(stopEvent.eventTags).isEqualTo(query.getEventTags())
    }

    @Test
    fun `should handle empty Mono from reactive query`() {
        // given
        val query = TestReactorQueryMono("id123")
        every { queryBus.dispatch(query) } returns Mono.empty()
        every { eventPublisher.publish(any<QueryEvent<*>>()) } just runs

        // when
        val resultMono = gateway.query(query)

        // then
        StepVerifier.create(resultMono)
            .verifyComplete()
    }

    // Validation Command Exception Tests
    @Test
    fun `should publish ValidationFailedCommandEvent when reactive command throws ValidationCommandHandlerException`() {
        // given
        val command = TestReactorCommandMono("test")
        val exception = TestValidationCommandException("Validation failed")
        every { commandBus.dispatch(command) } returns Mono.error(exception)

        val eventsSlot = mutableListOf<CommandEvent<*>>()
        every { eventPublisher.publish(capture(eventsSlot)) } just runs

        // when
        val resultMono = gateway.dispatch(command)

        // then
        StepVerifier.create(resultMono)
            .expectErrorMatches { it is TestValidationCommandException && it.message == "Validation failed" }
            .verify()

        verify(exactly = 1) { eventPublisher.publish(ofType(StartProcessCommandEvent::class)) }
        verify(exactly = 1) { eventPublisher.publish(ofType(StopProcessCommandEvent::class)) }
        verify(exactly = 1) { eventPublisher.publish(ofType(ValidationFailedCommandEvent::class)) }
        verify(exactly = 0) { eventPublisher.publish(ofType(ErrorProcessCommandEvent::class)) }

        val validationEvent = eventsSlot.first { it is ValidationFailedCommandEvent } as ValidationFailedCommandEvent
        assertThat(validationEvent.commandName).isEqualTo(command.commandName)
        assertThat(validationEvent.eventTags).isEqualTo(command.getEventTags())
        assertThat(validationEvent.exception).isEqualTo(exception)
    }

    // Validation Query Exception Tests
    @Test
    fun `should publish ValidationFailedQueryEvent when reactive query throws ValidationQueryHandlerException`() {
        // given
        val query = TestReactorQueryMono("id123")
        val exception = TestValidationQueryException("Query validation failed")
        every { queryBus.dispatch(query) } returns Mono.error(exception)

        val eventsSlot = mutableListOf<QueryEvent<*>>()
        every { eventPublisher.publish(capture(eventsSlot)) } just runs

        // when
        val resultMono = gateway.query(query)

        // then
        StepVerifier.create(resultMono)
            .expectErrorMatches { it is TestValidationQueryException && it.message == "Query validation failed" }
            .verify()

        verify(exactly = 1) { eventPublisher.publish(ofType(StartProcessQueryEvent::class)) }
        verify(exactly = 1) { eventPublisher.publish(ofType(StopProcessQueryEvent::class)) }
        verify(exactly = 1) { eventPublisher.publish(ofType(ValidationFailedQueryEvent::class)) }
        verify(exactly = 0) { eventPublisher.publish(ofType(ErrorProcessQueryEvent::class)) }

        val validationEvent = eventsSlot.first { it is ValidationFailedQueryEvent } as ValidationFailedQueryEvent
        assertThat(validationEvent.queryName).isEqualTo(query.queryName)
        assertThat(validationEvent.eventTags).isEqualTo(query.getEventTags())
        assertThat(validationEvent.exception).isEqualTo(exception)
    }

    @Test
    fun `should dispatch command reactively with Flux`() {
        // given
        val command = TestReactorCommandFlux("flux-test")
        val results = listOf(TestReactorCommandResult("r1"), TestReactorCommandResult("r2"))
        every { commandBus.dispatch(command) } returns Flux.fromIterable(results)

        val eventsSlot = mutableListOf<CommandEvent<*>>()
        every { eventPublisher.publish(capture(eventsSlot)) } just runs

        // when
        val resultFlux = gateway.dispatch(command)

        // then
        StepVerifier.create(resultFlux)
            .expectNext(results[0])
            .expectNext(results[1])
            .verifyComplete()

        verify(exactly = 1) { commandBus.dispatch(command) }
        verify(exactly = 1) { eventPublisher.publish(ofType(StartProcessCommandEvent::class)) }
        verify(exactly = 1) { eventPublisher.publish(ofType(StopProcessCommandEvent::class)) }
        verify(exactly = 0) { eventPublisher.publish(ofType(ErrorProcessCommandEvent::class)) }

        val startEvent = eventsSlot.first { it is StartProcessCommandEvent } as StartProcessCommandEvent
        val stopEvent = eventsSlot.first { it is StopProcessCommandEvent } as StopProcessCommandEvent

        assertThat(startEvent.commandName).isEqualTo(command.commandName)
        assertThat(startEvent.eventTags).isEqualTo(command.getEventTags())
        assertThat(stopEvent.commandName).isEqualTo(command.commandName)
        assertThat(stopEvent.eventTags).isEqualTo(command.getEventTags())
    }

    @Test
    fun `should handle error when Flux command dispatch fails`() {
        // given
        val command = TestReactorCommandFlux("flux-test")
        val exception = RuntimeException("Flux command failed")
        every { commandBus.dispatch(command) } returns Flux.error(exception)

        val eventsSlot = mutableListOf<CommandEvent<*>>()
        every { eventPublisher.publish(capture(eventsSlot)) } just runs

        // when
        val resultFlux = gateway.dispatch(command)

        // then
        StepVerifier.create(resultFlux)
            .expectErrorMatches { it is RuntimeException && it.message == "Flux command failed" }
            .verify()

        verify(exactly = 1) { eventPublisher.publish(ofType(StartProcessCommandEvent::class)) }
        verify(exactly = 1) { eventPublisher.publish(ofType(StopProcessCommandEvent::class)) }
        verify(exactly = 1) { eventPublisher.publish(ofType(ErrorProcessCommandEvent::class)) }

        val failEvent = eventsSlot.first { it is ErrorProcessCommandEvent } as ErrorProcessCommandEvent
        assertThat(failEvent.commandName).isEqualTo(command.commandName)
        assertThat(failEvent.eventTags).isEqualTo(command.getEventTags())
    }

    @Test
    fun `should handle empty Flux from command dispatch`() {
        // given
        val command = TestReactorCommandFlux("flux-test")
        every { commandBus.dispatch(command) } returns Flux.empty()
        every { eventPublisher.publish(any<CommandEvent<*>>()) } just runs

        // when
        val resultFlux = gateway.dispatch(command)

        // then
        StepVerifier.create(resultFlux)
            .verifyComplete()
    }

    @Test
    fun `should publish ValidationFailedCommandEvent when Flux command throws ValidationCommandHandlerException`() {
        // given
        val command = TestReactorCommandFlux("flux-test")
        val exception = TestValidationCommandException("Flux validation failed")
        every { commandBus.dispatch(command) } returns Flux.error(exception)

        val eventsSlot = mutableListOf<CommandEvent<*>>()
        every { eventPublisher.publish(capture(eventsSlot)) } just runs

        // when
        val resultFlux = gateway.dispatch(command)

        // then
        StepVerifier.create(resultFlux)
            .expectErrorMatches { it is TestValidationCommandException && it.message == "Flux validation failed" }
            .verify()

        verify(exactly = 1) { eventPublisher.publish(ofType(ValidationFailedCommandEvent::class)) }
        verify(exactly = 0) { eventPublisher.publish(ofType(ErrorProcessCommandEvent::class)) }

        val validationEvent = eventsSlot.first { it is ValidationFailedCommandEvent } as ValidationFailedCommandEvent
        assertThat(validationEvent.commandName).isEqualTo(command.commandName)
        assertThat(validationEvent.exception).isEqualTo(exception)
    }

    // Flux Query Tests
    @Test
    fun `should dispatch query reactively with Flux`() {
        // given
        val query = TestReactorQueryFlux("id123")
        val results = listOf(TestReactorQueryResult("r1"), TestReactorQueryResult("r2"), TestReactorQueryResult("r3"))
        every { queryBus.dispatch(query) } returns Flux.fromIterable(results)

        val eventsSlot = mutableListOf<QueryEvent<*>>()
        every { eventPublisher.publish(capture(eventsSlot)) } just runs

        // when
        val resultFlux = gateway.query(query)

        // then
        StepVerifier.create(resultFlux)
            .expectNext(results[0])
            .expectNext(results[1])
            .expectNext(results[2])
            .verifyComplete()

        verify(exactly = 1) { queryBus.dispatch(query) }
        verify(exactly = 1) { eventPublisher.publish(ofType(StartProcessQueryEvent::class)) }
        verify(exactly = 1) { eventPublisher.publish(ofType(StopProcessQueryEvent::class)) }
        verify(exactly = 0) { eventPublisher.publish(ofType(ErrorProcessQueryEvent::class)) }

        val startEvent = eventsSlot.first { it is StartProcessQueryEvent } as StartProcessQueryEvent
        val stopEvent = eventsSlot.first { it is StopProcessQueryEvent } as StopProcessQueryEvent

        assertThat(startEvent.queryName).isEqualTo(query.queryName)
        assertThat(startEvent.eventTags).isEqualTo(query.getEventTags())
        assertThat(stopEvent.queryName).isEqualTo(query.queryName)
        assertThat(stopEvent.eventTags).isEqualTo(query.getEventTags())
    }

    @Test
    fun `should handle error when Flux query dispatch fails`() {
        // given
        val query = TestReactorQueryFlux("id123")
        val exception = RuntimeException("Flux query failed")
        every { queryBus.dispatch(query) } returns Flux.error(exception)

        val eventsSlot = mutableListOf<QueryEvent<*>>()
        every { eventPublisher.publish(capture(eventsSlot)) } just runs

        // when
        val resultFlux = gateway.query(query)

        // then
        StepVerifier.create(resultFlux)
            .expectErrorMatches { it is RuntimeException && it.message == "Flux query failed" }
            .verify()

        verify(exactly = 1) { eventPublisher.publish(ofType(StartProcessQueryEvent::class)) }
        verify(exactly = 1) { eventPublisher.publish(ofType(StopProcessQueryEvent::class)) }
        verify(exactly = 1) { eventPublisher.publish(ofType(ErrorProcessQueryEvent::class)) }

        val failEvent = eventsSlot.first { it is ErrorProcessQueryEvent } as ErrorProcessQueryEvent
        assertThat(failEvent.queryName).isEqualTo(query.queryName)
        assertThat(failEvent.eventTags).isEqualTo(query.getEventTags())
    }

    @Test
    fun `should handle empty Flux from query`() {
        // given
        val query = TestReactorQueryFlux("id123")
        every { queryBus.dispatch(query) } returns Flux.empty()
        every { eventPublisher.publish(any<QueryEvent<*>>()) } just runs

        // when
        val resultFlux = gateway.query(query)

        // then
        StepVerifier.create(resultFlux)
            .verifyComplete()
    }

    @Test
    fun `should publish ValidationFailedQueryEvent when Flux query throws ValidationQueryHandlerException`() {
        // given
        val query = TestReactorQueryFlux("id123")
        val exception = TestValidationQueryException("Flux query validation failed")
        every { queryBus.dispatch(query) } returns Flux.error(exception)

        val eventsSlot = mutableListOf<QueryEvent<*>>()
        every { eventPublisher.publish(capture(eventsSlot)) } just runs

        // when
        val resultFlux = gateway.query(query)

        // then
        StepVerifier.create(resultFlux)
            .expectErrorMatches { it is TestValidationQueryException && it.message == "Flux query validation failed" }
            .verify()

        verify(exactly = 1) { eventPublisher.publish(ofType(ValidationFailedQueryEvent::class)) }
        verify(exactly = 0) { eventPublisher.publish(ofType(ErrorProcessQueryEvent::class)) }

        val validationEvent = eventsSlot.first { it is ValidationFailedQueryEvent } as ValidationFailedQueryEvent
        assertThat(validationEvent.queryName).isEqualTo(query.queryName)
        assertThat(validationEvent.exception).isEqualTo(exception)
    }

    // Test exception classes
    private class TestValidationCommandException(message: String) : ValidationCommandHandlerException(message)
    private class TestValidationQueryException(message: String) : ValidationQueryHandlerException(message)

    private class TestReactorCommandName: CommandName("test-reactor-command") {
        companion object {
            val INSTANCE = TestReactorCommandName()
        }
    }
    private data class TestReactorCommandResult(val result: String)
    private class TestReactorQueryName: QueryName("test-reactor-query") {
        companion object {
            val INSTANCE = TestReactorQueryName()
        }
    }
    private data class TestReactorQueryResult(val data: String)
    private class TestReactorCommandFluxName: CommandName("test-reactor-command-flux") {
        companion object {
            val INSTANCE = TestReactorCommandFluxName()
        }
    }
    private class TestReactorQueryFluxName: QueryName("test-reactor-query-flux") {
        companion object {
            val INSTANCE = TestReactorQueryFluxName()
        }
    }
    private data class TestReactorCommandFlux(val data: String): Command<Flux<TestReactorCommandResult>>(
        commandName = TestReactorCommandFluxName.INSTANCE
    ) {
        override fun getEventTags(): List<EventTag> = listOf(
            EventTag("test-tag", "test-value"),
            EventTag("command-data", data)
        )
    }
    private data class TestReactorQueryFlux(val id: String): Query<Flux<TestReactorQueryResult>>(
        queryName = TestReactorQueryFluxName.INSTANCE
    ) {
        override fun getEventTags(): List<EventTag> = listOf(
            EventTag("test-tag", "test-value"),
            EventTag("query-id", id)
        )
    }
    private data class TestReactorCommandMono(val data: String): Command<Mono<TestReactorCommandResult>>(
        commandName = TestReactorCommandName.INSTANCE
    ) {
        override fun getEventTags(): List<EventTag> {
            return listOf(
                EventTag("test-tag", "test-value"),
                EventTag("command-data", data)
            )
        }
    }
    private data class TestReactorQueryMono(val id: String): Query<Mono<TestReactorQueryResult>>(
        queryName = TestReactorQueryName.INSTANCE
    ) {
        override fun getEventTags(): List<EventTag> {
            return listOf(
                EventTag("test-tag", "test-value"),
                EventTag("query-id", id)
            )
        }
    }
}
