package com.kormichu.kqrs.query

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.kormichu.kqrs.AsyncDispatchers
import com.kormichu.kqrs.transaction.TransactionalExecutor
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext

class DefaultAsyncQueryExecutorTest {
    private val asyncDispatchers = mockk<AsyncDispatchers>()
    private lateinit var executor: DefaultAsyncQueryExecutor

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        executor = DefaultAsyncQueryExecutor(asyncDispatchers)
    }

    @Test
    fun `should execute async query with handler dispatcher`() = runTest {
        // given
        val query = TestAsyncQuery("async-value")
        val expectedResult = "async-result"
        val recordingDispatcher = RecordingDispatcher()
        val handler = DispatcherAwareAsyncQueryHandler(expectedResult, recordingDispatcher)

        every { asyncDispatchers.queryExecutorContext(recordingDispatcher) } returns recordingDispatcher

        // when
        val result = executor.executeAsync(query, handler)

        // then
        assertThat(result).isEqualTo(expectedResult)
        assertThat(recordingDispatcher.used.get()).isEqualTo(true)
        verify(exactly = 1) { asyncDispatchers.queryExecutorContext(recordingDispatcher) }
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

        every { asyncDispatchers.queryIOExecutorContext() } returns recordingDispatcher

        // when
        val result = executor.executeAsync(query, handler)

        // then
        assertThat(result).isEqualTo(expectedResult)
        assertThat(recordingDispatcher.used.get()).isEqualTo(true)
        verify(exactly = 1) { asyncDispatchers.queryIOExecutorContext() }
    }

    @Test
    fun `should execute async IO transactional command using transactional executor`() = runTest {
        // given
        val command = TestAsyncIOTransactionalQuery("io-transactional")
        val transactionalExecutor = RecordingTransactionalExecutor()
        val recordingDispatcher = RecordingDispatcher()
        val handler = TestAsyncIOTransactionalQueryHandler(
            result = "io-transactional-result",
            transactionalExecutor = transactionalExecutor
        )

        every { asyncDispatchers.queryIOExecutorContext() } returns recordingDispatcher

        // when
        val result = executor.executeAsync(command, handler)

        // then
        assertThat(result).isEqualTo("io-transactional-result")
        assertThat(recordingDispatcher.used.get()).isEqualTo(true)
        assertThat(transactionalExecutor.suspendExecutionCount).isEqualTo(1)
        verify(exactly = 1) { asyncDispatchers.queryIOExecutorContext() }
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

    // Test queries
    private data class TestAsyncQuery(val value: String) : Query<String?>()
    private data class TestAsyncIOQuery(val value: String) : Query<String?>()
    private data class TestAsyncIOTransactionalQuery(val value: String) : Query<String?>()

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

    private class TestAsyncIOTransactionalQueryHandler(
        private val result: String?,
        override val transactionalExecutor: TransactionalExecutor
    ) : AsyncIOTransactionalQueryHandler<TestAsyncIOTransactionalQuery, String?> {
        override fun handleInTransaction(query: TestAsyncIOTransactionalQuery): String? = result
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

    private class RecordingTransactionalExecutor : TransactionalExecutor {
        var suspendExecutionCount = 0

        override fun <T> execute(block: () -> T): T = block()
        override fun <T> executeReadOnly(block: () -> T): T {
            suspendExecutionCount++
            return block()
        }
    }
}

