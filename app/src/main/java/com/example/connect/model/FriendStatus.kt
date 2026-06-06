package com.example.connect.model

enum class FriendStatus(val value: String) {
    NONE("none"),
    SENT("sent"),
    PENDING("pending"),
    FRIEND("friend");

    companion object {
        fun from(value: String): FriendStatus =
            entries.firstOrNull { it.value == value } ?: NONE
    }
}
