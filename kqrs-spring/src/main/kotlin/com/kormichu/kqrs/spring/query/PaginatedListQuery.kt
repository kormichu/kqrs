package com.kormichu.kqrs.spring.query

import com.kormichu.kqrs.Id
import com.kormichu.kqrs.query.Query
import com.kormichu.kqrs.query.QueryId
import com.kormichu.kqrs.query.QueryName
import org.springframework.data.domain.Pageable

abstract class SpringPaginatedListQuery<R>(
    queryName: QueryName? = null,
    override val queryId: QueryId = Id.generateUuidV7(),
    open val pagination: Pageable
) : Query<R>(queryName, queryId)
