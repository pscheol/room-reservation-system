package com.devpaik.metting.domain.common.vo

@JvmInline
value class UserEmail(val value: String) {
    companion object {
        private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    }

    init {
        require(value.isNotBlank()) { "이메일은 필수입니다" }
        require(value.matches(EMAIL_REGEX)) { "유효하지 않은 이메일 형식입니다: $value" }
    }

    fun asLowerCase(): String = value.lowercase()
}