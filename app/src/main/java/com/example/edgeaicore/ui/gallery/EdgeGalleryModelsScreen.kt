package com.example.edgeaicore.ui.gallery

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
fun EdgeGalleryModelsScreen(
    edgeAI: EdgeAICore,
    onBack: () -> Unit,
    onLaunchModelUseCase: (GalleryModelCardInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val allModels = remember { GalleryModelData.ALL_GALLERY_MODELS }

    var searchQuery by remember { mutableStateOf("") }
    var downloadedModelIds by remember {
        mutableStateOf(allModels.filter { it.isDownloaded }.map { it.id }.toSet())
    }
    var downloadingModelId by remember { mutableStateOf<String?>(null) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showLicenseDialog by remember { mutableStateOf<GalleryModelCardInfo?>(null) }

    val filteredModels = remember(searchQuery, downloadedModelIds) {
        if (searchQuery.isBlank()) allModels
        else allModels.filter { it.name.contains(searchQuery, ignoreCase = true) || it.description.contains(searchQuery, ignoreCase = true) }
    }

    fun downloadModel(modelId: String) {
        downloadingModelId = modelId
        downloadProgress = 0f
        coroutineScope.launch {
            for (p in 1..10) {
                delay(100)
                downloadProgress = p / 10f
            }
            downloadedModelIds = downloadedModelIds + modelId
            downloadingModelId = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Models (${filteredModels.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showImportDialog = true },
                containerColor = Color(0xFF1A73E8),
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.testTag("gallery_import_model_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Import Model")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search on-device models...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Model Cards
            items(filteredModels) { model ->
                val isDownloaded = downloadedModelIds.contains(model.id)
                val isDownloading = downloadingModelId == model.id

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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
                                    Icon(
                                        if (isDownloaded) Icons.Default.CheckCircle else Icons.Default.CloudDownload,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = if (isDownloaded) Color(0xFF34A853) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = model.sizeDisplay,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (isDownloaded) {
                                Button(
                                    onClick = { onLaunchModelUseCase(model) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8)),
                                    shape = RoundedCornerShape(20.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                                ) {
                                    Text("Try it", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(12.dp))
                                }
                            } else if (isDownloading) {
                                CircularProgressIndicator(
                                    progress = { downloadProgress },
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 3.dp,
                                    color = Color(0xFF1A73E8)
                                )
                            } else {
                                OutlinedButton(
                                    onClick = { downloadModel(model.id) },
                                    shape = RoundedCornerShape(20.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Download", fontSize = 12.sp)
                                }
                            }
                        }

                        Text(
                            text = "↗ Learn more and see model license",
                            fontSize = 12.sp,
                            color = Color(0xFF1A73E8),
                            modifier = Modifier.clickable { showLicenseDialog = model }
                        )

                        Text(
                            text = model.description,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }

    // Import Model Modal
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import Custom On-Device Model") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select a local LiteRT (.litert), MediaPipe Task (.task), or GGUF (.gguf) model binary from device storage.")
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Supported Quantizations: INT4, INT8, FP16\nAccelerators: GPU (Vulkan/OpenCL), NPU (NNAPI)",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showImportDialog = false }) {
                    Text("Select File")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // License Dialog
    if (showLicenseDialog != null) {
        AlertDialog(
            onDismissRequest = { showLicenseDialog = null },
            title = { Text("Model License: ${showLicenseDialog!!.name}") },
            text = {
                Text("Governed by Google Gemma Terms of Use. Fully sovereign on-device processing.")
            },
            confirmButton = {
                TextButton(onClick = { showLicenseDialog = null }) {
                    Text("Close")
                }
            }
        )
    }
}
