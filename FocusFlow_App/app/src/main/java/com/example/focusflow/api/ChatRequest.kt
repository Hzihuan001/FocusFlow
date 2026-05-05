package com.example.focusflow.api

import com.google.gson.annotations.SerializedName

data class ChatMessage(
    val role: String,
    val content: String
)

data class ChatRequest(
    val model: String = "glm-4", // 假设使用的是智谱的 glm-4
    val messages: List<ChatMessage>,
    val stream: Boolean = true // 使用 SSE 流式输出
)

// 用于解析每一块流回来的 JSON snippet
data class ChatResponseChunk(
    val id: String?,
    val choices: List<Choice>?
) {
    data class Choice(
        val delta: Delta?
    )
    
    data class Delta(
        val role: String?,
        val content: String?
    )
}
