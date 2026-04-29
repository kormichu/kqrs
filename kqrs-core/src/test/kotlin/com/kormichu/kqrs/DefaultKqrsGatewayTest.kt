package com.kormichu.kqrs

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.kormichu.kqrs.command.AsyncCommandBus
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
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
import com.kormichu.kqrs.query.AsyncQueryBus
import com.kormichu.kqrs.query.ErrorProcessQueryEvent
import com.kormichu.kqrs.query.Query
import com.kormichu.kqrs.query.QueryBus
import com.kormichu.kqrs.query.QueryEvent
import com.kormichu.kqrs.query.QueryName
import com.kormichu.kqrs.query.StartProcessQueryEvent
import com.kormichu.kqrs.query.StopProcessQueryEvent
import com.kormichu.kqrs.query.ValidationFailedQueryEvent
import com.kormichu.kqrs.query.ValidationQueryHandlerException

class DefaultKqrsGatewayTest {
    private val commandBus = mockk<CommandBus>()
    private val asyncCommandBus = mockk<AsyncCommandBus>()
    private val queryBus = mockk<QueryBus>()
    private val asyncQueryBus = mockk<AsyncQueryBus>()
    private val eventPublisher = mockk<EventPublisher>()
    private lateinit var gateway: DefaultKqrsGateway

