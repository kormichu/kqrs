package com.kormichu.kqrs.query

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

class DefaultAsyncQueryBusTest {
    private val asyncQueryExecutor = mockk<AsyncQueryExecutor>()
    private val asyncHandlerStorage = mockk<AsyncQueryHandlerStorage>()

    private lateinit var queryBus: DefaultAsyncQueryBus

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        queryBus = DefaultAsyncQueryBus(
            asyncQueryExecutor = asyncQueryExecutor,
            asyncHandlerStorage = asyncHandlerStorage
        )
    }

    @Test
    fun `should dispatch async query with registered async handler`() = runTest {
        // given
        val query = TestAsyncQuery("async-value")
        val expectedResult = "async-result"
        val handler = mockk<AsyncQueryHandler<TestAsyncQuery, String?>>()

        every { asyncHandlerStorage.getHandler(TestAsyncQuery::class) } returns handler
        coEvery { asyncQueryExecutor.executeAsync(query, handler) } returns expectedResult

        // when
        val result = queryBus.dispatchAsync(query)

        // then
        assertThat(result).isEqualTo(expectedResult)
        coVerify(exactly = 1) { asyncQueryExecutor.executeAsync(query, handler) }
    }

    @Test
    fun `should throw UnsupportedAsyncQueryException when async handler not found`() = runTest {
        // given
        val query = TestAsyncQuery("async-value")

        every { asyncHandlerStorage.getHandler(TestAsyncQuery::class) } returns null

        // when & then
        assertThrows<UnsupportedAsyncQueryException> {
            queryBus.dispatchAsync(query)
        }
    }

    @Test
    fun `should dispatch multiple different async queries`() = runTest {
        // given
        val query1 = TestAsyncQuery("async1")
        val query2 = AnotherTestAsyncQuery(123)
        val handler1 = mockk<AsyncQueryHandler<TestAsyncQuery, String?>>()
        val handler2 = mockk<AsyncQueryHandler<AnotherTestAsyncQuery, String>>()

        every { asyncHandlerStorage.getHandler(TestAsyncQuery::class) } returns handler1
        every { asyncHandlerStorage.getHandler(AnotherTestAsyncQuery::class) } returns handler2
        coEvery { asyncQueryExecutor.executeAsync(query1, handler1) } returns "async-result1"
        coEvery { asyncQueryExecutor.executeAsync(query2, handler2) } returns "async-result2"

        // when
        val result1 = queryBus.dispatchAsync(query1)
        val result2 = queryBus.dispatchAsync(query2)

        // then
        assertThat(result1).isEqualTo("async-result1")
        assertThat(result2).isEqualTo("async-result2")
    }

    @Test
    fun `should handle async query with null result`() = runTest {
        // given
        val query = TestAsyncQuery("test")
        val handler = mockk<AsyncQueryHandler<TestAsyncQuery, String?>>()

        every { asyncHandlerStorage.getHandler(TestAsyncQuery::class) } returns handler
        coEvery { asyncQueryExecutor.executeAsync(query, handler) } returns null

        // when
        val result = queryBus.dispatchAsync(query)

        // then
        assertThat(result).isEqualTo(null)
    }

    // Test queries
    private data class TestAsyncQuery(val value: String) : Query<String?>()
    private data class AnotherTestAsyncQuery(val id: Int) : Query<String>()
}

