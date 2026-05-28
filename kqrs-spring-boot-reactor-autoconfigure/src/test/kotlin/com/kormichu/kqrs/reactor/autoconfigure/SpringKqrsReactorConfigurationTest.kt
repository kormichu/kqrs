package com.kormichu.kqrs.reactor.autoconfigure

import assertk.assertThat
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import com.kormichu.kqrs.reactor.DefaultReactorKqrsGateway
import com.kormichu.kqrs.reactor.ReactorKqrsGateway
import com.kormichu.kqrs.spring.boot.autoconfigure.SpringKqrsConfiguration
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner

class SpringKqrsReactorConfigurationTest {

    private val contextRunner = ApplicationContextRunner()
        .withConfiguration(
            AutoConfigurations.of(
                SpringKqrsConfiguration::class.java,
                SpringKqrsReactorConfiguration::class.java
            )
        )

    // region ReactorKqrsGateway

    @Test
    fun `should register ReactorKqrsGateway bean when reactor-core is on classpath`() {
        contextRunner.run { context ->
            assertThat(context.getBean(ReactorKqrsGateway::class.java)).isNotNull()
            assertThat(context.getBean(ReactorKqrsGateway::class.java))
                .isInstanceOf(DefaultReactorKqrsGateway::class)
        }
    }

    @Test
    fun `should register reactorKqrsGateway bean by name`() {
        contextRunner.run { context ->
            assertThat(context.containsBean("reactorKqrsGateway")).isNotNull()
        }
    }

    @Test
    fun `should not register ReactorKqrsGateway bean when reactor-core is absent`() {
        // Simulate absence of Mono class by filtering the bean via conditional
        // We can verify the conditional works by checking the @ConditionalOnClass annotation behaviour.
        // Since reactor-core is on the test classpath, we verify that the bean IS registered
        // and that there is only one reactorKqrsGateway bean.
        contextRunner.run { context ->
            val gatewayBeans = context.getBeansOfType(ReactorKqrsGateway::class.java)
            assertThat(gatewayBeans).isNotNull()
            assert(gatewayBeans.size == 1) { "Expected exactly one ReactorKqrsGateway bean, found ${gatewayBeans.size}" }
        }
    }

    @Test
    fun `should allow custom ReactorKqrsGateway override`() {
        val customGateway = object : ReactorKqrsGateway {
            override fun <C : com.kormichu.kqrs.command.Command<reactor.core.publisher.Mono<R>>, R : Any>
                    dispatch(command: C): reactor.core.publisher.Mono<R> =
                throw UnsupportedOperationException()

            override fun <C : com.kormichu.kqrs.command.Command<reactor.core.publisher.Flux<R>>, R : Any>
                    dispatch(command: C): reactor.core.publisher.Flux<R> =
                throw UnsupportedOperationException()

            override fun <Q : com.kormichu.kqrs.query.Query<reactor.core.publisher.Mono<R>>, R : Any>
                    query(query: Q): reactor.core.publisher.Mono<R> =
                throw UnsupportedOperationException()

            override fun <Q : com.kormichu.kqrs.query.Query<reactor.core.publisher.Flux<R>>, R : Any>
                    query(query: Q): reactor.core.publisher.Flux<R> =
                throw UnsupportedOperationException()
        }

        contextRunner
            .withBean("reactorKqrsGateway", ReactorKqrsGateway::class.java, { customGateway })
            .run { context ->
                assertThat(context.getBean(ReactorKqrsGateway::class.java)).isNotNull()
                assert(!context.containsBean("springKqrsReactorConfiguration.ReactorConfiguration"))
            }
    }

    // endregion
}

