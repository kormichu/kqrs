package com.kormichu.kqrs.spring.transaction

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test
import org.springframework.transaction.annotation.Transactional

class SpringTransactionalExecutorTest {
    private val executor = SpringTransactionalExecutor()

    @Test
    fun `should execute block and return result`() {
        // given
        var invocationCounter = 0

        // when
        val result = executor.execute {
            invocationCounter++
            "ok"
        }

        // then
        assertThat(result).isEqualTo("ok")
        assertThat(invocationCounter).isEqualTo(1)
    }

    @Test
    fun `should execute read only block and return result`() {
        // when
        val result = executor.executeReadOnly { 42 }

        // then
        assertThat(result).isEqualTo(42)
    }

    @Test
    fun `should have transactional annotations`() {
        // given
        val executeTransactional = SpringTransactionalExecutor::class.java
            .getDeclaredMethod("execute", Function0::class.java)
            .getAnnotation(Transactional::class.java)
        val executeReadOnlyTransactional = SpringTransactionalExecutor::class.java
            .getDeclaredMethod("executeReadOnly", Function0::class.java)
            .getAnnotation(Transactional::class.java)

        // then
        assertThat(executeTransactional.readOnly).isEqualTo(false)
        assertThat(executeReadOnlyTransactional.readOnly).isEqualTo(true)
    }
}

