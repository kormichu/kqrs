package com.kormichu.kqrs.spring.command

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import com.kormichu.kqrs.command.AsyncCommandHandler
import com.kormichu.kqrs.command.Command
import com.kormichu.kqrs.command.CommandHandler
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationContext
import kotlin.reflect.KClass

class SpringCommandHandlerStorageTest {
    private val applicationContext = mockk<ApplicationContext>()

    @BeforeEach
    fun setUp() {
        clearAllMocks()
    }

    @Nested
    inner class SpringCommandHandlerStorageTests {

        @Test
        fun `should return handler for registered command`() {
            // given
            val handler = TestCommandHandler("result")
            mockCommandHandlers(mapOf(TestCommand::class to handler))

            val storage = SpringCommandHandlerStorage(applicationContext)

            // when
            val result = storage.getHandler(TestCommand::class)

            // then
            assertThat(result).isNotNull()
            assertThat(result).isEqualTo(handler)
        }

        @Test
        fun `should return null when handler not found`() {
            // given
            mockCommandHandlers(emptyMap())

            val storage = SpringCommandHandlerStorage(applicationContext)

            // when
            val result = storage.getHandler(TestCommand::class)

            // then
            assertThat(result).isNull()
        }

        @Test
        fun `should return correct handler for multiple registered commands`() {
            // given
            val handler1 = TestCommandHandler("result1")
            val handler2 = AnotherTestCommandHandler("result2")
            mockCommandHandlers(mapOf(
                TestCommand::class to handler1,
                AnotherTestCommand::class to handler2
            ))

            val storage = SpringCommandHandlerStorage(applicationContext)

            // when
            val result1 = storage.getHandler(TestCommand::class)
            val result2 = storage.getHandler(AnotherTestCommand::class)

            // then
            assertThat(result1).isEqualTo(handler1)
            assertThat(result2).isEqualTo(handler2)
        }

        @Test
        fun `should return null for unregistered command when other handlers exist`() {
            // given
            val handler = TestCommandHandler("result")
            mockCommandHandlers(mapOf(TestCommand::class to handler))

            val storage = SpringCommandHandlerStorage(applicationContext)

            // when
            val result = storage.getHandler(AnotherTestCommand::class)

            // then
            assertThat(result).isNull()
        }
    }

    @Nested
    inner class SpringAsyncCommandHandlerStorageTests {

        @Test
        fun `should return async handler for registered command`() {
            // given
            val handler = TestAsyncCommandHandler("async-result")
            mockAsyncCommandHandlers(mapOf(TestAsyncCommand::class to handler))

            val storage = SpringAsyncCommandHandlerStorage(applicationContext)

            // when
            val result = storage.getHandler(TestAsyncCommand::class)

            // then
            assertThat(result).isNotNull()
            assertThat(result).isEqualTo(handler)
        }

        @Test
        fun `should return null when async handler not found`() {
            // given
            mockAsyncCommandHandlers(emptyMap())

            val storage = SpringAsyncCommandHandlerStorage(applicationContext)

            // when
            val result = storage.getHandler(TestAsyncCommand::class)

            // then
            assertThat(result).isNull()
        }

        @Test
        fun `should return correct async handler for multiple registered commands`() {
            // given
            val handler1 = TestAsyncCommandHandler("async-result1")
            val handler2 = AnotherTestAsyncCommandHandler("async-result2")
            mockAsyncCommandHandlers(mapOf(
                TestAsyncCommand::class to handler1,
                AnotherTestAsyncCommand::class to handler2
            ))

            val storage = SpringAsyncCommandHandlerStorage(applicationContext)

            // when
            val result1 = storage.getHandler(TestAsyncCommand::class)
            val result2 = storage.getHandler(AnotherTestAsyncCommand::class)

            // then
            assertThat(result1).isEqualTo(handler1)
            assertThat(result2).isEqualTo(handler2)
        }

        @Test
        fun `should return null for unregistered async command when other handlers exist`() {
            // given
            val handler = TestAsyncCommandHandler("async-result")
            mockAsyncCommandHandlers(mapOf(TestAsyncCommand::class to handler))

            val storage = SpringAsyncCommandHandlerStorage(applicationContext)

            // when
            val result = storage.getHandler(AnotherTestAsyncCommand::class)

            // then
            assertThat(result).isNull()
        }
    }

    private fun mockCommandHandlers(handlers: Map<KClass<*>, CommandHandler<*, *>>) {
        val beanNames = List(handlers.keys.size) { index -> "handler$index" }.toTypedArray()
        every { applicationContext.getBeanNamesForType(CommandHandler::class.java) } returns beanNames

        handlers.values.forEachIndexed { index, handler ->
            every { applicationContext.getBean("handler$index") } returns handler
        }
    }

    private fun mockAsyncCommandHandlers(handlers: Map<KClass<*>, AsyncCommandHandler<*, *>>) {
        val beanNames = List(handlers.keys.size) { index -> "asyncHandler$index" }.toTypedArray()
        every { applicationContext.getBeanNamesForType(AsyncCommandHandler::class.java) } returns beanNames

        handlers.values.forEachIndexed { index, handler ->
            every { applicationContext.getBean("asyncHandler$index") } returns handler
        }
    }

    // Test commands
    private data class TestCommand(val value: String) : Command<String?>()
    private data class AnotherTestCommand(val id: Int) : Command<String>()
    private data class TestAsyncCommand(val value: String) : Command<String?>()
    private data class AnotherTestAsyncCommand(val id: Int) : Command<String>()

    // Test handlers
    private class TestCommandHandler(
        private val result: String?
    ) : CommandHandler<TestCommand, String?> {
        override fun handle(command: TestCommand): String? = result
    }

    private class AnotherTestCommandHandler(
        private val result: String
    ) : CommandHandler<AnotherTestCommand, String> {
        override fun handle(command: AnotherTestCommand): String = result
    }

    private class TestAsyncCommandHandler(
        private val result: String?
    ) : AsyncCommandHandler<TestAsyncCommand, String?> {
        override suspend fun handle(command: TestAsyncCommand): String? = result
    }

    private class AnotherTestAsyncCommandHandler(
        private val result: String
    ) : AsyncCommandHandler<AnotherTestAsyncCommand, String> {
        override suspend fun handle(command: AnotherTestAsyncCommand): String = result
    }
}

