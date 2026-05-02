package com.griffith.chatbot.data.remote

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface ChatGptApi {


    @POST("v1/chat/completions")
    suspend fun sendMessage(
        @Header("Authorization") auth: String,
        @Body request: ChatRequest
    ): ChatResponse

    // 🔹 For image + file input (flexible GPT-4o vision payload)
    @POST("v1/chat/completions")
    suspend fun sendVisionMessage(
        @Header("Authorization") auth: String,
        @Body request: Map<String, @JvmSuppressWildcards Any>
    ): ChatResponse
}