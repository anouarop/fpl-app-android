package com.shellanddeploy.fpllive.domain.model

data class ChatMessage(
    val id: Long,
    val teamId: Int,
    val teamName: String,
    val text: String,
    val createdAt: String,
)
