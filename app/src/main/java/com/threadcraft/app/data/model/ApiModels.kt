package com.threadcraft.app.data.model

data class AiRequest(
    val model: String,
    val messages: List<AiMessage>,
    val temperature: Double = 0.8,
    val max_tokens: Int = 2048
)

data class AiMessage(
    val role: String,
    val content: String
)

data class AiResponse(
    val choices: List<AiChoice>
)

data class AiChoice(
    val message: AiMessage
)

data class ThreadsTokenResponse(
    val access_token: String,
    val user_id: Long
)

data class ThreadsUserResponse(
    val id: String,
    val username: String,
    val name: String? = null
)

data class ThreadsCreateResponse(
    val id: String
)

data class ThreadsPublishResponse(
    val id: String
)
