package com.example.connect.model

data class UserWithStatus(
    val user: User,
    val status: FriendStatus,
    val fav: Boolean? = false,
    val nickname: String? = null

)