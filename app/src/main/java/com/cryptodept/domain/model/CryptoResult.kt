package com.cryptodept.domain.model

sealed class CryptoResult<out T> {
    data class Success<out T>(
        val data: T,
    ) : CryptoResult<T>()

    data class Error(
        val throwable: Throwable,
        val code: Int? = null,
    ) : CryptoResult<Nothing>()

    object Loading : CryptoResult<Nothing>()

    fun onSuccess(action: (T) -> Unit): CryptoResult<T> {
        if (this is Success) action(data)
        return this
    }

    fun onFailure(action: (Throwable) -> Unit): CryptoResult<T> {
        if (this is Error) action(throwable)
        return this
    }
}

fun <T> CryptoResult<T>.getOrNull(): T? = (this as? CryptoResult.Success)?.data

inline fun <T, R> CryptoResult<T>.fold(
    onSuccess: (T) -> R,
    onFailure: (Throwable) -> R,
): R =
    when (this) {
        is CryptoResult.Success -> onSuccess(data)
        is CryptoResult.Error -> onFailure(throwable)
        is CryptoResult.Loading -> throw IllegalStateException("Cannot fold Loading state without specialized handler")
    }
