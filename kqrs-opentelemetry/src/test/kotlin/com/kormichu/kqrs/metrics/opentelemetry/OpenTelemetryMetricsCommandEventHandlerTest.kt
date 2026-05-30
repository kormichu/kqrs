package com.kormichu.kqrs.metrics.opentelemetry

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import com.kormichu.kqrs.command.CommandName
import com.kormichu.kqrs.command.ErrorProcessCommandEvent
import com.kormichu.kqrs.command.StartProcessCommandEvent
import com.kormichu.kqrs.command.StopProcessCommandEvent
import com.kormichu.kqrs.event.EventTag
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.time.Instant

class OpenTelemetryMetricsCommandEventHandlerTest {

    companion object {
        @JvmField
        @RegisterExtension
        val otelTesting: OpenTelemetryExtension = OpenTelemetryExtension.create()
    }

    @Nested
    inner class StartCommandEventHandlerTest {

        @Test
        fun `should increment start counter`() = runTest {
            // given
            val handler = OpenTelemetryMetricsStartCommandEventHandler(otelTesting.openTelemetry)
            val event = StartProcessCommandEvent(
                commandName = CommandName("test.command"),
                eventTags = emptyList()
            )

            // when
            handler.handle(event)

            // then
            val metrics = otelTesting.metrics
            val metricData = metrics.find { it.name == "kqrs_command_start" }
            assertThat(metricData).isNotNull()
            val points = metricData!!.longSumData.points
            assertThat(points.size).isEqualTo(1)
            val point = points.first()
            assertThat(point.value).isEqualTo(1)
            assertThat(point.attributes.get(AttributeKey.stringKey("command"))).isEqualTo("test.command")
        }

        @Test
        fun `should include event tags as metric attributes`() = runTest {
            // given
            val handler = OpenTelemetryMetricsStartCommandEventHandler(otelTesting.openTelemetry)
            val event = StartProcessCommandEvent(
                commandName = CommandName("test.command"),
                eventTags = listOf(EventTag("user.email", "test@example.com"))
            )

            // when
            handler.handle(event)

            // then
            val metricData = otelTesting.metrics.find { it.name == "kqrs_command_start" }
            assertThat(metricData).isNotNull()
            val point = metricData!!.longSumData.points.first()
            assertThat(point.attributes.get(AttributeKey.stringKey("command"))).isEqualTo("test.command")
            assertThat(point.attributes.get(AttributeKey.stringKey("user.email"))).isEqualTo("test@example.com")
        }

        @Test
        fun `should increment counter on multiple invocations`() = runTest {
            // given
            val handler = OpenTelemetryMetricsStartCommandEventHandler(otelTesting.openTelemetry)
            val event = StartProcessCommandEvent(
                commandName = CommandName("test.command"),
                eventTags = emptyList()
            )

            // when
            handler.handle(event)
            handler.handle(event)
            handler.handle(event)

            // then
            val metricData = otelTesting.metrics.find { it.name == "kqrs_command_start" }
            assertThat(metricData).isNotNull()
            val point = metricData!!.longSumData.points.first()
            assertThat(point.value).isEqualTo(3)
        }
    }

