package com.kormichu.kqrs.metrics.opentelemetry

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import com.kormichu.kqrs.event.EventTag
import com.kormichu.kqrs.query.ErrorProcessQueryEvent
import com.kormichu.kqrs.query.QueryName
import com.kormichu.kqrs.query.StartProcessQueryEvent
import com.kormichu.kqrs.query.StopProcessQueryEvent
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.time.Instant

class OpenTelemetryMetricsQueryEventHandlerTest {

    companion object {
        @JvmField
        @RegisterExtension
        val otelTesting: OpenTelemetryExtension = OpenTelemetryExtension.create()
    }

    @Nested
    inner class StartQueryEventHandlerTest {

        @Test
        fun `should increment start counter`() = runTest {
            // given
            val handler = OpenTelemetryMetricsStartQueryEventHandler(otelTesting.openTelemetry)
            val event = StartProcessQueryEvent(
                queryName = QueryName("test.query"),
                eventTags = emptyList()
            )

            // when
            handler.handle(event)

            // then
            val metricData = otelTesting.metrics.find { it.name == "kqrs_query_start" }
            assertThat(metricData).isNotNull()
            val point = metricData!!.longSumData.points.first()
            assertThat(point.value).isEqualTo(1)
            assertThat(point.attributes.get(AttributeKey.stringKey("query"))).isEqualTo("test.query")
        }

        @Test
        fun `should include event tags as metric attributes`() = runTest {
            // given
            val handler = OpenTelemetryMetricsStartQueryEventHandler(otelTesting.openTelemetry)
            val event = StartProcessQueryEvent(
                queryName = QueryName("test.query"),
                eventTags = listOf(EventTag("user.id", "abc-123"))
            )

            // when
            handler.handle(event)

            // then
            val metricData = otelTesting.metrics.find { it.name == "kqrs_query_start" }
            assertThat(metricData).isNotNull()
            val point = metricData!!.longSumData.points.first()
            assertThat(point.attributes.get(AttributeKey.stringKey("query"))).isEqualTo("test.query")
            assertThat(point.attributes.get(AttributeKey.stringKey("user.id"))).isEqualTo("abc-123")
        }

        @Test
        fun `should increment counter on multiple invocations`() = runTest {
            // given
            val handler = OpenTelemetryMetricsStartQueryEventHandler(otelTesting.openTelemetry)
            val event = StartProcessQueryEvent(
                queryName = QueryName("test.query"),
                eventTags = emptyList()
            )

            // when
            handler.handle(event)
            handler.handle(event)
            handler.handle(event)

            // then
            val metricData = otelTesting.metrics.find { it.name == "kqrs_query_start" }
            assertThat(metricData).isNotNull()
            val point = metricData!!.longSumData.points.first()
            assertThat(point.value).isEqualTo(3)
        }
    }

    @Nested
    inner class StopQueryEventHandlerTest {

        @Test
        fun `should increment stop counter and record duration`() = runTest {
            // given
            val handler = OpenTelemetryMetricsStopQueryEventHandler(otelTesting.openTelemetry)
            val event = StopProcessQueryEvent(
                queryName = QueryName("test.query"),
                eventTags = emptyList(),
                startProcessingAt = Instant.now().minusMillis(150)
            )

            // when
            handler.handle(event)

            // then
            val counterData = otelTesting.metrics.find { it.name == "kqrs_query_stop" }
            assertThat(counterData).isNotNull()
            val counterPoint = counterData!!.longSumData.points.first()
            assertThat(counterPoint.value).isEqualTo(1)

            val histogramData = otelTesting.metrics.find { it.name == "kqrs_query_duration" }
            assertThat(histogramData).isNotNull()
            val histogramPoint = histogramData!!.histogramData.points.first()
            assertThat(histogramPoint.count).isEqualTo(1)
        }

        @Test
        fun `should include event tags`() = runTest {
            // given
            val handler = OpenTelemetryMetricsStopQueryEventHandler(otelTesting.openTelemetry)
            val event = StopProcessQueryEvent(
                queryName = QueryName("test.query"),
                eventTags = listOf(EventTag("tenant", "acme")),
                startProcessingAt = Instant.now().minusMillis(50)
            )

            // when
            handler.handle(event)

            // then
            val counterData = otelTesting.metrics.find { it.name == "kqrs_query_stop" }
            assertThat(counterData).isNotNull()
            val counterPoint = counterData!!.longSumData.points.first()
            assertThat(counterPoint.attributes.get(AttributeKey.stringKey("query"))).isEqualTo("test.query")
            assertThat(counterPoint.attributes.get(AttributeKey.stringKey("tenant"))).isEqualTo("acme")
        }
    }

    @Nested
    inner class ErrorQueryEventHandlerTest {

        @Test
        fun `should increment error counter`() = runTest {
            // given
            val handler = OpenTelemetryMetricsErrorQueryEventHandler(otelTesting.openTelemetry)
            val event = ErrorProcessQueryEvent(
                queryName = QueryName("test.query"),
                eventTags = emptyList(),
                startProcessingAt = Instant.now().minusMillis(100),
                exception = RuntimeException("something went wrong")
            )

            // when
            handler.handle(event)

            // then
            val metricData = otelTesting.metrics.find { it.name == "kqrs_query_error" }
            assertThat(metricData).isNotNull()
            val point = metricData!!.longSumData.points.first()
            assertThat(point.value).isEqualTo(1)
            assertThat(point.attributes.get(AttributeKey.stringKey("query"))).isEqualTo("test.query")
        }

        @Test
        fun `should include event tags`() = runTest {
            // given
            val handler = OpenTelemetryMetricsErrorQueryEventHandler(otelTesting.openTelemetry)
            val event = ErrorProcessQueryEvent(
                queryName = QueryName("test.query"),
                eventTags = listOf(EventTag("filter", "active")),
                startProcessingAt = Instant.now().minusMillis(100),
                exception = IllegalStateException("invalid state")
            )

            // when
            handler.handle(event)

            // then
            val metricData = otelTesting.metrics.find { it.name == "kqrs_query_error" }
            assertThat(metricData).isNotNull()
            val point = metricData!!.longSumData.points.first()
            assertThat(point.attributes.get(AttributeKey.stringKey("query"))).isEqualTo("test.query")
            assertThat(point.attributes.get(AttributeKey.stringKey("filter"))).isEqualTo("active")
        }
    }

    @Nested
    inner class ToOpenTelemetryAttributesTest {

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
            val attributes = event.toOpenTelemetryAttributes()

            // then
            assertThat(attributes.size()).isEqualTo(3)
            assertThat(attributes.get(AttributeKey.stringKey("key1"))).isEqualTo("value1")
            assertThat(attributes.get(AttributeKey.stringKey("key2"))).isEqualTo("value2")
            assertThat(attributes.get(AttributeKey.stringKey("query"))).isEqualTo("my.query")
        }

        @Test
        fun `should return only query attribute when no event tags`() {
            // given
            val event = StartProcessQueryEvent(
                queryName = QueryName("my.query"),
                eventTags = emptyList()
            )

            // when
            val attributes = event.toOpenTelemetryAttributes()

            // then
            assertThat(attributes.size()).isEqualTo(1)
            assertThat(attributes.get(AttributeKey.stringKey("query"))).isEqualTo("my.query")
        }
    }
}
