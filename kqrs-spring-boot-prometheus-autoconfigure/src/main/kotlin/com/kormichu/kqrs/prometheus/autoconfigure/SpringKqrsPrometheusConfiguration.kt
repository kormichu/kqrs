package com.kormichu.kqrs.prometheus.autoconfigure

import com.kormichu.kqrs.logger.logger
import com.kormichu.kqrs.metrics.MetricsErrorProcessCommandEventHandler
import com.kormichu.kqrs.metrics.MetricsErrorProcessQueryEventHandler
import com.kormichu.kqrs.metrics.MetricsStartProcessCommandEventHandler
import com.kormichu.kqrs.metrics.MetricsStartProcessQueryEventHandler
import com.kormichu.kqrs.metrics.MetricsStopProcessCommandEventHandler
import com.kormichu.kqrs.metrics.MetricsStopProcessQueryEventHandler
import com.kormichu.kqrs.metrics.prometheus.PrometheusMetricsErrorCommandEventHandler
import com.kormichu.kqrs.metrics.prometheus.PrometheusMetricsErrorQueryEventHandler
import com.kormichu.kqrs.metrics.prometheus.PrometheusMetricsStartCommandEventHandler
import com.kormichu.kqrs.metrics.prometheus.PrometheusMetricsStartQueryEventHandler
import com.kormichu.kqrs.metrics.prometheus.PrometheusMetricsStopCommandEventHandler
import com.kormichu.kqrs.metrics.prometheus.PrometheusMetricsStopQueryEventHandler
import com.kormichu.kqrs.spring.boot.autoconfigure.SpringKqrsProperties
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@AutoConfiguration
@Configuration
@EnableConfigurationProperties(SpringKqrsProperties::class)
@ConditionalOnBean(MeterRegistry::class)
class SpringKqrsPrometheusConfiguration {
    private val logger by logger()

    @Bean
    @ConditionalOnProperty("kqrs.metrics.prometheus.enabled")
    fun prometheusMetricsStartCommandEventHandler(
        meterRegistry: MeterRegistry
    ): MetricsStartProcessCommandEventHandler {
        logger.debug("Using PrometheusMetricsStartCommandEventHandler")
        return PrometheusMetricsStartCommandEventHandler(meterRegistry)
    }

    @Bean
    @ConditionalOnProperty("kqrs.metrics.prometheus.enabled")
    fun prometheusMetricsStopCommandEventHandler(
        meterRegistry: MeterRegistry
    ): MetricsStopProcessCommandEventHandler {
        logger.debug("Using PrometheusMetricsStopCommandEventHandler")
        return PrometheusMetricsStopCommandEventHandler(meterRegistry)
    }

    @Bean
    @ConditionalOnProperty("kqrs.metrics.prometheus.enabled")
    fun prometheusMetricsErrorCommandEventHandler(
        meterRegistry: MeterRegistry
    ): MetricsErrorProcessCommandEventHandler {
        logger.debug("Using PrometheusMetricsErrorCommandEventHandler")
        return PrometheusMetricsErrorCommandEventHandler(meterRegistry)
    }

    @Bean
    @ConditionalOnProperty("kqrs.metrics.prometheus.enabled")
    fun prometheusMetricsStartQueryEventHandler(
        meterRegistry: MeterRegistry
    ): MetricsStartProcessQueryEventHandler {
        logger.debug("Using PrometheusMetricsStartQueryEventHandler")
        return PrometheusMetricsStartQueryEventHandler(meterRegistry)
    }

    @Bean
    @ConditionalOnProperty("kqrs.metrics.prometheus.enabled")
    fun prometheusMetricsStopQueryEventHandler(
        meterRegistry: MeterRegistry
    ): MetricsStopProcessQueryEventHandler {
        logger.debug("Using PrometheusMetricsStopQueryEventHandler")
        return PrometheusMetricsStopQueryEventHandler(meterRegistry)
    }

    @Bean
    @ConditionalOnProperty("kqrs.metrics.prometheus.enabled")
    fun prometheusMetricsErrorQueryEventHandler(
        meterRegistry: MeterRegistry
    ): MetricsErrorProcessQueryEventHandler {
        logger.debug("Using PrometheusMetricsErrorQueryEventHandler")
        return PrometheusMetricsErrorQueryEventHandler(meterRegistry)
    }
}
