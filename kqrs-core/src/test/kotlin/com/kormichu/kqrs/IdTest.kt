package com.kormichu.kqrs

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isLessThan
import assertk.assertions.isNotEqualTo
import assertk.assertions.isNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

class IdTest {
    @Test
    fun `should create Id with value`() {
        // given
        val uuidValue = UUID.randomUUID()

        // when
        val id = TestId(uuidValue)

        // then
        assertThat(id.value).isEqualTo(uuidValue)
    }

    @Test
    fun `should create Id with string value`() {
        // given
        val stringValue = "test-id-123"

        // when
        val id = StringId(stringValue)

        // then
        assertThat(id.value).isEqualTo(stringValue)
    }

    @Test
    fun `should create Id with integer value`() {
        // given
        val intValue = 42

        // when
        val id = IntId(intValue)

        // then
        assertThat(id.value).isEqualTo(intValue)
    }

    @Test
    fun `should generate random UUID for Id`() {
        // when
        val id1 = Id.generateUuid<TestId>()
        val id2 = Id.generateUuid<TestId>()

        // then
        assertThat(id1.value).isNotNull()
        assertThat(id2.value).isNotNull()
        assertThat(id1.value).isNotEqualTo(id2.value)
        assertThat(id1.value).isInstanceOf(UUID::class)
    }

    @Test
    fun `should generate UUID v7 for Id`() {
        // when
        val id1 = Id.generateUuidV7<TestId>()
        val id2 = Id.generateUuidV7<TestId>()

        // then
        assertThat(id1.value).isNotNull()
        assertThat(id2.value).isNotNull()
        assertThat(id1.value).isNotEqualTo(id2.value)
        assertThat(id1.value).isInstanceOf(UUID::class)
        // UUID v7 should have version 7
        assertThat(id1.value.version()).isEqualTo(7)
        assertThat(id2.value.version()).isEqualTo(7)
    }

    @Test
    fun `should generate time-ordered UUID v7`() {
        // when
        val id1 = Id.generateUuidV7<TestId>()
        Thread.sleep(10) // Small delay to ensure time difference
        val id2 = Id.generateUuidV7<TestId>()

        // then
        assertThat(id1.value.toString()).isLessThan(id2.value.toString())
    }

    @Test
    fun `should create Id from UUID string`() {
        // given
        val uuid = UUID.randomUUID()
        val uuidString = uuid.toString()

        // when
        val id = Id.fromUuidString<TestId>(uuidString)

        // then
        assertThat(id.value).isEqualTo(uuid)
    }

    @Test
    fun `should throw exception when creating Id from invalid UUID string`() {
        // given
        val invalidUuidString = "not-a-valid-uuid"

        // when & then
        assertThrows<IllegalArgumentException> {
            Id.fromUuidString<TestId>(invalidUuidString)
        }
    }

    @Test
    fun `should create different Id types with same UUID value`() {
        // given
        val uuid = UUID.randomUUID()

        // when
        val id1 = TestId(uuid)
        val id2 = AnotherTestId(uuid)

        // then
        assertThat(id1.value).isEqualTo(id2.value)
        assertThat(id1).isNotEqualTo(id2)
    }

    @Test
    fun `should support equality for same Id type and value`() {
        // given
        val uuid = UUID.randomUUID()

        // when
        val id1 = TestId(uuid)
        val id2 = TestId(uuid)

        // then
        assertThat(id1).isEqualTo(id2)
        assertThat(id1.hashCode()).isEqualTo(id2.hashCode())
    }

    @Test
    fun `should handle null UUID string`() {
        // when & then
        assertThrows<NullPointerException> {
            Id.fromUuidString<TestId>(null as String)
        }
    }

    @Test
    fun `should generate different UUIDs for different Id types`() {
        // when
        val testId = Id.generateUuid<TestId>()
        val anotherTestId = Id.generateUuid<AnotherTestId>()

        // then
        assertThat(testId.value).isNotEqualTo(anotherTestId.value)
    }

    @Test
    fun `should preserve UUID format when creating from string`() {
        // given
        val uuidString = "550e8400-e29b-41d4-a716-446655440000"

        // when
        val id = Id.fromUuidString<TestId>(uuidString)

        // then
        assertThat(id.value.toString()).isEqualTo(uuidString)
    }

    @Test
    fun `should handle uppercase UUID string`() {
        // given
        val uuidString = "550E8400-E29B-41D4-A716-446655440000"

        // when
        val id = Id.fromUuidString<TestId>(uuidString)

        // then
        assertThat(id.value.toString()).isEqualTo(uuidString.lowercase())
    }

    @Test
    fun `should generate multiple unique UUIDs`() {
        // when
        val ids = (1..100).map { Id.generateUuid<TestId>() }

        // then
        assertThat(ids.map { it.value }.toSet()).hasSize(100)
    }

    @Test
    fun `should generate multiple unique UUID v7s`() {
        // when
        val ids = (1..100).map { Id.generateUuidV7<TestId>() }

        // then
        assertThat(ids.map { it.value }.toSet()).hasSize(100)
    }
}

data class TestId(override val value: UUID) : Id<UUID>(value)
data class AnotherTestId(override val value: UUID) : Id<UUID>(value)
data class StringId(override val value: String) : Id<String>(value)
data class IntId(override val value: Int) : Id<Int>(value)
