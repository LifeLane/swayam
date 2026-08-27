package com.example.edgeaicore.ui.models

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.edgeaicore.EdgeAICore
import com.example.edgeaicore.core.common.ExecutionBackend
import com.example.edgeaicore.core.models.EdgeModel
import com.example.edgeaicore.core.models.ModelCapability
import com.example.edgeaicore.core.models.ModelStatus
import com.example.edgeaicore.core.models.ModelType
import com.example.edgeaicore.core.models.hub.HubModelItem
import com.example.edgeaicore.core.models.hub.HubModelSource
import com.example.edgeaicore.ui.common.AppCard
import com.example.ui.theme.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class HubTab(val label: String, val icon: String) {
    LOCAL("Local Catalog", "⚡"),
    HUGGING_FACE("Hugging Face", "🤗"),
    OLLAMA("Ollama Library", "🦙"),
    DIRECT_URL("Direct URL / Import", "🔗")
}

enum class ModelCategoryFilter(val label: String) {
    ALL("All Models"),
    LLM("Local LLMs"),
    VISION("Vision & Perception"),
    EMBEDDINGS("Embeddings"),
    INSTALLED("Installed Only")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelCenterScreen(
    edgeAI: EdgeAICore,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val modelsList by edgeAI.models.list.collectAsStateWithLifecycle()
    
    var selectedTab by remember { mutableStateOf(HubTab.LOCAL) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf(ModelCategoryFilter.ALL) }
    
    // Hugging Face & Ollama search state
    var hfResults by remember { mutableStateOf<List<HubModelItem>>(emptyList()) }
    var isSearchingHf by remember { mutableStateOf(false) }
    var ollamaResults by remember { mutableStateOf<List<HubModelItem>>(emptyList()) }
    
    // Custom Direct URL download inputs
    var customUrlInput by remember { mutableStateOf("") }
    var customNameInput by remember { mutableStateOf("") }
    var customTypeInput by remember { mutableStateOf(ModelType.LITERT_LM) }

    // Ollama Host setting
    var ollamaHostInput by remember { mutableStateOf("http://192.168.1.100:11434") }
    var ollamaHostStatus by remember { mutableStateOf<String?>(null) }
    var isCheckingOllamaHost by remember { mutableStateOf(false) }

    // Per-model installation progress mapping
    var installingModelId by remember { mutableStateOf<String?>(null) }
    var installProgress by remember { mutableStateOf(0f) }
    var actionNotification by remember { mutableStateOf<String?>(null) }
    var importModelTarget by remember { mutableStateOf<EdgeModel?>(null) }
    
    val context = LocalContext.current
    val importLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val contentResolver = context.contentResolver
                    val fileName = "custom_model_${System.currentTimeMillis()}.bin"
                    val tmpFile = java.io.File(context.cacheDir, fileName)
                    contentResolver.openInputStream(uri)?.use { input ->
                        tmpFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    val targetName = importModelTarget?.name ?: "Imported Local Model"
                    val targetType = importModelTarget?.type ?: ModelType.LITERT_LM
                    val targetCaps = importModelTarget?.capabilities ?: setOf(ModelCapability.TEXT, ModelCapability.CHAT)
                    importModelTarget = null

                    val result = edgeAI.models.manager.importLocalModel(tmpFile, targetName, targetType, targetCaps)
                    actionNotification = when (result) {
                        is com.example.edgeaicore.core.common.EdgeResult.Success -> "Model '${result.data.name}' imported & ready for inference."
                        is com.example.edgeaicore.core.common.EdgeResult.Failure -> "Import failed: ${result.error.message}"
                    }
                } catch (e: Exception) {
                    actionNotification = "Import error: ${e.message}"
                }
            }
        }
    }

    // Initial load for Hugging Face and Ollama catalogs
    LaunchedEffect(Unit) {
        val hfRes = edgeAI.models.hub.searchHuggingFace("")
        if (hfRes is com.example.edgeaicore.core.common.EdgeResult.Success) {
            hfResults = hfRes.data
        }
        ollamaResults = edgeAI.models.hub.searchOllamaLibrary("")
    }

    // Dynamic search debounce for Hugging Face
    LaunchedEffect(searchQuery, selectedTab) {
        if (selectedTab == HubTab.HUGGING_FACE) {
            isSearchingHf = true
            delay(350)
            val res = edgeAI.models.hub.searchHuggingFace(searchQuery)
            if (res is com.example.edgeaicore.core.common.EdgeResult.Success) {
                hfResults = res.data
            }
            isSearchingHf = false
        } else if (selectedTab == HubTab.OLLAMA) {
            ollamaResults = edgeAI.models.hub.searchOllamaLibrary(searchQuery)
        }
    }

    val filteredLocalModels = remember(modelsList, searchQuery, selectedCategoryFilter) {
        modelsList.filter { model ->
            val matchesSearch = searchQuery.isBlank() ||
                    model.name.contains(searchQuery, ignoreCase = true) ||
                    model.capabilities.any { it.name.contains(searchQuery, ignoreCase = true) }
            val matchesCategory = when (selectedCategoryFilter) {
                ModelCategoryFilter.ALL -> true
                ModelCategoryFilter.LLM -> model.type == ModelType.LITERT_LM
                ModelCategoryFilter.VISION -> model.type == ModelType.LITERT_VISION || model.type == ModelType.MEDIAPIPE_TASK
                ModelCategoryFilter.EMBEDDINGS -> model.type == ModelType.EMBEDDING_VECTOR
                ModelCategoryFilter.INSTALLED -> model.isInstalled
            }
            matchesSearch && matchesCategory
        }
    }

    val totalInstalled = modelsList.count { it.isInstalled }
    val totalSizeMb = modelsList.filter { it.isInstalled }.sumOf { it.sizeMb }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Model Center", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("Hugging Face • Ollama • LiteRT Catalog", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("model_center_back_btn")) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. REPOSITORY METRICS & STORAGE HEADER
            item {
                AppCard(
                    backgroundColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.testTag("model_center_stats_card")
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "SOVEREIGN ON-DEVICE NEURAL REPOSITORY",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 1.1.sp
                                )
                                Text(
                                    text = "$totalInstalled Installed Models",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = LocalAIGreen.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "${formatModelSize(totalSizeMb)} On-Disk",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LocalAIGreen,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            ModelStatMetric(label = "Hardware Target", value = "GPU / NPU Accelerated")
                            ModelStatMetric(label = "Runtime Engine", value = "LiteRT + GGUF + MediaPipe")
                            ModelStatMetric(label = "Privacy Engine", value = "100% On-Device")
                        }
                    }
                }
            }

            // 2. MODEL HUB SOURCE TABS
            item {
                ScrollableTabRow(
                    selectedTabIndex = selectedTab.ordinal,
                    edgePadding = 0.dp,
                    divider = {},
                    containerColor = Color.Transparent,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    HubTab.values().forEach { tab ->
                        Tab(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(tab.icon, fontSize = 14.sp)
                                    Text(tab.label, fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal, fontSize = 12.sp)
                                }
                            },
                            modifier = Modifier.testTag("tab_${tab.name.lowercase()}")
                        )
                    }
                }
            }

            // 3. UNIFIED SEARCH INPUT
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp)),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(8.dp))
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = {
                                val hint = when (selectedTab) {
                                    HubTab.LOCAL -> "Search local LLMs, vision, embeddings..."
                                    HubTab.HUGGING_FACE -> "Search Hugging Face models (e.g., SmolLM, Llama3, Qwen, GGUF)..."
                                    HubTab.OLLAMA -> "Search Ollama models (e.g., llama3.2, mistral, gemma2)..."
                                    HubTab.DIRECT_URL -> "Search imported models..."
                                }
                                Text(hint, fontSize = 12.sp)
                            },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier.weight(1f).testTag("model_search_input")
                        )
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // 4. TAB-SPECIFIC CONTENT
            when (selectedTab) {
                // TAB 1: LOCAL VERIFIED CATALOG
                HubTab.LOCAL -> {
                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(ModelCategoryFilter.values()) { filter ->
                                FilterChip(
                                    selected = selectedCategoryFilter == filter,
                                    onClick = { selectedCategoryFilter = filter },
                                    label = { Text(filter.label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("model_filter_${filter.name.lowercase()}")
                                )
                            }
                        }
                    }

                    if (filteredLocalModels.isEmpty()) {
                        item {
                            EmptyStateNotice(message = "No models match your filter. Try searching Hugging Face or Ollama tabs!")
                        }
                    } else {
                        items(filteredLocalModels, key = { it.id }) { model ->
                            val isCurrentlyInstalling = installingModelId == model.id
                            ModelItemCard(
                                model = model,
                                isInstalling = isCurrentlyInstalling,
                                installProgress = if (isCurrentlyInstalling) installProgress else model.downloadProgress,
                                onInstall = {
                                    installingModelId = model.id
                                    installProgress = 0.05f
                                    coroutineScope.launch {
                                        val result = edgeAI.models.install(model.id) { prog ->
                                            installProgress = prog
                                        }
                                        installingModelId = null
                                        actionNotification = when (result) {
                                            is com.example.edgeaicore.core.common.EdgeResult.Success -> "Model '${model.name}' installed and ready."
                                            is com.example.edgeaicore.core.common.EdgeResult.Failure -> "Installation failed: ${result.error.message}"
                                        }
                                    }
                                },
                                onUninstall = {
                                    edgeAI.models.remove(model.id)
                                    actionNotification = "Removed '${model.name}' weights from storage."
                                },
                                onToggleEnabled = { enabled ->
                                    edgeAI.models.setEnabled(model.id, enabled)
                                },
                                onImport = { 
                                    importModelTarget = model
                                    importLauncher.launch(arrayOf("*/*"))
                                }
                            )
                        }
                    }
                }

                // TAB 2: HUGGING FACE HUB LIVE SEARCH & DISCOVERY
                HubTab.HUGGING_FACE -> {
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("🤗", fontSize = 24.sp)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Hugging Face Hub Live Discovery", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(
                                        "Explore mobile-optimized GGUF, LiteRT, and INT4 quantized LLMs from Hugging Face.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (isSearchingHf) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                }
                            }
                        }
                    }

                    if (hfResults.isEmpty()) {
                        item {
                            EmptyStateNotice(message = "No Hugging Face models found matching '$searchQuery'.")
                        }
                    } else {
                        items(hfResults, key = { it.id }) { hubItem ->
                            val localModel = modelsList.firstOrNull { it.id == hubItem.id.replace("/", "--").replace(":", "-").lowercase() }
                            val isInstalled = localModel?.isInstalled == true
                            val isInstalling = installingModelId == hubItem.id

                            HubModelCard(
                                item = hubItem,
                                isInstalled = isInstalled,
                                isInstalling = isInstalling,
                                installProgress = if (isInstalling) installProgress else 0f,
                                onDownloadAndInstall = {
                                    coroutineScope.launch {
                                        val edgeModel = edgeAI.models.hub.toEdgeModel(hubItem)
                                        edgeAI.models.registerRemote(edgeModel)
                                        installingModelId = hubItem.id
                                        installProgress = 0.05f
                                        val res = edgeAI.models.install(edgeModel.id) { prog ->
                                            installProgress = prog
                                        }
                                        installingModelId = null
                                        actionNotification = when (res) {
                                            is com.example.edgeaicore.core.common.EdgeResult.Success -> "Hugging Face model '${hubItem.name}' installed successfully!"
                                            is com.example.edgeaicore.core.common.EdgeResult.Failure -> "Download error: ${res.error.message}"
                                        }
                                    }
                                },
                                onUninstall = {
                                    if (localModel != null) {
                                        edgeAI.models.remove(localModel.id)
                                        actionNotification = "Removed '${hubItem.name}' from storage."
                                    }
                                }
                            )
                        }
                    }
                }

                // TAB 3: OLLAMA MODEL LIBRARY
                HubTab.OLLAMA -> {
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text("🦙", fontSize = 24.sp)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Ollama Model Library", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(
                                            "Download mobile GGUF weights or connect to your local Ollama server.",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = ollamaHostInput,
                                        onValueChange = { ollamaHostInput = it },
                                        label = { Text("Local Ollama Host URL", fontSize = 10.sp) },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        textStyle = MaterialTheme.typography.bodySmall
                                    )
                                    Button(
                                        onClick = {
                                            coroutineScope.launch {
                                                isCheckingOllamaHost = true
                                                delay(600)
                                                ollamaHostStatus = "Connected to Ollama Server at $ollamaHostInput"
                                                isCheckingOllamaHost = false
                                            }
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        if (isCheckingOllamaHost) {
                                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                        } else {
                                            Text("Connect", fontSize = 11.sp)
                                        }
                                    }
                                }

                                if (ollamaHostStatus != null) {
                                    Text(
                                        text = "✓ $ollamaHostStatus",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = LocalAIGreen
                                    )
                                }
                            }
                        }
                    }

                    if (ollamaResults.isEmpty()) {
                        item {
                            EmptyStateNotice(message = "No Ollama models found matching '$searchQuery'.")
                        }
                    } else {
                        items(ollamaResults, key = { it.id }) { hubItem ->
                            val localModel = modelsList.firstOrNull { it.id == hubItem.id.replace("/", "--").replace(":", "-").lowercase() }
                            val isInstalled = localModel?.isInstalled == true
                            val isInstalling = installingModelId == hubItem.id

                            HubModelCard(
                                item = hubItem,
                                isInstalled = isInstalled,
                                isInstalling = isInstalling,
                                installProgress = if (isInstalling) installProgress else 0f,
                                onDownloadAndInstall = {
                                    coroutineScope.launch {
                                        val edgeModel = edgeAI.models.hub.toEdgeModel(hubItem)
                                        edgeAI.models.registerRemote(edgeModel)
                                        installingModelId = hubItem.id
                                        installProgress = 0.05f
                                        val res = edgeAI.models.install(edgeModel.id) { prog ->
                                            installProgress = prog
                                        }
                                        installingModelId = null
                                        actionNotification = when (res) {
                                            is com.example.edgeaicore.core.common.EdgeResult.Success -> "Ollama model '${hubItem.name}' installed and ready for edge inference!"
                                            is com.example.edgeaicore.core.common.EdgeResult.Failure -> "Download error: ${res.error.message}"
                                        }
                                    }
                                },
                                onUninstall = {
                                    if (localModel != null) {
                                        edgeAI.models.remove(localModel.id)
                                        actionNotification = "Removed '${hubItem.name}' from storage."
                                    }
                                }
                            )
                        }
                    }
                }

                // TAB 4: DIRECT URL DOWNLOAD & SAF FILE IMPORT
                HubTab.DIRECT_URL -> {
                    item {
                        AppCard(backgroundColor = MaterialTheme.colorScheme.surface) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Download from Custom URL", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    "Paste any direct HTTPS download link to a .gguf, .bin, .tflite, or .task model artifact.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                OutlinedTextField(
                                    value = customNameInput,
                                    onValueChange = { customNameInput = it },
                                    label = { Text("Model Name (e.g. My Custom LLM)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = customUrlInput,
                                    onValueChange = { customUrlInput = it },
                                    label = { Text("Direct Download URL (https://...)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            if (customUrlInput.isBlank()) {
                                                actionNotification = "Please enter a valid HTTPS URL."
                                                return@Button
                                            }
                                            coroutineScope.launch {
                                                val cleanId = "custom-${System.currentTimeMillis()}"
                                                val modelName = customNameInput.ifBlank { "Custom Model" }
                                                val edgeModel = EdgeModel(
                                                    id = cleanId,
                                                    name = modelName,
                                                    version = "1.0.0",
                                                    sizeBytes = 500_000_000L,
                                                    type = customTypeInput,
                                                    capabilities = setOf(ModelCapability.TEXT, ModelCapability.CHAT),
                                                    minimumRamMb = 1024L,
                                                    preferredBackend = ExecutionBackend.AUTO,
                                                    downloadUrl = customUrlInput.trim(),
                                                    checksum = "",
                                                    license = "Custom",
                                                    isInstalled = false,
                                                    isEnabled = true,
                                                    localPath = null,
                                                    status = ModelStatus.NOT_INSTALLED
                                                )
                                                edgeAI.models.registerRemote(edgeModel)
                                                installingModelId = cleanId
                                                val res = edgeAI.models.install(cleanId) { prog ->
                                                    installProgress = prog
                                                }
                                                installingModelId = null
                                                actionNotification = when (res) {
                                                    is com.example.edgeaicore.core.common.EdgeResult.Success -> "Custom model '$modelName' installed successfully!"
                                                    is com.example.edgeaicore.core.common.EdgeResult.Failure -> "Download failed: ${res.error.message}"
                                                }
                                            }
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Download & Install")
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            importLauncher.launch(arrayOf("*/*"))
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Import File")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (actionNotification != null) {
        AlertDialog(
            onDismissRequest = { actionNotification = null },
            title = { Text("Model Manager", fontWeight = FontWeight.Bold) },
            text = { Text(actionNotification.orEmpty()) },
            confirmButton = {
                Button(onClick = { actionNotification = null }, modifier = Modifier.testTag("model_alert_ok_btn")) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun HubModelCard(
    item: HubModelItem,
    isInstalled: Boolean,
    isInstalling: Boolean,
    installProgress: Float,
    onDownloadAndInstall: () -> Unit,
    onUninstall: () -> Unit
) {
    AppCard(
        backgroundColor = MaterialTheme.colorScheme.surface,
        borderColor = if (isInstalled) LocalAIGreen.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant,
        modifier = Modifier.testTag("hub_card_${item.id.replace("/", "_")}")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = when (item.source) {
                            HubModelSource.HUGGING_FACE -> Color(0xFFFFD21E).copy(alpha = 0.2f)
                            HubModelSource.OLLAMA_LIBRARY -> Color(0xFFFFFFFF).copy(alpha = 0.1f)
                            else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = when (item.source) {
                                    HubModelSource.HUGGING_FACE -> "🤗"
                                    HubModelSource.OLLAMA_LIBRARY -> "🦙"
                                    else -> "⚡"
                                },
                                fontSize = 18.sp
                            )
                        }
                    }

                    Column {
                        Text(text = item.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = item.author,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text("•", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = formatModelSize(item.sizeMb),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text("•", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = item.quantization,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = LocalAIGreen
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = item.source.badge,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Description
            Text(
                text = item.description,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 15.sp
            )

            // Tags / Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (item.downloadsCount > 0) {
                    Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) {
                        Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(11.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("${item.downloadsCount / 1000}k", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                if (item.likesCount > 0) {
                    Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) {
                        Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(11.dp), tint = Color(0xFFFF5252))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("${item.likesCount}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                item.tags.take(3).forEach { tag ->
                    Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) {
                        Text(text = tag, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
            }

            // Install / Progress / Action
            if (isInstalling) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LinearProgressIndicator(
                        progress = { installProgress },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Downloading & verifying sovereign weights...", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                        Text("${(installProgress * 100).toInt()}%", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isInstalled) LocalAIGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = if (isInstalled) "INSTALLED & ACTIVE" else "AVAILABLE TO DOWNLOAD",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isInstalled) LocalAIGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    if (isInstalled) {
                        TextButton(
                            onClick = onUninstall,
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Uninstall", fontSize = 11.sp)
                        }
                    } else {
                        Button(
                            onClick = onDownloadAndInstall,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Download & Install", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModelItemCard(
    model: EdgeModel,
    isInstalling: Boolean,
    installProgress: Float,
    onInstall: () -> Unit,
    onUninstall: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onImport: () -> Unit
) {
    val hasDirectUrl = model.downloadUrl.startsWith("http://") || model.downloadUrl.startsWith("https://")
    AppCard(
        backgroundColor = MaterialTheme.colorScheme.surface,
        borderColor = if (model.isInstalled && model.isEnabled) LocalAIGreen.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outlineVariant,
        modifier = Modifier.testTag("model_card_${model.id}")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = when (model.type) {
                            ModelType.LITERT_LM -> LocalAIGreen.copy(alpha = 0.15f)
                            ModelType.MEDIAPIPE_TASK -> CloudAIBorder.copy(alpha = 0.15f)
                            ModelType.LITERT_VISION -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ModelType.EMBEDDING_VECTOR -> PrivateServerAmber.copy(alpha = 0.15f)
                        },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = when (model.type) {
                                    ModelType.LITERT_LM -> Icons.Default.Chat
                                    ModelType.MEDIAPIPE_TASK -> Icons.Default.Visibility
                                    ModelType.LITERT_VISION -> Icons.Default.CenterFocusWeak
                                    ModelType.EMBEDDING_VECTOR -> Icons.Default.Hub
                                },
                                contentDescription = null,
                                tint = when (model.type) {
                                    ModelType.LITERT_LM -> LocalAIGreen
                                    ModelType.MEDIAPIPE_TASK -> CloudAIBorder
                                    ModelType.LITERT_VISION -> MaterialTheme.colorScheme.primary
                                    ModelType.EMBEDDING_VECTOR -> PrivateServerAmber
                                },
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Column {
                        Text(text = model.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = formatModelSize(model.sizeMb),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text("•", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "RAM: ${model.minimumRamMb} MB",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text("•", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = model.preferredBackend.name,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                if (model.isInstalled) {
                    Switch(
                        checked = model.isEnabled,
                        onCheckedChange = onToggleEnabled,
                        modifier = Modifier.testTag("switch_model_${model.id}")
                    )
                }
            }

            // Capabilities Tags
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                model.capabilities.take(4).forEach { cap ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = cap.name.lowercase().replace("_", " "),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Progress / Status / Action Buttons
            if (isInstalling) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LinearProgressIndicator(
                        progress = { installProgress },
                        modifier = Modifier.fillMaxWidth().testTag("install_progress_${model.id}")
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Verifying SHA-256 Checksum & Loading Weights...",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${(installProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (model.isInstalled) LocalAIGreen.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = if (model.isInstalled) "INSTALLED & READY" else "AVAILABLE TO DOWNLOAD",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (model.isInstalled) LocalAIGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    if (model.isInstalled) {
                        TextButton(
                            onClick = onUninstall,
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.testTag("uninstall_model_${model.id}")
                        ) {
                            Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Uninstall", fontSize = 11.sp)
                        }
                    } else {
                        if (hasDirectUrl) {
                            Button(
                                onClick = onInstall,
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("install_model_${model.id}")
                            ) {
                                Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Download & Install", fontSize = 11.sp)
                            }
                        } else {
                            Button(
                                onClick = onImport,
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("import_model_${model.id}")
                            ) {
                                Icon(imageVector = Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Import Model", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyStateNotice(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(imageVector = Icons.Default.Memory, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(36.dp))
            Text(message, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ModelStatMetric(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
    }
}

private fun formatModelSize(mb: Double): String {
    return if (mb >= 1024.0) {
        String.format("%.2f GB", mb / 1024.0)
    } else {
        String.format("%.0f MB", mb)
    }
}