    @Nested
    inner class StopCommandEventHandlerTest {

        @Test
        fun `should increment stop counter and record duration`() = runTest {
            // given
            val handler = OpenTelemetryMetricsStopCommandEventHandler(otelTesting.openTelemetry)
            val event = StopProcessCommandEvent(
                commandName = CommandName("test.command"),
                eventTags = emptyList(),
                startProcessingAt = Instant.now().minusMillis(150)
            )

            // when
            handler.handle(event)

            // then
            val counterData = otelTesting.metrics.find { it.name == "kqrs_command_stop" }
            assertThat(counterData).isNotNull()
            val counterPoint = counterData!!.longSumData.points.first()
            assertThat(counterPoint.value).isEqualTo(1)

            val histogramData = otelTesting.metrics.find { it.name == "kqrs_command_duration" }
            assertThat(histogramData).isNotNull()
            val histogramPoint = histogramData!!.histogramData.points.first()
            assertThat(histogramPoint.count).isEqualTo(1)
        }

        @Test
        fun `should include event tags`() = runTest {
            // given
            val handler = OpenTelemetryMetricsStopCommandEventHandler(otelTesting.openTelemetry)
            val event = StopProcessCommandEvent(
                commandName = CommandName("test.command"),
                eventTags = listOf(EventTag("tenant", "acme")),
                startProcessingAt = Instant.now().minusMillis(50)
            )

            // when
            handler.handle(event)

            // then
            val counterData = otelTesting.metrics.find { it.name == "kqrs_command_stop" }
            assertThat(counterData).isNotNull()
            val counterPoint = counterData!!.longSumData.points.first()
            assertThat(counterPoint.attributes.get(AttributeKey.stringKey("command"))).isEqualTo("test.command")
            assertThat(counterPoint.attributes.get(AttributeKey.stringKey("tenant"))).isEqualTo("acme")
        }
    }

    @Nested
    inner class ErrorCommandEventHandlerTest {

        @Test
        fun `should increment error counter`() = runTest {
            // given
            val handler = OpenTelemetryMetricsErrorCommandEventHandler(otelTesting.openTelemetry)
            val event = ErrorProcessCommandEvent(
                commandName = CommandName("test.command"),
                eventTags = emptyList(),
                startProcessingAt = Instant.now().minusMillis(100),
                exception = RuntimeException("something went wrong")
            )

            // when
            handler.handle(event)

            // then
            val metricData = otelTesting.metrics.find { it.name == "kqrs_command_error" }
            assertThat(metricData).isNotNull()
            val point = metricData!!.longSumData.points.first()
            assertThat(point.value).isEqualTo(1)
            assertThat(point.attributes.get(AttributeKey.stringKey("command"))).isEqualTo("test.command")
        }

        @Test
        fun `should include event tags`() = runTest {
            // given
            val handler = OpenTelemetryMetricsErrorCommandEventHandler(otelTesting.openTelemetry)
            val event = ErrorProcessCommandEvent(
                commandName = CommandName("test.command"),
                eventTags = listOf(EventTag("order.id", "abc-123")),
                startProcessingAt = Instant.now().minusMillis(100),
                exception = IllegalStateException("invalid state")
            )

            // when
            handler.handle(event)

            // then
            val metricData = otelTesting.metrics.find { it.name == "kqrs_command_error" }
            assertThat(metricData).isNotNull()
            val point = metricData!!.longSumData.points.first()
            assertThat(point.attributes.get(AttributeKey.stringKey("command"))).isEqualTo("test.command")
            assertThat(point.attributes.get(AttributeKey.stringKey("order.id"))).isEqualTo("abc-123")
        }
    }

    @Nested
    inner class ToOpenTelemetryAttributesTest {

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
            val attributes = event.toOpenTelemetryAttributes()

            // then
            assertThat(attributes.size()).isEqualTo(3)
            assertThat(attributes.get(AttributeKey.stringKey("key1"))).isEqualTo("value1")
            assertThat(attributes.get(AttributeKey.stringKey("key2"))).isEqualTo("value2")
            assertThat(attributes.get(AttributeKey.stringKey("command"))).isEqualTo("my.command")
        }

        @Test
        fun `should return only command attribute when no event tags`() {
            // given
            val event = StartProcessCommandEvent(
                commandName = CommandName("my.command"),
                eventTags = emptyList()
            )

            // when
            val attributes = event.toOpenTelemetryAttributes()

            // then
            assertThat(attributes.size()).isEqualTo(1)
            assertThat(attributes.get(AttributeKey.stringKey("command"))).isEqualTo("my.command")
        }
    }
}
