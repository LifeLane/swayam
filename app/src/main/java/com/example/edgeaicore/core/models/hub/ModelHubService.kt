package com.example.edgeaicore.core.models.hub

import android.content.Context
import com.example.edgeaicore.core.common.EdgeAIError
import com.example.edgeaicore.core.common.EdgeResult
import com.example.edgeaicore.core.common.ExecutionBackend
import com.example.edgeaicore.core.models.EdgeModel
import com.example.edgeaicore.core.models.ModelCapability
import com.example.edgeaicore.core.models.ModelStatus
import com.example.edgeaicore.core.models.ModelType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

enum class HubModelSource(val label: String, val badge: String) {
    LOCAL_CATALOG("Local Catalog", "LiteRT"),
    HUGGING_FACE("Hugging Face", "HF Hub"),
    OLLAMA_LIBRARY("Ollama Library", "Ollama"),
    CUSTOM_DIRECT("Direct URL", "Custom")
}

data class HubFileItem(
    val filename: String,
    val sizeBytes: Long,
    val downloadUrl: String,
    val quantization: String = "Q4_K_M"
) {
    val sizeMb: Double get() = sizeBytes / (1024.0 * 1024.0)
}

data class HubModelItem(
    val id: String,
    val name: String,
    val author: String,
    val source: HubModelSource,
    val description: String,
    val parameters: String = "1B",
    val quantization: String = "Q4_K_M / INT4",
    val format: String = "GGUF / LiteRT",
    val sizeBytes: Long = 0L,
    val capabilities: Set<ModelCapability> = setOf(ModelCapability.TEXT, ModelCapability.CHAT),
    val downloadUrl: String = "",
    val directFiles: List<HubFileItem> = emptyList(),
    val downloadsCount: Int = 0,
    val likesCount: Int = 0,
    val tags: List<String> = emptyList(),
    val license: String = "Open Weights",
    val isInstalled: Boolean = false,
    val isEnabled: Boolean = false,
    val localPath: String? = null
) {
    val sizeMb: Double get() = sizeBytes / (1024.0 * 1024.0)
}

/**
 * ModelHubService:
 * Unified model discovery, live search, metadata resolution, and download engine
 * for Hugging Face Hub, Ollama Model Library, and curated on-device neural models.
 */
class ModelHubService(private val context: Context) {

