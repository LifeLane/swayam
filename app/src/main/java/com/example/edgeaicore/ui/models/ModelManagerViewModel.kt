package com.example.edgeaicore.ui.models

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.edgeaicore.core.common.EdgeResult
import com.example.edgeaicore.core.common.ExecutionBackend
import com.example.edgeaicore.core.models.EdgeModel
import com.example.edgeaicore.core.models.LocalModelManager
import com.example.edgeaicore.core.models.ModelCapability
import com.example.edgeaicore.core.models.ModelStatus
import com.example.edgeaicore.core.models.ModelType
import com.example.edgeaicore.core.models.hub.HubModelItem
import com.example.edgeaicore.core.models.hub.HubModelSource
import com.example.edgeaicore.core.models.hub.ModelHubService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

data class ModelDownloadProgress(
    val modelId: String,
    val progress: Float,
    val status: ModelStatus,
    val error: String? = null
)

class ModelManagerViewModel(
    private val modelManager: LocalModelManager,
    private val hubService: ModelHubService
) : ViewModel() {

    val localModels: StateFlow<List<EdgeModel>> = modelManager.models

    private val _hfSearchResults = MutableStateFlow<List<HubModelItem>>(hubService.POPULAR_HUGGING_FACE_MODELS)
    val hfSearchResults: StateFlow<List<HubModelItem>> = _hfSearchResults.asStateFlow()

    private val _ollamaSearchResults = MutableStateFlow<List<HubModelItem>>(hubService.OLLAMA_LIBRARY_MODELS)
    val ollamaSearchResults: StateFlow<List<HubModelItem>> = _ollamaSearchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _activeDownloads = MutableStateFlow<Map<String, ModelDownloadProgress>>(emptyMap())
    val activeDownloads: StateFlow<Map<String, ModelDownloadProgress>> = _activeDownloads.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    fun searchHuggingFace(query: String) {
        viewModelScope.launch {
            _isSearching.value = true
            val result = hubService.searchHuggingFace(query)
            if (result is EdgeResult.Success) {
                _hfSearchResults.value = result.data
            }
            _isSearching.value = false
        }
    }

    fun searchOllama(query: String) {
        viewModelScope.launch {
            _isSearching.value = true
            val results = hubService.searchOllamaLibrary(query)
            _ollamaSearchResults.value = results
            _isSearching.value = false
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun installModel(modelId: String) {
        viewModelScope.launch {
            _activeDownloads.value = _activeDownloads.value + (modelId to ModelDownloadProgress(modelId, 0.05f, ModelStatus.DOWNLOADING))
            val result = modelManager.installModel(modelId) { progress ->
                _activeDownloads.value = _activeDownloads.value + (modelId to ModelDownloadProgress(
                    modelId,
                    progress,
                    if (progress >= 0.75f) ModelStatus.VERIFYING else ModelStatus.DOWNLOADING
                ))
            }
            when (result) {
                is EdgeResult.Success -> {
                    _activeDownloads.value = _activeDownloads.value - modelId
                    _statusMessage.value = "Successfully installed and verified ${result.data.name}"
                }
                is EdgeResult.Failure -> {
                    _activeDownloads.value = _activeDownloads.value + (modelId to ModelDownloadProgress(
                        modelId,
                        0f,
                        ModelStatus.ERROR,
                        result.error.message
                    ))
                    _statusMessage.value = "Installation failed: ${result.error.message}"
                }
            }
        }
    }

    fun installHubModel(item: HubModelItem, selectedDownloadUrl: String? = null) {
        viewModelScope.launch {
            val urlToDownload = selectedDownloadUrl ?: item.downloadUrl
            val safeId = item.id.replace("/", "--").replace(":", "-").lowercase()

            val edgeModel = EdgeModel(
                id = safeId,
                name = item.name,
                version = "1.0.0",
                sizeBytes = item.sizeBytes,
                type = when {
                    urlToDownload.contains(".task", true) -> ModelType.MEDIAPIPE_TASK
                    urlToDownload.contains(".tflite", true) -> ModelType.EMBEDDING_VECTOR
                    else -> ModelType.LITERT_LM
                },
                capabilities = item.capabilities,
                minimumRamMb = if (item.parameters.contains("7B", true)) 6144L else if (item.parameters.contains("3B", true) || item.parameters.contains("2B", true)) 3072L else 1024L,
                preferredBackend = ExecutionBackend.GPU,
                downloadUrl = urlToDownload,
                license = item.license,
                checksum = ""
            )

            modelManager.registerRemoteModel(edgeModel)
            installModel(edgeModel.id)
        }
    }

    fun removeModel(modelId: String) {
        viewModelScope.launch {
            val result = modelManager.removeModel(modelId)
            when (result) {
                is EdgeResult.Success -> {
                    _activeDownloads.value = _activeDownloads.value - modelId
                    _statusMessage.value = "Model removed successfully"
                }
                is EdgeResult.Failure -> {
                    _statusMessage.value = "Failed to remove model: ${result.error.message}"
                }
            }
        }
    }

    fun toggleModelEnabled(modelId: String, enabled: Boolean) {
        modelManager.setModelEnabled(modelId, enabled)
    }

    fun importLocalModel(file: File, name: String, type: ModelType, capabilities: Set<ModelCapability>) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = modelManager.importLocalModel(file, name, type, capabilities)
            when (result) {
                is EdgeResult.Success -> {
                    _statusMessage.value = "Imported and verified ${result.data.name}"
                }
                is EdgeResult.Failure -> {
                    _statusMessage.value = "Import failed: ${result.error.message}"
                }
            }
        }
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val modelManager = LocalModelManager(context)
                val hubService = ModelHubService(context)
                return ModelManagerViewModel(modelManager, hubService) as T
            }
        }
    }
}
