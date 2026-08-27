package com.example.edgeaicore.core.litertlm

import com.example.edgeaicore.core.common.AIProviderType

data class GenerationRequest(
    val prompt: String,
    val systemInstruction: String? = null,
    val maxTokens: Int = 1024,
    val temperature: Float = 0.7f,
    val topK: Int = 40,
    val topP: Float = 0.95f,
    val context: String? = null,
    val modelId: String = "auto",
    val stopSequences: List<String> = emptyList(),
    val stream: Boolean = false
)

data class GenerationResponse(
    val text: String,
    val model: String,
    val latencyMs: Long,
    val tokensGenerated: Int,
    val tokensPerSecond: Double,
    val provider: AIProviderType,
    val source: String
)
