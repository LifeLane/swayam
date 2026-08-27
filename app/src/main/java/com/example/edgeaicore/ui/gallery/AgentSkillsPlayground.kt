package com.example.edgeaicore.ui.gallery

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
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

data class AgentSkillItem(
    val id: String,
    val name: String,
    val description: String,
    val scriptPath: String,
    val icon: ImageVector,
    var isEnabled: Boolean = true
)

data class AgentStep(
    val title: String,
    val description: String
)

data class AgentSkillExecutionState(
    val skillName: String,
    val scriptName: String,
    val steps: List<AgentStep>,
    val outputText: String,
    val executionTimeSeconds: Double,
    val payloadJson: String = "",
    val widgetType: AgentWidgetType = AgentWidgetType.MAP
)

enum class AgentWidgetType {
    MAP,
    REMINDER,
    MOOD,
    DEVICE,
    MATH
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentSkillsPlayground(
    edgeAI: EdgeAICore,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val listState = rememberLazyListState()

    // 8 Registered Agent Skills
    val availableSkills = remember {
        mutableStateListOf(
            AgentSkillItem("interactive-map", "interactive-map", "Show an interactive map view for the given location.", "/assets/skills/interactive-map/scripts/index.html", Icons.Default.Map, true),
            AgentSkillItem("reminder-manager", "schedule-reminder", "Create and schedule system alarms and notifications.", "/assets/skills/schedule-reminder/scripts/index.html", Icons.Default.Notifications, true),
            AgentSkillItem("mood-tracker", "track-mood", "Log emotional health, sentiment scores, and energy levels.", "/assets/skills/track-mood/scripts/index.html", Icons.Default.SentimentSatisfied, true),
            AgentSkillItem("device-actions", "device-controls", "Direct hardware control for flashlight, volume, and connectivity.", "/assets/skills/device-controls/scripts/index.html", Icons.Default.Smartphone, true),
            AgentSkillItem("math-solver", "math-solver", "Symbolic math, statistics, and financial modeling engine.", "/assets/skills/math-solver/scripts/index.html", Icons.Default.Calculate, true),
            AgentSkillItem("weather-radar", "weather-radar", "Real-time atmospheric telemetry and doppler forecasts.", "/assets/skills/weather-radar/scripts/index.html", Icons.Default.WbSunny, true),
            AgentSkillItem("calendar-planner", "calendar-planner", "Conflict-free meeting and agenda scheduling.", "/assets/skills/calendar-planner/scripts/index.html", Icons.Default.CalendarToday, true),
            AgentSkillItem("notes-organizer", "notes-organizer", "Hierarchical note capture and markdown indexing.", "/assets/skills/notes-organizer/scripts/index.html", Icons.Default.EditNote, true)
        )
    }

    var selectedModel by remember { mutableStateOf("Gemma-4-E2B-it") }
    var promptInput by remember { mutableStateOf("") }
    var isExecuting by remember { mutableStateOf(false) }
    var executionStage by remember { mutableStateOf(0) } // 0 = idle, 1 = loading skill, 2 = calling script, 3 = finished
    var currentExecution by remember { mutableStateOf<AgentSkillExecutionState?>(null) }
    var lastUserPrompt by remember { mutableStateOf<String?>(null) }
    var isStepCardExpanded by remember { mutableStateOf(true) }

    // Dialogs & Sheets
    var showModelBottomSheet by remember { mutableStateOf(false) }
    var showSkillsSheet by remember { mutableStateOf(false) }
    var showMcpSheet by remember { mutableStateOf(false) }
    var showTunerSheet by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var showFullScreenMap by remember { mutableStateOf(false) }

    // Sample Prompts Chips
    val samplePrompts = listOf(
        Pair("Interactive Map", "Show me Googleplex on interactive map."),
        Pair("Schedule Reminder", "Remind me to drink water in 30 minutes"),
        Pair("Track my mood", "Track my mood as productive and energized"),
        Pair("Device Actions", "Turn on flashlight and set volume to 80%"),
        Pair("Calculate Math", "Calculate compound interest for $5,000 at 7% for 3 years")
    )

    fun runAgentSkill(prompt: String) {
        if (prompt.isBlank()) return
        lastUserPrompt = prompt
        promptInput = ""
        isExecuting = true
        executionStage = 1
        currentExecution = null
        isStepCardExpanded = true

        coroutineScope.launch {
            // Stage 1: Loading skill
            delay(500)
            executionStage = 2

            // Determine widget and payload based on query
            val lower = prompt.lowercase()
            val widgetType = when {
                lower.contains("map") || lower.contains("googleplex") || lower.contains("location") -> AgentWidgetType.MAP
                lower.contains("remind") || lower.contains("drink") || lower.contains("water") -> AgentWidgetType.REMINDER
                lower.contains("mood") || lower.contains("energ") || lower.contains("feel") -> AgentWidgetType.MOOD
                lower.contains("flashlight") || lower.contains("volume") || lower.contains("device") -> AgentWidgetType.DEVICE
                else -> AgentWidgetType.MATH
            }

            val skillName = when (widgetType) {
                AgentWidgetType.MAP -> "interactive-map"
                AgentWidgetType.REMINDER -> "schedule-reminder"
                AgentWidgetType.MOOD -> "track-mood"
                AgentWidgetType.DEVICE -> "device-controls"
                AgentWidgetType.MATH -> "math-solver"
            }

            val outputText = when (widgetType) {
                AgentWidgetType.MAP -> "The interactive map has been shown for the Googleplex."
                AgentWidgetType.REMINDER -> "Reminder set: 'Drink water' in 30 minutes. An alert notification is scheduled."
                AgentWidgetType.MOOD -> "Logged your mood as 'Productive & Energized' with a score of 9.2/10. Great momentum!"
                AgentWidgetType.DEVICE -> "Device actions executed: Flashlight turned ON, Volume set to 80%."
                AgentWidgetType.MATH -> "Compound interest calculated: Principal $5,000 at 7% for 3 years yields a Total Value of $6,125.22 (Interest: $1,125.22)."
            }

            val dataPayload = when (widgetType) {
                AgentWidgetType.MAP -> "{\"location\": \"Googleplex\"}"
                AgentWidgetType.REMINDER -> "{\"title\": \"Drink water\", \"due_in_minutes\": 30}"
                AgentWidgetType.MOOD -> "{\"mood\": \"Productive\", \"energy\": 9.2, \"tags\": [\"work\", \"flow\"]}"
                AgentWidgetType.DEVICE -> "{\"flashlight\": true, \"media_volume\": 0.80}"
                AgentWidgetType.MATH -> "{\"principal\": 5000, \"rate\": 0.07, \"years\": 3, \"compounding\": \"annually\"}"
            }

            delay(700)
            executionStage = 3
            isExecuting = false

            currentExecution = AgentSkillExecutionState(
                skillName = skillName,
                scriptName = "$skillName/index.html",
                steps = listOf(
                    AgentStep("Load \"$skillName\"", "Show an interactive view for the requested task."),
                    AgentStep("Call JS script: \"$skillName/index.html\"", "- URL: /assets/skills/$skillName/scripts/index.html\n- Data: $dataPayload")
                ),
                outputText = outputText,
                executionTimeSeconds = 55.6,
                payloadJson = dataPayload,
                widgetType = widgetType
            )

            listState.animateScrollToItem(0)
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
                        // Title
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.RocketLaunch,
                                contentDescription = null,
                                tint = Color(0xFFF9AB00),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                "Agent Skills",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                color = Color(0xFFB06000)
                            )
                        }

                        // Model Selector Pill (e.g. Gemma-4-E2B-it ▾)
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .clickable { showModelBottomSheet = true }
                                .testTag("agent_skills_model_pill")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.CloudDone,
                                    contentDescription = null,
                                    tint = Color(0xFFF9AB00),
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
            // Bottom Prompt Bar (Matches Screenshots 1, 3, 4)
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
                    // Rounded Text Field & Action Badges Container
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
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
                                    .fillMaxWidth()
                                    .testTag("agent_skills_input_field")
                            )

