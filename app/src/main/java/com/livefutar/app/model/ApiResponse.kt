package com.livefutar.app.model

data class ApiResponse<T>(
    val data: List<T>?,
    val pagination: Pagination? = null
)

data class Pagination(
    val totalCount: Int? = null,
    val offset: Int? = null,
    val limit: Int? = null
)
