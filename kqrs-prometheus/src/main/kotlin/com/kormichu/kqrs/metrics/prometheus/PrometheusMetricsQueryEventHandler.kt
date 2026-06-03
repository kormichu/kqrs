package com.kormichu.kqrs.metrics.prometheus

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import com.kormichu.kqrs.metrics.MetricsErrorProcessQueryEventHandler
import com.kormichu.kqrs.metrics.MetricsQueryEventHandler
import com.kormichu.kqrs.metrics.MetricsStartProcessQueryEventHandler
import com.kormichu.kqrs.metrics.MetricsStopProcessQueryEventHandler
import com.kormichu.kqrs.metrics.MetricsValidationFailedQueryEventHandler
import com.kormichu.kqrs.query.ErrorProcessQueryEvent
import com.kormichu.kqrs.query.QueryEvent
import com.kormichu.kqrs.query.StartProcessQueryEvent
import com.kormichu.kqrs.query.StopProcessQueryEvent
import com.kormichu.kqrs.query.ValidationFailedQueryEvent
import kotlin.collections.plus

interface PrometheusMetricsQueryEventHandler<E : QueryEvent<*>>: MetricsQueryEventHandler<E>

class PrometheusMetricsStartQueryEventHandler(
    private val meterRegistry: MeterRegistry
):  PrometheusMetricsQueryEventHandler<StartProcessQueryEvent<*>>,
    MetricsStartProcessQueryEventHandler {
    override suspend fun handle(event: StartProcessQueryEvent<*>) {
        meterRegistry.counter(
            METRIC_QUERY_START_PROCESS,
            event.toMicrometerTags()
        ).increment()
    }
}

class PrometheusMetricsStopQueryEventHandler(
    private val meterRegistry: MeterRegistry
):  PrometheusMetricsQueryEventHandler<StopProcessQueryEvent<*>>,
    MetricsStopProcessQueryEventHandler {
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
):  PrometheusMetricsQueryEventHandler<ErrorProcessQueryEvent<*>>,
    MetricsErrorProcessQueryEventHandler {
    override suspend fun handle(event: ErrorProcessQueryEvent<*>) {
        meterRegistry.counter(
            METRIC_QUERY_ERROR_PROCESS,
            event.toMicrometerTags()
        ).increment()
    }
}

class PrometheusMetricsValidationFailedQueryEventHandler(
    private val meterRegistry: MeterRegistry
):  PrometheusMetricsQueryEventHandler<ValidationFailedQueryEvent<*>>,
    MetricsValidationFailedQueryEventHandler {
    override suspend fun handle(event: ValidationFailedQueryEvent<*>) {
        meterRegistry.counter(
            METRIC_QUERY_VALIDATION_FAILED_PROCESS,
            event.toMicrometerTags()
        ).increment()
    }
}

fun QueryEvent<*>.toMicrometerTags(): List<Tag> =
    eventTags.map { Tag.of(it.key, it.value) } +
        listOf(Tag.of("query", queryName.value))

private const val METRIC_QUERY_START_PROCESS = "kqrs_query_start"
private const val METRIC_QUERY_STOP_PROCESS = "kqrs_query_stop"
private const val METRIC_QUERY_ERROR_PROCESS = "kqrs_query_error"
private const val METRIC_QUERY_VALIDATION_FAILED_PROCESS = "kqrs_query_validation_failed"
private const val METRIC_QUERY_DURATION = "kqrs_query_duration"
