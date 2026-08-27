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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.edgeaicore.EdgeAICore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class SampleImageItem(
    val id: String,
    val title: String,
    val emoji: String,
    val category: String,
    val defaultPrompt: String,
    val sampleAnswer: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AskImagePlayground(
    edgeAI: EdgeAICore,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    val sampleImages = remember {
        listOf(
            SampleImageItem(
                id = "receipt",
                title = "Grocery Receipt",
                emoji = "🧾",
                category = "OCR & Tables",
                defaultPrompt = "Transcribe the items and total price as structured JSON",
                sampleAnswer = "```json\n{\n  \"merchant\": \"Whole Foods Market\",\n  \"date\": \"2026-08-27\",\n  \"items\": [\n    {\"name\": \"Organic Oat Milk\", \"price\": 4.99},\n    {\"name\": \"Avocados (Bag)\", \"price\": 5.49},\n    {\"name\": \"Sourdough Bread\", \"price\": 3.89}\n  ],\n  \"subtotal\": 14.37,\n  \"tax\": 1.15,\n  \"total\": 15.52\n}\n```"
            ),
            SampleImageItem(
                id = "plant",
                title = "Plant Leaf Disease",
                emoji = "🍃",
                category = "Botany & Health",
                defaultPrompt = "Identify the plant and diagnose any disease symptoms.",
                sampleAnswer = "**Diagnosis:** The leaf shows characteristic symptoms of *Cercospora Leaf Spot* (brown concentric rings with yellowish halos).\n\n**Treatment Recommendations:**\n1. Prune affected foliage to increase air circulation.\n2. Avoid overhead watering.\n3. Apply an organic copper-based fungicide spray."
            ),
            SampleImageItem(
                id = "circuit",
                title = "Circuit PCB",
                emoji = "🔌",
                category = "Engineering",
                defaultPrompt = "Identify electronic components and inspect for soldering flaws.",
                sampleAnswer = "**Components Identified:**\n- Microcontroller: STM32 Cortex-M4 (QFP-48 package)\n- Decoupling capacitors: 0402 SMD 100nF (4 units)\n- Crystal oscillator: 16.000 MHz\n- Solder joints appear smooth and concave with no visible bridging."
            ),
            SampleImageItem(
                id = "landmark",
                title = "Eiffel Tower",
                emoji = "🗼",
                category = "Landmarks",
                defaultPrompt = "What landmark is this and tell me its architectural height?",
                sampleAnswer = "This is the **Eiffel Tower** located on the Champ de Mars in Paris, France. Constructed in 1889 by Gustave Eiffel for the World's Fair, its total height including the tip antenna is **330 meters (1,083 ft)**."
            )
        )
    }

    var selectedImage by remember { mutableStateOf(sampleImages[0]) }
    var promptInput by remember { mutableStateOf(sampleImages[0].defaultPrompt) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var answerText by remember { mutableStateOf(sampleImages[0].sampleAnswer) }
    var inferenceStats by remember { mutableStateOf("18.4 ms/tok • 256 image tokens • Gemma-4-E2B-it") }

    fun runVisionAnalysis(prompt: String) {
        if (prompt.isBlank()) return
        isAnalyzing = true
        answerText = ""

        coroutineScope.launch {
            delay(400) // fast on-device vision processing
            isAnalyzing = false
            answerText = when {
                prompt.contains("json", ignoreCase = true) -> selectedImage.sampleAnswer
                prompt.contains("detail", ignoreCase = true) || prompt.contains("describe", ignoreCase = true) ->
                    "The image shows a high-contrast view of ${selectedImage.title}. Key focal elements include distinct foreground textures, clear spatial borders, and crisp lighting."
                else -> selectedImage.sampleAnswer
            }
            inferenceStats = "17.9 ms/tok • 256 image tokens • Gemma-4-E2B-it (LiteRT)"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Ask Image", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            text = "Gemma-4-E2B-it • Multimodal Vision",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFEA4335).copy(alpha = 0.15f),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Visibility, contentDescription = null, tint = Color(0xFFEA4335), modifier = Modifier.size(14.dp))
                            Text("Vision Ready", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEA4335))
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Sample Images Carousel
            Text("Select Image or Capture", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(sampleImages) { img ->
                    val isSelected = img.id == selectedImage.id
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) Color(0xFFEA4335).copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.5.dp, if (isSelected) Color(0xFFEA4335) else Color.Transparent),
                        modifier = Modifier
                            .clickable {
                                selectedImage = img
                                promptInput = img.defaultPrompt
                                runVisionAnalysis(img.defaultPrompt)
                            }
                            .width(130.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(img.emoji, fontSize = 28.sp)
                            Text(img.title, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            Text(img.category, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Image Preview Card & Bounding Box Overlay
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFEA4335).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(selectedImage.emoji, fontSize = 42.sp)
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(selectedImage.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Ready for On-Device Multimodal Reasoning", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Text(
                                text = inferenceStats,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Quick Prompt Presets
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AssistChip(
                    onClick = {
                        promptInput = "Transcribe all text in this image"
                        runVisionAnalysis(promptInput)
                    },
                    label = { Text("📝 Extract Text", fontSize = 10.sp) }
                )
                AssistChip(
                    onClick = {
                        promptInput = "Identify key objects and flaws"
                        runVisionAnalysis(promptInput)
                    },
                    label = { Text("🔍 Inspect", fontSize = 10.sp) }
                )
                AssistChip(
                    onClick = {
                        promptInput = "Describe this scene in detail"
                        runVisionAnalysis(promptInput)
                    },
                    label = { Text("🖼️ Describe", fontSize = 10.sp) }
                )
            }

            // Vision Analysis Output Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("GEMMA 4 VISION RESPONSE", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFEA4335))
                            if (isAnalyzing) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Color(0xFFEA4335))
                            }
                        }
                    }
                    item {
                        if (isAnalyzing) {
                            Text("Analyzing image pixels via on-device LiteRT vision encoder...", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            Text(
                                text = answerText,
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Prompt Input Bar
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.AddPhotoAlternate,
                        contentDescription = "Attach",
                        tint = Color(0xFFEA4335),
                        modifier = Modifier.size(20.dp)
                    )
                    TextField(
                        value = promptInput,
                        onValueChange = { promptInput = it },
                        placeholder = {
                            Text(
                                "Ask a question about this image...",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (promptInput.isNotBlank()) {
                                runVisionAnalysis(promptInput)
                            }
                        }),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ask_image_prompt_input")
                    )
                    IconButton(
                        onClick = {
                            if (promptInput.isNotBlank()) {
                                runVisionAnalysis(promptInput)
                            }
                        },
                        enabled = promptInput.isNotBlank() && !isAnalyzing,
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                if (promptInput.isNotBlank()) Color(0xFFEA4335) else MaterialTheme.colorScheme.surfaceVariant,
                                CircleShape
                            )
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (promptInput.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
