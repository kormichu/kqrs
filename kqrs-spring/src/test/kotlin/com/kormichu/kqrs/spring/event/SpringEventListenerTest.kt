package com.kormichu.kqrs.spring.event

import com.kormichu.kqrs.Id
import com.kormichu.kqrs.event.AggregateEvent
import com.kormichu.kqrs.event.Event
import com.kormichu.kqrs.event.EventBus
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.context.PayloadApplicationEvent

class SpringEventListenerTest {
    private lateinit var eventBus: EventBus
    private lateinit var springEventListener: SpringEventListener

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        eventBus = mockk(relaxed = true)
        springEventListener = SpringEventListener(eventBus)
    }

    @Test
    fun `should dispatch event to event bus`() {
        // given
        val event = TestEvent(Id.fromString("test-id"))

        // when
        springEventListener.onApplicationEvent(PayloadApplicationEvent(this, event))

        // then
        verify(exactly = 1) { eventBus.dispatch(event) }
    }

    @Test
    fun `should dispatch different event types`() {
        // given
        val testEvent = TestEvent(Id.fromString("test-id"))
        val anotherEvent = AnotherTestEvent(Id.fromString("another-id"))

        // when
        springEventListener.onApplicationEvent(PayloadApplicationEvent(this, testEvent))
        springEventListener.onApplicationEvent(PayloadApplicationEvent(this, anotherEvent))

        // then
        verify(exactly = 1) { eventBus.dispatch(testEvent) }
        verify(exactly = 1) { eventBus.dispatch(anotherEvent) }
    }

    @Test
    fun `should dispatch base event type`() {
        // given
        val event = Event()

        // when
        springEventListener.onApplicationEvent(PayloadApplicationEvent(this, event))

        // then
        verify(exactly = 1) { eventBus.dispatch(event) }
    }

    @Test
    fun `should propagate exception when event bus throws`() {
        // given
        val event = TestEvent(Id.fromString("test-id"))
        every { eventBus.dispatch(any<TestEvent>()) } throws IllegalStateException("dispatch failed")

        // when & then
        assertThrows<IllegalStateException> {
            springEventListener.onApplicationEvent(PayloadApplicationEvent(this, event))
        }
    }

    @Test
    fun `should dispatch multiple events of the same type`() {
        // given
        val event1 = TestEvent(Id.fromString("id-1"))
        val event2 = TestEvent(Id.fromString("id-2"))
        val event3 = TestEvent(Id.fromString("id-3"))

        // when
        springEventListener.onApplicationEvent(PayloadApplicationEvent(this, event1))
        springEventListener.onApplicationEvent(PayloadApplicationEvent(this, event2))
        springEventListener.onApplicationEvent(PayloadApplicationEvent(this, event3))

        // then
        verify(exactly = 1) { eventBus.dispatch(event1) }
        verify(exactly = 1) { eventBus.dispatch(event2) }
        verify(exactly = 1) { eventBus.dispatch(event3) }
        verify(exactly = 3) { eventBus.dispatch(any<TestEvent>()) }
    }
}

data class TestId(override val value: String): Id<String>(value)
data class TestEvent(override val aggregateId: TestId) : AggregateEvent(aggregateId)
data class AnotherTestEvent(override val aggregateId: TestId) : AggregateEvent(aggregateId)

