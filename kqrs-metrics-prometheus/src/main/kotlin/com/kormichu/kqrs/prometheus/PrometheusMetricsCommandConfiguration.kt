package com.kormichu.kqrs.prometheus

import io.micrometer.core.instrument.MeterRegistry
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import com.kormichu.kqrs.logger.logger
import com.kormichu.kqrs.metrics.MetricsErrorProcessCommandEventHandler
import com.kormichu.kqrs.metrics.MetricsStartProcessCommandEventHandler
import com.kormichu.kqrs.metrics.MetricsStopProcessCommandEventHandler
import com.kormichu.kqrs.infrastructure.spring.SpringCqrsProperties

@Configuration
@ConditionalOnBean(MeterRegistry::class)
@AutoConfiguration
@EnableConfigurationProperties(SpringCqrsProperties::class)
class PrometheusMetricsCommandConfiguration {
    private val logger by logger()

    @ConditionalOnProperty("kqrs.metrics.prometheus.enabled")
    @Bean
    fun prometheusMetricsStartCommandEventHandler(
        meterRegistry: MeterRegistry
    ): MetricsStartProcessCommandEventHandler  {
        logger.debug("Using PrometheusMetricsStartCommandEventHandler")
        return PrometheusMetricsStartCommandEventHandler(meterRegistry)
    }

    @ConditionalOnProperty("kqrs.metrics.prometheus.enabled")
    @Bean
    fun prometheusMetricsStopCommandEventHandler(
        meterRegistry: MeterRegistry
    ): MetricsStopProcessCommandEventHandler {
        logger.debug("Using PrometheusMetricsStopCommandEventHandler")
        return PrometheusMetricsStopCommandEventHandler(meterRegistry)
    }

    @ConditionalOnProperty("kqrs.metrics.prometheus.enabled")
    @Bean
    fun prometheusMetricsErrorCommandEventHandler(
        meterRegistry: MeterRegistry
    ): MetricsErrorProcessCommandEventHandler {
        logger.debug("Using PrometheusMetricsErrorCommandEventHandler")
        return PrometheusMetricsErrorCommandEventHandler(meterRegistry)
    }
}
