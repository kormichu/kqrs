package com.kormichu.kqrs.query

import assertk.assertThat
import assertk.assertions.isEqualTo
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class DefaultQueryExecutorTest {
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

    // Test queries
    private data class TestQuery(val value: String) : Query<String?>()
}

