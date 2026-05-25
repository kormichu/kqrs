package com.kormichu.kqrs.metrics.prometheus

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import com.kormichu.kqrs.event.EventTag
import com.kormichu.kqrs.query.ErrorProcessQueryEvent
import com.kormichu.kqrs.query.QueryName
import com.kormichu.kqrs.query.StartProcessQueryEvent
import com.kormichu.kqrs.query.StopProcessQueryEvent
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant

class PrometheusMetricsQueryEventHandlerTest {

    private lateinit var meterRegistry: SimpleMeterRegistry

    @BeforeEach
    fun setUp() {
        meterRegistry = SimpleMeterRegistry()
    }

    @Nested
    inner class StartQueryEventHandlerTest {

        @Test
        fun `should increment start counter`() = runTest {
            // given
            val handler = PrometheusMetricsStartQueryEventHandler(meterRegistry)
            val event = StartProcessQueryEvent(
                queryName = QueryName("test.query"),
                eventTags = emptyList()
            )

            // when
            handler.handle(event)

            // then
            val counter = meterRegistry.find("kqrs_query_start")
                .tag("query", "test.query")
                .counter()
            assertThat(counter).isNotNull()
            assertThat(counter!!.count()).isEqualTo(1.0)
        }

        @Test
        fun `should include event tags as metric tags`() = runTest {
            // given
            val handler = PrometheusMetricsStartQueryEventHandler(meterRegistry)
            val event = StartProcessQueryEvent(
                queryName = QueryName("test.query"),
                eventTags = listOf(EventTag("user.id", "abc-123"))
            )

            // when
            handler.handle(event)

            // then
            val counter = meterRegistry.find("kqrs_query_start")
                .tag("query", "test.query")
                .tag("user.id", "abc-123")
                .counter()
            assertThat(counter).isNotNull()
            assertThat(counter!!.count()).isEqualTo(1.0)
        }

        @Test
        fun `should increment counter on multiple invocations`() = runTest {
            // given
            val handler = PrometheusMetricsStartQueryEventHandler(meterRegistry)
            val event = StartProcessQueryEvent(
                queryName = QueryName("test.query"),
                eventTags = emptyList()
            )

            // when
            handler.handle(event)
            handler.handle(event)
            handler.handle(event)

            // then
            val counter = meterRegistry.find("kqrs_query_start")
                .tag("query", "test.query")
                .counter()
            assertThat(counter).isNotNull()
            assertThat(counter!!.count()).isEqualTo(3.0)
        }
    }

    @Nested
    inner class StopQueryEventHandlerTest {

        @Test
        fun `should increment stop counter and record duration`() = runTest {
            // given
            val handler = PrometheusMetricsStopQueryEventHandler(meterRegistry)
            val event = StopProcessQueryEvent(
                queryName = QueryName("test.query"),
                eventTags = emptyList(),
                startProcessingAt = Instant.now().minusMillis(150)
            )

            // when
            handler.handle(event)

            // then
            val counter = meterRegistry.find("kqrs_query_stop")
                .tag("query", "test.query")
                .counter()
            assertThat(counter).isNotNull()
            assertThat(counter!!.count()).isEqualTo(1.0)

            val timer = meterRegistry.find("kqrs_query_duration")
                .tag("query", "test.query")
                .timer()
            assertThat(timer).isNotNull()
            assertThat(timer!!.count()).isEqualTo(1)
        }

        @Test
        fun `should include event tags`() = runTest {
            // given
            val handler = PrometheusMetricsStopQueryEventHandler(meterRegistry)
            val event = StopProcessQueryEvent(
                queryName = QueryName("test.query"),
                eventTags = listOf(EventTag("tenant", "acme")),
                startProcessingAt = Instant.now().minusMillis(50)
            )

            // when
            handler.handle(event)

            // then
            val counter = meterRegistry.find("kqrs_query_stop")
                .tag("query", "test.query")
                .tag("tenant", "acme")
                .counter()
            assertThat(counter).isNotNull()
            assertThat(counter!!.count()).isEqualTo(1.0)
        }
    }

    @Nested
    inner class ErrorQueryEventHandlerTest {

        @Test
        fun `should increment error counter`() = runTest {
            // given
            val handler = PrometheusMetricsErrorQueryEventHandler(meterRegistry)
            val event = ErrorProcessQueryEvent(
                queryName = QueryName("test.query"),
                eventTags = emptyList(),
                startProcessingAt = Instant.now().minusMillis(100),
                exception = RuntimeException("something went wrong")
            )

            // when
            handler.handle(event)

            // then
            val counter = meterRegistry.find("kqrs_query_error")
                .tag("query", "test.query")
                .counter()
            assertThat(counter).isNotNull()
            assertThat(counter!!.count()).isEqualTo(1.0)
        }

        @Test
        fun `should include event tags`() = runTest {
            // given
            val handler = PrometheusMetricsErrorQueryEventHandler(meterRegistry)
            val event = ErrorProcessQueryEvent(
                queryName = QueryName("test.query"),
                eventTags = listOf(EventTag("filter", "active")),
                startProcessingAt = Instant.now().minusMillis(100),
                exception = IllegalStateException("invalid state")
            )

            // when
            handler.handle(event)

            // then
            val counter = meterRegistry.find("kqrs_query_error")
                .tag("query", "test.query")
                .tag("filter", "active")
                .counter()
            assertThat(counter).isNotNull()
            assertThat(counter!!.count()).isEqualTo(1.0)
        }
    }

    @Nested
    inner class ToMicrometerTagsTest {

        @Test
        fun `should convert event tags and include query name`() {
            // given
            val event = StartProcessQueryEvent(
                queryName = QueryName("my.query"),
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
            assertThat(tags[2]).isEqualTo(Tag.of("query", "my.query"))
        }

        @Test
        fun `should return only query tag when no event tags`() {
            // given
            val event = StartProcessQueryEvent(
                queryName = QueryName("my.query"),
                eventTags = emptyList()
            )

            // when
            val tags = event.toMicrometerTags()

            // then
            assertThat(tags.size).isEqualTo(1)
            assertThat(tags[0]).isEqualTo(Tag.of("query", "my.query"))
        }
    }
}
