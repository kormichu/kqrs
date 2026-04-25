package com.kormichu.kqrs.spring.transaction

import com.kormichu.kqrs.transaction.TransactionalExecutor
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@AutoConfiguration
@Configuration
class SpringKqrsTransactionConfiguration {
    @Bean
    @ConditionalOnMissingBean(TransactionalExecutor::class)
    fun kqrsTransactionalExecutor(): TransactionalExecutor = SpringTransactionalExecutor()
}
