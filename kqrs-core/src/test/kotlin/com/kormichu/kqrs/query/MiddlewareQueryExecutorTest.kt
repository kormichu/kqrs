package com.kormichu.kqrs.query

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

class MiddlewareQueryExecutorTest {

    @Test
    fun `should execute query middlewares in order`() {
        val invocationOrder = mutableListOf<String>()
        val query = TestQuery("value")
        val handler = mockk<QueryHandler<TestQuery, String>>()

        every { handler.handle(query) } answers {
            invocationOrder += "handler"
            "ok"
        }

        val first = object : QueryMiddleware {
            override fun <Q : Query<R>, R> intercept(context: QueryExecutionContext<Q, R>, next: () -> R): R {
                invocationOrder += "first-before"
                return next().also { invocationOrder += "first-after" }
            }
        }
        val second = object : QueryMiddleware {
            override fun <Q : Query<R>, R> intercept(context: QueryExecutionContext<Q, R>, next: () -> R): R {
                invocationOrder += "second-before"
                return next().also { invocationOrder += "second-after" }
            }
        }

        val executor = MiddlewareQueryExecutor(listOf(first, second))

        val result = executor.execute(query, handler)

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
    fun `should allow short-circuiting query middleware`() {
        val query = TestQuery("value")
        val handler = mockk<QueryHandler<TestQuery, String>>()
        val shortCircuit = object : QueryMiddleware {
            override fun <Q : Query<R>, R> intercept(context: QueryExecutionContext<Q, R>, next: () -> R): R {
                @Suppress("UNCHECKED_CAST")
                return "short-circuited" as R
            }
        }

        val executor = MiddlewareQueryExecutor(listOf(shortCircuit))

        val result = executor.execute(query, handler)

        assertThat(result).isEqualTo("short-circuited")
        verify(exactly = 0) { handler.handle(any()) }
    }

    @Test
    fun `should retry query middleware on failure according to policy`() {
        val attempts = AtomicInteger(0)
        val query = TestQuery("value")
        val handler = mockk<QueryHandler<TestQuery, String>>()

        every { handler.handle(query) } answers {
            if (attempts.incrementAndGet() < 3) {
                throw IllegalStateException("transient")
            }
            "ok"
        }

        val retryMiddleware = RetryQueryMiddleware(maxAttempts = 3)
        val executor = MiddlewareQueryExecutor(listOf(retryMiddleware))

        val result = executor.execute(query, handler)

        assertThat(result).isEqualTo("ok")
        assertThat(attempts.get()).isEqualTo(3)
    }

    @Test
    fun `should trace async query middleware callbacks`() = runTest {
        val query = TestQuery("value")
        val handler = mockk<AsyncQueryHandler<TestQuery, String>>()
        val events = mutableListOf<String>()

        coEvery { handler.handle(query) } returns "ok"

        val tracingMiddleware = TracingAsyncQueryMiddleware(
            onStart = { events += "start" },
            onSuccess = { _, _ -> events += "success" },
            onError = { _, _ -> events += "error" },
        )
        val delegate = mockk<AsyncQueryExecutor>()
        coEvery { delegate.executeAsync(query, handler) } coAnswers { handler.handle(query) }

        val executor = MiddlewareAsyncQueryExecutor(listOf(tracingMiddleware), delegate)

        val result = executor.executeAsync(query, handler)

        assertThat(result).isEqualTo("ok")
        assertThat(events).containsExactly("start", "success")
        coVerify(exactly = 1) { handler.handle(query) }
    }

    @Test
    fun `should propagate async query exception after retry attempts exhausted`() = runTest {
        val query = TestQuery("value")
        val handler = mockk<AsyncQueryHandler<TestQuery, String>>()
        val retryMiddleware = RetryAsyncQueryMiddleware(maxAttempts = 2)
        val delegate = mockk<AsyncQueryExecutor>()

        coEvery { delegate.executeAsync(query, handler) } throws IllegalStateException("still failing")

        val executor = MiddlewareAsyncQueryExecutor(listOf(retryMiddleware), delegate)

        val result = runCatching { executor.executeAsync(query, handler) }

        assertThat(result.exceptionOrNull()?.message).isEqualTo("still failing")
        coVerify(exactly = 2) { delegate.executeAsync(query, handler) }
    }

    private data class TestQuery(val value: String) : Query<String>()
}
