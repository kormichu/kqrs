package com.kormichu.kqrs.prometheus.autoconfigure

import assertk.assertThat
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import com.kormichu.kqrs.metrics.MetricsErrorProcessCommandEventHandler
import com.kormichu.kqrs.metrics.MetricsErrorProcessQueryEventHandler
import com.kormichu.kqrs.metrics.MetricsStartProcessCommandEventHandler
import com.kormichu.kqrs.metrics.MetricsStartProcessQueryEventHandler
import com.kormichu.kqrs.metrics.MetricsStopProcessCommandEventHandler
import com.kormichu.kqrs.metrics.MetricsStopProcessQueryEventHandler
import com.kormichu.kqrs.metrics.MetricsValidationFailedCommandEventHandler
import com.kormichu.kqrs.metrics.MetricsValidationFailedQueryEventHandler
import com.kormichu.kqrs.metrics.prometheus.PrometheusMetricsErrorCommandEventHandler
import com.kormichu.kqrs.metrics.prometheus.PrometheusMetricsErrorQueryEventHandler
import com.kormichu.kqrs.metrics.prometheus.PrometheusMetricsStartCommandEventHandler
import com.kormichu.kqrs.metrics.prometheus.PrometheusMetricsStartQueryEventHandler
import com.kormichu.kqrs.metrics.prometheus.PrometheusMetricsStopCommandEventHandler
import com.kormichu.kqrs.metrics.prometheus.PrometheusMetricsStopQueryEventHandler
import com.kormichu.kqrs.metrics.prometheus.PrometheusMetricsValidationFailedCommandEventHandler
import com.kormichu.kqrs.metrics.prometheus.PrometheusMetricsValidationFailedQueryEventHandler
import com.kormichu.kqrs.spring.boot.autoconfigure.SpringKqrsConfiguration
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.getBean
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner

class SpringKqrsPrometheusConfigurationTest {

    private val contextRunner = ApplicationContextRunner()
        .withConfiguration(
            AutoConfigurations.of(
                SpringKqrsConfiguration::class.java,
                SpringKqrsPrometheusConfiguration::class.java
            )
        )

    // region Command handlers — enabled

    @Test
    fun `should register MetricsStartProcessCommandEventHandler bean when enabled and MeterRegistry present`() {
        contextRunner
            .withBean(MeterRegistry::class.java, { SimpleMeterRegistry() })
            .withPropertyValues("kqrs.metrics.prometheus.enabled=true")
            .run { context ->
                assertThat(context.getBean<MetricsStartProcessCommandEventHandler>()).isNotNull()
                assertThat(context.getBean<MetricsStartProcessCommandEventHandler>())
                    .isInstanceOf(PrometheusMetricsStartCommandEventHandler::class)
            }
    }

    @Test
    fun `should register MetricsStopProcessCommandEventHandler bean when enabled and MeterRegistry present`() {
        contextRunner
            .withBean(MeterRegistry::class.java, { SimpleMeterRegistry() })
            .withPropertyValues("kqrs.metrics.prometheus.enabled=true")
            .run { context ->
                assertThat(context.getBean<MetricsStopProcessCommandEventHandler>()).isNotNull()
                assertThat(context.getBean<MetricsStopProcessCommandEventHandler>())
                    .isInstanceOf(PrometheusMetricsStopCommandEventHandler::class)
            }
    }

    @Test
    fun `should register MetricsErrorProcessCommandEventHandler bean when enabled and MeterRegistry present`() {
        contextRunner
            .withBean(MeterRegistry::class.java, { SimpleMeterRegistry() })
            .withPropertyValues("kqrs.metrics.prometheus.enabled=true")
            .run { context ->
                assertThat(context.getBean<MetricsErrorProcessCommandEventHandler>()).isNotNull()
                assertThat(context.getBean<MetricsErrorProcessCommandEventHandler>())
                    .isInstanceOf(PrometheusMetricsErrorCommandEventHandler::class)
            }
    }

