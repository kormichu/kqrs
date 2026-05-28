package com.kormichu.kqrs.spring.query

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import com.kormichu.kqrs.query.AsyncQueryHandler
import com.kormichu.kqrs.query.Query
import com.kormichu.kqrs.query.QueryHandler
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationContext
import kotlin.reflect.KClass

class SpringQueryHandlerStorageTest {
    private val applicationContext = mockk<ApplicationContext>()

    @BeforeEach
    fun setUp() {
        clearAllMocks()
    }

    @Nested
    inner class SpringQueryHandlerStorageTests {

        @Test
        fun `should return handler for registered query`() {
            // given
            val handler = TestQueryHandler("result")
            mockQueryHandlers(mapOf(TestQuery::class to handler))

            val storage = SpringQueryHandlerStorage(applicationContext)

            // when
            val result = storage.getHandler(TestQuery::class)

            // then
            assertThat(result).isNotNull()
            assertThat(result).isEqualTo(handler)
        }

        @Test
        fun `should return null when handler not found`() {
            // given
            mockQueryHandlers(emptyMap())

            val storage = SpringQueryHandlerStorage(applicationContext)

            // when
            val result = storage.getHandler(TestQuery::class)

            // then
            assertThat(result).isNull()
        }

        @Test
        fun `should return correct handler for multiple registered queries`() {
            // given
            val handler1 = TestQueryHandler("result1")
            val handler2 = AnotherTestQueryHandler("result2")
            mockQueryHandlers(mapOf(
                TestQuery::class to handler1,
                AnotherTestQuery::class to handler2
            ))

            val storage = SpringQueryHandlerStorage(applicationContext)

            // when
            val result1 = storage.getHandler(TestQuery::class)
            val result2 = storage.getHandler(AnotherTestQuery::class)

            // then
            assertThat(result1).isEqualTo(handler1)
            assertThat(result2).isEqualTo(handler2)
        }

        @Test
        fun `should return null for unregistered query when other handlers exist`() {
            // given
            val handler = TestQueryHandler("result")
            mockQueryHandlers(mapOf(TestQuery::class to handler))

            val storage = SpringQueryHandlerStorage(applicationContext)

            // when
            val result = storage.getHandler(AnotherTestQuery::class)

            // then
            assertThat(result).isNull()
        }
    }

    @Nested
    inner class SpringAsyncQueryHandlerStorageTests {

        @Test
        fun `should return async handler for registered query`() {
            // given
            val handler = TestAsyncQueryHandler("async-result")
            mockAsyncQueryHandlers(mapOf(TestAsyncQuery::class to handler))

            val storage = SpringAsyncQueryHandlerStorage(applicationContext)

            // when
            val result = storage.getHandler(TestAsyncQuery::class)

            // then
            assertThat(result).isNotNull()
            assertThat(result).isEqualTo(handler)
        }

        @Test
        fun `should return null when async handler not found`() {
            // given
            mockAsyncQueryHandlers(emptyMap())

            val storage = SpringAsyncQueryHandlerStorage(applicationContext)

            // when
            val result = storage.getHandler(TestAsyncQuery::class)

            // then
            assertThat(result).isNull()
        }

        @Test
        fun `should return correct async handler for multiple registered queries`() {
            // given
            val handler1 = TestAsyncQueryHandler("async-result1")
            val handler2 = AnotherTestAsyncQueryHandler("async-result2")
            mockAsyncQueryHandlers(mapOf(
                TestAsyncQuery::class to handler1,
                AnotherTestAsyncQuery::class to handler2
            ))

            val storage = SpringAsyncQueryHandlerStorage(applicationContext)

            // when
            val result1 = storage.getHandler(TestAsyncQuery::class)
            val result2 = storage.getHandler(AnotherTestAsyncQuery::class)

            // then
            assertThat(result1).isEqualTo(handler1)
            assertThat(result2).isEqualTo(handler2)
        }

        @Test
        fun `should return null for unregistered async query when other handlers exist`() {
            // given
            val handler = TestAsyncQueryHandler("async-result")
            mockAsyncQueryHandlers(mapOf(TestAsyncQuery::class to handler))

            val storage = SpringAsyncQueryHandlerStorage(applicationContext)

            // when
            val result = storage.getHandler(AnotherTestAsyncQuery::class)

            // then
            assertThat(result).isNull()
        }
    }

    private fun mockQueryHandlers(handlers: Map<KClass<*>, QueryHandler<*, *>>) {
        val beanNames = List(handlers.keys.size) { index -> "handler$index" }.toTypedArray()
        every { applicationContext.getBeanNamesForType(QueryHandler::class.java) } returns beanNames

        handlers.values.forEachIndexed { index, handler ->
            every { applicationContext.getBean("handler$index") } returns handler
        }
    }

    private fun mockAsyncQueryHandlers(handlers: Map<KClass<*>, AsyncQueryHandler<*, *>>) {
        val beanNames = List(handlers.keys.size) { index -> "asyncHandler$index" }.toTypedArray()
        every { applicationContext.getBeanNamesForType(AsyncQueryHandler::class.java) } returns beanNames

        handlers.values.forEachIndexed { index, handler ->
            every { applicationContext.getBean("asyncHandler$index") } returns handler
        }
    }

    // Test queries
    private data class TestQuery(val value: String) : Query<String?>()
    private data class AnotherTestQuery(val id: Int) : Query<String>()
    private data class TestAsyncQuery(val value: String) : Query<String?>()
    private data class AnotherTestAsyncQuery(val id: Int) : Query<String>()

    // Test handlers
    private class TestQueryHandler(
        private val result: String?
    ) : QueryHandler<TestQuery, String?> {
        override fun handle(query: TestQuery): String? = result
    }

    private class AnotherTestQueryHandler(
        private val result: String
    ) : QueryHandler<AnotherTestQuery, String> {
        override fun handle(query: AnotherTestQuery): String = result
    }

    private class TestAsyncQueryHandler(
        private val result: String?
    ) : AsyncQueryHandler<TestAsyncQuery, String?> {
        override suspend fun handle(query: TestAsyncQuery): String? = result
    }

    private class AnotherTestAsyncQueryHandler(
        private val result: String
    ) : AsyncQueryHandler<AnotherTestAsyncQuery, String> {
        override suspend fun handle(query: AnotherTestAsyncQuery): String = result
    }
}

