package com.example.ai

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

interface GeminiApiService {
    @POST("v1beta/models/gemini-2.5-flash-image:generateContent")
    suspend fun generateMemeCaptions(
        @Query("key") apiKey: String,
        @Body request: GeminiGenerateContentRequest
    ): GeminiGenerateContentResponse

    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateTextCaptions(
        @Query("key") apiKey: String,
        @Body request: GeminiGenerateContentRequest
    ): GeminiGenerateContentResponse
}
