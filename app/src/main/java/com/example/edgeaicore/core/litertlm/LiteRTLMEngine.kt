package com.example.edgeaicore.core.litertlm

import android.content.Context
import com.example.edgeaicore.core.common.EdgeAIError
import com.example.edgeaicore.core.common.EdgeResult
import com.example.edgeaicore.core.common.AIProviderType
import com.example.edgeaicore.core.common.ExecutionBackend
import com.example.edgeaicore.core.models.EdgeModel
import com.example.edgeaicore.core.models.LocalModelManager
import com.example.edgeaicore.core.litertlm.ModelCapabilities
import com.example.edgeaicore.core.models.ModelStatus
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File

/**
 * LiteRTLMEngine:
 * High-performance on-device neural language model inference engine using MediaPipe LlmInference.
 * Guarantees zero network egress and sovereign execution.
 */
class LiteRTLMEngine(
    private val context: Context,
    private val modelManager: LocalModelManager? = null
) : PrivateModelRuntime {

    private val _status = MutableStateFlow(ModelStatus.NOT_INSTALLED)
    val status: StateFlow<ModelStatus> = _status.asStateFlow()

    private val _activeBackend = MutableStateFlow(ExecutionBackend.CPU)
    val activeBackend: StateFlow<ExecutionBackend> = _activeBackend.asStateFlow()

    private var activeModel: EdgeModel? = null
    private var activeModelPath: String? = null

    private var llmInference: LlmInference? = null
    private val tokenizer = LiteRTTokenizer()

    suspend fun initialize(modelId: String, backend: ExecutionBackend): EdgeResult<Boolean> = withContext(Dispatchers.IO) {
        val mgr = modelManager ?: LocalModelManager(context)
        mgr.scanAndVerifyInstalledModels()

        val targetModel = mgr.getModelInfo(modelId)?.takeIf { it.isInstalled && !it.localPath.isNullOrBlank() }
            ?: mgr.getInstalledModels().firstOrNull()
            ?: mgr.getModelInfo("gemma-2b-it-litert")
            ?: mgr.getModelInfo("tinyllama-1.1b-chat")
            ?: mgr.models.value.firstOrNull()

        val localPath = targetModel?.localPath ?: File(context.filesDir, "edge_models/${targetModel?.id ?: modelId}.bin").absolutePath
        val file = File(localPath)

        if (file.exists() && file.length() > 0) {
            return@withContext load(localPath, backend)
        }
        
        _status.value = ModelStatus.UNLOADED
        EdgeResult.Failure(
            EdgeAIError.ModelUnavailable("SWAYAM local intelligence is unavailable because no verified local model is loaded.")
        )
    }

    override suspend fun loadModel(modelPath: String, backend: ExecutionBackend): EdgeResult<Boolean> = load(modelPath, backend)

    override suspend fun unloadModel(): EdgeResult<Boolean> = unload()

    override fun getCapabilities(): ModelCapabilities {
        return ModelCapabilities(
            supportsStreaming = true,
            maxContextTokens = 2048,
            supportedBackends = listOf(ExecutionBackend.CPU, ExecutionBackend.GPU, ExecutionBackend.NPU),
            supportsVision = activeModel?.capabilities?.contains(com.example.edgeaicore.core.models.ModelCapability.VISION) == true,
            supportsFunctionCalling = true,
            quantization = "INT4"
        )
    }

    override fun getModelInfo(): EdgeModel? = activeModel

    suspend fun load(modelPath: String, backend: ExecutionBackend): EdgeResult<Boolean> = withContext(Dispatchers.IO) {
        val file = File(modelPath)
        if (!file.exists() || file.length() <= 0) {
            _status.value = ModelStatus.ERROR
            return@withContext EdgeResult.Failure(
                EdgeAIError.ModelUnavailable("Model file at '$modelPath' not found or empty.")
            )
        }

        _status.value = ModelStatus.LOADING

        try {
            val resolvedBackend = if (backend == ExecutionBackend.AUTO) ExecutionBackend.GPU else backend
            
            // Unload any existing model first
            llmInference?.close()
            llmInference = null

            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelPath)
                .setMaxTokens(1024)
                .build()

            llmInference = LlmInference.createFromOptions(context, options)

            _activeBackend.value = resolvedBackend
            activeModelPath = modelPath
            val mgr = modelManager ?: LocalModelManager(context)
            activeModel = mgr.getInstalledModels().firstOrNull { it.localPath == modelPath }

            _status.value = ModelStatus.READY
            EdgeResult.Success(true)
        } catch (e: Exception) {
            _status.value = ModelStatus.ERROR
            EdgeResult.Failure(EdgeAIError.Unknown("Failed to load LiteRT-LM runtime: ${e.message}", e))
        }
    }

    suspend fun unload(): EdgeResult<Boolean> = withContext(Dispatchers.IO) {
        try {
            llmInference?.close()
            llmInference = null
            activeModel = null
            activeModelPath = null
            _status.value = ModelStatus.UNLOADED
            EdgeResult.Success(true)
        } catch (e: Exception) {
            EdgeResult.Failure(EdgeAIError.Unknown("Failed to unload LiteRT-LM: ${e.message}", e))
        }
    }

    override fun isReady(): Boolean {
        return _status.value == ModelStatus.READY && llmInference != null
    }

    fun modelInfo(): EdgeModel? = activeModel

    fun runtimeInfo(): String = "LiteRT-LM On-Device Neural Engine"

    fun backendInfo(): ExecutionBackend = _activeBackend.value

    override suspend fun generate(request: GenerationRequest): EdgeResult<GenerationResponse> = withContext(Dispatchers.IO) {
        if (!isReady()) {
            return@withContext EdgeResult.Failure(
                EdgeAIError.ModelUnavailable("SWAYAM local intelligence is unavailable because no verified local model is loaded.")
            )
        }

        val startTime = System.currentTimeMillis()
        try {
            val formattedPrompt = tokenizer.formatPrompt(
                prompt = request.prompt,
                systemInstruction = request.systemInstruction,
                context = request.context
            )

            val inference = llmInference ?: throw IllegalStateException("Model not loaded")
            val generatedText = inference.generateResponse(formattedPrompt)

            val latency = (System.currentTimeMillis() - startTime).coerceAtLeast(1)
            val tokenCount = generatedText.split("\\s+".toRegex()).size.coerceAtLeast(1)
            val tokensPerSec = (tokenCount.toDouble() / (latency.toDouble() / 1000.0))

            EdgeResult.Success(
                GenerationResponse(
                    text = generatedText,
                    model = activeModel?.name ?: request.modelId,
                    latencyMs = latency,
                    tokensGenerated = tokenCount,
                    tokensPerSecond = tokensPerSec,
                    provider = AIProviderType.LOCAL,
                    source = "LiteRT-LM Neural Engine (${_activeBackend.value.name})"
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            EdgeResult.Failure(EdgeAIError.Unknown("On-device inference execution failed: ${e.message}", e))
        }
    }

    override fun stream(request: GenerationRequest): Flow<String> = callbackFlow {
        if (!isReady()) {
            close(IllegalStateException("SWAYAM local intelligence is unavailable because no verified local model is loaded."))
            return@callbackFlow
        }

        try {
            val formattedPrompt = tokenizer.formatPrompt(
                prompt = request.prompt,
                systemInstruction = request.systemInstruction,
                context = request.context
            )

            val inference = llmInference ?: throw IllegalStateException("Model not loaded")
            
            // Simulating stream until we implement Async properly with MediaPipe progress listener
            val generatedText = inference.generateResponse(formattedPrompt)
            val words = generatedText.split(" ")
            words.forEach { word ->
                trySend("$word ")
                kotlinx.coroutines.delay(20)
            }
            close()
        } catch (e: Exception) {
            close(e)
        }
        awaitClose { /* Cleanup */ }
    }.flowOn(Dispatchers.Default)
}
