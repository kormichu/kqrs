package com.kormichu.kqrs.query

import assertk.assertThat
import assertk.assertions.isEqualTo
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import com.kormichu.kqrs.coroutines.CoroutineDispatchers
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext

class DefaultQueryExecutorTest {
    @Nested
    inner class DefaultQueryExecutorTests {
        private lateinit var executor: DefaultQueryExecutor

        @BeforeEach
        fun setUp() {
            clearAllMocks()
            executor = DefaultQueryExecutor()
        }

        @Test
        fun `should delegate query execution to handler`() {
            // given
            val query = TestQuery("test-value")
            val expectedResult = "result"
            val handler = mockk<QueryHandler<TestQuery, String?>>()

            every { handler.handle(query) } returns expectedResult

            // when
            val result = executor.execute(query, handler)

            // then
            assertThat(result).isEqualTo(expectedResult)
            verify(exactly = 1) { handler.handle(query) }
        }

        @Test
        fun `should handle null result from handler`() {
            // given
            val query = TestQuery("test")
            val handler = mockk<QueryHandler<TestQuery, String?>>()

            every { handler.handle(query) } returns null

            // when
            val result = executor.execute(query, handler)

            // then
            assertThat(result).isEqualTo(null)
        }

        @Test
        fun `should propagate exception from handler`() {
            // given
            val query = TestQuery("test")
            val handler = mockk<QueryHandler<TestQuery, String?>>()

            every { handler.handle(query) } throws RuntimeException("handler error")

            // when & then
            val exception = assertThrows<RuntimeException> {
                executor.execute(query, handler)
            }

            assertThat(exception.message).isEqualTo("handler error")
        }
    }

    @Nested
    inner class DefaultAsyncQueryExecutorTests {
        private val coroutineDispatchers = mockk<CoroutineDispatchers>()
        private lateinit var executor: DefaultAsyncQueryExecutor

        @BeforeEach
        fun setUp() {
            clearAllMocks()
            executor = DefaultAsyncQueryExecutor(coroutineDispatchers)
        }

        @Test
        fun `should execute async query with handler dispatcher`() = runTest {
            // given
            val query = TestAsyncQuery("async-value")
            val expectedResult = "async-result"
            val recordingDispatcher = RecordingDispatcher()
            val handler = DispatcherAwareAsyncQueryHandler(expectedResult, recordingDispatcher)

            every { coroutineDispatchers.queryExecutorContext(recordingDispatcher) } returns recordingDispatcher

            // when
            val result = executor.executeAsync(query, handler)

            // then
            assertThat(result).isEqualTo(expectedResult)
            assertThat(recordingDispatcher.used.get()).isEqualTo(true)
            verify(exactly = 1) { coroutineDispatchers.queryExecutorContext(recordingDispatcher) }
        }

        @Test
        fun `should execute async query without dispatcher using default behavior`() = runTest {
            // given
            val query = TestAsyncQuery("async-value")
            val expectedResult = "async-result"
            val handler = TestAsyncQueryHandler(expectedResult)

            // when
            val result = executor.executeAsync(query, handler)

            // then
            assertThat(result).isEqualTo(expectedResult)
        }

        @Test
        fun `should execute async IO query on IO dispatcher`() = runTest {
            // given
            val query = TestAsyncIOQuery("io-value")
            val expectedResult = "io-result"
            val recordingDispatcher = RecordingDispatcher()
            val handler = TestAsyncIOQueryHandler(expectedResult)

            every { coroutineDispatchers.queryIOExecutorContext() } returns recordingDispatcher

            // when
            val result = executor.executeAsync(query, handler)

            // then
            assertThat(result).isEqualTo(expectedResult)
            assertThat(recordingDispatcher.used.get()).isEqualTo(true)
            verify(exactly = 1) { coroutineDispatchers.queryIOExecutorContext() }
        }

        @Test
        fun `should handle null result from async handler`() = runTest {
            // given
            val query = TestAsyncQuery("test")
            val handler = TestAsyncQueryHandler(null)

            // when
            val result = executor.executeAsync(query, handler)

            // then
            assertThat(result).isEqualTo(null)
        }

        @Test
        fun `should propagate exception from async handler`() = runTest {
            // given
            val query = TestAsyncQuery("test")
            val handler = FailingAsyncQueryHandler(RuntimeException("async error"))

            // when & then
            val exception = assertThrows<RuntimeException> {
                executor.executeAsync(query, handler)
            }

            assertThat(exception.message).isEqualTo("async error")
        }
    }

    // Test queries
    private data class TestQuery(val value: String) : Query<String?>()
    private data class TestAsyncQuery(val value: String) : Query<String?>()
    private data class TestAsyncIOQuery(val value: String) : Query<String?>()

    // Test handlers
    private class TestAsyncQueryHandler(
        private val result: String?
    ) : AsyncQueryHandler<TestAsyncQuery, String?> {
        override suspend fun handle(query: TestAsyncQuery): String? = result
    }

    private class TestAsyncIOQueryHandler(
        private val result: String?
    ) : AsyncIOQueryHandler<TestAsyncIOQuery, String?> {
        override suspend fun handle(query: TestAsyncIOQuery): String? = result
    }

    private class DispatcherAwareAsyncQueryHandler(
        private val result: String,
        override val dispatcher: CoroutineDispatcher
    ) : AsyncQueryHandler<TestAsyncQuery, String?> {
        override suspend fun handle(query: TestAsyncQuery): String? = result
    }

    private class FailingAsyncQueryHandler(
        private val exception: RuntimeException
    ) : AsyncQueryHandler<TestAsyncQuery, String?> {
        override suspend fun handle(query: TestAsyncQuery): String? = throw exception
    }

    private class RecordingDispatcher : CoroutineDispatcher() {
        val used = AtomicBoolean(false)

        override fun dispatch(context: CoroutineContext, block: Runnable) {
            used.set(true)
            block.run()
        }
    }
}