                            // Controls Row: [+] [Skills 8] [MCP 0] [Send >]
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // + Attachment Button
                                    IconButton(
                                        onClick = { /* attachments */ },
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(18.dp))
                                    }

                                    // Skills Badge (e.g. Skills 8)
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                        modifier = Modifier.clickable { showSkillsSheet = true }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text("Skills", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                            Box(
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFE8EAED)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("${availableSkills.count { it.isEnabled }}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3C4043))
                                            }
                                        }
                                    }

                                    // MCP Badge (e.g. MCP 0)
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                        modifier = Modifier.clickable { showMcpSheet = true }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text("MCP", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                            Box(
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFE8EAED)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("0", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3C4043))
                                            }
                                        }
                                    }
                                }

                                // Send Button in circle
                                IconButton(
                                    onClick = { runAgentSkill(promptInput) },
                                    enabled = promptInput.isNotBlank() && !isExecuting,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(if (promptInput.isNotBlank()) Color(0xFFF9AB00) else Color(0xFFF9AB00).copy(alpha = 0.3f))
                                        .testTag("agent_skills_send_btn")
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
            // If no interaction yet, show Introducing screen (Screenshot 4)
            if (lastUserPrompt == null && !isExecuting && currentExecution == null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Spacer(modifier = Modifier.weight(0.2f))

                    // Intro Hero
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Introducing",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Text(
                            text = "Agent Skills",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A73E8)
                        )

                        Text(
                            text = "Use specialized, high-order reasoning by loading different skills or creating your own. Explore community contributed skills on GitHub discussions.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 20.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Text(
                            text = "Try tapping a sample prompt below to see Agent Skills in action!",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(top = 10.dp)
                        )
                    }

                    Spacer(modifier = Modifier.weight(0.3f))

                    // Quick Prompt Chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        items(samplePrompts) { (label, prompt) ->
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier
                                    .clickable {
                                        promptInput = prompt
                                        runAgentSkill(prompt)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val icon = when (label) {
                                        "Interactive Map" -> Icons.Default.Map
                                        "Schedule Reminder" -> Icons.Default.Notifications
                                        "Track my mood" -> Icons.Default.SentimentSatisfied
                                        "Device Actions" -> Icons.Default.Smartphone
                                        else -> Icons.Default.Calculate
                                    }
                                    Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                }
            } else {
                // Active Conversation / Execution Stream (Screenshots 1 & 3)
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. User Message (Dark Blue Bubble on Right)
                    item {
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
                                    color = Color(0xFF2C5E8A), // Navy/Blue bubble
                                    modifier = Modifier.widthIn(max = 300.dp)
                                ) {
                                    Text(
                                        text = lastUserPrompt ?: "",
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 2. "Model on GPU" indicator
                    item {
                        Text(
                            text = "Model on GPU",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // 3. Execution Card (Collapsible)
                    item {
                        if (isExecuting && executionStage < 3) {
                            // Loading state (Screenshot 3)
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFFEEF2FA),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp,
                                                color = Color(0xFF1A73E8)
                                            )
                                            Text(
                                                "Loading skill \"interactive-map\"",
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp
                                            )
                                        }
                                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = null)
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color.White,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF8AB4F8))
                                                    .align(Alignment.Top)
                                            )
                                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Text("Load \"interactive-map\"", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                Text(
                                                    "Description: Show an interactive map view for the given location.",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else if (currentExecution != null) {
                            // Completed Execution Card (Screenshot 1)
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFFEEF2FA),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Header
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { isStepCardExpanded = !isStepCardExpanded },
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Build,
                                                contentDescription = null,
                                                tint = Color(0xFF3C4043),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                "Called JS script \"${currentExecution!!.scriptName}\"",
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp
                                            )
                                        }
                                        Icon(
                                            imageVector = if (isStepCardExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                            contentDescription = null
                                        )
                                    }

                                    // Steps inside
                                    if (isStepCardExpanded) {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            // Step 1
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = Color.White,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(12.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(10.dp)
                                                            .clip(CircleShape)
                                                            .background(Color(0xFF8AB4F8))
                                                            .align(Alignment.Top)
                                                    )
                                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                        Text("Load \"${currentExecution!!.skillName}\"", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                        Text(
                                                            "Description: Show an interactive map view for the given location.",
                                                            fontSize = 11.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            }

                                            // Step 2
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = Color.White,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(12.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(10.dp)
                                                            .clip(CircleShape)
                                                            .background(Color(0xFF8AB4F8))
                                                            .align(Alignment.Top)
                                                    )
                                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                        Text("Call JS script: \"${currentExecution!!.scriptName}\"", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                        Text(
                                                            "- URL: /assets/skills/${currentExecution!!.skillName}/scripts/index.html\n- Data: ${currentExecution!!.payloadJson}",
                                                            fontSize = 11.sp,
                                                            fontFamily = FontFamily.Monospace,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 4. Assistant Output Text & Timing
                    if (currentExecution != null) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = currentExecution!!.outputText,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                    color = MaterialTheme.colorScheme.onBackground
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "${currentExecution!!.executionTimeSeconds} s",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(currentExecution!!.outputText))
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }

                        // 5. Rich Rendered Interactive Widget (Screenshot 1: Interactive Map)
                        item {
                            when (currentExecution!!.widgetType) {
                                AgentWidgetType.MAP -> {
                                    InteractiveMapWidget(
                                        onViewFullScreen = { showFullScreenMap = true }
                                    )
                                }
                                AgentWidgetType.REMINDER -> {
                                    InteractiveReminderWidget()
                                }
                                AgentWidgetType.MOOD -> {
                                    InteractiveMoodTrackerWidget()
                                }
                                AgentWidgetType.DEVICE -> {
                                    InteractiveDeviceControlsWidget()
                                }
                                AgentWidgetType.MATH -> {
                                    InteractiveMathSolverWidget()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Model Selector Bottom Sheet (Matches Screenshot 7 & 5)
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
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.RocketLaunch,
                        contentDescription = null,
                        tint = Color(0xFFF9AB00),
                        modifier = Modifier.size(20.dp)
                    )
                    Text("Agent Skills models", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB06000))
                }

                // Models List
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

    // Skills Sheet
    if (showSkillsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSkillsSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Installed Agent Skills (${availableSkills.size})", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("Toggle tool capabilities available to on-device reasoning models.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                availableSkills.forEach { skill ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(skill.icon, contentDescription = null, tint = Color(0xFFF9AB00), modifier = Modifier.size(20.dp))
                            Column {
                                Text(skill.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(skill.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                            }
                        }
                        Switch(
                            checked = skill.isEnabled,
                            onCheckedChange = { skill.isEnabled = it }
                        )
                    }
                }
            }
        }
    }

    // MCP (Model Context Protocol) Sheet
    if (showMcpSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMcpSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Model Context Protocol (MCP)", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("Connect local or remote MCP servers to provide dynamic tools and context to Gemma.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF1F3F4),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "0 MCP Servers connected. Add endpoints in Settings > Tool Integrations.",
                        fontSize = 12.sp,
                        color = Color(0xFF5F6368),
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }

    // Tuner Settings Sheet
    if (showTunerSheet) {
        var temp by remember { mutableFloatStateOf(0.7f) }
        var topK by remember { mutableFloatStateOf(40f) }
        var topP by remember { mutableFloatStateOf(0.95f) }

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

                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Top-P", fontSize = 13.sp)
                        Text("${(topP * 100).toInt() / 100f}", fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                    }
                    Slider(value = topP, onValueChange = { topP = it }, valueRange = 0.1f..1.0f)
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

    // Full Screen Map Dialog
    if (showFullScreenMap) {
        AlertDialog(
            onDismissRequest = { showFullScreenMap = false },
            title = { Text("Googleplex — Interactive Map View") },
            text = {
                InteractiveMapWidget(onViewFullScreen = {}, isExpandedView = true)
            },
            confirmButton = {
                TextButton(onClick = { showFullScreenMap = false }) {
                    Text("Close")
                }
            }
        )
    }
}

