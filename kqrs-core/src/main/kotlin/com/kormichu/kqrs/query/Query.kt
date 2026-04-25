package com.kormichu.kqrs.query

import com.kormichu.kqrs.Id
import com.kormichu.kqrs.event.EventTag
import com.kormichu.kqrs.helper.ClassParameterizedHelper

open class Query<R> (
    queryName: QueryName? = null,
    open val queryId: QueryId = Id.Companion.generateUuidV7()
) {
    open val queryName: QueryName = queryName ?: QueryName.of(this::class)
    val resultClass: Class<R> get() = ClassParameterizedHelper.getClassParameter(this)

    open fun getEventTags(): List<EventTag> = emptyList()
}
