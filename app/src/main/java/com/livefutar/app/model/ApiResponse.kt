package com.livefutar.app.model

data class ApiResponse<T>(
    val data: List<T>?
)
