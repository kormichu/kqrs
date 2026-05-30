package com.kormichu.kqrs.opentelemetry.autoconfigure

import assertk.assertThat
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
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
import com.kormichu.kqrs.spring.boot.autoconfigure.SpringKqrsConfiguration
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.beans.factory.getBean
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner

class SpringKqrsOpenTelemetryConfigurationTest {

    companion object {
        @JvmField
        @RegisterExtension
        val otelTesting: OpenTelemetryExtension = OpenTelemetryExtension.create()
    }

    private val contextRunner = ApplicationContextRunner()
        .withConfiguration(
            AutoConfigurations.of(
                SpringKqrsConfiguration::class.java,
                SpringKqrsOpenTelemetryConfiguration::class.java
            )
        )

    // region Command handlers — enabled

    @Test
    fun `should register MetricsStartProcessCommandEventHandler bean when enabled and OpenTelemetry present`() {
        contextRunner
            .withBean(OpenTelemetry::class.java, { otelTesting.openTelemetry })
            .withPropertyValues("kqrs.metrics.opentelemetry.enabled=true")
            .run { context ->
                assertThat(context.getBean<MetricsStartProcessCommandEventHandler>()).isNotNull()
                assertThat(context.getBean<MetricsStartProcessCommandEventHandler>())
                    .isInstanceOf(OpenTelemetryMetricsStartCommandEventHandler::class)
            }
    }

    @Test
    fun `should register MetricsStopProcessCommandEventHandler bean when enabled and OpenTelemetry present`() {
        contextRunner
            .withBean(OpenTelemetry::class.java, { otelTesting.openTelemetry })
            .withPropertyValues("kqrs.metrics.opentelemetry.enabled=true")
            .run { context ->
                assertThat(context.getBean<MetricsStopProcessCommandEventHandler>()).isNotNull()
                assertThat(context.getBean<MetricsStopProcessCommandEventHandler>())
                    .isInstanceOf(OpenTelemetryMetricsStopCommandEventHandler::class)
            }
    }

    @Test
    fun `should register MetricsErrorProcessCommandEventHandler bean when enabled and OpenTelemetry present`() {
        contextRunner
            .withBean(OpenTelemetry::class.java, { otelTesting.openTelemetry })
            .withPropertyValues("kqrs.metrics.opentelemetry.enabled=true")
            .run { context ->
                assertThat(context.getBean<MetricsErrorProcessCommandEventHandler>()).isNotNull()
                assertThat(context.getBean<MetricsErrorProcessCommandEventHandler>())
                    .isInstanceOf(OpenTelemetryMetricsErrorCommandEventHandler::class)
            }
    }

    // endregion

    // region Query handlers — enabled

    @Test
    fun `should register MetricsStartProcessQueryEventHandler bean when enabled and OpenTelemetry present`() {
        contextRunner
            .withBean(OpenTelemetry::class.java, { otelTesting.openTelemetry })
            .withPropertyValues("kqrs.metrics.opentelemetry.enabled=true")
            .run { context ->
                assertThat(context.getBean<MetricsStartProcessQueryEventHandler>()).isNotNull()
                assertThat(context.getBean<MetricsStartProcessQueryEventHandler>())
                    .isInstanceOf(OpenTelemetryMetricsStartQueryEventHandler::class)
            }
    }

    @Test
    fun `should register MetricsStopProcessQueryEventHandler bean when enabled and OpenTelemetry present`() {
        contextRunner
            .withBean(OpenTelemetry::class.java, { otelTesting.openTelemetry })
            .withPropertyValues("kqrs.metrics.opentelemetry.enabled=true")
            .run { context ->
                assertThat(context.getBean<MetricsStopProcessQueryEventHandler>()).isNotNull()
                assertThat(context.getBean<MetricsStopProcessQueryEventHandler>())
                    .isInstanceOf(OpenTelemetryMetricsStopQueryEventHandler::class)
            }
    }

    @Test
    fun `should register MetricsErrorProcessQueryEventHandler bean when enabled and OpenTelemetry present`() {
        contextRunner
            .withBean(OpenTelemetry::class.java, { otelTesting.openTelemetry })
            .withPropertyValues("kqrs.metrics.opentelemetry.enabled=true")
            .run { context ->
                assertThat(context.getBean<MetricsErrorProcessQueryEventHandler>()).isNotNull()
                assertThat(context.getBean<MetricsErrorProcessQueryEventHandler>())
                    .isInstanceOf(OpenTelemetryMetricsErrorQueryEventHandler::class)
            }
    }

