package com.threadcraft.app.data.model

data class AiRequest(
    val model: String,
    val messages: List<AiMessage>,
    val temperature: Double = 0.8,
    val max_tokens: Int = 2048
)

data class AiMessage(
    val role: String? = "user",
    val content: String? = ""
)

data class AiResponse(
    val choices: List<AiChoice>? = null,
    val error: AiErrorResponse? = null
)

data class AiChoice(
    val message: AiMessage? = null
)

data class AiErrorResponse(
    val message: String? = null,
    val type: String? = null,
    val code: String? = null
)

data class ThreadsTokenResponse(
    val access_token: String,
    val user_id: Long
)

data class ThreadsLongLivedTokenResponse(
    val access_token: String,
    val token_type: String? = null,
    val expires_in: Long? = null
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

data class ThreadsContainerStatusResponse(
    val id: String,
    val status: String? = null,
    val error_message: String? = null
)
