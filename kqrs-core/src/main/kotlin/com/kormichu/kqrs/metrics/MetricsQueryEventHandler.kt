package com.kormichu.kqrs.metrics

import com.kormichu.kqrs.Handler
import com.kormichu.kqrs.event.EventHandler
import com.kormichu.kqrs.query.ErrorProcessQueryEvent
import com.kormichu.kqrs.query.QueryEvent
import com.kormichu.kqrs.query.StartProcessQueryEvent
import com.kormichu.kqrs.query.StopProcessQueryEvent
import kotlin.reflect.KClass

interface MetricsQueryEventHandler<E: QueryEvent<*>>: EventHandler<E> {
    @Suppress("UNCHECKED_CAST")
    override fun getBaseHandlerClass(): KClass<Handler<E>> =
        MetricsQueryEventHandler::class as KClass<Handler<E>>
}
interface MetricsStartProcessQueryEventHandler: MetricsQueryEventHandler<StartProcessQueryEvent<*>>
interface MetricsStopProcessQueryEventHandler: MetricsQueryEventHandler<StopProcessQueryEvent<*>>
interface MetricsErrorProcessQueryEventHandler: MetricsQueryEventHandler<ErrorProcessQueryEvent<*>>
