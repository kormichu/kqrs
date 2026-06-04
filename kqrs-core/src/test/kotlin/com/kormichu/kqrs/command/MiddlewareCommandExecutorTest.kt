package com.kormichu.kqrs.command

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger

class MiddlewareCommandExecutorTest {

    @Test
    fun `should execute command middlewares in order`() {
        val invocationOrder = mutableListOf<String>()
        val command = TestCommand("value")
        val handler = mockk<CommandHandler<TestCommand, String>>()

        every { handler.handle(command) } answers {
            invocationOrder += "handler"
            "ok"
        }

        val first = object : CommandMiddleware {
            override fun <C : Command<R>, R> intercept(context: CommandExecutionContext<C, R>, next: () -> R): R {
                invocationOrder += "first-before"
                return next().also { invocationOrder += "first-after" }
            }
        }
        val second = object : CommandMiddleware {
            override fun <C : Command<R>, R> intercept(context: CommandExecutionContext<C, R>, next: () -> R): R {
                invocationOrder += "second-before"
                return next().also { invocationOrder += "second-after" }
            }
        }

        val executor = MiddlewareCommandExecutor(listOf(first, second))

        val result = executor.execute(command, handler)

        assertThat(result).isEqualTo("ok")
        assertThat(invocationOrder).containsExactly(
            "first-before",
            "second-before",
            "handler",
            "second-after",
            "first-after",
        )
    }

    @Test
    fun `should allow short-circuiting command middleware`() {
        val command = TestCommand("value")
        val handler = mockk<CommandHandler<TestCommand, String>>()
        val shortCircuit = object : CommandMiddleware {
            override fun <C : Command<R>, R> intercept(context: CommandExecutionContext<C, R>, next: () -> R): R {
                @Suppress("UNCHECKED_CAST")
                return "short-circuited" as R
            }
        }

        val executor = MiddlewareCommandExecutor(listOf(shortCircuit))

        val result = executor.execute(command, handler)

        assertThat(result).isEqualTo("short-circuited")
        verify(exactly = 0) { handler.handle(any()) }
    }

    @Test
    fun `should retry command middleware on failure according to policy`() {
        val attempts = AtomicInteger(0)
        val command = TestCommand("value")
        val handler = mockk<CommandHandler<TestCommand, String>>()

        every { handler.handle(command) } answers {
            if (attempts.incrementAndGet() < 3) {
                throw IllegalStateException("transient")
            }
            "ok"
        }

        val retryMiddleware = RetryCommandMiddleware(maxAttempts = 3)
        val executor = MiddlewareCommandExecutor(listOf(retryMiddleware))

        val result = executor.execute(command, handler)

        assertThat(result).isEqualTo("ok")
        assertThat(attempts.get()).isEqualTo(3)
    }

    @Test
    fun `should trace async command middleware callbacks`() = runTest {
        val command = TestCommand("value")
        val handler = mockk<AsyncCommandHandler<TestCommand, String>>()
        val events = mutableListOf<String>()

        coEvery { handler.handle(command) } returns "ok"

        val tracingMiddleware = TracingAsyncCommandMiddleware(
            onStart = { events += "start" },
            onSuccess = { _, _ -> events += "success" },
            onError = { _, _ -> events += "error" },
        )
        val delegate = mockk<AsyncCommandExecutor>()
        coEvery { delegate.executeAsync(command, handler) } coAnswers { handler.handle(command) }

        val executor = MiddlewareAsyncCommandExecutor(listOf(tracingMiddleware), delegate)

        val result = executor.executeAsync(command, handler)

        assertThat(result).isEqualTo("ok")
        assertThat(events).containsExactly("start", "success")
        coVerify(exactly = 1) { handler.handle(command) }
    }

    @Test
    fun `should propagate async command exception after retry attempts exhausted`() = runTest {
        val command = TestCommand("value")
        val handler = mockk<AsyncCommandHandler<TestCommand, String>>()
        val retryMiddleware = RetryAsyncCommandMiddleware(maxAttempts = 2)
        val delegate = mockk<AsyncCommandExecutor>()

        coEvery { delegate.executeAsync(command, handler) } throws IllegalStateException("still failing")

        val executor = MiddlewareAsyncCommandExecutor(listOf(retryMiddleware), delegate)

        val result = runCatching { executor.executeAsync(command, handler) }

        assertThat(result.exceptionOrNull()?.message).isEqualTo("still failing")
        coVerify(exactly = 2) { delegate.executeAsync(command, handler) }
    }

    private data class TestCommand(val value: String) : Command<String>()
}
