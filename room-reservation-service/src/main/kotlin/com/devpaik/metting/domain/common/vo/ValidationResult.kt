package com.devpaik.metting.domain.common.vo

/**
 * 검증 결과
 */
sealed class ValidationResult {
    data class Success(val message: String = "검증 성공") : ValidationResult()
    data class Failure(val reason: String) : ValidationResult()

    fun isSuccess(): Boolean = this is Success
    fun isFailure(): Boolean = this is Failure

    fun getFailureReason(): String? {
        return when (this) {
            is Failure -> reason
            is Success -> null
        }
    }

    companion object {
        fun success() = Success()
        fun failure(reason: String) = Failure(reason)
    }
}