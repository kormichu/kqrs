package com.kormichu.kqrs.helper

import java.lang.reflect.ParameterizedType

internal object ClassParameterizedHelper {
    fun <T> getClassParameter(instance: Any, index: Int = 0): Class<T> {
        return findClassParameterInHierarchy(instance.javaClass, index)
            ?: error("Could not resolve generic type at position $index for ${instance.javaClass}")
    }

    private fun <T> findClassParameterInHierarchy(clazz: Class<*>, index: Int): Class<T>? {
        var currentClass: Class<*>? = clazz

        while (currentClass != null && currentClass != Any::class.java) {
            val result = findClassParameterInSuperclass<T>(currentClass, index)
                ?: findClassParameterInInterfaces(currentClass, index)

            if (result != null) {
                return result
            }

            currentClass = currentClass.superclass
        }

        return null
    }

    private fun <T> findClassParameterInSuperclass(clazz: Class<*>, index: Int): Class<T>? {
        val superType = clazz.genericSuperclass
        if (superType is ParameterizedType) {
            return extractClassParameter(superType, index)
        }
        return null
    }

    private fun <T> findClassParameterInInterfaces(clazz: Class<*>, index: Int): Class<T>? {
        for (interfaceType in clazz.genericInterfaces) {
            if (interfaceType is ParameterizedType) {
                extractClassParameter<T>(interfaceType, index)?.let { return it }
            }
        }
        return null
    }

    private fun <T> extractClassParameter(parameterizedType: ParameterizedType, index: Int): Class<T>? {
        val actualType = parameterizedType.actualTypeArguments[index]
        if (actualType is Class<*>) {
            @Suppress("UNCHECKED_CAST")
            return actualType as Class<T>
        }
        return null
    }
}
