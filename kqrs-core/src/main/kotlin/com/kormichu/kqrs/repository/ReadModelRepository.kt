package com.kormichu.kqrs.repository

import com.kormichu.kqrs.Id

interface ReadModelRepository<T, ID : Id<*>> {
    fun findById(id: ID): T?
    fun existsById(id: ID): Boolean
}
