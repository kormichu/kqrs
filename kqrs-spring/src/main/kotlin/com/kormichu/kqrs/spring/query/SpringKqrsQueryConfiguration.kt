package com.kormichu.kqrs.spring.query

import com.kormichu.kqrs.coroutines.CoroutineDispatchers
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
import com.kormichu.kqrs.spring.SpringKqrsProperties
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@AutoConfiguration
@Configuration
@EnableConfigurationProperties(SpringKqrsProperties::class)
class SpringKqrsQueryConfiguration {
    @Bean
    @ConditionalOnMissingBean(QueryExecutor::class)
    fun kqrsQueryExecutor(): QueryExecutor = DefaultQueryExecutor()

    @Bean
    @ConditionalOnMissingBean(AsyncQueryExecutor::class)
    fun kqrsAsyncQueryExecutor(
        coroutineDispatchers: CoroutineDispatchers
    ): AsyncQueryExecutor = DefaultAsyncQueryExecutor(coroutineDispatchers)

    @Bean
    @ConditionalOnMissingBean(QueryHandlerStorage::class)
    fun kqrsQueryHandlerStorage(
        applicationContext: ApplicationContext
    ): QueryHandlerStorage = SpringQueryHandlerStorage(applicationContext)

    @Bean
    @ConditionalOnMissingBean(AsyncQueryHandlerStorage::class)
    fun kqrsAsyncQueryHandlerStorage(
        applicationContext: ApplicationContext
    ): AsyncQueryHandlerStorage = SpringAsyncQueryHandlerStorage(applicationContext)

    @Bean
    @ConditionalOnMissingBean(QueryBus::class)
    fun kqrsQueryBus(
        queryExecutor: QueryExecutor,
        handlerStorage: QueryHandlerStorage
    ): QueryBus = DefaultQueryBus(
        queryExecutor = queryExecutor,
        handlerStorage = handlerStorage,
    )

    @Bean
    @ConditionalOnMissingBean(AsyncQueryBus::class)
    fun kqrsAsyncQueryBus(
        asyncQueryExecutor: AsyncQueryExecutor,
        asyncHandlerStorage: AsyncQueryHandlerStorage
    ): AsyncQueryBus = DefaultAsyncQueryBus(
        asyncQueryExecutor = asyncQueryExecutor,
        asyncHandlerStorage = asyncHandlerStorage,
    )
}
