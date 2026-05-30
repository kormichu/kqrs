package com.kormichu.kqrs.opentelemetry.autoconfigure

import com.kormichu.kqrs.logger.logger
import com.kormichu.kqrs.metrics.MetricsErrorProcessCommandEventHandler
import com.kormichu.kqrs.metrics.MetricsErrorProcessQueryEventHandler
import com.kormichu.kqrs.metrics.MetricsStartProcessCommandEventHandler
import com.kormichu.kqrs.metrics.MetricsStartProcessQueryEventHandler
import com.kormichu.kqrs.metrics.MetricsStopProcessCommandEventHandler
import com.kormichu.kqrs.metrics.MetricsStopProcessQueryEventHandler
import com.kormichu.kqrs.metrics.opentelemetry.OpenTelemetryMetricsErrorCommandEventHandler
import com.kormichu.kqrs.metrics.opentelemetry.OpenTelemetryMetricsErrorQueryEventHandler
import com.kormichu.kqrs.metrics.opentelemetry.OpenTelemetryMetricsStartCommandEventHandler
import com.kormichu.kqrs.metrics.opentelemetry.OpenTelemetryMetricsStartQueryEventHandler
import com.kormichu.kqrs.metrics.opentelemetry.OpenTelemetryMetricsStopCommandEventHandler
import com.kormichu.kqrs.metrics.opentelemetry.OpenTelemetryMetricsStopQueryEventHandler
import com.kormichu.kqrs.spring.boot.autoconfigure.SpringKqrsProperties
import io.opentelemetry.api.OpenTelemetry
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@AutoConfiguration
@Configuration
@EnableConfigurationProperties(SpringKqrsProperties::class)
@ConditionalOnBean(OpenTelemetry::class)
class SpringKqrsOpenTelemetryConfiguration {
    private val logger by logger()

    @Bean
    @ConditionalOnProperty("kqrs.metrics.opentelemetry.enabled")
    fun openTelemetryMetricsStartCommandEventHandler(
        openTelemetry: OpenTelemetry
    ): MetricsStartProcessCommandEventHandler {
        logger.debug("Using OpenTelemetryMetricsStartCommandEventHandler")
        return OpenTelemetryMetricsStartCommandEventHandler(openTelemetry)
    }

    @Bean
    @ConditionalOnProperty("kqrs.metrics.opentelemetry.enabled")
    fun openTelemetryMetricsStopCommandEventHandler(
        openTelemetry: OpenTelemetry
    ): MetricsStopProcessCommandEventHandler {
        logger.debug("Using OpenTelemetryMetricsStopCommandEventHandler")
        return OpenTelemetryMetricsStopCommandEventHandler(openTelemetry)
    }

    @Bean
    @ConditionalOnProperty("kqrs.metrics.opentelemetry.enabled")
    fun openTelemetryMetricsErrorCommandEventHandler(
        openTelemetry: OpenTelemetry
    ): MetricsErrorProcessCommandEventHandler {
        logger.debug("Using OpenTelemetryMetricsErrorCommandEventHandler")
        return OpenTelemetryMetricsErrorCommandEventHandler(openTelemetry)
    }

    @Bean
    @ConditionalOnProperty("kqrs.metrics.opentelemetry.enabled")
    fun openTelemetryMetricsStartQueryEventHandler(
        openTelemetry: OpenTelemetry
    ): MetricsStartProcessQueryEventHandler {
        logger.debug("Using OpenTelemetryMetricsStartQueryEventHandler")
        return OpenTelemetryMetricsStartQueryEventHandler(openTelemetry)
    }

    @Bean
    @ConditionalOnProperty("kqrs.metrics.opentelemetry.enabled")
    fun openTelemetryMetricsStopQueryEventHandler(
        openTelemetry: OpenTelemetry
    ): MetricsStopProcessQueryEventHandler {
        logger.debug("Using OpenTelemetryMetricsStopQueryEventHandler")
        return OpenTelemetryMetricsStopQueryEventHandler(openTelemetry)
    }

    @Bean
    @ConditionalOnProperty("kqrs.metrics.opentelemetry.enabled")
    fun openTelemetryMetricsErrorQueryEventHandler(
        openTelemetry: OpenTelemetry
    ): MetricsErrorProcessQueryEventHandler {
        logger.debug("Using OpenTelemetryMetricsErrorQueryEventHandler")
        return OpenTelemetryMetricsErrorQueryEventHandler(openTelemetry)
    }
}
