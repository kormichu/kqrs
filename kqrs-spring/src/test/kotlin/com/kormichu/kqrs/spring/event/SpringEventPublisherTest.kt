package com.kormichu.kqrs.spring.event

import com.kormichu.kqrs.Id
import com.kormichu.kqrs.event.Event
import io.mockk.clearAllMocks
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifySequence
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationEventPublisher

class SpringEventPublisherTest {
    private lateinit var applicationEventPublisher: ApplicationEventPublisher
    private lateinit var publisher: SpringEventPublisher

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        applicationEventPublisher = mockk(relaxed = true)
        publisher = SpringEventPublisher(
            eventPublisher = applicationEventPublisher
        )
    }

    @Test
    fun `should publish single event via list`() {
        // given
        val event = TestEvent(Id.fromString("test-id"))

        // when
        publisher.publish(listOf(event))

        // then
        verify(exactly = 1) { applicationEventPublisher.publishEvent(event) }
    }

    @Test
    fun `should publish single event via convenience method`() {
        // given
        val event = TestEvent(Id.fromString("test-id"))

        // when
        publisher.publish(event)

        // then
        verify(exactly = 1) { applicationEventPublisher.publishEvent(event) }
    }

    @Test
    fun `should publish multiple events`() {
        // given
        val event1 = TestEvent(Id.fromString("test-id-1"))
        val event2 = TestEvent(Id.fromString("test-id-2"))
        val event3 = TestEvent(Id.fromString("test-id-3"))
        val events = listOf(event1, event2, event3)

        // when
        publisher.publish(events)

        // then
        verify(exactly = 1) { applicationEventPublisher.publishEvent(event1) }
        verify(exactly = 1) { applicationEventPublisher.publishEvent(event2) }
        verify(exactly = 1) { applicationEventPublisher.publishEvent(event3) }
    }

    @Test
    fun `should publish events in order`() {
        // given
        val event1 = TestEvent(Id.fromString("test-id-1"))
        val event2 = AnotherTestEvent(Id.fromString("test-id-2"))
        val event3 = TestEvent(Id.fromString("test-id-3"))
        val events = listOf(event1, event2, event3)

        // when
        publisher.publish(events)

        // then
        verifySequence {
            applicationEventPublisher.publishEvent(event1)
            applicationEventPublisher.publishEvent(event2)
            applicationEventPublisher.publishEvent(event3)
        }
    }

    @Test
    fun `should not publish any event when list is empty`() {
        // given
        val events = emptyList<Event>()

        // when
        publisher.publish(events)

        // then
        verify(exactly = 0) { applicationEventPublisher.publishEvent(any()) }
    }

    @Test
    fun `should publish different event types`() {
        // given
        val event1 = TestEvent(Id.fromString("test-id-1"))
        val event2 = AnotherTestEvent(Id.fromString("test-id-2"))

        // when
        publisher.publish(listOf(event1, event2))

        // then
        verify(exactly = 1) { applicationEventPublisher.publishEvent(event1) }
        verify(exactly = 1) { applicationEventPublisher.publishEvent(event2) }
    }

    @Test
    fun `should delegate each event to application event publisher`() {
        // given
        val events = (1..5).map { TestEvent(Id.fromString("test-id-$it")) }

        // when
        publisher.publish(events)

        // then
        events.forEach { event ->
            verify(exactly = 1) { applicationEventPublisher.publishEvent(event) }
        }
    }
}