    @BeforeEach
    fun setUp() {
        gateway = DefaultKqrsGateway(
            commandBus = commandBus,
            asyncCommandBus = asyncCommandBus,
            queryBus = queryBus,
            asyncQueryBus = asyncQueryBus,
            eventPublisher = eventPublisher
        )
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    // Command Tests
    @Test
    fun `should dispatch command successfully`() {
        // given
        val command = TestCommand("test")
        val expectedResult = TestCommandResult("success")
        every { commandBus.execute(command) } returns expectedResult

        val eventsSlot = mutableListOf<CommandEvent<TestCommandName>>()
        every { eventPublisher.publish(capture(eventsSlot)) } just runs

        // when
        val result = gateway.dispatch(command)

        // then
        assertThat(result).isEqualTo(expectedResult)
        verify(exactly = 1) { commandBus.execute(command) }
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
    fun `should propagate exception when command dispatch fails`() {
        // given
        val command = TestCommand("test")
        val exception = RuntimeException("Command failed")
        every { commandBus.execute(command) } throws exception

        val eventsSlot = mutableListOf<CommandEvent<*>>()
        every { eventPublisher.publish(capture(eventsSlot)) } just runs

        // when & then
        val thrown = assertThrows<RuntimeException> {
            gateway.dispatch(command)
        }
        assertThat(thrown.message, "Command failed")

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

    // Query Tests
    @Test
    fun `should execute query successfully`() {
        // given
        val query = TestQuery("id123")
        val expectedResult = TestQueryResult("result")
        every { queryBus.dispatch(query) } returns expectedResult

        val eventsSlot = mutableListOf<QueryEvent<TestQueryName>>()
        every { eventPublisher.publish(capture(eventsSlot)) } just runs

        // when
        val result = gateway.query(query)

        // then
        assertThat(result).isEqualTo(expectedResult)
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
    fun `should propagate exception when query fails`() {
        // given
        val query = TestQuery("id123")
        val exception = IllegalStateException("Query failed")
        every { queryBus.dispatch(query) } throws exception

        val eventsSlot = mutableListOf<QueryEvent<*>>()
        every { eventPublisher.publish(capture(eventsSlot)) } just runs

        // when & then
        val thrown = assertThrows<IllegalStateException> {
            gateway.query(query)
        }
        assertThat(thrown.message).isEqualTo("Query failed")

        verify(exactly = 1) { eventPublisher.publish(ofType(StartProcessQueryEvent::class)) }
        verify(exactly = 1) { eventPublisher.publish(ofType(StopProcessQueryEvent::class)) }
        verify(exactly = 1) { eventPublisher.publish(ofType(ErrorProcessQueryEvent::class)) }

        val startEvent = eventsSlot.first { it is StartProcessQueryEvent } as StartProcessQueryEvent
        val stopEvent = eventsSlot.first { it is StopProcessQueryEvent } as StopProcessQueryEvent
        val failEvent = eventsSlot.first { it is ErrorProcessQueryEvent } as ErrorProcessQueryEvent

        assertThat(startEvent.queryName).isEqualTo(query.queryName)
        assertThat(startEvent.eventTags).isEqualTo(query.getEventTags())
        assertThat(stopEvent.queryName).isEqualTo(query.queryName)
        assertThat(stopEvent.eventTags).isEqualTo(query.getEventTags())
        assertThat(failEvent.queryName).isEqualTo(query.queryName)
        assertThat(failEvent.eventTags).isEqualTo(query.getEventTags())
    }

    // Async Command Tests (Coroutines)
    @Test
    fun `should dispatch command asynchronously with coroutines`() = runTest {
        // given
        val command = TestCommand("test")
        val expectedResult = TestCommandResult("async-success")
        coEvery { asyncCommandBus.executeAsync(command) } returns expectedResult

        val eventsSlot = mutableListOf<CommandEvent<*>>()
        coEvery { eventPublisher.publish(capture(eventsSlot)) } just runs

        // when
        val result = gateway.dispatch(command)

        // then
        assertThat(result).isEqualTo(expectedResult)
        coVerify (exactly = 1) { asyncCommandBus.executeAsync(command) }
        coVerify(exactly = 1) { eventPublisher.publish(ofType(StartProcessCommandEvent::class)) }
        coVerify(exactly = 1) { eventPublisher.publish(ofType(StopProcessCommandEvent::class)) }
        coVerify(exactly = 0) { eventPublisher.publish(ofType(ErrorProcessCommandEvent::class)) }

        val startEvent = eventsSlot.first { it is StartProcessCommandEvent } as StartProcessCommandEvent
        val stopEvent = eventsSlot.first { it is StopProcessCommandEvent } as StopProcessCommandEvent

        assertThat(startEvent.commandName).isEqualTo(command.commandName)
        assertThat(startEvent.eventTags).isEqualTo(command.getEventTags())
        assertThat(stopEvent.commandName).isEqualTo(command.commandName)
        assertThat(stopEvent.eventTags).isEqualTo(command.getEventTags())
    }

    @Test
    fun `should propagate exception when async command dispatch fails`() = runTest {
        // given
        val command = TestCommand("test")
        val exception = RuntimeException("Async command failed")
        coEvery { asyncCommandBus.executeAsync(command) } throws exception

        val eventsSlot = mutableListOf<CommandEvent<*>>()
        coEvery { eventPublisher.publish(capture(eventsSlot)) } just runs

        // when & then
        val thrown = assertThrows<RuntimeException> {
            gateway.dispatchAsync(command)
        }
        assertThat(thrown.message).isEqualTo("Async command failed")

        coVerify(exactly = 1) { eventPublisher.publish(ofType(StartProcessCommandEvent::class)) }
        coVerify(exactly = 1) { eventPublisher.publish(ofType(StopProcessCommandEvent::class)) }
        coVerify(exactly = 1) { eventPublisher.publish(ofType(ErrorProcessCommandEvent::class)) }

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

    // Async Query Tests (Coroutines)
    @Test
    fun `should execute query asynchronously with coroutines`() = runTest {
        // given
        val query = TestQuery("id123")
        val expectedResult = TestQueryResult("async-result")
        coEvery { asyncQueryBus.dispatchAsync(query) } returns expectedResult

        val eventsSlot = mutableListOf<QueryEvent<*>>()
        coEvery { eventPublisher.publish(capture(eventsSlot)) } just runs

        // when
        val result = gateway.queryAsync(query)

        // then
        assertThat(result).isEqualTo(expectedResult)
        coVerify(exactly = 1) { asyncQueryBus.dispatchAsync(query) }
        coVerify(exactly = 1) { eventPublisher.publish(ofType(StartProcessQueryEvent::class)) }
        coVerify(exactly = 1) { eventPublisher.publish(ofType(StopProcessQueryEvent::class)) }
        coVerify(exactly = 0) { eventPublisher.publish(ofType(ErrorProcessQueryEvent::class)) }

        val startEvent = eventsSlot.first { it is StartProcessQueryEvent } as StartProcessQueryEvent
        val stopEvent = eventsSlot.first { it is StopProcessQueryEvent } as StopProcessQueryEvent

        assertThat(startEvent.queryName).isEqualTo(query.queryName)
        assertThat(startEvent.eventTags).isEqualTo(query.getEventTags())
        assertThat(stopEvent.queryName).isEqualTo(query.queryName)
        assertThat(stopEvent.eventTags).isEqualTo(query.getEventTags())
    }

    // Validation Command Exception Tests
    @Test
    fun `should publish ValidationFailedCommandEvent when command throws ValidationCommandHandlerException`() {
        // given
        val command = TestCommand("test")
        val exception = TestValidationCommandException("Validation failed")
        every { commandBus.execute(command) } throws exception

        val eventsSlot = mutableListOf<CommandEvent<*>>()
        every { eventPublisher.publish(capture(eventsSlot)) } just runs

        // when & then
        val thrown = assertThrows<TestValidationCommandException> {
            gateway.dispatch(command)
        }
        assertThat(thrown.message).isEqualTo("Validation failed")

        verify(exactly = 1) { eventPublisher.publish(ofType(StartProcessCommandEvent::class)) }
        verify(exactly = 1) { eventPublisher.publish(ofType(StopProcessCommandEvent::class)) }
        verify(exactly = 1) { eventPublisher.publish(ofType(ValidationFailedCommandEvent::class)) }
        verify(exactly = 0) { eventPublisher.publish(ofType(ErrorProcessCommandEvent::class)) }

        val validationEvent = eventsSlot.first { it is ValidationFailedCommandEvent } as ValidationFailedCommandEvent
        assertThat(validationEvent.commandName).isEqualTo(command.commandName)
        assertThat(validationEvent.eventTags).isEqualTo(command.getEventTags())
        assertThat(validationEvent.exception).isEqualTo(exception)
    }

    @Test
    fun `should publish ValidationFailedCommandEvent when async command throws ValidationCommandHandlerException`() = runTest {
        // given
        val command = TestCommand("test")
        val exception = TestValidationCommandException("Async validation failed")
        coEvery { asyncCommandBus.executeAsync(command) } throws exception

        val eventsSlot = mutableListOf<CommandEvent<*>>()
        coEvery { eventPublisher.publish(capture(eventsSlot)) } just runs

        // when & then
        val thrown = assertThrows<TestValidationCommandException> {
            gateway.dispatchAsync(command)
        }
        assertThat(thrown.message).isEqualTo("Async validation failed")

        coVerify(exactly = 1) { eventPublisher.publish(ofType(StartProcessCommandEvent::class)) }
        coVerify(exactly = 1) { eventPublisher.publish(ofType(StopProcessCommandEvent::class)) }
        coVerify(exactly = 1) { eventPublisher.publish(ofType(ValidationFailedCommandEvent::class)) }
        coVerify(exactly = 0) { eventPublisher.publish(ofType(ErrorProcessCommandEvent::class)) }

        val validationEvent = eventsSlot.first { it is ValidationFailedCommandEvent } as ValidationFailedCommandEvent
        assertThat(validationEvent.commandName).isEqualTo(command.commandName)
        assertThat(validationEvent.eventTags).isEqualTo(command.getEventTags())
        assertThat(validationEvent.exception).isEqualTo(exception)
    }

    // Validation Query Exception Tests
    @Test
    fun `should publish ValidationFailedQueryEvent when query throws ValidationQueryHandlerException`() {
        // given
        val query = TestQuery("id123")
        val exception = TestValidationQueryException("Query validation failed")
        every { queryBus.dispatch(query) } throws exception

        val eventsSlot = mutableListOf<QueryEvent<*>>()
        every { eventPublisher.publish(capture(eventsSlot)) } just runs

        // when & then
        val thrown = assertThrows<TestValidationQueryException> {
            gateway.query(query)
        }
        assertThat(thrown.message).isEqualTo("Query validation failed")

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
    fun `should publish ValidationFailedQueryEvent when async query throws ValidationQueryHandlerException`() = runTest {
        // given
        val query = TestQuery("id123")
        val exception = TestValidationQueryException("Async query validation failed")
        coEvery { asyncQueryBus.dispatchAsync(query) } throws exception

        val eventsSlot = mutableListOf<QueryEvent<*>>()
        coEvery { eventPublisher.publish(capture(eventsSlot)) } just runs

        // when & then
        val thrown = assertThrows<TestValidationQueryException> {
            gateway.queryAsync(query)
        }
        assertThat(thrown.message).isEqualTo("Async query validation failed")

        coVerify(exactly = 1) { eventPublisher.publish(ofType(StartProcessQueryEvent::class)) }
        coVerify(exactly = 1) { eventPublisher.publish(ofType(StopProcessQueryEvent::class)) }
        coVerify(exactly = 1) { eventPublisher.publish(ofType(ValidationFailedQueryEvent::class)) }
        coVerify(exactly = 0) { eventPublisher.publish(ofType(ErrorProcessQueryEvent::class)) }

        val validationEvent = eventsSlot.first { it is ValidationFailedQueryEvent } as ValidationFailedQueryEvent
        assertThat(validationEvent.queryName).isEqualTo(query.queryName)
        assertThat(validationEvent.eventTags).isEqualTo(query.getEventTags())
        assertThat(validationEvent.exception).isEqualTo(exception)
    }

    // Test exception classes
    private class TestValidationCommandException(message: String) : ValidationCommandHandlerException(message)
    private class TestValidationQueryException(message: String) : ValidationQueryHandlerException(message)

    private class TestCommandName: CommandName("test-command") {
        companion object {
            val INSTANCE = TestCommandName()
        }
    }
    private data class TestCommand(val data: String): Command<TestCommandResult>(
        commandName = TestCommandName.INSTANCE
    ) {
        override fun getEventTags(): List<EventTag> {
            return listOf(
                EventTag("test-tag", "test-value"),
                EventTag("command-data", data)
            )
        }
    }
    private data class TestCommandResult(val result: String)
    private class TestQueryName: QueryName("test-query") {
        companion object {
            val INSTANCE = TestQueryName()
        }
    }
    private data class TestQuery(val id: String): Query<TestQueryResult>(
        queryName = TestQueryName.INSTANCE
    ) {
        override fun getEventTags(): List<EventTag> {
            return listOf(
                EventTag("test-tag", "test-value"),
                EventTag("query-id", id)
            )
        }
    }
    private data class TestQueryResult(val data: String)
}
