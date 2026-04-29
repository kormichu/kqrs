package com.kormichu.kqrs.metrics.prometheus

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import com.kormichu.kqrs.metrics.MetricsErrorProcessQueryEventHandler
import com.kormichu.kqrs.metrics.MetricsStartProcessQueryEventHandler
import com.kormichu.kqrs.metrics.MetricsStopProcessQueryEventHandler
import com.kormichu.kqrs.query.ErrorProcessQueryEvent
import com.kormichu.kqrs.query.QueryEvent
import com.kormichu.kqrs.query.StartProcessQueryEvent
import com.kormichu.kqrs.query.StopProcessQueryEvent
import kotlin.collections.plus

class PrometheusMetricsStartQueryEventHandler(
    private val meterRegistry: MeterRegistry
): MetricsStartProcessQueryEventHandler {
    override suspend fun handle(event: StartProcessQueryEvent<*>) {
        meterRegistry.counter(
            METRIC_QUERY_START_PROCESS,
            event.toMicrometerTags()
        ).increment()
    }
}

class PrometheusMetricsStopQueryEventHandler(
    private val meterRegistry: MeterRegistry
): MetricsStopProcessQueryEventHandler {
    override suspend fun handle(event: StopProcessQueryEvent<*>) {
        val tags = event.toMicrometerTags()
        meterRegistry.counter(
            METRIC_QUERY_STOP_PROCESS,
            tags
        ).increment()
        meterRegistry.timer(
            METRIC_QUERY_DURATION,
            tags
        ).record(event.duration)
    }
}

class PrometheusMetricsErrorQueryEventHandler(
    private val meterRegistry: MeterRegistry
): MetricsErrorProcessQueryEventHandler {
    override suspend fun handle(event: ErrorProcessQueryEvent<*>) {
        meterRegistry.counter(
            METRIC_QUERY_ERROR_PROCESS,
            event.toMicrometerTags()
        ).increment()
    }
}

fun QueryEvent<*>.toMicrometerTags(): List<Tag> =
    eventTags.map { Tag.of(it.key, it.value) } +
        listOf(Tag.of("query", queryName.value))

private const val METRIC_QUERY_START_PROCESS = "cqrs_query_start"
private const val METRIC_QUERY_STOP_PROCESS = "cqrs_query_stop"
private const val METRIC_QUERY_ERROR_PROCESS = "cqrs_query_error"
private const val METRIC_QUERY_DURATION = "cqrs_query_duration"
