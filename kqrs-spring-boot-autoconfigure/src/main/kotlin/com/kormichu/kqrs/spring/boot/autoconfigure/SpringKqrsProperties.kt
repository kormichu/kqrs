package com.kormichu.kqrs.spring.boot.autoconfigure

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.NestedConfigurationProperty

@ConfigurationProperties("kqrs")
data class SpringKqrsProperties(
    @NestedConfigurationProperty
    val eventBus: SpringKqrsEventBusProperties = SpringKqrsEventBusProperties.DEFAULT,
    @NestedConfigurationProperty
    val metrics: SpringKqrsMetricsProperties = SpringKqrsMetricsProperties.DEFAULT
)

data class SpringKqrsEventBusProperties(
    val blockingListener: Boolean
) {
    companion object {
        val DEFAULT = SpringKqrsEventBusProperties(
            blockingListener = false,
        )
    }
}

data class SpringKqrsMetricsProperties(
    @NestedConfigurationProperty
    val prometheus: SpringKqrsMetricsPrometheusProperties = SpringKqrsMetricsPrometheusProperties.DEFAULT
) {
    companion object {
        val DEFAULT = SpringKqrsMetricsProperties(
            prometheus = SpringKqrsMetricsPrometheusProperties.DEFAULT,
        )
    }
}

data class SpringKqrsMetricsPrometheusProperties(
    val enabled: Boolean
) {
    companion object {
        val DEFAULT = SpringKqrsMetricsPrometheusProperties(
            enabled = false,
        )
    }
}