// -------------------------------------------------------------
// INTERACTIVE MAP WIDGET (Matches Screenshot 1 Pixel for Pixel)
// -------------------------------------------------------------
@Composable
fun InteractiveMapWidget(
    onViewFullScreen: () -> Unit,
    isExpandedView: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFDADCE0)),
        color = Color(0xFFE5F1E8), // Map greenish tint
        modifier = modifier
            .fillMaxWidth()
            .height(if (isExpandedView) 340.dp else 260.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Simulated Vector Map Canvas with roads, rivers, and pins
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Road network
                val roadColor = Color(0xFFFFFFFF)
                val riverColor = Color(0xFFB5D0F8)

                // Draw river
                val riverPath = Path().apply {
                    moveTo(w * 0.05f, 0f)
                    cubicTo(w * 0.15f, h * 0.4f, w * 0.25f, h * 0.6f, w * 0.1f, h)
                }
                drawPath(riverPath, riverColor, style = Stroke(width = 18f))

                // Roads
                drawLine(roadColor, Offset(0f, h * 0.25f), Offset(w, h * 0.2f), strokeWidth = 10f) // Amphitheatre Pkwy
                drawLine(roadColor, Offset(0f, h * 0.5f), Offset(w, h * 0.45f), strokeWidth = 14f) // Charleston Rd
                drawLine(roadColor, Offset(w * 0.48f, 0f), Offset(w * 0.52f, h), strokeWidth = 10f) // Alta Ave
                drawLine(roadColor, Offset(w * 0.8f, 0f), Offset(w * 0.85f, h), strokeWidth = 8f) // Huff Ave
            }

            // Floating Location Info Card in top-left
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.White,
                shadowElevation = 4.dp,
                modifier = Modifier
                    .padding(12.dp)
                    .widthIn(max = 240.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Googleplex", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(
                            "1600 Amphitheatre Pkwy, Mountain ...",
                            fontSize = 10.sp,
                            color = Color(0xFF5F6368),
                            maxLines = 1
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("4.1", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("★", fontSize = 10.sp, color = Color(0xFFF9AB00))
                            Text("(9,671)", fontSize = 10.sp, color = Color(0xFF1A73E8))
                            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(10.dp), tint = Color(0xFF5F6368))
                        }
                    }

                    Icon(
                        Icons.Default.OpenInNew,
                        contentDescription = "Open",
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFF1A73E8)
                    )
                }
            }

            // Googleplex Red Pin in Center
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Color(0xFFEA4335), // Google Red Pin
                    modifier = Modifier.size(36.dp)
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color.White.copy(alpha = 0.9f)
                ) {
                    Text(
                        "Googleplex",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF202124),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }

            // Satellite preview thumbnail in bottom-left
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
                    .size(36.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF3C4043))
                    .border(1.dp, Color.White, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Layers, contentDescription = "Satellite", tint = Color.White, modifier = Modifier.size(18.dp))
            }

            // Expand Map Icon in bottom-right
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable { onViewFullScreen() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Fullscreen, contentDescription = "Expand", tint = Color(0xFF5F6368), modifier = Modifier.size(20.dp))
            }
        }
    }

    if (!isExpandedView) {
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedButton(
            onClick = onViewFullScreen,
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier.testTag("agent_skills_fullscreen_map_btn")
        ) {
            Icon(Icons.Default.FitScreen, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("View in full screen", fontSize = 12.sp)
        }
    }
}