    @Test
    fun `should register MetricsValidationFailedCommandEventHandler bean when enabled and MeterRegistry present`() {
        contextRunner
            .withBean(MeterRegistry::class.java, { SimpleMeterRegistry() })
            .withPropertyValues("kqrs.metrics.prometheus.enabled=true")
            .run { context ->
                assertThat(context.getBean<MetricsValidationFailedCommandEventHandler>()).isNotNull()
                assertThat(context.getBean<MetricsValidationFailedCommandEventHandler>())
                    .isInstanceOf(PrometheusMetricsValidationFailedCommandEventHandler::class)
            }
    }

    // endregion

    // region Query handlers — enabled

    @Test
    fun `should register MetricsStartProcessQueryEventHandler bean when enabled and MeterRegistry present`() {
        contextRunner
            .withBean(MeterRegistry::class.java, { SimpleMeterRegistry() })
            .withPropertyValues("kqrs.metrics.prometheus.enabled=true")
            .run { context ->
                assertThat(context.getBean<MetricsStartProcessQueryEventHandler>()).isNotNull()
                assertThat(context.getBean<MetricsStartProcessQueryEventHandler>())
                    .isInstanceOf(PrometheusMetricsStartQueryEventHandler::class)
            }
    }

    @Test
    fun `should register MetricsStopProcessQueryEventHandler bean when enabled and MeterRegistry present`() {
        contextRunner
            .withBean(MeterRegistry::class.java, { SimpleMeterRegistry() })
            .withPropertyValues("kqrs.metrics.prometheus.enabled=true")
            .run { context ->
                assertThat(context.getBean<MetricsStopProcessQueryEventHandler>()).isNotNull()
                assertThat(context.getBean<MetricsStopProcessQueryEventHandler>())
                    .isInstanceOf(PrometheusMetricsStopQueryEventHandler::class)
            }
    }

    @Test
    fun `should register MetricsErrorProcessQueryEventHandler bean when enabled and MeterRegistry present`() {
        contextRunner
            .withBean(MeterRegistry::class.java, { SimpleMeterRegistry() })
            .withPropertyValues("kqrs.metrics.prometheus.enabled=true")
            .run { context ->
                assertThat(context.getBean<MetricsErrorProcessQueryEventHandler>()).isNotNull()
                assertThat(context.getBean<MetricsErrorProcessQueryEventHandler>())
                    .isInstanceOf(PrometheusMetricsErrorQueryEventHandler::class)
            }
    }

    @Test
    fun `should register MetricsValidationFailedQueryEventHandler bean when enabled and MeterRegistry present`() {
        contextRunner
            .withBean(MeterRegistry::class.java, { SimpleMeterRegistry() })
            .withPropertyValues("kqrs.metrics.prometheus.enabled=true")
            .run { context ->
                assertThat(context.getBean<MetricsValidationFailedQueryEventHandler>()).isNotNull()
                assertThat(context.getBean<MetricsValidationFailedQueryEventHandler>())
                    .isInstanceOf(PrometheusMetricsValidationFailedQueryEventHandler::class)
            }
    }

    // endregion

    // region Command handlers — disabled / absent

    @Test
    fun `should not register command handler beans when prometheus disabled`() {
        contextRunner
            .withBean(MeterRegistry::class.java, { SimpleMeterRegistry() })
            .withPropertyValues("kqrs.metrics.prometheus.enabled=false")
            .run { context ->
                assert(!context.containsBean("prometheusMetricsStartCommandEventHandler"))
                assert(!context.containsBean("prometheusMetricsStopCommandEventHandler"))
                assert(!context.containsBean("prometheusMetricsErrorCommandEventHandler"))
                assert(!context.containsBean("prometheusMetricsValidationFailedCommandEventHandler"))
            }
    }

