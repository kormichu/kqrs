package com.kormichu.kqrs.command

import assertk.assertThat
import assertk.assertions.isEqualTo
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class DefaultAsyncCommandBusTest {
    private val asyncCommandExecutor = mockk<AsyncCommandExecutor>()
    private val asyncHandlerStorage = mockk<AsyncCommandHandlerStorage>()

    private lateinit var commandBus: DefaultAsyncCommandBus

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        commandBus = DefaultAsyncCommandBus(
            asyncCommandExecutor = asyncCommandExecutor,
            asyncHandlerStorage = asyncHandlerStorage
        )
    }

    @Test
    fun `should execute async command with registered async handler`() = runTest {
        // given
        val command = TestAsyncCommand("async-value")
        val expectedResult = "async-result"
        val handler = mockk<AsyncCommandHandler<TestAsyncCommand, String?>>()

        every { asyncHandlerStorage.getHandler(TestAsyncCommand::class) } returns handler
        coEvery { asyncCommandExecutor.executeAsync(command, handler) } returns expectedResult

        // when
        val result = commandBus.dispatchAsync(command)

        // then
        assertThat(result).isEqualTo(expectedResult)
        coVerify(exactly = 1) { asyncCommandExecutor.executeAsync(command, handler) }
    }

    @Test
    fun `should throw UnsupportedAsyncCommandException when async handler not found`() = runTest {
        // given
        val command = TestAsyncCommand("async-value")

        every { asyncHandlerStorage.getHandler(TestAsyncCommand::class) } returns null

        // when & then
        assertThrows<UnsupportedAsyncCommandException> {
            commandBus.dispatchAsync(command)
        }
    }

    @Test
    fun `should execute multiple different async commands`() = runTest {
        // given
        val command1 = TestAsyncCommand("async1")
        val command2 = AnotherTestAsyncCommand(123)
        val handler1 = mockk<AsyncCommandHandler<TestAsyncCommand, String?>>()
        val handler2 = mockk<AsyncCommandHandler<AnotherTestAsyncCommand, String>>()

        every { asyncHandlerStorage.getHandler(TestAsyncCommand::class) } returns handler1
        every { asyncHandlerStorage.getHandler(AnotherTestAsyncCommand::class) } returns handler2
        coEvery { asyncCommandExecutor.executeAsync(command1, handler1) } returns "async-result1"
        coEvery { asyncCommandExecutor.executeAsync(command2, handler2) } returns "async-result2"

        // when
        val result1 = commandBus.dispatchAsync(command1)
        val result2 = commandBus.dispatchAsync(command2)

        // then
        assertThat(result1).isEqualTo("async-result1")
        assertThat(result2).isEqualTo("async-result2")
    }

    @Test
    fun `should handle async command with null result`() = runTest {
        // given
        val command = TestAsyncCommand("test")
        val handler = mockk<AsyncCommandHandler<TestAsyncCommand, String?>>()

        every { asyncHandlerStorage.getHandler(TestAsyncCommand::class) } returns handler
        coEvery { asyncCommandExecutor.executeAsync(command, handler) } returns null

        // when
        val result = commandBus.dispatchAsync(command)

        // then
        assertThat(result).isEqualTo(null)
    }

    // Test commands
    private data class TestAsyncCommand(val value: String) : Command<String?>()
    private data class AnotherTestAsyncCommand(val id: Int) : Command<String>()
}