    /**
     * Curated Popular Mobile-Optimized Models from Hugging Face Hub.
     */
    val POPULAR_HUGGING_FACE_MODELS = listOf(
        HubModelItem(
            id = "bartowski/SmolLM-135M-Instruct-GGUF",
            name = "SmolLM 135M Instruct (Ultra-Compact Mobile LLM)",
            author = "Hugging Face / bartowski",
            source = HubModelSource.HUGGING_FACE,
            description = "Ultra-lightweight 135M parameter transformer designed specifically for on-device real-time reasoning with near-zero latency.",
            parameters = "135M",
            quantization = "Q4_K_M",
            format = "GGUF",
            sizeBytes = 145_000_000L,
            capabilities = setOf(ModelCapability.TEXT, ModelCapability.CHAT, ModelCapability.REASONING),
            downloadUrl = "https://huggingface.co/bartowski/SmolLM-135M-Instruct-GGUF/resolve/main/SmolLM-135M-Instruct-Q4_K_M.gguf",
            downloadsCount = 148200,
            likesCount = 890,
            tags = listOf("gguf", "smollm", "ultra-compact", "mobile-optimized", "text-generation"),
            license = "Apache-2.0"
        ),
        HubModelItem(
            id = "bartowski/Llama-3.2-1B-Instruct-GGUF",
            name = "Llama 3.2 1B Instruct (Mobile Optimized)",
            author = "Meta / bartowski",
            source = HubModelSource.HUGGING_FACE,
            description = "Meta's flagship on-device multilingual text generation & function calling model quantized for edge mobile runtime.",
            parameters = "1.23B",
            quantization = "Q4_K_M",
            format = "GGUF",
            sizeBytes = 780_000_000L,
            capabilities = setOf(ModelCapability.TEXT, ModelCapability.CHAT, ModelCapability.SUMMARIZATION, ModelCapability.REASONING),
            downloadUrl = "https://huggingface.co/bartowski/Llama-3.2-1B-Instruct-GGUF/resolve/main/Llama-3.2-1B-Instruct-Q4_K_M.gguf",
            downloadsCount = 384000,
            likesCount = 2450,
            tags = listOf("llama3", "llama-3.2", "meta", "gguf", "mobile", "text-generation"),
            license = "Llama 3.2 Community License"
        ),
        HubModelItem(
            id = "Qwen/Qwen2.5-0.5B-Instruct-GGUF",
            name = "Qwen 2.5 0.5B Instruct (GGUF)",
            author = "Qwen / Alibaba",
            source = HubModelSource.HUGGING_FACE,
            description = "High-efficiency 500M instruction model excelling at multi-turn dialogue, coding assistance, and multilingual reasoning.",
            parameters = "0.5B",
            quantization = "Q4_K_M",
            format = "GGUF",
            sizeBytes = 398_000_000L,
            capabilities = setOf(ModelCapability.TEXT, ModelCapability.CHAT, ModelCapability.REASONING),
            downloadUrl = "https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q4_k_m.gguf",
            downloadsCount = 210000,
            likesCount = 1820,
            tags = listOf("qwen2.5", "qwen", "gguf", "instruct", "mobile"),
            license = "Apache-2.0"
        ),
        HubModelItem(
            id = "TheBloke/TinyLlama-1.1B-Chat-v1.0-GGUF",
            name = "TinyLlama 1.1B Chat v1.0",
            author = "TinyLlama / TheBloke",
            source = HubModelSource.HUGGING_FACE,
            description = "Compact 1.1B open-source LLM pre-trained on 3 trillion tokens, optimized for fast on-device inference on ARM CPUs/GPUs.",
            parameters = "1.1B",
            quantization = "Q4_K_M",
            format = "GGUF",
            sizeBytes = 669_000_000L,
            capabilities = setOf(ModelCapability.TEXT, ModelCapability.CHAT),
            downloadUrl = "https://huggingface.co/TheBloke/TinyLlama-1.1B-Chat-v1.0-GGUF/resolve/main/tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf",
            downloadsCount = 520000,
            likesCount = 3100,
            tags = listOf("tinyllama", "gguf", "chat", "lightweight"),
            license = "Apache-2.0"
        ),
        HubModelItem(
            id = "google/gemma-2-2b-it-GGUF",
            name = "Gemma 2 2B IT (GGUF)",
            author = "Google / bartowski",
            source = HubModelSource.HUGGING_FACE,
            description = "Google's state-of-the-art lightweight instruction tuned Gemma 2 architecture quantized with 4-bit precision.",
            parameters = "2.6B",
            quantization = "Q4_K_M",
            format = "GGUF",
            sizeBytes = 1_650_000_000L,
            capabilities = setOf(ModelCapability.TEXT, ModelCapability.CHAT, ModelCapability.SUMMARIZATION, ModelCapability.REASONING),
            downloadUrl = "https://huggingface.co/bartowski/gemma-2-2b-it-GGUF/resolve/main/gemma-2-2b-it-Q4_K_M.gguf",
            downloadsCount = 195000,
            likesCount = 1420,
            tags = listOf("gemma2", "google", "gguf", "conversational"),
            license = "Gemma Terms of Use"
        ),
        HubModelItem(
            id = "bartowski/Phi-3.5-mini-instruct-GGUF",
            name = "Phi-3.5 Mini Instruct (3.8B GGUF)",
            author = "Microsoft / bartowski",
            source = HubModelSource.HUGGING_FACE,
            description = "Microsoft's premier high-reasoning small language model with advanced multilingual and multi-turn instruction capabilities.",
            parameters = "3.8B",
            quantization = "Q4_K_M",
            format = "GGUF",
            sizeBytes = 2_200_000_000L,
            capabilities = setOf(ModelCapability.TEXT, ModelCapability.CHAT, ModelCapability.REASONING),
            downloadUrl = "https://huggingface.co/bartowski/Phi-3.5-mini-instruct-GGUF/resolve/main/Phi-3.5-mini-instruct-Q4_K_M.gguf",
            downloadsCount = 175000,
            likesCount = 1120,
            tags = listOf("phi3.5", "microsoft", "gguf", "reasoning"),
            license = "MIT"
        ),
        HubModelItem(
            id = "nomic-ai/nomic-embed-text-v1.5-GGUF",
            name = "Nomic Embed Text v1.5 (High Dimension Embedding)",
            author = "Nomic AI",
            source = HubModelSource.HUGGING_FACE,
            description = "High-accuracy open embedding model for semantic vector search and RAG knowledge grounding on edge devices.",
            parameters = "137M",
            quantization = "Q4_K_M",
            format = "GGUF",
            sizeBytes = 150_000_000L,
            capabilities = setOf(ModelCapability.EMBEDDING),
            downloadUrl = "https://huggingface.co/nomic-ai/nomic-embed-text-v1.5-GGUF/resolve/main/nomic-embed-text-v1.5.Q4_K_M.gguf",
            downloadsCount = 98000,
            likesCount = 760,
            tags = listOf("embedding", "rag", "vector", "gguf"),
            license = "Apache-2.0"
        )
    )

