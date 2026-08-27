package com.example.edgeaicore.ui.gallery

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class EdgeUseCaseType(
    val title: String,
    val shortSubtitle: String,
    val fullDescription: String,
    val icon: ImageVector,
    val themeColor: Color,
    val availableModelsCount: Int,
    val isExperimental: Boolean = false,
    val isNew: Boolean = false,
    val defaultModelId: String = "gemma-4-e2b-it"
) {
    ASK_IMAGE(
        title = "Ask Image",
        shortSubtitle = "Ask questions about images",
        fullDescription = "Ask questions about images with on-device large language models",
        icon = Icons.Filled.Image,
        themeColor = Color(0xFFEA4335), // Google Red / Coral
        availableModelsCount = 4,
        defaultModelId = "gemma-4-e2b-it"
    ),
    AUDIO_SCRIBE(
        title = "Audio Scribe",
        shortSubtitle = "Transcribe and translate audio",
        fullDescription = "Instantly transcribe and/or translate audio clips using on-device large language models",
        icon = Icons.Filled.Mic,
        themeColor = Color(0xFF34A853), // Google Green
        availableModelsCount = 4,
        defaultModelId = "gemma-4-e2b-it"
    ),
    AI_CHAT(
        title = "AI Chat",
        shortSubtitle = "Chat with an on-device LLM",
        fullDescription = "Chat with on-device large language models with rich reasoning and private local state",
        icon = Icons.Filled.Forum,
        themeColor = Color(0xFF1A73E8), // Google Blue
        availableModelsCount = 7,
        defaultModelId = "gemma-4-e2b-it"
    ),
    AGENT_SKILLS(
        title = "Agent Skills",
        shortSubtitle = "Complete agentic tasks with chat",
        fullDescription = "Chat with on-device large language models with skills and tools",
        icon = Icons.Filled.RocketLaunch,
        themeColor = Color(0xFFF9AB00), // Google Gold/Amber
        availableModelsCount = 2,
        isNew = true,
        defaultModelId = "gemma-4-e2b-it"
    ),
    PROMPT_LAB(
        title = "Prompt Lab",
        shortSubtitle = "Single turn use cases",
        fullDescription = "Single turn use cases with on-device large language models",
        icon = Icons.Filled.Widgets,
        themeColor = Color(0xFFEA4335), // Google Red/Coral
        availableModelsCount = 7,
        defaultModelId = "gemma-4-e2b-it"
    ),
    TINY_GARDEN(
        title = "Tiny Garden",
        shortSubtitle = "Use natural language to plant",
        fullDescription = "Use natural language to plant, water, and harvest in this fully offline mini-game.\n\nNote: This is powered by the experimental FunctionGemma model optimized for latency. Due to its compact size (270M), it works well on simple instructions but responses may vary to more complex interactions.",
        icon = Icons.Filled.LocalFlorist,
        themeColor = Color(0xFF34A853), // Green
        availableModelsCount = 1,
        isExperimental = true,
        defaultModelId = "tinygarden-270m"
    ),
    MOBILE_ACTIONS(
        title = "Mobile Actions",
        shortSubtitle = "Perform various device actions through Function Gemma",
        fullDescription = "Perform various device actions through Function Gemma",
        icon = Icons.Filled.Smartphone,
        themeColor = Color(0xFF1A73E8), // Blue
        availableModelsCount = 1,
        isExperimental = true,
        defaultModelId = "mobileactions-270m"
    )
}

data class GalleryModelCardInfo(
    val id: String,
    val name: String,
    val sizeDisplay: String,
    val description: String,
    val isBestOverall: Boolean = false,
    val isDownloaded: Boolean = true,
    val downloadProgress: Float = 1.0f,
    val licenseTitle: String = "Learn more and see model license",
    val licenseUrl: String = "https://ai.google.dev/gemma/terms",
    val contextLength: String = "32K context length",
    val modalities: String = "multi-modality input (text, vision, audio)"
)

