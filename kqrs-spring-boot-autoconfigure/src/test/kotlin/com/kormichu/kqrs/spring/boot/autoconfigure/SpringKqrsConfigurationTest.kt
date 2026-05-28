package com.kormichu.kqrs.spring.boot.autoconfigure

import assertk.assertThat
import assertk.assertions.isNotNull
import com.kormichu.kqrs.AsyncDispatchers
import com.kormichu.kqrs.DefaultKqrsGateway
import com.kormichu.kqrs.KqrsGateway
import com.kormichu.kqrs.command.AsyncCommandBus
import com.kormichu.kqrs.command.AsyncCommandExecutor
import com.kormichu.kqrs.command.AsyncCommandHandlerStorage
import com.kormichu.kqrs.command.CommandBus
import com.kormichu.kqrs.command.CommandExecutor
import com.kormichu.kqrs.command.CommandHandlerStorage
import com.kormichu.kqrs.command.DefaultAsyncCommandBus
import com.kormichu.kqrs.command.DefaultAsyncCommandExecutor
import com.kormichu.kqrs.command.DefaultCommandBus
import com.kormichu.kqrs.command.DefaultCommandExecutor
import com.kormichu.kqrs.event.DefaultEventBus
import com.kormichu.kqrs.event.DefaultEventExecutor
import com.kormichu.kqrs.event.EventBus
import com.kormichu.kqrs.event.EventExecutor
import com.kormichu.kqrs.event.EventHandlerStorage
import com.kormichu.kqrs.event.EventPublisher
import com.kormichu.kqrs.query.AsyncQueryBus
import com.kormichu.kqrs.query.AsyncQueryExecutor
import com.kormichu.kqrs.query.AsyncQueryHandlerStorage
import com.kormichu.kqrs.query.DefaultAsyncQueryBus
import com.kormichu.kqrs.query.DefaultAsyncQueryExecutor
import com.kormichu.kqrs.query.DefaultQueryBus
import com.kormichu.kqrs.query.DefaultQueryExecutor
import com.kormichu.kqrs.query.QueryBus
import com.kormichu.kqrs.query.QueryExecutor
import com.kormichu.kqrs.query.QueryHandlerStorage
import com.kormichu.kqrs.spring.event.SpringEventHandlerStorage
import com.kormichu.kqrs.spring.event.SpringEventListener
import com.kormichu.kqrs.spring.event.SpringEventPublisher
import com.kormichu.kqrs.transaction.TransactionalExecutor
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.getBean
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner

class SpringKqrsConfigurationTest {

