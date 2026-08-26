package com.example.edgeaicore.core.litertlm

import com.example.edgeaicore.core.common.EdgeResult
import com.example.edgeaicore.core.common.ExecutionBackend
import com.example.edgeaicore.core.models.EdgeModel
import kotlinx.coroutines.flow.Flow

data class ModelCapabilities(
    val supportsStreaming: Boolean = true,
    val maxContextTokens: Int = 2048,
    val supportedBackends: List<ExecutionBackend> = listOf(ExecutionBackend.CPU, ExecutionBackend.GPU, ExecutionBackend.NPU),
    val supportsVision: Boolean = false,
    val supportsFunctionCalling: Boolean = true,
    val quantization: String = "INT4"
)

/**
 * PrivateModelRuntime:
 * Standardized sovereign abstraction for genuine local on-device neural model execution.
 */
interface PrivateModelRuntime {
    suspend fun loadModel(modelPath: String, backend: ExecutionBackend = ExecutionBackend.AUTO): EdgeResult<Boolean>
    suspend fun unloadModel(): EdgeResult<Boolean>
    fun isReady(): Boolean
    suspend fun generate(request: GenerationRequest): EdgeResult<GenerationResponse>
    fun stream(request: GenerationRequest): Flow<String>
    fun getCapabilities(): ModelCapabilities
    fun getModelInfo(): EdgeModel?
}
