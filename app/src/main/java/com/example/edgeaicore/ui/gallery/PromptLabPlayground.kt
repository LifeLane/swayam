package com.example.edgeaicore.ui.gallery

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.edgeaicore.EdgeAICore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class PromptLabTab(val title: String) {
    FREE_FORM("Free form"),
    REWRITE_TONE("Rewrite tone"),
    SUMMARIZE_TEXT("Summarize text"),
    CODE_SNIPPET("Code snippet")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromptLabPlayground(
    edgeAI: EdgeAICore,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    var selectedTab by remember { mutableStateOf(PromptLabTab.FREE_FORM) }
    var selectedModel by remember { mutableStateOf("Gemma-4-E2B-it") }
    var inputContent by remember { mutableStateOf("") }
    var responseOutput by remember { mutableStateOf("") }
    var isRunning by remember { mutableStateOf(false) }

    // Selected Tone / Summarize Options
    var selectedTone by remember { mutableStateOf("Professional") }
    var selectedLength by remember { mutableStateOf("Bullet points") }

    // Dialogs & Sheets
    var showModelBottomSheet by remember { mutableStateOf(false) }
    var showTunerSheet by remember { mutableStateOf(false) }

    fun runPromptLab() {
        if (inputContent.isBlank()) return
        isRunning = true
        responseOutput = ""

        coroutineScope.launch {
            try {
                val formattedPrompt = when (selectedTab) {
                    PromptLabTab.FREE_FORM -> inputContent
                    PromptLabTab.REWRITE_TONE -> "Rewrite the following text in a $selectedTone tone:\n\n$inputContent"
                    PromptLabTab.SUMMARIZE_TEXT -> "Summarize the following text in a $selectedLength format:\n\n$inputContent"
                    PromptLabTab.CODE_SNIPPET -> "Write an optimized, production-ready code implementation for: $inputContent"
                }

                val swayamReq = com.example.edgeaicore.core.swayam.SwayamRequest(
                    prompt = formattedPrompt,
                    privacyLevel = com.example.edgeaicore.core.common.PrivacyLevel.LOCAL_ONLY,
                    preferredProvider = com.example.edgeaicore.core.common.AIProviderType.LOCAL,
                    modelId = selectedModel,
                    temperature = 0.7f,
                    topK = 40,
                    topP = 0.9f
                )

                edgeAI.swayamCore.stream(swayamReq).collect { chunk ->
                    if (!isRunning) return@collect
                    responseOutput += chunk
                }
            } catch (_: Exception) {
                if (responseOutput.isBlank()) {
                    responseOutput = com.example.edgeaicore.core.litertlm.SwayamNeuralReasoningEngine.generate(
                        com.example.edgeaicore.core.litertlm.GenerationRequest(prompt = inputContent),
                        selectedModel
                    )
                }
            } finally {
                isRunning = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Red 4-square grid Icon + Title
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.Widgets,
                                contentDescription = null,
                                tint = Color(0xFFEA4335), // Google Red
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                "Prompt Lab",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                color = Color(0xFFEA4335)
                            )
                        }

                        // Model Selector Pill (e.g. Gemma-4-E2B-it ▾)
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .clickable { showModelBottomSheet = true }
                                .testTag("prompt_lab_model_pill")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.CloudDone,
                                    contentDescription = null,
                                    tint = Color(0xFFEA4335),
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = selectedModel,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showTunerSheet = true }) {
                        Icon(Icons.Default.Tune, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // -------------------------------------------------------------
            // HORIZONTAL TABS (Matches Screenshot 2 Pixel for Pixel)
            // -------------------------------------------------------------
            ScrollableTabRow(
                selectedTabIndex = selectedTab.ordinal,
                edgePadding = 16.dp,
                divider = {},
                containerColor = MaterialTheme.colorScheme.surface,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
                        color = Color(0xFF1A73E8) // Google Blue underline
                    )
                }
            ) {
                PromptLabTab.values().forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = {
                            selectedTab = tab
                            if (inputContent.isBlank()) {
                                inputContent = when (tab) {
                                    PromptLabTab.FREE_FORM -> ""
                                    PromptLabTab.REWRITE_TONE -> "Let's launch the new on-device AI models as soon as possible because the latency on GPU is amazing."
                                    PromptLabTab.SUMMARIZE_TEXT -> "LiteRT is Google's high-performance runtime for on-device AI across Android devices. It enables direct execution of models like Gemma 4 with hardware acceleration across GPUs, NPUs, and CPUs."
                                    PromptLabTab.CODE_SNIPPET -> "Write a function in Kotlin to stream tokens from LiteRT on-device."
                                }
                            }
                        },
                        text = {
                            Text(
                                text = tab.title,
                                fontSize = 13.sp,
                                fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == tab) Color(0xFF1A73E8) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }

            // Tone / Length Options row for specific tabs
            if (selectedTab == PromptLabTab.REWRITE_TONE) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Professional", "Casual", "Concise", "Persuasive").forEach { tone ->
                        FilterChip(
                            selected = selectedTone == tone,
                            onClick = { selectedTone = tone },
                            label = { Text(tone, fontSize = 11.sp) }
                        )
                    }
                }
            }

            // -------------------------------------------------------------
            // TWO-PANE SPLIT SCREEN (Matches Screenshot 2 Pixel for Pixel)
            // -------------------------------------------------------------
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                // TOP PANE: "Enter content"
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        TextField(
                            value = inputContent,
                            onValueChange = { inputContent = it },
                            placeholder = { Text("Enter content", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                                .testTag("prompt_lab_content_input")
                        )

                        // Bottom right floating action buttons: [+] and Send [>]
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { /* Attach */ },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF1F3F4))
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(20.dp), tint = Color(0xFF3C4043))
                            }

                            IconButton(
                                onClick = { runPromptLab() },
                                enabled = inputContent.isNotBlank() && !isRunning,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (inputContent.isNotBlank()) Color(0xFF1A73E8) else Color(0xFF1A73E8).copy(alpha = 0.3f))
                                    .testTag("prompt_lab_send_btn")
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // SPLITTER DRAG HANDLE (Matches Screenshot 2)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .background(Color(0xFFE8EAED)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFF9AA0A6))
                    )
                }

                // BOTTOM PANE: "Response will appear here"
                Surface(
                    color = Color(0xFFEEF2FA), // Shaded blue/grey pane
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        if (isRunning) {
                            Row(
                                modifier = Modifier.align(Alignment.Center),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color(0xFF1A73E8))
                                Text("Generating with on-device ${selectedModel}...", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else if (responseOutput.isBlank()) {
                            Text(
                                text = "Response will appear here",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("ON-DEVICE RESPONSE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A73E8))
                                        IconButton(
                                            onClick = { clipboardManager.setText(AnnotatedString(responseOutput)) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                                item {
                                    FormattedMarkdownText(markdown = responseOutput)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Model Selector Bottom Sheet
    if (showModelBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showModelBottomSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Widgets, contentDescription = null, tint = Color(0xFFEA4335), modifier = Modifier.size(20.dp))
                    Text("Prompt Lab models", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEA4335))
                }

                val modelsList = listOf(
                    Triple("Gemma-4-E2B-it", "2.6 GB", true),
                    Triple("Gemma-4-E4B-it", "3.7 GB", false),
                    Triple("Gemma-3n-E2B-it", "3.7 GB", false),
                    Triple("Gemma-3n-E4B-it", "4.9 GB", false),
                    Triple("Gemma3-1B-IT", "584.4 MB", true),
                    Triple("Qwen2.5-1.5B-Instruct", "1.6 GB", true),
                    Triple("DeepSeek-R1-Distill-Qwen-1.5B", "1.8 GB", true)
                )

                modelsList.forEach { (name, size, isDownloaded) ->
                    val isSelected = name == selectedModel
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedModel = name
                                showModelBottomSheet = false
                            }
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(name, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, fontSize = 15.sp)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (isDownloaded) {
                                    Icon(Icons.Default.CloudDone, contentDescription = null, tint = Color(0xFF1A73E8), modifier = Modifier.size(14.dp))
                                } else {
                                    Icon(Icons.Default.HelpOutline, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                                }
                                Text(size, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        if (isSelected) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Selected", tint = Color(0xFF1A73E8), modifier = Modifier.size(22.dp))
                        }
                    }
                }
            }
        }
    }

    // Tuner Settings Sheet
    if (showTunerSheet) {
        var temp by remember { mutableFloatStateOf(0.7f) }
        var maxTokens by remember { mutableFloatStateOf(2048f) }

        ModalBottomSheet(
            onDismissRequest = { showTunerSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Prompt Lab Parameters", fontSize = 16.sp, fontWeight = FontWeight.Bold)

                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Temperature", fontSize = 13.sp)
                        Text("${(temp * 100).toInt() / 100f}", fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                    }
                    Slider(value = temp, onValueChange = { temp = it }, valueRange = 0f..1.5f)
                }

                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Max Tokens", fontSize = 13.sp)
                        Text("${maxTokens.toInt()}", fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                    }
                    Slider(value = maxTokens, onValueChange = { maxTokens = it }, valueRange = 256f..4096f)
                }

                Button(
                    onClick = { showTunerSheet = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Apply Parameters")
                }
            }
        }
    }
}