// -------------------------------------------------------------
// INTERACTIVE REMINDER WIDGET
// -------------------------------------------------------------
@Composable
fun InteractiveReminderWidget() {
    var isDone by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (isDone) Color(0xFF34A853) else Color(0xFFF9AB00)),
        color = if (isDone) Color(0xFFE8F5E9) else Color(0xFFFEF7E0),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                    Icon(
                        if (isDone) Icons.Default.CheckCircle else Icons.Default.Alarm,
                        contentDescription = null,
                        tint = if (isDone) Color(0xFF34A853) else Color(0xFFF9AB00)
                    )
                    Text("Drink Water Reminder", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White
                ) {
                    Text(
                        if (isDone) "COMPLETED" else "IN 30 MIN",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDone) Color(0xFF34A853) else Color(0xFFB06000),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Text("Stay hydrated! Aim for 250ml of clean drinking water.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = { isDone = !isDone },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isDone) Color(0xFF5F6368) else Color(0xFF34A853)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isDone) "Reopen" else "Mark Complete", fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = { /* snooze */ },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Snooze +10m", fontSize = 12.sp)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// INTERACTIVE MOOD TRACKER WIDGET
// -------------------------------------------------------------
@Composable
fun InteractiveMoodTrackerWidget() {
    var moodScore by remember { mutableFloatStateOf(9.2f) }

    Surface(
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF8AB4F8)),
        color = Color(0xFFF1F6FD),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("⚡ Mood & Energy Log", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF1A73E8))
                Text("${moodScore}/10", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color(0xFF1A73E8))
            }

            Slider(
                value = moodScore,
                onValueChange = { moodScore = (it * 10).toInt() / 10f },
                valueRange = 1f..10f
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf("🚀 High Flow", "💡 Creative", "🧘 Calm", "☕ Energized").forEach { tag ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White
                    ) {
                        Text(tag, fontSize = 11.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// INTERACTIVE DEVICE CONTROLS WIDGET
// -------------------------------------------------------------
@Composable
fun InteractiveDeviceControlsWidget() {
    var flashlightOn by remember { mutableStateOf(true) }
    var volume by remember { mutableFloatStateOf(0.8f) }

    Surface(
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("📱 Mobile Hardware Controls", fontWeight = FontWeight.Bold, fontSize = 15.sp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.FlashlightOn, contentDescription = null, tint = if (flashlightOn) Color(0xFFF9AB00) else Color.Gray)
                    Text("Flashlight", fontSize = 13.sp)
                }
                Switch(checked = flashlightOn, onCheckedChange = { flashlightOn = it })
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Media Volume", fontSize = 13.sp)
                    Text("${(volume * 100).toInt()}%", fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
                Slider(value = volume, onValueChange = { volume = it })
            }
        }
    }
}

// -------------------------------------------------------------
// INTERACTIVE MATH SOLVER WIDGET
// -------------------------------------------------------------
@Composable
fun InteractiveMathSolverWidget() {
    Surface(
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFD2E3FC)),
        color = Color(0xFFF8F9FA),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("📊 Financial Compound Interest", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF1A73E8))
            Text("Formula: A = P(1 + r/n)^(nt)", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF5F6368))

            Surface(shape = RoundedCornerShape(8.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Initial Principal (P):", fontSize = 12.sp)
                        Text("$5,000.00", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Annual Interest (r):", fontSize = 12.sp)
                        Text("7.0%", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Time Period (t):", fontSize = 12.sp)
                        Text("3 Years", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Final Balance (A):", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF1A73E8))
                        Text("$6,125.22", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = Color(0xFF1A73E8))
                    }
                }
            }
        }
    }
}
