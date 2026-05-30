package com.kormichu.kqrs.metrics.opentelemetry

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import com.kormichu.kqrs.metrics.MetricsErrorProcessQueryEventHandler
import com.kormichu.kqrs.metrics.MetricsQueryEventHandler
import com.kormichu.kqrs.metrics.MetricsStartProcessQueryEventHandler
import com.kormichu.kqrs.metrics.MetricsStopProcessQueryEventHandler
import com.kormichu.kqrs.query.ErrorProcessQueryEvent
import com.kormichu.kqrs.query.QueryEvent
import com.kormichu.kqrs.query.StartProcessQueryEvent
import com.kormichu.kqrs.query.StopProcessQueryEvent

interface OpenTelemetryMetricsQueryEventHandler<E : QueryEvent<*>> : MetricsQueryEventHandler<E>

class OpenTelemetryMetricsStartQueryEventHandler(
    openTelemetry: OpenTelemetry
) : OpenTelemetryMetricsQueryEventHandler<StartProcessQueryEvent<*>>,
    MetricsStartProcessQueryEventHandler {

    private val counter = openTelemetry.kqrsMeter()
        .counterBuilder(METRIC_QUERY_START_PROCESS)
        .build()

    override suspend fun handle(event: StartProcessQueryEvent<*>) {
        counter.add(1, event.toOpenTelemetryAttributes())
    }
}

class OpenTelemetryMetricsStopQueryEventHandler(
    openTelemetry: OpenTelemetry
) : OpenTelemetryMetricsQueryEventHandler<StopProcessQueryEvent<*>>,
    MetricsStopProcessQueryEventHandler {

    private val counter = openTelemetry.kqrsMeter()
        .counterBuilder(METRIC_QUERY_STOP_PROCESS)
        .build()

    private val histogram = openTelemetry.kqrsMeter()
        .histogramBuilder(METRIC_QUERY_DURATION)
        .setUnit("s")
        .build()

    override suspend fun handle(event: StopProcessQueryEvent<*>) {
        val attributes = event.toOpenTelemetryAttributes()
        counter.add(1, attributes)
        histogram.record(event.duration.toMillis() / 1000.0, attributes)
    }
}

class OpenTelemetryMetricsErrorQueryEventHandler(
    openTelemetry: OpenTelemetry
) : OpenTelemetryMetricsQueryEventHandler<ErrorProcessQueryEvent<*>>,
    MetricsErrorProcessQueryEventHandler {

    private val counter = openTelemetry.kqrsMeter()
        .counterBuilder(METRIC_QUERY_ERROR_PROCESS)
        .build()

    override suspend fun handle(event: ErrorProcessQueryEvent<*>) {
        counter.add(1, event.toOpenTelemetryAttributes())
    }
}

fun QueryEvent<*>.toOpenTelemetryAttributes(): Attributes {
    val builder = Attributes.builder()
    eventTags.forEach { builder.put(AttributeKey.stringKey(it.key), it.value) }
    builder.put(AttributeKey.stringKey("query"), queryName.value)
    return builder.build()
}

private const val METRIC_QUERY_START_PROCESS = "kqrs_query_start"
private const val METRIC_QUERY_STOP_PROCESS = "kqrs_query_stop"
private const val METRIC_QUERY_ERROR_PROCESS = "kqrs_query_error"
private const val METRIC_QUERY_DURATION = "kqrs_query_duration"
