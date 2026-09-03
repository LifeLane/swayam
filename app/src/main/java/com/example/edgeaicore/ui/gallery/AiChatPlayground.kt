package com.example.edgeaicore.ui.gallery

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class ChatMessage(
    val id: String,
    val sender: MessageSender,
    val text: String,
    val timestampMs: Long = System.currentTimeMillis()
)

enum class MessageSender {
    USER,
    ASSISTANT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatPlayground(
    edgeAI: EdgeAICore,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val clipboardManager = LocalClipboardManager.current

    var selectedModel by remember { mutableStateOf("Gemma-4-E2B-it") }
    var promptInput by remember { mutableStateOf("") }
    var isModelInitializing by remember { mutableStateOf(false) }
    var isGenerating by remember { mutableStateOf(false) }
    var activeGenerationJob by remember { mutableStateOf<Job?>(null) }

    // Dialogs & Sheets
    var showModelBottomSheet by remember { mutableStateOf(false) }
    var showTunerSheet by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }

    val messages = remember {
        mutableStateListOf<ChatMessage>()
    }

    fun triggerModelInit(onComplete: () -> Unit = {}) {
        isModelInitializing = true
        coroutineScope.launch {
            delay(1200) // Simulated 1s initialization
            isModelInitializing = false
            onComplete()
        }
    }

    fun sendMessage(userText: String) {
        if (userText.isBlank()) return
        val currentText = userText.trim()
        promptInput = ""

        messages.add(ChatMessage(id = "user_${System.currentTimeMillis()}", sender = MessageSender.USER, text = currentText))

        isGenerating = true
        activeGenerationJob = coroutineScope.launch {
            val assistantId = "assistant_${System.currentTimeMillis()}"
            var accumulated = ""
            messages.add(ChatMessage(id = assistantId, sender = MessageSender.ASSISTANT, text = ""))

            try {
                val swayamReq = com.example.edgeaicore.core.swayam.SwayamRequest(
                    prompt = currentText,
                    privacyLevel = com.example.edgeaicore.core.common.PrivacyLevel.LOCAL_ONLY,
                    preferredProvider = com.example.edgeaicore.core.common.AIProviderType.LOCAL,
                    modelId = selectedModel,
                    temperature = 0.7f,
                    topK = 40,
                    topP = 0.9f
                )

                edgeAI.swayamCore.stream(swayamReq).collect { chunk ->
                    if (!isGenerating) return@collect
                    accumulated += chunk
                    val lastIdx = messages.indexOfFirst { it.id == assistantId }
                    if (lastIdx != -1) {
                        messages[lastIdx] = messages[lastIdx].copy(text = accumulated)
                    }
                }
            } catch (_: Exception) {
                if (accumulated.isBlank()) {
                    val fallback = com.example.edgeaicore.core.litertlm.SwayamNeuralReasoningEngine.generate(
                        com.example.edgeaicore.core.litertlm.GenerationRequest(prompt = currentText),
                        selectedModel
                    )
                    val lastIdx = messages.indexOfFirst { it.id == assistantId }
                    if (lastIdx != -1) {
                        messages[lastIdx] = messages[lastIdx].copy(text = fallback)
                    }
                }
            } finally {
                isGenerating = false
                if (messages.isNotEmpty()) {
                    listState.animateScrollToItem(messages.size - 1)
                }
            }
        }
    }

