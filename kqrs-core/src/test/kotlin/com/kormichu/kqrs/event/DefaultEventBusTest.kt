package com.kormichu.kqrs.event

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import com.kormichu.kqrs.Id

class DefaultEventBusTest {
    private lateinit var executor: EventExecutor
    private lateinit var handlerStorage: EventHandlerStorage
    private lateinit var eventBus: DefaultEventBus

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        executor = mockk(relaxed = true)
        handlerStorage = mockk()
        eventBus = DefaultEventBus(
            executor = executor,
            handlerStorage = handlerStorage
        )
    }

    @Test
    fun `should dispatch event to all matching handlers`() {
        // given
        val event = TestEvent(Id.fromString("test-id"))
        val handler1 = mockk<EventHandler<TestEvent>>(relaxed = true)
        val handler2 = mockk<EventHandler<TestEvent>>(relaxed = true)
        every { handlerStorage.getHandlers(event::class) } returns listOf(handler1, handler2)

        // when
        eventBus.dispatch(event)

        // then
        verify(exactly = 1) { executor.execute(event, handler1) }
        verify(exactly = 1) { executor.execute(event, handler2) }
    }

    @Test
    fun `should not call executor when no handlers found`() {
        // given
        val event = TestEvent(Id.fromString("test-id"))
        every { handlerStorage.getHandlers(event::class) } returns emptyList()

        // when
        eventBus.dispatch(event)

        // then
        verify(exactly = 0) { executor.execute(any(), any()) }
    }

    @Test
    fun `should dispatch only to handlers matching the event type`() {
        // given
        val testEvent = TestEvent(Id.fromString("test-id"))
        val anotherEvent = AnotherTestEvent(Id.fromString("another-id"))
        val testHandler = mockk<EventHandler<TestEvent>>(relaxed = true)
        val anotherHandler = mockk<EventHandler<AnotherTestEvent>>(relaxed = true)

        every { handlerStorage.getHandlers(testEvent::class) } returns listOf(testHandler)
        every { handlerStorage.getHandlers(anotherEvent::class) } returns listOf(anotherHandler)

        // when
        eventBus.dispatch(testEvent)

        // then
        verify(exactly = 1) { executor.execute(testEvent, testHandler) }
        verify(exactly = 0) { executor.execute(any(), anotherHandler) }
    }

    @Test
    fun `should dispatch event to single handler`() {
        // given
        val event = TestEvent(Id.fromString("test-id"))
        val handler = mockk<EventHandler<TestEvent>>(relaxed = true)
        every { handlerStorage.getHandlers(event::class) } returns listOf(handler)

        // when
        eventBus.dispatch(event)

        // then
        verify(exactly = 1) { executor.execute(event, handler) }
    }

    @Test
    fun `should dispatch multiple events independently`() {
        // given
        val event1 = TestEvent(Id.fromString("id-1"))
        val event2 = AnotherTestEvent(Id.fromString("id-2"))
        val handler1 = mockk<EventHandler<TestEvent>>(relaxed = true)
        val handler2 = mockk<EventHandler<AnotherTestEvent>>(relaxed = true)

        every { handlerStorage.getHandlers(event1::class) } returns listOf(handler1)
        every { handlerStorage.getHandlers(event2::class) } returns listOf(handler2)

        // when
        eventBus.dispatch(event1)
        eventBus.dispatch(event2)

        // then
        verify(exactly = 1) { executor.execute(event1, handler1) }
        verify(exactly = 1) { executor.execute(event2, handler2) }
    }
}

data class TestId(override val value: String) : Id<String>(value)
data class TestEvent(override val aggregateId: TestId) : AggregateEvent(aggregateId)
data class AnotherTestEvent(override val aggregateId: TestId) : AggregateEvent(aggregateId)

