package com.kormichu.kqrs.prometheus

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import com.kormichu.kqrs.command.CommandEvent
import com.kormichu.kqrs.command.ErrorProcessCommandEvent
import com.kormichu.kqrs.command.StartProcessCommandEvent
import com.kormichu.kqrs.command.StopProcessCommandEvent
import com.kormichu.kqrs.metrics.MetricsErrorProcessCommandEventHandler
import com.kormichu.kqrs.metrics.MetricsStartProcessCommandEventHandler
import com.kormichu.kqrs.metrics.MetricsStopProcessCommandEventHandler
import kotlin.collections.plus

class PrometheusMetricsStartCommandEventHandler(
    private val meterRegistry: MeterRegistry
): MetricsStartProcessCommandEventHandler {
    override suspend fun handle(event: StartProcessCommandEvent<*>) {
        meterRegistry.counter(
            METRIC_COMMAND_START_PROCESS,
            event.toMicrometerTags()
        ).increment()
    }
}

class PrometheusMetricsStopCommandEventHandler(
    private val meterRegistry: MeterRegistry
): MetricsStopProcessCommandEventHandler {
    override suspend fun handle(event: StopProcessCommandEvent<*>) {
        val tags = event.toMicrometerTags()
        meterRegistry.counter(
            METRIC_COMMAND_STOP_PROCESS,
            tags
        ).increment()
        meterRegistry.timer(
            METRIC_COMMAND_DURATION,
            tags
        ).record(event.duration)
    }
}

class PrometheusMetricsErrorCommandEventHandler(
    private val meterRegistry: MeterRegistry
): MetricsErrorProcessCommandEventHandler {
    override suspend fun handle(event: ErrorProcessCommandEvent<*>) {
        meterRegistry.counter(
            METRIC_COMMAND_ERROR_PROCESS,
            event.toMicrometerTags()
        ).increment()
    }
}

fun CommandEvent<*>.toMicrometerTags(): List<Tag> =
    eventTags.map { Tag.of(it.key, it.value) } +
        listOf(Tag.of("command", commandName.value))

private const val METRIC_COMMAND_START_PROCESS = "cqrs_command_start"
private const val METRIC_COMMAND_STOP_PROCESS = "cqrs_command_stop"
private const val METRIC_COMMAND_ERROR_PROCESS = "cqrs_command_error"
private const val METRIC_COMMAND_DURATION = "cqrs_command_duration"