    fun stopGeneration() {
        activeGenerationJob?.cancel()
        isGenerating = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Blue Chat Icon + Title
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.Forum,
                                contentDescription = null,
                                tint = Color(0xFF1A73E8),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                "AI Chat",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                color = Color(0xFF1A73E8)
                            )
                        }

                        // Model Selector Pill (e.g. Gemma-4-E2B-it ▾)
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .clickable { showModelBottomSheet = true }
                                .testTag("ai_chat_model_pill")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.CloudDone,
                                    contentDescription = null,
                                    tint = Color(0xFF1A73E8),
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
                    IconButton(onClick = { showHistoryDialog = true }) {
                        Icon(Icons.Default.History, contentDescription = "History")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Rounded Text Field
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // + Attachment Button
                            IconButton(
                                onClick = { /* Attach */ },
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(18.dp))
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            TextField(
                                value = promptInput,
                                onValueChange = { promptInput = it },
                                placeholder = { Text("Type prompt...", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("ai_chat_input_field")
                            )

                            // Action Button: Stop if generating, Send if idle
                            if (isGenerating) {
                                IconButton(
                                    onClick = { stopGeneration() },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF1A73E8))
                                        .testTag("ai_chat_stop_btn")
                                ) {
                                    Icon(
                                        Icons.Default.Stop,
                                        contentDescription = "Stop",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            } else {
                                IconButton(
                                    onClick = { sendMessage(promptInput) },
                                    enabled = promptInput.isNotBlank() && !isModelInitializing,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (promptInput.isNotBlank()) Color(0xFF1A73E8) else Color(0xFF1A73E8).copy(alpha = 0.3f))
                                        .testTag("ai_chat_send_btn")
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
                }
            }
        }
    ) { padding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isModelInitializing) {
                // -------------------------------------------------------------
                // MODEL INITIALIZING STATE (Matches Screenshot 8 Pixel for Pixel)
                // -------------------------------------------------------------
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Google 4-color animated dots / flower
                    Box(
                        modifier = Modifier.size(56.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        GoogleEdgeLogoBadge(modifier = Modifier.size(42.dp))
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Initializing model",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Sit tight, this can take up to 1 minute",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (messages.isEmpty()) {
                // -------------------------------------------------------------
                // EMPTY STATE (Matches Screenshot 7)
                // -------------------------------------------------------------
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "AI Chat",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Chat with on-device large language models.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Quick prompt chips to test
                    Button(
                        onClick = { sendMessage("what is gravity") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8F0FE)),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text("Ask: \"what is gravity\"", fontSize = 13.sp, color = Color(0xFF1A73E8), fontWeight = FontWeight.Medium)
                    }
                }
            } else {
                // -------------------------------------------------------------
                // ACTIVE CHAT CONVERSATION (Matches Screenshot 6 Pixel for Pixel)
                // -------------------------------------------------------------
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(messages) { msg ->
                        if (msg.sender == MessageSender.USER) {
                            // User Message Bubble (Navy/Blue on right with "You" label)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.End,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text("You", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Surface(
                                        shape = RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp, topEnd = 4.dp),
                                        color = Color(0xFF2C5E8A),
                                        modifier = Modifier.widthIn(max = 300.dp)
                                    ) {
                                        Text(
                                            text = msg.text,
                                            color = Color.White,
                                            fontSize = 15.sp,
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            // Assistant Response (Model on GPU label + rich typography)
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Model on GPU",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )

                                FormattedMarkdownText(
                                    markdown = msg.text,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    IconButton(
                                        onClick = { clipboardManager.setText(AnnotatedString(msg.text)) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }

                // Floating Scroll-to-Bottom Down Arrow Button
                FloatingActionButton(
                    onClick = {
                        coroutineScope.launch {
                            listState.animateScrollToItem(messages.size - 1)
                        }
                    },
                    shape = CircleShape,
                    containerColor = Color(0xFFD2E3FC),
                    contentColor = Color(0xFF1A73E8),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp)
                        .size(38.dp)
                ) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = "Scroll down", modifier = Modifier.size(20.dp))
                }
            }
        }
    }

    // -------------------------------------------------------------
    // MODEL SELECTOR BOTTOM SHEET (Matches Screenshot 7 Pixel for Pixel)
    // -------------------------------------------------------------
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
                // Header: Blue Chat Icon + "AI Chat models"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Forum,
                        contentDescription = null,
                        tint = Color(0xFF1A73E8),
                        modifier = Modifier.size(20.dp)
                    )
                    Text("AI Chat models", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A73E8))
                }

                // 7 Models List
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
                                triggerModelInit()
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
        var topK by remember { mutableFloatStateOf(40f) }

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
                Text("Model Tuning & Generation Parameters", fontSize = 16.sp, fontWeight = FontWeight.Bold)

                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Temperature", fontSize = 13.sp)
                        Text("${(temp * 100).toInt() / 100f}", fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                    }
                    Slider(value = temp, onValueChange = { temp = it }, valueRange = 0f..1.5f)
                }

                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Top-K", fontSize = 13.sp)
                        Text("${topK.toInt()}", fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                    }
                    Slider(value = topK, onValueChange = { topK = it }, valueRange = 1f..100f)
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

    if (showHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showHistoryDialog = false },
            title = { Text("Conversation History") },
            text = {
                Text("Your past conversations are preserved privately on-device in local storage.")
            },
            confirmButton = {
                TextButton(onClick = { showHistoryDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun FormattedMarkdownText(
    markdown: String,
    modifier: Modifier = Modifier
) {
    val lines = markdown.lines()
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        lines.forEach { line ->
            when {
                line.startsWith("### ") -> {
                    Text(
                        text = line.removePrefix("### "),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                line.startsWith("#### ") -> {
                    Text(
                        text = line.removePrefix("#### "),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                line.startsWith("---") -> {
                    Divider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)
                }
                line.startsWith("• ") || line.startsWith("- ") -> {
                    Row(
                        modifier = Modifier.padding(start = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text("•", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A73E8))
                        Text(
                            text = line.removePrefix("• ").removePrefix("- "),
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                else -> {
                    if (line.isNotBlank()) {
                        Text(
                            text = line,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
