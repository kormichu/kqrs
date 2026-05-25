package com.kormichu.kqrs.metrics.prometheus

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import com.kormichu.kqrs.command.CommandName
import com.kormichu.kqrs.command.ErrorProcessCommandEvent
import com.kormichu.kqrs.command.StartProcessCommandEvent
import com.kormichu.kqrs.command.StopProcessCommandEvent
import com.kormichu.kqrs.event.EventTag
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant

class PrometheusMetricsCommandEventHandlerTest {
    private lateinit var meterRegistry: SimpleMeterRegistry

    @BeforeEach
    fun setUp() {
        meterRegistry = SimpleMeterRegistry()
    }

    @Nested
    inner class StartCommandEventHandlerTest {

        @Test
        fun `should increment start counter`() = runTest {
            // given
            val handler = PrometheusMetricsStartCommandEventHandler(meterRegistry)
            val event = StartProcessCommandEvent(
                commandName = CommandName("test.command"),
                eventTags = emptyList()
            )

            // when
            handler.handle(event)

            // then
            val counter = meterRegistry.find("kqrs_command_start")
                .tag("command", "test.command")
                .counter()
            assertThat(counter).isNotNull()
            assertThat(counter!!.count()).isEqualTo(1.0)
        }

        @Test
        fun `should include event tags as metric tags`() = runTest {
            // given
            val handler = PrometheusMetricsStartCommandEventHandler(meterRegistry)
            val event = StartProcessCommandEvent(
                commandName = CommandName("test.command"),
                eventTags = listOf(EventTag("user.email", "test@example.com"))
            )

            // when
            handler.handle(event)

            // then
            val counter = meterRegistry.find("kqrs_command_start")
                .tag("command", "test.command")
                .tag("user.email", "test@example.com")
                .counter()
            assertThat(counter).isNotNull()
            assertThat(counter!!.count()).isEqualTo(1.0)
        }

        @Test
        fun `should increment counter on multiple invocations`() = runTest {
            // given
            val handler = PrometheusMetricsStartCommandEventHandler(meterRegistry)
            val event = StartProcessCommandEvent(
                commandName = CommandName("test.command"),
                eventTags = emptyList()
            )

            // when
            handler.handle(event)
            handler.handle(event)
            handler.handle(event)

            // then
            val counter = meterRegistry.find("kqrs_command_start")
                .tag("command", "test.command")
                .counter()
            assertThat(counter).isNotNull()
            assertThat(counter!!.count()).isEqualTo(3.0)
        }
    }

    @Nested
    inner class StopCommandEventHandlerTest {

        @Test
        fun `should increment stop counter and record duration`() = runTest {
            // given
            val handler = PrometheusMetricsStopCommandEventHandler(meterRegistry)
            val event = StopProcessCommandEvent(
                commandName = CommandName("test.command"),
                eventTags = emptyList(),
                startProcessingAt = Instant.now().minusMillis(150)
            )

            // when
            handler.handle(event)

            // then
            val counter = meterRegistry.find("kqrs_command_stop")
                .tag("command", "test.command")
                .counter()
            assertThat(counter).isNotNull()
            assertThat(counter!!.count()).isEqualTo(1.0)

            val timer = meterRegistry.find("kqrs_command_duration")
                .tag("command", "test.command")
                .timer()
            assertThat(timer).isNotNull()
            assertThat(timer!!.count()).isEqualTo(1)
        }

        @Test
        fun `should include event tags`() = runTest {
            // given
            val handler = PrometheusMetricsStopCommandEventHandler(meterRegistry)
            val event = StopProcessCommandEvent(
                commandName = CommandName("test.command"),
                eventTags = listOf(EventTag("tenant", "acme")),
                startProcessingAt = Instant.now().minusMillis(50)
            )

            // when
            handler.handle(event)

            // then
            val counter = meterRegistry.find("kqrs_command_stop")
                .tag("command", "test.command")
                .tag("tenant", "acme")
                .counter()
            assertThat(counter).isNotNull()
            assertThat(counter!!.count()).isEqualTo(1.0)
        }
    }

    @Nested
    inner class ErrorCommandEventHandlerTest {

        @Test
        fun `should increment error counter`() = runTest {
            // given
            val handler = PrometheusMetricsErrorCommandEventHandler(meterRegistry)
            val event = ErrorProcessCommandEvent(
                commandName = CommandName("test.command"),
                eventTags = emptyList(),
                startProcessingAt = Instant.now().minusMillis(100),
                exception = RuntimeException("something went wrong")
            )

            // when
            handler.handle(event)

            // then
            val counter = meterRegistry.find("kqrs_command_error")
                .tag("command", "test.command")
                .counter()
            assertThat(counter).isNotNull()
            assertThat(counter!!.count()).isEqualTo(1.0)
        }

        @Test
        fun `should include event tags`() = runTest {
            // given
            val handler = PrometheusMetricsErrorCommandEventHandler(meterRegistry)
            val event = ErrorProcessCommandEvent(
                commandName = CommandName("test.command"),
                eventTags = listOf(EventTag("order.id", "abc-123")),
                startProcessingAt = Instant.now().minusMillis(100),
                exception = IllegalStateException("invalid state")
            )

            // when
            handler.handle(event)

            // then
            val counter = meterRegistry.find("kqrs_command_error")
                .tag("command", "test.command")
                .tag("order.id", "abc-123")
                .counter()
            assertThat(counter).isNotNull()
            assertThat(counter!!.count()).isEqualTo(1.0)
        }
    }

    @Nested
    inner class ToMicrometerTagsTest {

        @Test
        fun `should convert event tags and include command name`() {
            // given
            val event = StartProcessCommandEvent(
                commandName = CommandName("my.command"),
                eventTags = listOf(
                    EventTag("key1", "value1"),
                    EventTag("key2", "value2")
                )
            )

            // when
            val tags = event.toMicrometerTags()

            // then
            assertThat(tags.size).isEqualTo(3)
            assertThat(tags[0]).isEqualTo(Tag.of("key1", "value1"))
            assertThat(tags[1]).isEqualTo(Tag.of("key2", "value2"))
            assertThat(tags[2]).isEqualTo(Tag.of("command", "my.command"))
        }

        @Test
        fun `should return only command tag when no event tags`() {
            // given
            val event = StartProcessCommandEvent(
                commandName = CommandName("my.command"),
                eventTags = emptyList()
            )

            // when
            val tags = event.toMicrometerTags()

            // then
            assertThat(tags.size).isEqualTo(1)
            assertThat(tags[0]).isEqualTo(Tag.of("command", "my.command"))
        }
    }
}
