package com.griffith.chatbot.data.remote

data class Message(
    val role: String,
    val content: String
)

data class ChatRequest(
    val model: String = "gpt-4o",
    val messages: List<Message>
)

data class ChatResponse(
    val choices: List<Choice>
)

data class Choice(
    val message: Message
)