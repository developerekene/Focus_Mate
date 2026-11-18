package com.example.model

enum class SessionType(val storageKey: String) {
    FOCUS("focus"),
    SHORT_BREAK("short_break"),
    LONG_BREAK("long_break");

    val isBreak: Boolean
        get() = this != FOCUS

    companion object {
        fun fromKey(key: String): SessionType =
            entries.firstOrNull { it.storageKey == key } ?: FOCUS
    }
}
