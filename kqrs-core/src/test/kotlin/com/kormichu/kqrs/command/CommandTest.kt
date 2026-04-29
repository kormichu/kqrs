package com.kormichu.kqrs.command

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import org.junit.jupiter.api.Test

class CommandTest {
    @Test
    fun `should create command with result type Unit`() {
        // given
        val command = TestCommandWithUnit("test")

        // when & then
        assertThat(command).isNotNull()
        assertThat(command.resultClass).isEqualTo(Unit::class.java)
    }

    @Test
    fun `should create command with nullable result type`() {
        // given
        val command = TestCommandWithNullableResult("test")

        // when & then
        assertThat(command).isNotNull()
        assertThat(command.resultClass).isEqualTo(String::class.java)
    }

    @Test
    fun `should create command with complex result type`() {
        // given
        val command = TestCommandWithComplexResult("test")

        // when & then
        assertThat(command).isNotNull()
        assertThat(command.resultClass).isEqualTo(ComplexResult::class.java)
    }

    private data class TestCommandWithUnit(val value: String) : Command<Unit>()
    private data class TestCommandWithNullableResult(val value: String) : Command<String?>()
    private data class TestCommandWithComplexResult(val value: String) : Command<ComplexResult>()
    private data class ComplexResult(val id: String, val status: String, val data: Map<String, Any>)
}
