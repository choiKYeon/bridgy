package org.grr.bridgy.common.exception

data class ErrorResponse(
    val status: Int,
    val message: String
)