object GalleryModelData {
    fun getModelsForUseCase(type: EdgeUseCaseType): List<GalleryModelCardInfo> {
        return when (type) {
            EdgeUseCaseType.TINY_GARDEN -> listOf(
                GalleryModelCardInfo(
                    id = "tinygarden-270m",
                    name = "TinyGarden-270M",
                    sizeDisplay = "289.0 MB",
                    description = "Fine-tuned Function Gemma 270M model for Tiny Garden.",
                    isBestOverall = true,
                    isDownloaded = true,
                    contextLength = "2K context length",
                    modalities = "text + function calling"
                )
            )
            EdgeUseCaseType.MOBILE_ACTIONS -> listOf(
                GalleryModelCardInfo(
                    id = "mobileactions-270m",
                    name = "MobileActions-270M",
                    sizeDisplay = "289.0 MB",
                    description = "Fine-tuned Function Gemma 270M model for Mobile Actions.",
                    isBestOverall = true,
                    isDownloaded = true,
                    contextLength = "2K context length",
                    modalities = "text + Android OS tool invocations"
                )
            )
            EdgeUseCaseType.AGENT_SKILLS -> listOf(
                GalleryModelCardInfo(
                    id = "gemma-4-e2b-it",
                    name = "Gemma-4-E2B-it",
                    sizeDisplay = "2.6 GB",
                    description = "A variant of Gemma 4 E2B ready for deployment on Android using LiteRT-LM. It supports multi-modality input, with up to 32K context length.",
                    isBestOverall = true,
                    isDownloaded = true,
                    contextLength = "32K context length"
                ),
                GalleryModelCardInfo(
                    id = "gemma-4-e4b-it",
                    name = "Gemma-4-E4B-it",
                    sizeDisplay = "3.7 GB",
                    description = "A variant of Gemma 4 E4B ready for deployment on Android using LiteRT-LM. It supports multi-modality input, with up to 32K context length.",
                    isBestOverall = false,
                    isDownloaded = false,
                    contextLength = "32K context length"
                )
            )
            else -> listOf(
                GalleryModelCardInfo(
                    id = "gemma-4-e2b-it",
                    name = "Gemma-4-E2B-it",
                    sizeDisplay = "2.6 GB",
                    description = "A variant of Gemma 4 E2B ready for deployment on Android using LiteRT-LM. It supports multi-modality input, with up to 32K context length.",
                    isBestOverall = true,
                    isDownloaded = true,
                    contextLength = "32K context length"
                ),
                GalleryModelCardInfo(
                    id = "gemma-4-e4b-it",
                    name = "Gemma-4-E4B-it",
                    sizeDisplay = "3.7 GB",
                    description = "A variant of Gemma 4 E4B ready for deployment on Android using LiteRT-LM. It supports multi-modality input, with up to 32K context length.",
                    isBestOverall = false,
                    isDownloaded = false,
                    contextLength = "32K context length"
                ),
                GalleryModelCardInfo(
                    id = "gemma-3n-e2b-it",
                    name = "Gemma-3n-E2B-it",
                    sizeDisplay = "3.7 GB",
                    description = "A variant of Gemma 3n E2B ready for deployment on Android using LiteRT-LM. It supports text, vision, and audio input, with 4096 context length.",
                    isBestOverall = false,
                    isDownloaded = false,
                    contextLength = "4096 context length"
                ),
                GalleryModelCardInfo(
                    id = "gemma-3n-e4b-it",
                    name = "Gemma-3n-E4B-it",
                    sizeDisplay = "4.9 GB",
                    description = "A variant of Gemma 3n E4B ready for deployment on Android using LiteRT-LM. It supports text, vision, and audio input, with 4096 context length.",
                    isBestOverall = false,
                    isDownloaded = false,
                    contextLength = "4096 context length"
                ),
                GalleryModelCardInfo(
                    id = "gemma3-1b-it",
                    name = "Gemma3-1B-IT",
                    sizeDisplay = "584.4 MB",
                    description = "Lightweight on-device language model for rapid single and multi-turn execution.",
                    isBestOverall = false,
                    isDownloaded = true,
                    contextLength = "8K context length"
                )
            )
        }
    }

    val ALL_GALLERY_MODELS = listOf(
        GalleryModelCardInfo(
            id = "gemma-4-e2b-it",
            name = "Gemma-4-E2B-it",
            sizeDisplay = "2.6 GB",
            description = "A variant of Gemma 4 E2B ready for deployment on Android using LiteRT-LM. It supports multi-modality input, with up to 32K context length.",
            isBestOverall = true,
            isDownloaded = true
        ),
        GalleryModelCardInfo(
            id = "gemma-4-e4b-it",
            name = "Gemma-4-E4B-it",
            sizeDisplay = "3.7 GB",
            description = "A variant of Gemma 4 E4B ready for deployment on Android using LiteRT-LM. It supports multi-modality input, with up to 32K context length.",
            isBestOverall = false,
            isDownloaded = false
        ),
        GalleryModelCardInfo(
            id = "gemma-3n-e2b-it",
            name = "Gemma-3n-E2B-it",
            sizeDisplay = "3.7 GB",
            description = "A variant of Gemma 3n E2B ready for deployment on Android using LiteRT-LM. It supports text, vision, and audio input, with 4096 context length.",
            isBestOverall = false,
            isDownloaded = false
        ),
        GalleryModelCardInfo(
            id = "gemma-3n-e4b-it",
            name = "Gemma-3n-E4B-it",
            sizeDisplay = "4.9 GB",
            description = "A variant of Gemma 3n E4B ready for deployment on Android using LiteRT-LM. It supports text, vision, and audio input, with 4096 context length.",
            isBestOverall = false,
            isDownloaded = false
        ),
        GalleryModelCardInfo(
            id = "gemma3-1b-it",
            name = "Gemma3-1B-IT",
            sizeDisplay = "584.4 MB",
            description = "Lightweight on-device language model for rapid single and multi-turn execution.",
            isBestOverall = false,
            isDownloaded = true
        ),
        GalleryModelCardInfo(
            id = "tinygarden-270m",
            name = "TinyGarden-270M",
            sizeDisplay = "289.0 MB",
            description = "Fine-tuned Function Gemma 270M model for Tiny Garden.",
            isBestOverall = false,
            isDownloaded = true
        ),
        GalleryModelCardInfo(
            id = "mobileactions-270m",
            name = "MobileActions-270M",
            sizeDisplay = "289.0 MB",
            description = "Fine-tuned Function Gemma 270M model for Mobile Actions.",
            isBestOverall = false,
            isDownloaded = true
        ),
        GalleryModelCardInfo(
            id = "smollm-135m-instruct",
            name = "SmolLM-135M-Instruct",
            sizeDisplay = "145.0 MB",
            description = "Ultra-lightweight mobile language model.",
            isBestOverall = false,
            isDownloaded = false
        ),
        GalleryModelCardInfo(
            id = "tinyllama-1.1b-chat",
            name = "TinyLlama-1.1B-Chat",
            sizeDisplay = "669.0 MB",
            description = "Compact 1.1B conversational model optimized for mobile GPUs.",
            isBestOverall = false,
            isDownloaded = false
        )
    )
}
