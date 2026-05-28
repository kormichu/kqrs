package com.kormichu.kqrs.spring.transaction

import com.kormichu.kqrs.transaction.TransactionalExecutor
import org.springframework.transaction.annotation.Transactional

open class SpringTransactionalExecutor: TransactionalExecutor {
    @Transactional
    override fun <T> execute(block: () -> T): T =
        block()

    @Transactional(readOnly = true)
    override fun <T> executeReadOnly(block: () -> T): T =
        block()
}