    // endregion

    // region Command handlers — disabled / absent

    @Test
    fun `should not register command handler beans when opentelemetry disabled`() {
        contextRunner
            .withBean(OpenTelemetry::class.java, { otelTesting.openTelemetry })
            .withPropertyValues("kqrs.metrics.opentelemetry.enabled=false")
            .run { context ->
                assert(!context.containsBean("openTelemetryMetricsStartCommandEventHandler"))
                assert(!context.containsBean("openTelemetryMetricsStopCommandEventHandler"))
                assert(!context.containsBean("openTelemetryMetricsErrorCommandEventHandler"))
            }
    }

    @Test
    fun `should not register command handler beans when OpenTelemetry is absent`() {
        contextRunner
            .withPropertyValues("kqrs.metrics.opentelemetry.enabled=true")
            .run { context ->
                assert(!context.containsBean("openTelemetryMetricsStartCommandEventHandler"))
                assert(!context.containsBean("openTelemetryMetricsStopCommandEventHandler"))
                assert(!context.containsBean("openTelemetryMetricsErrorCommandEventHandler"))
            }
    }

    @Test
    fun `should not register command handler beans when both disabled and OpenTelemetry absent`() {
        contextRunner
            .withPropertyValues("kqrs.metrics.opentelemetry.enabled=false")
            .run { context ->
                assert(!context.containsBean("openTelemetryMetricsStartCommandEventHandler"))
                assert(!context.containsBean("openTelemetryMetricsStopCommandEventHandler"))
                assert(!context.containsBean("openTelemetryMetricsErrorCommandEventHandler"))
            }
    }

    // endregion

    // region Query handlers — disabled / absent

    @Test
    fun `should not register query handler beans when opentelemetry disabled`() {
        contextRunner
            .withBean(OpenTelemetry::class.java, { otelTesting.openTelemetry })
            .withPropertyValues("kqrs.metrics.opentelemetry.enabled=false")
            .run { context ->
                assert(!context.containsBean("openTelemetryMetricsStartQueryEventHandler"))
                assert(!context.containsBean("openTelemetryMetricsStopQueryEventHandler"))
                assert(!context.containsBean("openTelemetryMetricsErrorQueryEventHandler"))
            }
    }

    @Test
    fun `should not register query handler beans when OpenTelemetry is absent`() {
        contextRunner
            .withPropertyValues("kqrs.metrics.opentelemetry.enabled=true")
            .run { context ->
                assert(!context.containsBean("openTelemetryMetricsStartQueryEventHandler"))
                assert(!context.containsBean("openTelemetryMetricsStopQueryEventHandler"))
                assert(!context.containsBean("openTelemetryMetricsErrorQueryEventHandler"))
            }
    }

    @Test
    fun `should not register query handler beans when both disabled and OpenTelemetry absent`() {
        contextRunner
            .withPropertyValues("kqrs.metrics.opentelemetry.enabled=false")
            .run { context ->
                assert(!context.containsBean("openTelemetryMetricsStartQueryEventHandler"))
                assert(!context.containsBean("openTelemetryMetricsStopQueryEventHandler"))
                assert(!context.containsBean("openTelemetryMetricsErrorQueryEventHandler"))
            }
    }

    // endregion

    // region Bean names

    @Test
    fun `should register beans with correct names when enabled`() {
        contextRunner
            .withBean(OpenTelemetry::class.java, { otelTesting.openTelemetry })
            .withPropertyValues("kqrs.metrics.opentelemetry.enabled=true")
            .run { context ->
                assert(context.containsBean("openTelemetryMetricsStartCommandEventHandler"))
                assert(context.containsBean("openTelemetryMetricsStopCommandEventHandler"))
                assert(context.containsBean("openTelemetryMetricsErrorCommandEventHandler"))
                assert(context.containsBean("openTelemetryMetricsStartQueryEventHandler"))
                assert(context.containsBean("openTelemetryMetricsStopQueryEventHandler"))
                assert(context.containsBean("openTelemetryMetricsErrorQueryEventHandler"))
            }
    }

    // endregion
}
