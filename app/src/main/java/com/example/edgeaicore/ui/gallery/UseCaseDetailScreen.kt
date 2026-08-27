package com.example.edgeaicore.ui.gallery

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.edgeaicore.EdgeAICore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UseCaseDetailScreen(
    useCaseType: EdgeUseCaseType,
    edgeAI: EdgeAICore,
    onBack: () -> Unit,
    onLaunchPlayground: (EdgeUseCaseType, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val models = remember(useCaseType) { GalleryModelData.getModelsForUseCase(useCaseType) }

    var showApiDocsDialog by remember { mutableStateOf(false) }
    var showExampleCodeDialog by remember { mutableStateOf(false) }
    var showLicenseDialog by remember { mutableStateOf(false) }
    var selectedModelLicense by remember { mutableStateOf<GalleryModelCardInfo?>(null) }

    // Download state simulator for models
    var downloadedModels by remember { mutableStateOf(models.filter { it.isDownloaded }.map { it.id }.toSet()) }
    var downloadingModelId by remember { mutableStateOf<String?>(null) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }

    fun triggerModelDownload(modelId: String) {
        downloadingModelId = modelId
        downloadProgress = 0f
        coroutineScope.launch {
            for (p in 1..10) {
                delay(120)
                downloadProgress = p / 10f
            }
            downloadedModels = downloadedModels + modelId
            downloadingModelId = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(useCaseType.title, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("usecase_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 1. HERO BADGE & TITLE
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    // Big Icon Squircle
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(useCaseType.themeColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = useCaseType.icon,
                            contentDescription = null,
                            tint = useCaseType.themeColor,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Title & Badges
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = useCaseType.title,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        if (useCaseType.isExperimental) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF1A73E8).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "EXPERIMENTAL",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1A73E8),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    // Description text
                    Text(
                        text = useCaseType.fullDescription,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )

                    // Links Row: API Documentation & Example Code
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.clickable { showApiDocsDialog = true }
                        ) {
                            Icon(Icons.Outlined.Description, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF1A73E8))
                            Text("API Documentation", fontSize = 13.sp, color = Color(0xFF1A73E8), fontWeight = FontWeight.Medium)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.clickable { showExampleCodeDialog = true }
                        ) {
                            Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF1A73E8))
                            Text("Example code", fontSize = 13.sp, color = Color(0xFF1A73E8), fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            // 2. MODELS SECTION HEADER
            item {
                Text(
                    text = "${useCaseType.availableModelsCount} models available",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }

            item {
                Text(
                    text = "Recommended models",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // 3. MODEL CARDS
            items(models) { model ->
                val isDownloaded = downloadedModels.contains(model.id)
                val isCurrentlyDownloading = downloadingModelId == model.id

                ModelDetailCard(
                    model = model,
                    isDownloaded = isDownloaded,
                    isDownloading = isCurrentlyDownloading,
                    downloadProgress = if (isCurrentlyDownloading) downloadProgress else 1f,
                    themeColor = useCaseType.themeColor,
                    onTryIt = { onLaunchPlayground(useCaseType, model.id) },
                    onDownload = { triggerModelDownload(model.id) },
                    onShowLicense = {
                        selectedModelLicense = model
                        showLicenseDialog = true
                    }
                )
            }
        }
    }

    // API Documentation Dialog
    if (showApiDocsDialog) {
        AlertDialog(
            onDismissRequest = { showApiDocsDialog = false },
            title = { Text("LiteRT-LM Android API") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Integrate on-device Gemma models with LiteRT-LM in Kotlin:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1E1E1E),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = """
                            val lmEngine = LiteRtEngine.builder()
                              .setModelPath("models/${useCaseType.defaultModelId}.litert")
                              .setBackend(Backend.GPU)
                              .setMaxTokens(4096)
                              .build()

                            lmEngine.generateStream("Prompt") { token ->
                              print(token)
                            }
                            """.trimIndent(),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF81C784),
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showApiDocsDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Example Code Dialog
    if (showExampleCodeDialog) {
        AlertDialog(
            onDismissRequest = { showExampleCodeDialog = false },
            title = { Text("Example Code: ${useCaseType.title}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1E1E1E),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = when (useCaseType) {
                                EdgeUseCaseType.TINY_GARDEN -> """
                                val functionGemma = FunctionGemmaEngine(model = "TinyGarden-270M")
                                val toolCall = functionGemma.parse("Plant sunflower in plot 1")
                                gardenController.execute(toolCall)
                                """.trimIndent()
                                EdgeUseCaseType.MOBILE_ACTIONS -> """
                                val functionGemma = FunctionGemmaEngine(model = "MobileActions-270M")
                                val toolCall = functionGemma.parse("Turn on flashlight and set timer")
                                deviceActionDispatcher.dispatch(toolCall)
                                """.trimIndent()
                                else -> """
                                val session = GemmaSession.create(
                                  model = "${useCaseType.defaultModelId}",
                                  temperature = 0.7f
                                )
                                val response = session.chat("Hello on-device Gemma!")
                                """.trimIndent()
                            },
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF81C784),
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showExampleCodeDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // License Dialog
    if (showLicenseDialog && selectedModelLicense != null) {
        AlertDialog(
            onDismissRequest = { showLicenseDialog = false },
            title = { Text("Model License: ${selectedModelLicense!!.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Governed under the Google Gemma Terms of Use.", fontSize = 13.sp)
                    Text("Free for research and commercial mobile applications on-device.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                TextButton(onClick = { showLicenseDialog = false }) {
                    Text("I Agree")
                }
            }
        )
    }
}

@Composable
fun ModelDetailCard(
    model: GalleryModelCardInfo,
    isDownloaded: Boolean,
    isDownloading: Boolean,
    downloadProgress: Float,
    themeColor: Color,
    onTryIt: () -> Unit,
    onDownload: () -> Unit,
    onShowLicense: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (model.isBestOverall) themeColor.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Best overall badge if applicable
            if (model.isBestOverall) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("⭐", fontSize = 12.sp)
                    Text(
                        text = "Best overall",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF9AB00)
                    )
                }
            }

            // Model Title & Size
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = model.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (isDownloaded) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color(0xFF34A853))
                        } else {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            text = model.sizeDisplay,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Action Button: "Try it" or "Download"
                if (isDownloaded) {
                    Button(
                        onClick = onTryIt,
                        colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("try_model_${model.id}")
                    ) {
                        Text("Try it", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
                    }
                } else if (isDownloading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = { downloadProgress },
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 3.dp,
                            color = themeColor
                        )
                        Text("${(downloadProgress * 100).toInt()}%", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                } else {
                    OutlinedButton(
                        onClick = onDownload,
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Download", fontSize = 12.sp)
                    }
                }
            }

            // License Link
            Text(
                text = "↗ ${model.licenseTitle}",
                fontSize = 12.sp,
                color = Color(0xFF1A73E8),
                modifier = Modifier.clickable(onClick = onShowLicense)
            )

            // Description
            Text(
                text = model.description,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
        }
    }
}
