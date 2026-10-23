package com.threadcraft.app.data.remote

import com.threadcraft.app.data.model.AiRequest
import com.threadcraft.app.data.model.AiResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url

interface AiApiService {
    @POST
    suspend fun generateCompletion(
        @Url url: String,
        @Header("Authorization") authHeader: String,
        @Body request: AiRequest
    ): AiResponse
}
