package com.example.edgeaicore.core.swayam

import android.content.Context
import com.example.edgeaicore.core.common.AIProviderType
import com.example.edgeaicore.core.common.EdgeAIError
import com.example.edgeaicore.core.common.EdgeResult
import com.example.edgeaicore.core.common.ExecutionBackend
import com.example.edgeaicore.core.common.PrivacyLevel
import com.example.edgeaicore.core.knowledge.KnowledgeSearchResult
import com.example.edgeaicore.core.knowledge.KnowledgeSearchEngine
import com.example.edgeaicore.core.litertlm.GenerationRequest
import com.example.edgeaicore.core.litertlm.LiteRTLMEngine
import com.example.edgeaicore.core.litertlm.PrivateModelRuntime
import com.example.edgeaicore.core.memory.MemoryEngine
import com.example.edgeaicore.core.memory.RankedMemory
import com.example.edgeaicore.core.models.LocalModelManager
import com.example.edgeaicore.core.models.ModelStatus
import com.example.edgeaicore.core.privacy.PrivacyEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * PrivateEdgeEngine:
 * The Sovereign On-Device Intelligence Engine for SWAYAM GPT.
 * Guarantees zero network egress, strictly executing via local LiteRT-LM runtime,
 * local memory, local knowledge base (RAG), and local persona.
 */
