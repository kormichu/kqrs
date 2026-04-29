package com.kormichu.kqrs.command

import assertk.assertThat
import assertk.assertions.isEqualTo
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class DefaultCommandExecutorTest {
    private lateinit var executor: DefaultCommandExecutor

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        executor = DefaultCommandExecutor()
    }

    @Test
    fun `should delegate command execution to handler`() {
        // given
        val command = TestCommand("test-value")
        val expectedResult = "result"
        val handler = mockk<CommandHandler<TestCommand, String?>>()

        every { handler.handle(command) } returns expectedResult

        // when
        val result = executor.execute(command, handler)

        // then
        assertThat(result).isEqualTo(expectedResult)
        verify(exactly = 1) { handler.handle(command) }
    }

    @Test
    fun `should handle null result from handler`() {
        // given
        val command = TestCommand("test")
        val handler = mockk<CommandHandler<TestCommand, String?>>()

        every { handler.handle(command) } returns null

        // when
        val result = executor.execute(command, handler)

        // then
        assertThat(result).isEqualTo(null)
    }

    @Test
    fun `should propagate exception from handler`() {
        // given
        val command = TestCommand("test")
        val handler = mockk<CommandHandler<TestCommand, String?>>()

        every { handler.handle(command) } throws RuntimeException("handler error")

        // when & then
        val exception = assertThrows<RuntimeException> {
            executor.execute(command, handler)
        }

        assertThat(exception.message).isEqualTo("handler error")
    }

    private data class TestCommand(val value: String) : Command<String?>()
}

