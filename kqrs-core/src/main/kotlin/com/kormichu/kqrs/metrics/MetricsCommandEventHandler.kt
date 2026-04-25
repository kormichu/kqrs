package com.kormichu.kqrs.metrics

import com.kormichu.kqrs.Handler
import com.kormichu.kqrs.command.CommandEvent
import com.kormichu.kqrs.command.ErrorProcessCommandEvent
import com.kormichu.kqrs.command.StartProcessCommandEvent
import com.kormichu.kqrs.command.StopProcessCommandEvent
import com.kormichu.kqrs.event.EventHandler
import kotlin.reflect.KClass

interface MetricsCommandEventHandler<E: CommandEvent<*>>: EventHandler<E> {
    @Suppress("UNCHECKED_CAST")
    override fun getBaseHandlerClass(): KClass<Handler<E>> =
        MetricsCommandEventHandler::class as KClass<Handler<E>>
}
interface MetricsStartProcessCommandEventHandler: MetricsCommandEventHandler<StartProcessCommandEvent<*>>
interface MetricsStopProcessCommandEventHandler: MetricsCommandEventHandler<StopProcessCommandEvent<*>>
interface MetricsErrorProcessCommandEventHandler: MetricsCommandEventHandler<ErrorProcessCommandEvent<*>>
