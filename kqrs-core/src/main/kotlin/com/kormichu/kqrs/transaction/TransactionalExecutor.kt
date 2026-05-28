package com.kormichu.kqrs.transaction

interface TransactionalExecutor {
    fun <T> execute(block: () -> T): T
    fun <T> executeReadOnly(block: () -> T): T
}

