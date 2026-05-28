package com.kormichu.kqrs.query

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

class DefaultQueryBusTest {
    private val queryExecutor = mockk<QueryExecutor>()
    private val handlerStorage = mockk<QueryHandlerStorage>()

    private lateinit var queryBus: DefaultQueryBus

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        queryBus = DefaultQueryBus(
            queryExecutor = queryExecutor,
            handlerStorage = handlerStorage
        )
    }

    @Test
    fun `should dispatch query with registered handler`() {
        // given
        val query = TestQuery("test-value")
        val expectedResult = "result"
        val handler = mockk<QueryHandler<TestQuery, String?>>()

        every { handlerStorage.getHandler(TestQuery::class) } returns handler
        every { queryExecutor.execute(query, handler) } returns expectedResult

        // when
        val result = queryBus.dispatch(query)

        // then
        assertThat(result).isEqualTo(expectedResult)
        verify(exactly = 1) { queryExecutor.execute(query, handler) }
    }

    @Test
    fun `should throw UnsupportedQueryException when handler not found`() {
        // given
        val query = TestQuery("test-value")

        every { handlerStorage.getHandler(TestQuery::class) } returns null

        // when & then
        val exception = assertThrows<UnsupportedQueryException> {
            queryBus.dispatch(query)
        }

        assertThat(exception).isInstanceOf(UnsupportedQueryException::class)
    }

    @Test
    fun `should dispatch multiple different queries`() {
        // given
        val query1 = TestQuery("value1")
        val query2 = AnotherTestQuery(42)
        val handler1 = mockk<QueryHandler<TestQuery, String?>>()
        val handler2 = mockk<QueryHandler<AnotherTestQuery, String>>()

        every { handlerStorage.getHandler(TestQuery::class) } returns handler1
        every { handlerStorage.getHandler(AnotherTestQuery::class) } returns handler2
        every { queryExecutor.execute(query1, handler1) } returns "result1"
        every { queryExecutor.execute(query2, handler2) } returns "result2"

        // when
        val result1 = queryBus.dispatch(query1)
        val result2 = queryBus.dispatch(query2)

        // then
        assertThat(result1).isEqualTo("result1")
        assertThat(result2).isEqualTo("result2")
    }

    @Test
    fun `should handle query with null result`() {
        // given
        val query = TestQuery("test")
        val handler = mockk<QueryHandler<TestQuery, String?>>()

        every { handlerStorage.getHandler(TestQuery::class) } returns handler
        every { queryExecutor.execute(query, handler) } returns null

        // when
        val result = queryBus.dispatch(query)

        // then
        assertThat(result).isEqualTo(null)
    }

    // Test queries
    private data class TestQuery(val value: String) : Query<String?>()
    private data class AnotherTestQuery(val id: Int) : Query<String>()
}

