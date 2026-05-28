package com.kormichu.kqrs.spring

import com.kormichu.kqrs.Handler
import org.springframework.context.ApplicationContext
import kotlin.reflect.KClass

object SpringBeansHelper {
    inline fun <reified H : Handler<*>> getHandlers(
        applicationContext: ApplicationContext,
        handlerClassName: Class<H>
    ): Map<KClass<*>, H> = buildMap {
        val names: Array<String> = applicationContext.getBeanNamesForType(handlerClassName)
        for (name in names) {
            applicationContext.getBean(name).let {
                if (it is H) {
                    put(it.objectClass, it)
                }
            }
        }
    }

    inline fun <reified H : Handler<*>> getListHandlers(
        applicationContext: ApplicationContext,
        handlerClassName: Class<H>
    ): Map<KClass<*>, List<H>> = buildMap {
        val names: Array<String> = applicationContext.getBeanNamesForType(handlerClassName)
        for (name in names) {
            applicationContext.getBean(name).let {
                if (it is H) {
                    val handlerClass = it.objectClass
                    val handlers = getOrDefault(handlerClass, emptyList()) + it
                    put(handlerClass, handlers)
                }
            }
        }
    }

    fun <K, V> buildMap(builderAction: MutableMap<K, V>.() -> Unit): Map<K, V> =
        mutableMapOf<K, V>().apply(builderAction)
}
