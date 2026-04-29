package com.kormichu.kqrs.spring.prometheus

import io.micrometer.core.instrument.MeterRegistry
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import com.kormichu.kqrs.logger.logger
import com.kormichu.kqrs.metrics.MetricsErrorProcessQueryEventHandler
import com.kormichu.kqrs.metrics.MetricsStartProcessQueryEventHandler
import com.kormichu.kqrs.metrics.MetricsStopProcessQueryEventHandler
import com.kormichu.kqrs.metrics.prometheus.PrometheusMetricsErrorQueryEventHandler
import com.kormichu.kqrs.metrics.prometheus.PrometheusMetricsStartQueryEventHandler
import com.kormichu.kqrs.metrics.prometheus.PrometheusMetricsStopQueryEventHandler

@Configuration
@ConditionalOnBean(MeterRegistry::class)
@AutoConfiguration
class PrometheusMetricsQueryConfiguration {
    private val logger by logger()

    @ConditionalOnProperty("kqrs.metrics.prometheus.enabled")
    @Bean
    fun prometheusMetricsStartQueryEventHandler(
        meterRegistry: MeterRegistry
    ): MetricsStartProcessQueryEventHandler {
        logger.debug("Using PrometheusMetricsStartQueryEventHandler")
        return PrometheusMetricsStartQueryEventHandler(meterRegistry)
    }

    @ConditionalOnProperty("kqrs.metrics.prometheus.enabled")
    @Bean
    fun prometheusMetricsStopQueryEventHandler(
        meterRegistry: MeterRegistry
    ): MetricsStopProcessQueryEventHandler {
        logger.debug("Using PrometheusMetricsStopQueryEventHandler")
        return PrometheusMetricsStopQueryEventHandler(meterRegistry)
    }

    @ConditionalOnProperty("kqrs.metrics.prometheus.enabled")
    @Bean
    fun prometheusMetricsErrorQueryEventHandler(
        meterRegistry: MeterRegistry
    ): MetricsErrorProcessQueryEventHandler {
        logger.debug("Using PrometheusMetricsErrorQueryEventHandler")
        return PrometheusMetricsErrorQueryEventHandler(meterRegistry)
    }
}
