package com.example.edgeaicore.ui.gallery

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class SampleAudioRecording(
    val id: String,
    val title: String,
    val language: String,
    val duration: String,
    val transcriptOriginal: String,
    val transcriptEnglish: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioScribePlayground(
    edgeAI: EdgeAICore,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    val sampleClips = remember {
        listOf(
            SampleAudioRecording(
                id = "meeting",
                title = "Product Standup (EN)",
                language = "English (US)",
                duration = "0:14",
                transcriptOriginal = "[00:00.200 - 00:04.100] Let's finalize the on-device LiteRT integration for Gemma 4.\n[00:04.200 - 00:09.500] Benchmarks show 24 ms per token latency on the mobile NPU.\n[00:09.600 - 00:14.000] We will deploy the Tiny Garden and Mobile Actions tools today.",
                transcriptEnglish = "[00:00.200 - 00:04.100] Let's finalize the on-device LiteRT integration for Gemma 4.\n[00:04.200 - 00:09.500] Benchmarks show 24 ms per token latency on the mobile NPU.\n[00:09.600 - 00:14.000] We will deploy the Tiny Garden and Mobile Actions tools today."
            ),
            SampleAudioRecording(
                id = "french",
                title = "Medical Note (FR)",
                language = "French (FR)",
                duration = "0:11",
                transcriptOriginal = "[00:00.100 - 00:05.300] Le patient présente des douleurs thoraciques légères après un effort physique.\n[00:05.400 - 00:11.000] L'électrocardiogramme initial est normal, aucun antécédent particulier.",
                transcriptEnglish = "[00:00.100 - 00:05.300] The patient presents with mild chest pain following physical exertion.\n[00:05.400 - 00:11.000] Initial electrocardiogram is normal, no notable prior history."
            ),
            SampleAudioRecording(
                id = "spanish",
                title = "Travel Note (ES)",
                language = "Spanish (ES)",
                duration = "0:08",
                transcriptOriginal = "[00:00.200 - 00:04.800] ¿Dónde puedo tomar el tren de alta velocidad hacia Barcelona?\n[00:04.900 - 00:08.200] La salida está en el andén número cuatro.",
                transcriptEnglish = "[00:00.200 - 00:04.800] Where can I take the high-speed train to Barcelona?\n[00:04.900 - 00:08.200] The departure is on platform number four."
            )
        )
    }

    var selectedModel by remember { mutableStateOf("Gemma-4-E2B-it") }
    var promptText by remember { mutableStateOf("") }
    var selectedClip by remember { mutableStateOf<SampleAudioRecording?>(null) }
    var isTranslateMode by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var isTranscribing by remember { mutableStateOf(false) }
    var transcriptOutput by remember { mutableStateOf("") }

    // Dialogs & Sheets
    var showModelBottomSheet by remember { mutableStateOf(false) }
    var showTunerSheet by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }

    fun runTranscription(clip: SampleAudioRecording, translate: Boolean) {
        isTranscribing = true
        transcriptOutput = ""
        coroutineScope.launch {
            delay(400)
            isTranscribing = false
            transcriptOutput = if (translate) clip.transcriptEnglish else clip.transcriptOriginal
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
                        // Green Mic Icon + Title
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.Mic,
                                contentDescription = null,
                                tint = Color(0xFF34A853), // Google Green
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                "Audio Scribe",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                color = Color(0xFF1B5E20)
                            )
                        }

                        // Model Selector Pill (e.g. Gemma-4-E2B-it ▾)
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .clickable { showModelBottomSheet = true }
                                .testTag("audio_scribe_model_pill")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.CloudDone,
                                    contentDescription = null,
                                    tint = Color(0xFF34A853),
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
                            // + Attachment Button (Loads audio clip)
                            IconButton(
                                onClick = {
                                    selectedClip = sampleClips[0]
                                    runTranscription(sampleClips[0], isTranslateMode)
                                },
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .testTag("audio_scribe_attach_btn")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add audio", modifier = Modifier.size(18.dp))
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            TextField(
                                value = promptText,
                                onValueChange = { promptText = it },
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
                                    .testTag("audio_scribe_input_field")
                            )

                            IconButton(
                                onClick = {
                                    if (selectedClip == null) {
                                        selectedClip = sampleClips[0]
                                    }
                                    runTranscription(selectedClip!!, promptText.lowercase().contains("translate"))
                                },
                                enabled = !isTranscribing,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF34A853))
                                    .testTag("audio_scribe_send_btn")
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
    ) { padding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (selectedClip == null && transcriptOutput.isBlank()) {
                // -------------------------------------------------------------
                // EMPTY STATE (Matches Screenshot 5 Pixel for Pixel)
                // -------------------------------------------------------------
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Ask Audio",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "To get started, tap the + below to add a audio clip (limited to 1 clip up to 30 seconds) and type a prompt to transcribe or translate it!",
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Sample Clip quick-starter cards
                    Text("Or choose a sample clip:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        sampleClips.forEach { clip ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        selectedClip = clip
                                        runTranscription(clip, isTranslateMode)
                                    }
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(clip.title, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                    Text("⏱️ ${clip.duration}", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }
                }
            } else {
                // -------------------------------------------------------------
                // ACTIVE AUDIO PROCESSING VIEW
                // -------------------------------------------------------------
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Audio Clip Player & Waveform
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.AudioFile, contentDescription = null, tint = Color(0xFF34A853))
                                        Column {
                                            Text(selectedClip?.title ?: "Recorded Audio", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text(selectedClip?.language ?: "Audio Clip", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }

                                    Text(selectedClip?.duration ?: "0:15", fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                }

                                // Waveform bars
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(36.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val heights = listOf(14, 28, 36, 18, 32, 28, 26, 34, 20, 36, 32, 16, 30, 22, 34, 18, 24)
                                    heights.forEach { h ->
                                        Box(
                                            modifier = Modifier
                                                .width(4.dp)
                                                .height(if (isTranscribing) h.dp else (h * 0.5).dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(Color(0xFF34A853))
                                        )
                                    }
                                }

                                // Mode toggles
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilterChip(
                                        selected = !isTranslateMode,
                                        onClick = {
                                            isTranslateMode = false
                                            if (selectedClip != null) runTranscription(selectedClip!!, false)
                                        },
                                        label = { Text("Transcribe", fontSize = 12.sp) },
                                        leadingIcon = { Icon(Icons.Default.GraphicEq, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                    )

                                    FilterChip(
                                        selected = isTranslateMode,
                                        onClick = {
                                            isTranslateMode = true
                                            if (selectedClip != null) runTranscription(selectedClip!!, true)
                                        },
                                        label = { Text("Translate (English)", fontSize = 12.sp) },
                                        leadingIcon = { Icon(Icons.Default.Translate, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                    )
                                }
                            }
                        }
                    }

                    // Transcript Output
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFF1F8F3),
                            border = BorderStroke(1.dp, Color(0xFFC8E6C9)),
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
                                    Text(
                                        if (isTranslateMode) "ENGLISH TRANSLATION" else "TIMESTAMPED TRANSCRIPT",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1B5E20)
                                    )

                                    IconButton(
                                        onClick = { clipboardManager.setText(AnnotatedString(transcriptOutput)) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(14.dp), tint = Color(0xFF1B5E20))
                                    }
                                }

                                if (isTranscribing) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color(0xFF34A853))
                                        Text("Transcribing audio on-device...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                } else {
                                    Text(
                                        text = transcriptOutput,
                                        fontSize = 13.sp,
                                        lineHeight = 20.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------
    // AUDIO SCRIBE MODEL SELECTOR SHEET (Matches Screenshot 5 Pixel for Pixel)
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
                // Header: Green Mic Icon + "Audio Scribe models"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = null,
                        tint = Color(0xFF34A853),
                        modifier = Modifier.size(20.dp)
                    )
                    Text("Audio Scribe models", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20))
                }

                // Models List
                val modelsList = listOf(
                    Triple("Gemma-4-E2B-it", "2.6 GB", true),
                    Triple("Gemma-4-E4B-it", "3.7 GB", false),
                    Triple("Gemma-3n-E2B-it", "3.7 GB", false),
                    Triple("Gemma-3n-E4B-it", "4.9 GB", false)
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
                                    Icon(Icons.Default.CloudDone, contentDescription = null, tint = Color(0xFF34A853), modifier = Modifier.size(14.dp))
                                } else {
                                    Icon(Icons.Default.HelpOutline, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                                }
                                Text(size, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        if (isSelected) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Selected", tint = Color(0xFF34A853), modifier = Modifier.size(22.dp))
                        }
                    }
                }
            }
        }
    }

    // Tuner Settings Sheet
    if (showTunerSheet) {
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
                Text("Audio Transcription Engine Parameters", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("Backend: Mobile NPU Accelerated (LiteRT-LM Audio AudioTokenizer)", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(
                    onClick = { showTunerSheet = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close")
                }
            }
        }
    }
}
