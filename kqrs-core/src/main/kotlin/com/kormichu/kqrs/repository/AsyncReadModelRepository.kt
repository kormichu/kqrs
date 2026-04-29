package com.kormichu.kqrs.repository

import com.kormichu.kqrs.Id

interface AsyncReadModelRepository<T, ID : Id<*>> {
    suspend fun findById(id: ID): T?
    suspend fun existsById(id: ID): Boolean
}