    /**
     * Curated & Searchable Ollama Model Library.
     */
    val OLLAMA_LIBRARY_MODELS = listOf(
        HubModelItem(
            id = "ollama:llama3.2:1b",
            name = "Llama 3.2 1B (Ollama Library)",
            author = "Meta / Ollama",
            source = HubModelSource.OLLAMA_LIBRARY,
            description = "Meta Llama 3.2 1B model in Ollama format. Optimized for rapid on-device reasoning and tool execution.",
            parameters = "1B",
            quantization = "Q4_K_M",
            format = "Ollama / GGUF",
            sizeBytes = 780_000_000L,
            capabilities = setOf(ModelCapability.TEXT, ModelCapability.CHAT, ModelCapability.REASONING),
            downloadUrl = "https://huggingface.co/bartowski/Llama-3.2-1B-Instruct-GGUF/resolve/main/Llama-3.2-1B-Instruct-Q4_K_M.gguf",
            downloadsCount = 890000,
            likesCount = 4200,
            tags = listOf("ollama", "llama3.2", "1b", "meta", "instruct"),
            license = "Llama 3.2 Community"
        ),
        HubModelItem(
            id = "ollama:llama3.2:3b",
            name = "Llama 3.2 3B (Ollama Library)",
            author = "Meta / Ollama",
            source = HubModelSource.OLLAMA_LIBRARY,
            description = "Meta Llama 3.2 3B model in Ollama format. Rich conversational coherence and summarization for mobile AI.",
            parameters = "3.2B",
            quantization = "Q4_K_M",
            format = "Ollama / GGUF",
            sizeBytes = 2_000_000_000L,
            capabilities = setOf(ModelCapability.TEXT, ModelCapability.CHAT, ModelCapability.SUMMARIZATION, ModelCapability.REASONING),
            downloadUrl = "https://huggingface.co/bartowski/Llama-3.2-3B-Instruct-GGUF/resolve/main/Llama-3.2-3B-Instruct-Q4_K_M.gguf",
            downloadsCount = 650000,
            likesCount = 3800,
            tags = listOf("ollama", "llama3.2", "3b", "meta"),
            license = "Llama 3.2 Community"
        ),
        HubModelItem(
            id = "ollama:qwen2.5:0.5b",
            name = "Qwen 2.5 0.5B (Ollama Library)",
            author = "Alibaba / Ollama",
            source = HubModelSource.OLLAMA_LIBRARY,
            description = "Ultra-fast Qwen 2.5 0.5B instruction tuned model with excellent multilingual capabilities.",
            parameters = "0.5B",
            quantization = "Q4_K_M",
            format = "Ollama / GGUF",
            sizeBytes = 398_000_000L,
            capabilities = setOf(ModelCapability.TEXT, ModelCapability.CHAT, ModelCapability.REASONING),
            downloadUrl = "https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q4_k_m.gguf",
            downloadsCount = 420000,
            likesCount = 2100,
            tags = listOf("ollama", "qwen2.5", "0.5b", "fast", "multilingual"),
            license = "Apache-2.0"
        ),
        HubModelItem(
            id = "ollama:smollm2:135m",
            name = "SmolLM2 135M (Ollama Library)",
            author = "Hugging Face / Ollama",
            source = HubModelSource.OLLAMA_LIBRARY,
            description = "SmolLM2 ultra-compact 135M model, instant response times, minimal RAM footprint.",
            parameters = "135M",
            quantization = "Q4_K_M",
            format = "Ollama / GGUF",
            sizeBytes = 145_000_000L,
            capabilities = setOf(ModelCapability.TEXT, ModelCapability.CHAT),
            downloadUrl = "https://huggingface.co/bartowski/SmolLM-135M-Instruct-GGUF/resolve/main/SmolLM-135M-Instruct-Q4_K_M.gguf",
            downloadsCount = 310000,
            likesCount = 1900,
            tags = listOf("ollama", "smollm", "135m", "compact"),
            license = "Apache-2.0"
        ),
        HubModelItem(
            id = "ollama:gemma2:2b",
            name = "Gemma 2 2B (Ollama Library)",
            author = "Google / Ollama",
            source = HubModelSource.OLLAMA_LIBRARY,
            description = "Google's Gemma 2 2B model in Ollama catalog. Advanced reasoning and coding capabilities.",
            parameters = "2B",
            quantization = "Q4_K_M",
            format = "Ollama / GGUF",
            sizeBytes = 1_650_000_000L,
            capabilities = setOf(ModelCapability.TEXT, ModelCapability.CHAT, ModelCapability.REASONING),
            downloadUrl = "https://huggingface.co/bartowski/gemma-2-2b-it-GGUF/resolve/main/gemma-2-2b-it-Q4_K_M.gguf",
            downloadsCount = 780000,
            likesCount = 3400,
            tags = listOf("ollama", "gemma2", "google", "2b"),
            license = "Gemma Terms of Use"
        ),
        HubModelItem(
            id = "ollama:phi3:mini",
            name = "Phi-3 Mini 3.8B (Ollama Library)",
            author = "Microsoft / Ollama",
            source = HubModelSource.OLLAMA_LIBRARY,
            description = "Microsoft Phi-3 Mini with 3.8B parameters, high reasoning density and context comprehension.",
            parameters = "3.8B",
            quantization = "Q4_K_M",
            format = "Ollama / GGUF",
            sizeBytes = 2_200_000_000L,
            capabilities = setOf(ModelCapability.TEXT, ModelCapability.CHAT, ModelCapability.REASONING),
            downloadUrl = "https://huggingface.co/bartowski/Phi-3.5-mini-instruct-GGUF/resolve/main/Phi-3.5-mini-instruct-Q4_K_M.gguf",
            downloadsCount = 590000,
            likesCount = 2800,
            tags = listOf("ollama", "phi3", "microsoft", "3.8b"),
            license = "MIT"
        ),
        HubModelItem(
            id = "ollama:deepseek-r1:1.5b",
            name = "DeepSeek-R1 1.5B (Ollama Library)",
            author = "DeepSeek / Ollama",
            source = HubModelSource.OLLAMA_LIBRARY,
            description = "DeepSeek R1 reasoning and thinking model distilled for edge devices and step-by-step logic.",
            parameters = "1.5B",
            quantization = "Q4_K_M",
            format = "Ollama / GGUF",
            sizeBytes = 1_100_000_000L,
            capabilities = setOf(ModelCapability.TEXT, ModelCapability.CHAT, ModelCapability.REASONING),
            downloadUrl = "https://huggingface.co/bartowski/DeepSeek-R1-Distill-Qwen-1.5B-GGUF/resolve/main/DeepSeek-R1-Distill-Qwen-1.5B-Q4_K_M.gguf",
            downloadsCount = 920000,
            likesCount = 5600,
            tags = listOf("ollama", "deepseek-r1", "reasoning", "1.5b"),
            license = "MIT"
        ),
        HubModelItem(
            id = "ollama:tinyllama:1.1b",
            name = "TinyLlama 1.1B (Ollama Library)",
            author = "TinyLlama / Ollama",
            source = HubModelSource.OLLAMA_LIBRARY,
            description = "TinyLlama 1.1B conversational model in Ollama. Lightweight, fast inference.",
            parameters = "1.1B",
            quantization = "Q4_K_M",
            format = "Ollama / GGUF",
            sizeBytes = 669_000_000L,
            capabilities = setOf(ModelCapability.TEXT, ModelCapability.CHAT),
            downloadUrl = "https://huggingface.co/TheBloke/TinyLlama-1.1B-Chat-v1.0-GGUF/resolve/main/tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf",
            downloadsCount = 480000,
            likesCount = 2100,
            tags = listOf("ollama", "tinyllama", "1.1b", "chat"),
            license = "Apache-2.0"
        ),
        HubModelItem(
            id = "ollama:nomic-embed-text",
            name = "Nomic Embed Text (Ollama Library)",
            author = "Nomic AI / Ollama",
            source = HubModelSource.OLLAMA_LIBRARY,
            description = "Ollama default high-performance embedding model for local retrieval augmented generation (RAG).",
            parameters = "137M",
            quantization = "Q4_K_M",
            format = "Ollama / GGUF",
            sizeBytes = 150_000_000L,
            capabilities = setOf(ModelCapability.EMBEDDING),
            downloadUrl = "https://huggingface.co/nomic-ai/nomic-embed-text-v1.5-GGUF/resolve/main/nomic-embed-text-v1.5.Q4_K_M.gguf",
            downloadsCount = 720000,
            likesCount = 3100,
            tags = listOf("ollama", "embedding", "rag", "vector"),
            license = "Apache-2.0"
        )
    )