    private val contextRunner = ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(SpringKqrsConfiguration::class.java))

    // region Gateway

    @Test
    fun `should register KqrsGateway bean`() {
        contextRunner.run { context ->
            assertThat(context.getBean<KqrsGateway>()).isNotNull()
            assertThat(context.getBean<DefaultKqrsGateway>()).isNotNull()
        }
    }

    @Test
    fun `should register AsyncDispatchers bean`() {
        contextRunner.run { context ->
            assertThat(context.getBean<AsyncDispatchers>()).isNotNull()
        }
    }

    // endregion

    // region Commands

    @Test
    fun `should register CommandExecutor bean`() {
        contextRunner.run { context ->
            assertThat(context.getBean<CommandExecutor>()).isNotNull()
            assertThat(context.getBean<DefaultCommandExecutor>()).isNotNull()
        }
    }

    @Test
    fun `should register AsyncCommandExecutor bean`() {
        contextRunner.run { context ->
            assertThat(context.getBean<AsyncCommandExecutor>()).isNotNull()
            assertThat(context.getBean<DefaultAsyncCommandExecutor>()).isNotNull()
        }
    }

    @Test
    fun `should register CommandHandlerStorage bean`() {
        contextRunner.run { context ->
            assertThat(context.getBean<CommandHandlerStorage>()).isNotNull()
        }
    }

    @Test
    fun `should register AsyncCommandHandlerStorage bean`() {
        contextRunner.run { context ->
            assertThat(context.getBean<AsyncCommandHandlerStorage>()).isNotNull()
        }
    }

    @Test
    fun `should register CommandBus bean`() {
        contextRunner.run { context ->
            assertThat(context.getBean<CommandBus>()).isNotNull()
            assertThat(context.getBean<DefaultCommandBus>()).isNotNull()
        }
    }

    @Test
    fun `should register AsyncCommandBus bean`() {
        contextRunner.run { context ->
            assertThat(context.getBean<AsyncCommandBus>()).isNotNull()
            assertThat(context.getBean<DefaultAsyncCommandBus>()).isNotNull()
        }
    }

    @Test
    fun `should allow custom CommandBus override`() {
        val customBus = object : CommandBus {
            override fun <C : com.kormichu.kqrs.command.Command<R>, R> dispatch(command: C): R =
                throw UnsupportedOperationException()
        }
        contextRunner
            .withBean(CommandBus::class.java, { customBus })
            .run { context ->
                assertThat(context.getBean<CommandBus>()).isNotNull()
                // custom bean is used, DefaultCommandBus should not exist
                assert(!context.containsBean("kqrsCommandBus"))
            }
    }

    // endregion

    // region Queries

    @Test
    fun `should register QueryExecutor bean`() {
        contextRunner.run { context ->
            assertThat(context.getBean<QueryExecutor>()).isNotNull()
            assertThat(context.getBean<DefaultQueryExecutor>()).isNotNull()
        }
    }

    @Test
    fun `should register AsyncQueryExecutor bean`() {
        contextRunner.run { context ->
            assertThat(context.getBean<AsyncQueryExecutor>()).isNotNull()
            assertThat(context.getBean<DefaultAsyncQueryExecutor>()).isNotNull()
        }
    }

    @Test
    fun `should register QueryHandlerStorage bean`() {
        contextRunner.run { context ->
            assertThat(context.getBean<QueryHandlerStorage>()).isNotNull()
        }
    }

    @Test
    fun `should register AsyncQueryHandlerStorage bean`() {
        contextRunner.run { context ->
            assertThat(context.getBean<AsyncQueryHandlerStorage>()).isNotNull()
        }
    }

    @Test
    fun `should register QueryBus bean`() {
        contextRunner.run { context ->
            assertThat(context.getBean<QueryBus>()).isNotNull()
            assertThat(context.getBean<DefaultQueryBus>()).isNotNull()
        }
    }

    @Test
    fun `should register AsyncQueryBus bean`() {
        contextRunner.run { context ->
            assertThat(context.getBean<AsyncQueryBus>()).isNotNull()
            assertThat(context.getBean<DefaultAsyncQueryBus>()).isNotNull()
        }
    }

    // endregion

    // region Events

    @Test
    fun `should register EventHandlerStorage bean`() {
        contextRunner.run { context ->
            assertThat(context.getBean<EventHandlerStorage>()).isNotNull()
            assertThat(context.getBean<SpringEventHandlerStorage>()).isNotNull()
        }
    }

    @Test
    fun `should register EventExecutor bean`() {
        contextRunner.run { context ->
            assertThat(context.getBean<EventExecutor>()).isNotNull()
            assertThat(context.getBean<DefaultEventExecutor>()).isNotNull()
        }
    }

    @Test
    fun `should register EventBus bean`() {
        contextRunner.run { context ->
            assertThat(context.getBean<EventBus>()).isNotNull()
            assertThat(context.getBean<DefaultEventBus>()).isNotNull()
        }
    }

    @Test
    fun `should register SpringEventListener bean`() {
        contextRunner.run { context ->
            assertThat(context.getBean<SpringEventListener>()).isNotNull()
        }
    }

    @Test
    fun `should register SpringEventPublisher bean`() {
        contextRunner.run { context ->
            assertThat(context.getBean<EventPublisher>()).isNotNull()
            assertThat(context.getBean<SpringEventPublisher>()).isNotNull()
        }
    }

    // endregion

    // region Transaction

    @Test
    fun `should register TransactionalExecutor bean`() {
        contextRunner.run { context ->
            assertThat(context.getBean<TransactionalExecutor>()).isNotNull()
        }
    }

    // endregion

    // region Properties

    @Test
    fun `should use default blockingListener=false`() {
        contextRunner.run { context ->
            val props = context.getBean<SpringKqrsProperties>()
            assert(!props.eventBus.blockingListener)
        }
    }

    @Test
    fun `should apply blockingListener property from config`() {
        contextRunner
            .withPropertyValues("kqrs.event-bus.blocking-listener=true")
            .run { context ->
                val props = context.getBean<SpringKqrsProperties>()
                assert(props.eventBus.blockingListener)
            }
    }

    // endregion
}

