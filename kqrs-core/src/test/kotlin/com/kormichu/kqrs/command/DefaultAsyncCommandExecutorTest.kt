package com.kormichu.kqrs.command

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.kormichu.kqrs.AsyncDispatchers
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import com.kormichu.kqrs.transaction.TransactionalExecutor
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext

class DefaultAsyncCommandExecutorTest {
    private val asyncDispatchers = mockk<AsyncDispatchers>()
    private lateinit var executor: DefaultAsyncCommandExecutor

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        executor = DefaultAsyncCommandExecutor(asyncDispatchers)
    }

    @Test
    fun `should execute async command with handler dispatcher`() = runTest {
        // given
        val command = TestAsyncCommand("async-value")
        val expectedResult = "async-result"
        val recordingDispatcher = RecordingDispatcher()
        val handler = DispatcherAwareAsyncCommandHandler(expectedResult, recordingDispatcher)

        every { asyncDispatchers.commandExecutorContext(recordingDispatcher) } returns recordingDispatcher

        // when
        val result = executor.executeAsync(command, handler)

        // then
        assertThat(result).isEqualTo(expectedResult)
        assertThat(recordingDispatcher.used.get()).isEqualTo(true)
        verify(exactly = 1) { asyncDispatchers.commandExecutorContext(recordingDispatcher) }
    }

    @Test
    fun `should execute async command without dispatcher using default behavior`() = runTest {
        // given
        val command = TestAsyncCommand("async-value")
        val expectedResult = "async-result"
        val handler = TestAsyncCommandHandler(expectedResult)

        // when
        val result = executor.executeAsync(command, handler)

        // then
        assertThat(result).isEqualTo(expectedResult)
    }

    @Test
    fun `should execute async IO command on IO dispatcher`() = runTest {
        // given
        val command = TestAsyncIOCommand("io-value")
        val expectedResult = "io-result"
        val recordingDispatcher = RecordingDispatcher()
        val handler = TestAsyncIOCommandHandler(expectedResult)

        every { asyncDispatchers.commandIOExecutorContext() } returns recordingDispatcher

        // when
        val result = executor.executeAsync(command, handler)

        // then
        assertThat(result).isEqualTo(expectedResult)
        assertThat(recordingDispatcher.used.get()).isEqualTo(true)
        verify(exactly = 1) { asyncDispatchers.commandIOExecutorContext() }
    }

    @Test
    fun `should execute async IO transactional command using transactional executor`() = runTest {
        // given
        val command = TestAsyncIOTransactionalCommand("io-transactional")
        val transactionalExecutor = RecordingTransactionalExecutor()
        val recordingDispatcher = RecordingDispatcher()
        val handler = TestAsyncIOTransactionalCommandHandler(
            result = "io-transactional-result",
            transactionalExecutor = transactionalExecutor
        )

        every { asyncDispatchers.commandIOExecutorContext() } returns recordingDispatcher

        // when
        val result = executor.executeAsync(command, handler)

        // then
        assertThat(result).isEqualTo("io-transactional-result")
        assertThat(recordingDispatcher.used.get()).isEqualTo(true)
        assertThat(transactionalExecutor.suspendExecutionCount).isEqualTo(1)
        verify(exactly = 1) { asyncDispatchers.commandIOExecutorContext() }
    }

    @Test
    fun `should handle null result from async handler`() = runTest {
        // given
        val command = TestAsyncCommand("test")
        val handler = TestAsyncCommandHandler(null)

        // when
        val result = executor.executeAsync(command, handler)

        // then
        assertThat(result).isEqualTo(null)
    }

    @Test
    fun `should propagate exception from async handler`() = runTest {
        // given
        val command = TestAsyncCommand("test")
        val handler = FailingAsyncCommandHandler(RuntimeException("async error"))

        // when & then
        val exception = assertThrows<RuntimeException> {
            executor.executeAsync(command, handler)
        }

        assertThat(exception.message).isEqualTo("async error")
    }

    // Test commands
    private data class TestCommand(val value: String) : Command<String?>()
    private data class TestAsyncCommand(val value: String) : Command<String?>()
    private data class TestAsyncIOCommand(val value: String) : Command<String?>()
    private data class TestAsyncIOTransactionalCommand(val value: String) : Command<String?>()

    // Test handlers
    private class TestAsyncCommandHandler(
        private val result: String?
    ) : AsyncCommandHandler<TestAsyncCommand, String?> {
        override suspend fun handle(command: TestAsyncCommand): String? = result
    }

    private class TestAsyncIOCommandHandler(
        private val result: String?
    ) : AsyncIOCommandHandler<TestAsyncIOCommand, String?> {
        override suspend fun handle(command: TestAsyncIOCommand): String? = result
    }

    private class TestAsyncIOTransactionalCommandHandler(
        private val result: String?,
        override val transactionalExecutor: TransactionalExecutor
    ) : AsyncIOTransactionalCommandHandler<TestAsyncIOTransactionalCommand, String?> {
        override fun handleInTransaction(command: TestAsyncIOTransactionalCommand): String? = result
    }

    private class DispatcherAwareAsyncCommandHandler(
        private val result: String,
        override val dispatcher: CoroutineDispatcher
    ) : AsyncCommandHandler<TestAsyncCommand, String?> {
        override suspend fun handle(command: TestAsyncCommand): String? = result
    }

    private class FailingAsyncCommandHandler(
        private val exception: RuntimeException
    ) : AsyncCommandHandler<TestAsyncCommand, String?> {
        override suspend fun handle(command: TestAsyncCommand): String? = throw exception
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
        override fun <T> executeReadOnly(block: () -> T): T = block()
    }
}

