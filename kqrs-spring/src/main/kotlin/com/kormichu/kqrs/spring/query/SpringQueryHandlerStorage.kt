package com.kormichu.kqrs.spring.query

import com.kormichu.kqrs.query.AsyncQueryHandler
import com.kormichu.kqrs.query.AsyncQueryHandlerStorage
import com.kormichu.kqrs.query.Query
import com.kormichu.kqrs.query.QueryHandler
import com.kormichu.kqrs.query.QueryHandlerStorage
import com.kormichu.kqrs.spring.SpringBeansHelper
import org.springframework.context.ApplicationContext
import kotlin.reflect.KClass

internal class SpringQueryHandlerStorage(
    applicationContext: ApplicationContext
) : QueryHandlerStorage {
    private val handlersMap = SpringBeansHelper.getHandlers(
        applicationContext = applicationContext,
        handlerClassName = QueryHandler::class.java,
    )

    @Suppress("UNCHECKED_CAST")
    override fun <Q : Query<R>, R> getHandler(queryClass: KClass<Q>): QueryHandler<Q, R>? {
        return when (val handler = handlersMap[queryClass]) {
            is QueryHandler<*, *> -> handler as QueryHandler<Q, R>
            else -> null
        }
    }
}

internal class SpringAsyncQueryHandlerStorage(
    applicationContext: ApplicationContext
) : AsyncQueryHandlerStorage {
    private val handlersMap = SpringBeansHelper.getHandlers(
        applicationContext = applicationContext,
        handlerClassName = AsyncQueryHandler::class.java,
    )

    @Suppress("UNCHECKED_CAST")
    override fun <C : Query<R>, R> getHandler(queryClass: KClass<C>): AsyncQueryHandler<C, R>? {
        return when (val handler = handlersMap[queryClass]) {
            is AsyncQueryHandler<*, *> -> handler as AsyncQueryHandler<C, R>
            else -> null
        }
    }
}
