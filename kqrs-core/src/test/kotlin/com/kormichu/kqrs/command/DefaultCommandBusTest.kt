package com.kormichu.kqrs.command

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class DefaultCommandBusTest {
    private val commandExecutor = mockk<CommandExecutor>()
    private val handlerStorage = mockk<CommandHandlerStorage>()

    private lateinit var commandBus: DefaultCommandBus

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        commandBus = DefaultCommandBus(
            commandExecutor = commandExecutor,
            handlerStorage = handlerStorage
        )
    }

    @Test
    fun `should execute command with registered handler`() {
        // given
        val command = TestCommand("test-value")
        val expectedResult = "result"
        val handler = mockk<CommandHandler<TestCommand, String?>>()

        every { handlerStorage.getHandler(TestCommand::class) } returns handler
        every { commandExecutor.execute(command, handler) } returns expectedResult

        // when
        val result = commandBus.execute(command)

        // then
        assertThat(result).isEqualTo(expectedResult)
        verify(exactly = 1) { commandExecutor.execute(command, handler) }
    }

    @Test
    fun `should throw UnsupportedCommandException when handler not found`() {
        // given
        val command = TestCommand("test-value")

        every { handlerStorage.getHandler(TestCommand::class) } returns null

        // when & then
        val exception = assertThrows<UnsupportedCommandException> {
            commandBus.execute(command)
        }

        assertThat(exception).isInstanceOf(UnsupportedCommandException::class)
    }

    @Test
    fun `should execute multiple different commands`() {
        // given
        val command1 = TestCommand("value1")
        val command2 = AnotherTestCommand(42)
        val handler1 = mockk<CommandHandler<TestCommand, String?>>()
        val handler2 = mockk<CommandHandler<AnotherTestCommand, String>>()

        every { handlerStorage.getHandler(TestCommand::class) } returns handler1
        every { handlerStorage.getHandler(AnotherTestCommand::class) } returns handler2
        every { commandExecutor.execute(command1, handler1) } returns "result1"
        every { commandExecutor.execute(command2, handler2) } returns "result2"

        // when
        val result1 = commandBus.execute(command1)
        val result2 = commandBus.execute(command2)

        // then
        assertThat(result1).isEqualTo("result1")
        assertThat(result2).isEqualTo("result2")
    }

    @Test
    fun `should handle command with null result`() {
        // given
        val command = TestCommand("test")
        val handler = mockk<CommandHandler<TestCommand, String?>>()

        every { handlerStorage.getHandler(TestCommand::class) } returns handler
        every { commandExecutor.execute(command, handler) } returns null

        // when
        val result = commandBus.execute(command)

        // then
        assertThat(result).isEqualTo(null)
    }

    // Test commands
    private data class TestCommand(val value: String) : Command<String?>()
    private data class AnotherTestCommand(val id: Int) : Command<String>()
}

