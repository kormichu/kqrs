package com.kormichu.kqrs.query

import com.kormichu.kqrs.Handler
import kotlinx.coroutines.CoroutineDispatcher
import kotlin.reflect.KClass

interface AsyncQueryHandler<Q : Query<R>, R>: Handler<Q> {
    val dispatcher: CoroutineDispatcher?
        get() = null

    suspend fun handle(query: Q): R

    @Suppress("UNCHECKED_CAST")
    override fun getBaseHandlerClass(): KClass<Handler<Q>> =
        AsyncQueryHandler::class as KClass<Handler<Q>>
}

interface AsyncIOQueryHandler<Q : Query<R>, R>: AsyncQueryHandler<Q, R> {
    @Suppress("UNCHECKED_CAST")
    override fun getBaseHandlerClass(): KClass<Handler<Q>> =
        AsyncIOQueryHandler::class as KClass<Handler<Q>>
}
