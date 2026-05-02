package com.cryptodept.domain.model

sealed class DomainError : Throwable() {
    data class NetworkError(val code: Int, override val message: String) : DomainError()
    data class DatabaseError(override val message: String) : DomainError()
    data class ApiError(val provider: String, override val message: String) : DomainError()
    data class ParseError(val context: String, override val message: String) : DomainError()
    data class UnknownError(override val message: String? = null) : DomainError()
}