    /**
     * Searches Hugging Face Hub using the official public API with fallback to popular mobile models.
     */
    suspend fun searchHuggingFace(query: String, filter: String? = null): EdgeResult<List<HubModelItem>> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        
        // If empty query, return curated list
        if (trimmed.isBlank()) {
            return@withContext EdgeResult.Success(POPULAR_HUGGING_FACE_MODELS)
        }

        try {
            val encodedQuery = URLEncoder.encode(trimmed, "UTF-8")
            val filterParam = if (!filter.isNullOrBlank()) "&filter=$filter" else ""
            val apiUrl = "https://huggingface.co/api/models?search=$encodedQuery&limit=25&full=true$filterParam"
            
            val url = URL(apiUrl)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 12000
                readTimeout = 15000
                setRequestProperty("User-Agent", "SWAYAM-EdgeAI/3.0.2 Android Mobile")
                setRequestProperty("Accept", "application/json")
            }

            if (conn.responseCode in 200..299) {
                val jsonText = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()
                val jsonArray = JSONArray(jsonText)
                val results = mutableListOf<HubModelItem>()

                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.optJSONObject(i) ?: continue
                    val id = obj.optString("id", "")
                    if (id.isBlank()) continue

                    val author = id.substringBefore("/", "Community")
                    val modelName = id.substringAfter("/")
                    val downloads = obj.optInt("downloads", 0)
                    val likes = obj.optInt("likes", 0)
                    val pipelineTag = obj.optString("pipeline_tag", "text-generation")
                    
                    val tagsArray = obj.optJSONArray("tags")
                    val tagsList = mutableListOf<String>()
                    if (tagsArray != null) {
                        for (t in 0 until tagsArray.length()) {
                            tagsList.add(tagsArray.optString(t))
                        }
                    }

                    val isGguf = tagsList.any { it.contains("gguf", ignoreCase = true) } || id.contains("gguf", ignoreCase = true)
                    val isTflite = tagsList.any { it.contains("tflite", ignoreCase = true) } || id.contains("tflite", ignoreCase = true)
                    val isLiteRT = tagsList.any { it.contains("litert", ignoreCase = true) } || id.contains("litert", ignoreCase = true)

                    val capabilities = mutableSetOf<ModelCapability>()
                    when {
                        pipelineTag.contains("text-generation", ignoreCase = true) || pipelineTag.contains("conversational", ignoreCase = true) -> {
                            capabilities.add(ModelCapability.TEXT)
                            capabilities.add(ModelCapability.CHAT)
                        }
                        pipelineTag.contains("feature-extraction", ignoreCase = true) || pipelineTag.contains("sentence-similarity", ignoreCase = true) -> {
                            capabilities.add(ModelCapability.EMBEDDING)
                        }
                        pipelineTag.contains("image", ignoreCase = true) || pipelineTag.contains("vision", ignoreCase = true) -> {
                            capabilities.add(ModelCapability.VISION)
                        }
                        else -> {
                            capabilities.add(ModelCapability.TEXT)
                            capabilities.add(ModelCapability.CHAT)
                        }
                    }

                    val format = when {
                        isGguf -> "GGUF"
                        isLiteRT -> "LiteRT-LM"
                        isTflite -> "TFLite"
                        else -> "GGUF / Bin"
                    }

                    // Extract estimated parameters from name
                    val params = when {
                        id.contains("135m", ignoreCase = true) -> "135M"
                        id.contains("0.5b", ignoreCase = true) || id.contains("500m", ignoreCase = true) -> "0.5B"
                        id.contains("1.1b", ignoreCase = true) -> "1.1B"
                        id.contains("1b", ignoreCase = true) || id.contains("1.2b", ignoreCase = true) || id.contains("1.5b", ignoreCase = true) -> "1B"
                        id.contains("2b", ignoreCase = true) -> "2B"
                        id.contains("3b", ignoreCase = true) || id.contains("3.8b", ignoreCase = true) -> "3B"
                        id.contains("7b", ignoreCase = true) || id.contains("8b", ignoreCase = true) -> "7B - 8B"
                        else -> "1B - 3B"
                    }

                    val estimatedSize = when (params) {
                        "135M" -> 145_000_000L
                        "0.5B" -> 398_000_000L
                        "1.1B" -> 669_000_000L
                        "1B" -> 780_000_000L
                        "2B" -> 1_500_000_000L
                        "3B" -> 2_100_000_000L
                        else -> 800_000_000L
                    }

                    // Build canonical HuggingFace resolve direct download URL
                    val downloadUrl = if (isGguf) {
                        "https://huggingface.co/$id/resolve/main/${modelName.removeSuffix("-GGUF")}-Q4_K_M.gguf"
                    } else if (isTflite || isLiteRT) {
                        "https://huggingface.co/$id/resolve/main/model.tflite"
                    } else {
                        "https://huggingface.co/$id/resolve/main/model.bin"
                    }

                    results.add(
                        HubModelItem(
                            id = id,
                            name = modelName.replace("-", " "),
                            author = author,
                            source = HubModelSource.HUGGING_FACE,
                            description = "Hugging Face model repository with $downloads downloads and $likes likes. Optimized for mobile inference.",
                            parameters = params,
                            quantization = "Q4_K_M",
                            format = format,
                            sizeBytes = estimatedSize,
                            capabilities = capabilities,
                            downloadUrl = downloadUrl,
                            downloadsCount = downloads,
                            likesCount = likes,
                            tags = tagsList.take(6),
                            license = "Open Weights"
                        )
                    )
                }

