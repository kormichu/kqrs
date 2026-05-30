package com.kormichu.kqrs.metrics.opentelemetry

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.metrics.Meter
import com.kormichu.kqrs.command.CommandEvent
import com.kormichu.kqrs.command.ErrorProcessCommandEvent
import com.kormichu.kqrs.command.StartProcessCommandEvent
import com.kormichu.kqrs.command.StopProcessCommandEvent
import com.kormichu.kqrs.metrics.MetricsCommandEventHandler
import com.kormichu.kqrs.metrics.MetricsErrorProcessCommandEventHandler
import com.kormichu.kqrs.metrics.MetricsStartProcessCommandEventHandler
import com.kormichu.kqrs.metrics.MetricsStopProcessCommandEventHandler

interface OpenTelemetryMetricsCommandEventHandler<E : CommandEvent<*>> : MetricsCommandEventHandler<E>

class OpenTelemetryMetricsStartCommandEventHandler(
    openTelemetry: OpenTelemetry
) : OpenTelemetryMetricsCommandEventHandler<StartProcessCommandEvent<*>>,
    MetricsStartProcessCommandEventHandler {

    private val counter = openTelemetry.kqrsMeter()
        .counterBuilder(METRIC_COMMAND_START_PROCESS)
        .build()

    override suspend fun handle(event: StartProcessCommandEvent<*>) {
        counter.add(1, event.toOpenTelemetryAttributes())
    }
}

class OpenTelemetryMetricsStopCommandEventHandler(
    openTelemetry: OpenTelemetry
) : OpenTelemetryMetricsCommandEventHandler<StopProcessCommandEvent<*>>,
    MetricsStopProcessCommandEventHandler {

    private val counter = openTelemetry.kqrsMeter()
        .counterBuilder(METRIC_COMMAND_STOP_PROCESS)
        .build()

    private val histogram = openTelemetry.kqrsMeter()
        .histogramBuilder(METRIC_COMMAND_DURATION)
        .setUnit("s")
        .build()

    override suspend fun handle(event: StopProcessCommandEvent<*>) {
        val attributes = event.toOpenTelemetryAttributes()
        counter.add(1, attributes)
        histogram.record(event.duration.toMillis() / 1000.0, attributes)
    }
}

class OpenTelemetryMetricsErrorCommandEventHandler(
    openTelemetry: OpenTelemetry
) : OpenTelemetryMetricsCommandEventHandler<ErrorProcessCommandEvent<*>>,
    MetricsErrorProcessCommandEventHandler {

    private val counter = openTelemetry.kqrsMeter()
        .counterBuilder(METRIC_COMMAND_ERROR_PROCESS)
        .build()

    override suspend fun handle(event: ErrorProcessCommandEvent<*>) {
        counter.add(1, event.toOpenTelemetryAttributes())
    }
}

fun CommandEvent<*>.toOpenTelemetryAttributes(): Attributes {
    val builder = Attributes.builder()
    eventTags.forEach { builder.put(AttributeKey.stringKey(it.key), it.value) }
    builder.put(AttributeKey.stringKey("command"), commandName.value)
    return builder.build()
}

internal fun OpenTelemetry.kqrsMeter(): Meter =
    meterProvider.meterBuilder(INSTRUMENTATION_SCOPE_NAME).build()

private const val INSTRUMENTATION_SCOPE_NAME = "com.kormichu.kqrs"
private const val METRIC_COMMAND_START_PROCESS = "kqrs_command_start"
private const val METRIC_COMMAND_STOP_PROCESS = "kqrs_command_stop"
private const val METRIC_COMMAND_ERROR_PROCESS = "kqrs_command_error"
private const val METRIC_COMMAND_DURATION = "kqrs_command_duration"
