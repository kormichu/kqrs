package com.kormichu.kqrs

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.reflect.KClass

class HandlerTest {

    @Test
    fun `should extract type parameter from direct handler implementation`() {
        // given
        val handler = SimpleTestHandler()

        // when
        val objectClass = handler.objectClass

        // then
        assertThat(objectClass).isEqualTo(TestObject::class)
    }

    @Test
    fun `should extract type parameter from nested handler inheritance`() {
        // given
        val handler = NestedTestHandler()

        // when
        val objectClass = handler.objectClass

        // then
        assertThat(objectClass).isEqualTo(TestObject::class)
    }

    @Test
    fun `should extract type parameter from multiple inheritance levels`() {
        // given
        val handler = DeepNestedTestHandler()

        // when
        val objectClass = handler.objectClass

        // then
        assertThat(objectClass).isEqualTo(TestObject::class)
    }

    @Test
    fun `should extract different type parameters for different handlers`() {
        // given
        val handler1 = SimpleTestHandler()
        val handler2 = AnotherTestHandler()

        // when
        val objectClass1 = handler1.objectClass
        val objectClass2 = handler2.objectClass

        // then
        assertThat(objectClass1).isEqualTo(TestObject::class)
        assertThat(objectClass2).isEqualTo(AnotherTestObject::class)
    }

    @Test
    fun `should throw exception when type parameter cannot be determined`() {
        // given
        val handler = InvalidHandler()

        // when & then
        val exception = assertThrows<IllegalStateException> {
            handler.objectClass
        }
        assertThat(exception.message).isEqualTo("Could not determine type parameter O")
    }

    @Test
    fun `should cache type parameter on multiple accesses`() {
        // given
        val handler = SimpleTestHandler()

        // when
        val objectClass1 = handler.objectClass
        val objectClass2 = handler.objectClass

        // then
        assertThat(objectClass1).isEqualTo(objectClass2)
        assertThat(objectClass1).isEqualTo(TestObject::class)
    }

    @Test
    fun `should handle generic type with bounds`() {
        // given
        val handler = BoundedTypeHandler()

        // when
        val objectClass = handler.objectClass

        // then
        assertThat(objectClass).isEqualTo(TestObject::class)
    }

    @Test
    fun `should find type argument from abstract intermediate class`() {
        // given
        val handler = ConcreteHandlerWithAbstractParent()

        // when
        val objectClass = handler.objectClass

        // then
        assertThat(objectClass).isEqualTo(TestObject::class)
    }

    @Test
    fun `should handle handler with complex type hierarchy`() {
        // given
        val handler = ComplexHierarchyHandler()

        // when
        val objectClass = handler.objectClass

        // then
        assertThat(objectClass).isEqualTo(ComplexTestObject::class)
    }

    // Test classes
    private data class TestObject(val value: String)
    private data class AnotherTestObject(val id: Int)
    private data class ComplexTestObject(val data: List<String>)

    private class SimpleTestHandler : Handler<TestObject> {
        override fun getBaseHandlerClass(): KClass<Handler<TestObject>> {
            @Suppress("UNCHECKED_CAST")
            return Handler::class as KClass<Handler<TestObject>>
        }
    }

    private class AnotherTestHandler : Handler<AnotherTestObject> {
        override fun getBaseHandlerClass(): KClass<Handler<AnotherTestObject>> {
            @Suppress("UNCHECKED_CAST")
            return Handler::class as KClass<Handler<AnotherTestObject>>
        }
    }

    private abstract class BaseTestHandler : Handler<TestObject> {
        override fun getBaseHandlerClass(): KClass<Handler<TestObject>> {
            @Suppress("UNCHECKED_CAST")
            return Handler::class as KClass<Handler<TestObject>>
        }
    }

    private class NestedTestHandler : BaseTestHandler()

    private abstract class IntermediateTestHandler : BaseTestHandler()

    private class DeepNestedTestHandler : IntermediateTestHandler()

    private class BoundedTypeHandler : Handler<TestObject> {
        override fun getBaseHandlerClass(): KClass<Handler<TestObject>> {
            @Suppress("UNCHECKED_CAST")
            return Handler::class as KClass<Handler<TestObject>>
        }
    }

    private abstract class AbstractParentHandler : Handler<TestObject> {
        override fun getBaseHandlerClass(): KClass<Handler<TestObject>> {
            @Suppress("UNCHECKED_CAST")
            return Handler::class as KClass<Handler<TestObject>>
        }
    }

    private class ConcreteHandlerWithAbstractParent : AbstractParentHandler()

    private interface HandlerMixin

    private class ComplexHierarchyHandler : Handler<ComplexTestObject>, HandlerMixin {
        override fun getBaseHandlerClass(): KClass<Handler<ComplexTestObject>> {
            @Suppress("UNCHECKED_CAST")
            return Handler::class as KClass<Handler<ComplexTestObject>>
        }
    }

    @Suppress("UNCHECKED_CAST")
    private class InvalidHandler : Handler<TestObject> {
        override fun getBaseHandlerClass(): KClass<Handler<TestObject>> {
            // Return wrong class to simulate type parameter not found
            return Any::class as KClass<Handler<TestObject>>
        }
    }
}