                // If API returned 0 matching items, filter curated list
                if (results.isEmpty()) {
                    val matchingCurated = POPULAR_HUGGING_FACE_MODELS.filter {
                        it.name.contains(trimmed, ignoreCase = true) ||
                        it.id.contains(trimmed, ignoreCase = true) ||
                        it.tags.any { t -> t.contains(trimmed, ignoreCase = true) }
                    }
                    return@withContext EdgeResult.Success(matchingCurated)
                }

                EdgeResult.Success(results)
            } else {
                conn.disconnect()
                // Fallback to local search in curated models
                val matchingCurated = POPULAR_HUGGING_FACE_MODELS.filter {
                    it.name.contains(trimmed, ignoreCase = true) ||
                    it.id.contains(trimmed, ignoreCase = true) ||
                    it.tags.any { t -> t.contains(trimmed, ignoreCase = true) }
                }
                EdgeResult.Success(matchingCurated)
            }
        } catch (e: Exception) {
            // Offline or network error fallback
            val matchingCurated = POPULAR_HUGGING_FACE_MODELS.filter {
                it.name.contains(trimmed, ignoreCase = true) ||
                it.id.contains(trimmed, ignoreCase = true) ||
                it.tags.any { t -> t.contains(trimmed, ignoreCase = true) }
            }
            EdgeResult.Success(matchingCurated)
        }
    }

    /**
     * Searches Ollama Library models matching query or returns all curated Ollama models.
     */
    fun searchOllamaLibrary(query: String): List<HubModelItem> {
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            return OLLAMA_LIBRARY_MODELS
        }
        return OLLAMA_LIBRARY_MODELS.filter {
            it.name.contains(trimmed, ignoreCase = true) ||
            it.id.contains(trimmed, ignoreCase = true) ||
            it.description.contains(trimmed, ignoreCase = true) ||
            it.tags.any { tag -> tag.contains(trimmed, ignoreCase = true) }
        }
    }

    /**
     * Converts a HubModelItem into an EdgeModel entity ready for LocalModelManager installation.
     */
    fun toEdgeModel(item: HubModelItem): EdgeModel {
        val cleanId = item.id.replace("/", "--").replace(":", "-").lowercase()
        val modelType = when {
            item.capabilities.contains(ModelCapability.EMBEDDING) -> ModelType.EMBEDDING_VECTOR
            item.capabilities.contains(ModelCapability.VISION) -> ModelType.LITERT_VISION
            else -> ModelType.LITERT_LM
        }
        return EdgeModel(
            id = cleanId,
            name = item.name,
            version = "1.0.0",
            sizeBytes = item.sizeBytes,
            type = modelType,
            capabilities = item.capabilities,
            minimumRamMb = when (item.parameters) {
                "135M", "0.5B" -> 512L
                "1.1B", "1B" -> 1024L
                "2B" -> 2048L
                "3B", "3.8B" -> 3072L
                else -> 2048L
            },
            preferredBackend = ExecutionBackend.AUTO,
            downloadUrl = item.downloadUrl,
            checksum = "",
            license = item.license,
            isInstalled = false,
            isEnabled = true,
            localPath = null,
            status = ModelStatus.NOT_INSTALLED
        )
    }
}
