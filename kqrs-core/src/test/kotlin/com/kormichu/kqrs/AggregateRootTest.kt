package com.kormichu.kqrs

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import org.junit.jupiter.api.Test
import com.kormichu.kqrs.event.AggregateEvent
import java.util.UUID

class AggregateRootTest {
    @Test
    fun `should create aggregate with id`() {
        // given & when
        val aggregateId = TestAggregateId(UUID.randomUUID())
        val aggregate = TestAggregate(aggregateId)

        // then
        assertThat(aggregateId).isEqualTo(aggregate.id)
        assertThat(aggregate.pullEvents()).isEmpty()
    }

    @Test
    fun `should publish event while changing aggregate`() {
        // given
        val aggregate = TestAggregate(TestAggregateId(UUID.randomUUID()))

        // when
        aggregate.changeSomething1()

        // then
        val events = aggregate.pullEvents()
        assertThat(events).hasSize(1)
        assertThat(events[0]).isInstanceOf(TestEvent1::class.java)
    }

    @Test
    fun `should publish multiple events`() {
        // given
        val aggregate = TestAggregate(TestAggregateId(UUID.randomUUID()))

        // when
        aggregate.changeSomething1()
        aggregate.changeSomething2()

        // then
        val events = aggregate.pullEvents()
        assertThat(events).hasSize(2)
        assertThat(events[0]).isInstanceOf(TestEvent1::class.java)
        assertThat(events[1]).isInstanceOf(TestEvent2::class.java)
    }

    @Test
    fun `should clear domain events after pulling`() {
        // given
        val aggregate = TestAggregate(TestAggregateId(UUID.randomUUID()))

        // when
        aggregate.changeSomething1()

        // then
        val events = aggregate.pullEvents()
        assertThat(events).hasSize(1)
        assertThat(events[0]).isInstanceOf(TestEvent1::class.java)

        // and when pulling again
        val eventsAfterClear = aggregate.pullEvents()

        // then
        assertThat(eventsAfterClear).isEmpty()
    }

    // Test classes
    private data class TestAggregateId(override val value: UUID) : Id<UUID>(value)

    private class TestAggregate(
        override val id: TestAggregateId
    ) : AggregateRoot<TestAggregateId>() {

        fun changeSomething1() {
            record(TestEvent1(id))
        }
        fun changeSomething2() {
            record(TestEvent2(id))
        }
    }

    private data class TestEvent1(override val aggregateId: TestAggregateId) : AggregateEvent(aggregateId)
    private data class TestEvent2(override val aggregateId: TestAggregateId) : AggregateEvent(aggregateId)
}