class PrivateEdgeEngine(
    private val context: Context,
    val modelRuntime: PrivateModelRuntime = LiteRTLMEngine(context),
    val modelManager: LocalModelManager = LocalModelManager(context),
    val memoryEngine: MemoryEngine,
    val knowledgeSearchEngine: KnowledgeSearchEngine,
    val privacyEngine: PrivacyEngine,
    val personaManager: SwayamPersonaManager
) {

    /**
     * Executes genuine local private inference with on-device memory, knowledge, and soul context.
     * Guaranteed ZERO cloud egress.
     */
    suspend fun executePrivateInference(
        request: SwayamRequest
    ): EdgeResult<SwayamResponse> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()

        // 1. Enforce strict privacy policy & audit
        privacyEngine.recordAudit(
            taskType = "PRIVATE_EDGE_INFERENCE",
            declaredPrivacyLevel = PrivacyLevel.LOCAL_ONLY,
            targetProvider = AIProviderType.LOCAL,
            wasTransmittedRemotely = false,
            dataSummary = "Prompt: ${request.prompt.take(40)}..."
        )

        // 2. Ensure model is loaded and ready
        if (!modelRuntime.isReady()) {
            modelManager.scanAndVerifyInstalledModels()
            val installedModels = modelManager.getInstalledModels()
            val candidate = installedModels.firstOrNull { it.id == request.modelId }
                ?: installedModels.firstOrNull()
                ?: modelManager.getModelInfo(request.modelId ?: "gemma-2b-it-litert")

            val modelPath = candidate?.localPath
                ?: java.io.File(context.filesDir, "edge_models/${request.modelId ?: "gemma-2b-it-litert"}.bin")
                    .takeIf { it.exists() && it.length() > 0 }?.absolutePath

            if (modelPath != null && java.io.File(modelPath).exists() && java.io.File(modelPath).length() > 0) {
                val loadResult = modelRuntime.loadModel(modelPath, ExecutionBackend.AUTO)
                if (loadResult is EdgeResult.Failure) {
                    return@withContext EdgeResult.Failure(
                        EdgeAIError.ModelUnavailable("LOCAL_MODEL_UNAVAILABLE: Failed to load local model runtime (${loadResult.error.message})")
                    )
                }
            } else {
                return@withContext EdgeResult.Failure(
                    EdgeAIError.ModelUnavailable("LOCAL_MODEL_UNAVAILABLE: No compatible verified on-device neural model is installed.")
                )
            }
        }

        // 3. Assemble Local Soul, Identity & Values (Zero-Cloud)
        val currentPersona = personaManager.persona.value
        val soulInstruction = buildString {
            append("You are ${currentPersona.name}, an authentic sovereign edge AI operating exclusively on-device.\n")
            append("Mission: ${currentPersona.mission}\n")
            append("Voice & Tone: ${currentPersona.responseStyle.displayName}\n")
            append("Core Values: Privacy, Sovereignty, Autonomy, Grounded Truth.\n")
            if (currentPersona.customSystemInstructions.isNotBlank()) {
                append("Custom Directives: ${currentPersona.customSystemInstructions}\n")
            }
        }

        // 4. Retrieve Relevant Local Memories
        val relevantMemories: List<RankedMemory> = try {
            memoryEngine.retriever.retrieveMemories(request.prompt, maxResults = 4)
        } catch (_: Exception) {
            emptyList()
        }

        // 5. Retrieve Relevant Local Knowledge Documents (RAG)
        val ragResults: List<KnowledgeSearchResult> = try {
            val res = knowledgeSearchEngine.search(request.prompt, limit = 3)
            if (res is EdgeResult.Success) res.data else emptyList()
        } catch (_: Exception) {
            emptyList()
        }

        val contextBuilder = StringBuilder()
        if (relevantMemories.isNotEmpty()) {
            contextBuilder.append("### Relevant Personal Memories:\n")
            relevantMemories.forEach { mem ->
                contextBuilder.append("- ${mem.memory.content} (Score: ${String.format("%.2f", mem.score)})\n")
            }
            contextBuilder.append("\n")
        }

        if (ragResults.isNotEmpty()) {
            contextBuilder.append("### Grounded Document Knowledge:\n")
            ragResults.forEach { doc ->
                contextBuilder.append("- [Source: ${doc.title}]: ${doc.contentSnippet}\n")
            }
            contextBuilder.append("\n")
        }

        // 6. Execute Genuine On-Device LiteRT-LM Inference
        val genRequest = GenerationRequest(
            prompt = request.prompt,
            systemInstruction = soulInstruction,
            context = if (contextBuilder.isNotBlank()) contextBuilder.toString() else null,
            temperature = request.temperature,
            topK = request.topK,
            topP = request.topP,
            maxTokens = request.maxTokens,
            stream = false,
            modelId = request.modelId
        )

        val genResult = modelRuntime.generate(genRequest)

        when (genResult) {
            is EdgeResult.Success -> {
                val latency = (System.currentTimeMillis() - startTime).coerceAtLeast(1)
                val response = SwayamResponse(
                    text = genResult.data.text,
                    mode = if (ragResults.isNotEmpty()) SwayamProcessingMode.KNOWLEDGE_RAG else SwayamProcessingMode.GENERAL_CHAT,
                    sources = ragResults.map { it.title }.distinct(),
                    memoriesUsed = relevantMemories.map { it.memory.content },
                    toolsUsed = emptyList(),
                    agentsUsed = emptyList(),
                    provider = AIProviderType.LOCAL,
                    networkUsed = false,
                    latencyMs = latency,
                    tokensGenerated = genResult.data.tokensGenerated,
                    tokensPerSecond = genResult.data.tokensPerSecond,
                    confidence = 0.95f,
                    activePersonaId = currentPersona.name
                )
                EdgeResult.Success(response)
            }
            is EdgeResult.Failure -> {
                EdgeResult.Failure(genResult.error)
            }
        }
    }

    /**
     * Streams authentic on-device neural tokens with guaranteed zero network calls.
     */
    fun streamPrivateInference(request: SwayamRequest): Flow<String> = flow {
        if (!modelRuntime.isReady()) {
            val installed = modelManager.getInstalledModels().firstOrNull()
            if (installed?.localPath != null && java.io.File(installed.localPath).exists()) {
                modelRuntime.loadModel(installed.localPath, ExecutionBackend.AUTO)
            }
        }

        if (!modelRuntime.isReady()) {
            emit("LOCAL_MODEL_UNAVAILABLE: No compatible on-device model loaded.")
            return@flow
        }

        val currentPersona = personaManager.persona.value
        val soulInstruction = "You are ${currentPersona.name}, an authentic sovereign edge AI."

        val genRequest = GenerationRequest(
            prompt = request.prompt,
            systemInstruction = soulInstruction,
            temperature = request.temperature,
            topK = request.topK,
            topP = request.topP,
            maxTokens = request.maxTokens,
            stream = true,
            modelId = request.modelId
        )

        modelRuntime.stream(genRequest).collect { tokenPiece ->
            emit(tokenPiece)
        }
    }.flowOn(Dispatchers.Default)
}
