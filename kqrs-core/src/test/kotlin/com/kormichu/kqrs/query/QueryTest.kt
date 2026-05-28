package com.kormichu.kqrs.query

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import org.junit.jupiter.api.Test

class QueryTest {
    @Test
    fun `should create query with result type Unit`() {
        // given
        val query = TestQueryWithUnit("test")

        // when & then
        assertThat(query).isNotNull()
        assertThat(query.resultClass).isEqualTo(Unit::class.java)
    }

    @Test
    fun `should create query with nullable result type`() {
        // given
        val query = TestQueryWithNullableResult("test")

        // when & then
        assertThat(query).isNotNull()
        assertThat(query.resultClass).isEqualTo(String::class.java)
    }

    @Test
    fun `should create query with complex result type`() {
        // given
        val query = TestQueryWithComplexResult("test")

        // when & then
        assertThat(query).isNotNull()
        assertThat(query.resultClass).isEqualTo(ComplexResult::class.java)
    }

    private data class TestQueryWithUnit(val value: String) : Query<Unit>()
    private data class TestQueryWithNullableResult(val value: String) : Query<String?>()
    private data class TestQueryWithComplexResult(val value: String) : Query<ComplexResult>()
    private data class ComplexResult(val id: String, val status: String, val data: Map<String, Any>)
}