    @Test
    fun `should not register command handler beans when MeterRegistry is absent`() {
        contextRunner
            .withPropertyValues("kqrs.metrics.prometheus.enabled=true")
            .run { context ->
                assert(!context.containsBean("prometheusMetricsStartCommandEventHandler"))
                assert(!context.containsBean("prometheusMetricsStopCommandEventHandler"))
                assert(!context.containsBean("prometheusMetricsErrorCommandEventHandler"))
                assert(!context.containsBean("prometheusMetricsValidationFailedCommandEventHandler"))
            }
    }

    @Test
    fun `should not register command handler beans when both disabled and MeterRegistry absent`() {
        contextRunner
            .withPropertyValues("kqrs.metrics.prometheus.enabled=false")
            .run { context ->
                assert(!context.containsBean("prometheusMetricsStartCommandEventHandler"))
                assert(!context.containsBean("prometheusMetricsStopCommandEventHandler"))
                assert(!context.containsBean("prometheusMetricsErrorCommandEventHandler"))
                assert(!context.containsBean("prometheusMetricsValidationFailedCommandEventHandler"))
            }
    }

    // endregion

    // region Query handlers — disabled / absent

    @Test
    fun `should not register query handler beans when prometheus disabled`() {
        contextRunner
            .withBean(MeterRegistry::class.java, { SimpleMeterRegistry() })
            .withPropertyValues("kqrs.metrics.prometheus.enabled=false")
            .run { context ->
                assert(!context.containsBean("prometheusMetricsStartQueryEventHandler"))
                assert(!context.containsBean("prometheusMetricsStopQueryEventHandler"))
                assert(!context.containsBean("prometheusMetricsErrorQueryEventHandler"))
                assert(!context.containsBean("prometheusMetricsValidationFailedQueryEventHandler"))
            }
    }

    @Test
    fun `should not register query handler beans when MeterRegistry is absent`() {
        contextRunner
            .withPropertyValues("kqrs.metrics.prometheus.enabled=true")
            .run { context ->
                assert(!context.containsBean("prometheusMetricsStartQueryEventHandler"))
                assert(!context.containsBean("prometheusMetricsStopQueryEventHandler"))
                assert(!context.containsBean("prometheusMetricsErrorQueryEventHandler"))
                assert(!context.containsBean("prometheusMetricsValidationFailedQueryEventHandler"))
            }
    }

    @Test
    fun `should not register query handler beans when both disabled and MeterRegistry absent`() {
        contextRunner
            .withPropertyValues("kqrs.metrics.prometheus.enabled=false")
            .run { context ->
                assert(!context.containsBean("prometheusMetricsStartQueryEventHandler"))
                assert(!context.containsBean("prometheusMetricsStopQueryEventHandler"))
                assert(!context.containsBean("prometheusMetricsErrorQueryEventHandler"))
                assert(!context.containsBean("prometheusMetricsValidationFailedQueryEventHandler"))
            }
    }

    // endregion

    // region Bean names

    @Test
    fun `should register beans with correct names when enabled`() {
        contextRunner
            .withBean(MeterRegistry::class.java, { SimpleMeterRegistry() })
            .withPropertyValues("kqrs.metrics.prometheus.enabled=true")
            .run { context ->
                assert(context.containsBean("prometheusMetricsStartCommandEventHandler"))
                assert(context.containsBean("prometheusMetricsStopCommandEventHandler"))
                assert(context.containsBean("prometheusMetricsErrorCommandEventHandler"))
                assert(context.containsBean("prometheusMetricsValidationFailedCommandEventHandler"))
                assert(context.containsBean("prometheusMetricsStartQueryEventHandler"))
                assert(context.containsBean("prometheusMetricsStopQueryEventHandler"))
                assert(context.containsBean("prometheusMetricsErrorQueryEventHandler"))
                assert(context.containsBean("prometheusMetricsValidationFailedQueryEventHandler"))
            }
    }

    // endregion
}

