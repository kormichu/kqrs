package com.kormichu.kqrs.metrics.prometheus

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import com.kormichu.kqrs.command.CommandEvent
import com.kormichu.kqrs.command.ErrorProcessCommandEvent
import com.kormichu.kqrs.command.StartProcessCommandEvent
import com.kormichu.kqrs.command.StopProcessCommandEvent
import com.kormichu.kqrs.metrics.MetricsCommandEventHandler
import com.kormichu.kqrs.metrics.MetricsErrorProcessCommandEventHandler
import com.kormichu.kqrs.metrics.MetricsStartProcessCommandEventHandler
import com.kormichu.kqrs.metrics.MetricsStopProcessCommandEventHandler
import kotlin.collections.plus

interface PrometheusMetricsCommandEventHandler<E : CommandEvent<*>>: MetricsCommandEventHandler<E>

class PrometheusMetricsStartCommandEventHandler(
    private val meterRegistry: MeterRegistry
):  PrometheusMetricsCommandEventHandler<StartProcessCommandEvent<*>>,
    MetricsStartProcessCommandEventHandler {
    override suspend fun handle(event: StartProcessCommandEvent<*>) {
        meterRegistry.counter(
            METRIC_COMMAND_START_PROCESS,
            event.toMicrometerTags()
        ).increment()
    }
}

class PrometheusMetricsStopCommandEventHandler(
    private val meterRegistry: MeterRegistry
):  PrometheusMetricsCommandEventHandler<StopProcessCommandEvent<*>>,
    MetricsStopProcessCommandEventHandler {
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
):  PrometheusMetricsCommandEventHandler<ErrorProcessCommandEvent<*>>,
    MetricsErrorProcessCommandEventHandler {
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

private const val METRIC_COMMAND_START_PROCESS = "kqrs_command_start"
private const val METRIC_COMMAND_STOP_PROCESS = "kqrs_command_stop"
private const val METRIC_COMMAND_ERROR_PROCESS = "kqrs_command_error"
private const val METRIC_COMMAND_DURATION = "kqrs_command_duration"
