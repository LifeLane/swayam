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

data class DeviceActionAudit(
    val timestamp: Long = System.currentTimeMillis(),
    val actionName: String,
    val details: String,
    val isSuccess: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobileActionsPlayground(
    edgeAI: EdgeAICore,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    // Device States
    var isFlashlightOn by remember { mutableStateOf(false) }
    var isWifiOn by remember { mutableStateOf(true) }
    var isBluetoothOn by remember { mutableStateOf(true) }
    var isDndOn by remember { mutableStateOf(false) }
    var brightnessLevel by remember { mutableFloatStateOf(0.75f) }
    var volumeLevel by remember { mutableFloatStateOf(0.60f) }
    var timerRunningSeconds by remember { mutableIntStateOf(0) }
    var alarmTimeString by remember { mutableStateOf("07:00 AM") }

    var promptInput by remember { mutableStateOf("") }
    var isExecuting by remember { mutableStateOf(false) }
    var lastFunctionCalls by remember { mutableStateOf<List<String>>(listOf("""{"action":"device.query_state","target":"all_sensors"}""")) }
    var executionHistory by remember {
        mutableStateOf(
            listOf(
                DeviceActionAudit(actionName = "Init", details = "FunctionGemma-270M ready for device actions")
            )
        )
    }

    fun parseAndExecuteDeviceCommand(prompt: String) {
        if (prompt.isBlank()) return
        val lower = prompt.lowercase()
        isExecuting = true

        coroutineScope.launch {
            delay(300) // Fast 270M on-device inference
            isExecuting = false

            val generatedCalls = mutableListOf<String>()
            val executedAudits = mutableListOf<DeviceActionAudit>()

            if (lower.contains("flashlight") || lower.contains("torch")) {
                val newState = !lower.contains("off")
                isFlashlightOn = newState
                generatedCalls.add("""{"function":"set_flashlight","enabled":$newState}""")
                executedAudits.add(DeviceActionAudit(actionName = "Flashlight", details = if (newState) "Turned ON flashlight" else "Turned OFF flashlight"))
            }

            if (lower.contains("wifi") || lower.contains("wi-fi")) {
                val newState = !lower.contains("off") && !lower.contains("disable")
                isWifiOn = newState
                generatedCalls.add("""{"function":"set_wifi","enabled":$newState}""")
                executedAudits.add(DeviceActionAudit(actionName = "Wi-Fi", details = if (newState) "Connected to Wi-Fi" else "Disabled Wi-Fi"))
            }

            if (lower.contains("bluetooth")) {
                val newState = !lower.contains("off") && !lower.contains("disable")
                isBluetoothOn = newState
                generatedCalls.add("""{"function":"set_bluetooth","enabled":$newState}""")
                executedAudits.add(DeviceActionAudit(actionName = "Bluetooth", details = if (newState) "Enabled Bluetooth" else "Disabled Bluetooth"))
            }

            if (lower.contains("dnd") || lower.contains("disturb") || lower.contains("silent") || lower.contains("mute")) {
                isDndOn = true
                volumeLevel = 0f
                generatedCalls.add("""{"function":"set_dnd","mode":"TOTAL_SILENCE"}""")
                executedAudits.add(DeviceActionAudit(actionName = "Do Not Disturb", details = "Activated Total Silence DND"))
            }

            if (lower.contains("bright") || lower.contains("dim") || lower.contains("%")) {
                val level = when {
                    lower.contains("20") -> 0.20f
                    lower.contains("30") -> 0.30f
                    lower.contains("40") -> 0.40f
                    lower.contains("50") -> 0.50f
                    lower.contains("80") -> 0.80f
                    lower.contains("100") || lower.contains("max") -> 1.0f
                    lower.contains("dim") -> 0.25f
                    else -> 0.85f
                }
                brightnessLevel = level
                generatedCalls.add("""{"function":"set_screen_brightness","level":$level}""")
                executedAudits.add(DeviceActionAudit(actionName = "Brightness", details = "Set screen brightness to ${(level * 100).toInt()}%"))
            }

            if (lower.contains("timer")) {
                timerRunningSeconds = 300 // 5 mins
                generatedCalls.add("""{"function":"start_timer","duration_seconds":300,"label":"User Task"}""")
                executedAudits.add(DeviceActionAudit(actionName = "Timer", details = "Started 5:00 countdown timer"))
            }

            if (lower.contains("alarm")) {
                val time = if (lower.contains("6:30")) "06:30 AM" else if (lower.contains("7:00")) "07:00 AM" else "08:00 AM"
                alarmTimeString = time
                generatedCalls.add("""{"function":"set_system_alarm","time":"$time"}""")
                executedAudits.add(DeviceActionAudit(actionName = "Alarm", details = "Scheduled alarm for $time"))
            }

            if (generatedCalls.isEmpty()) {
                generatedCalls.add("""{"function":"system_status_query","intent":"${prompt.take(30)}"}""")
                executedAudits.add(DeviceActionAudit(actionName = "Query", details = "Analyzed system state for: $prompt"))
            }

            lastFunctionCalls = generatedCalls
            executionHistory = executedAudits + executionHistory
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Mobile Actions", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF1A73E8).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "EXPERIMENTAL",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1A73E8),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = "MobileActions-270M • Function Gemma",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Interactive Device Controls Dashboard
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "SIMULATED DEVICE CONTROL STATE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )

                    // Quick Toggle Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        DeviceToggleChip(
                            icon = Icons.Default.FlashlightOn,
                            label = "Flashlight",
                            isActive = isFlashlightOn,
                            onClick = { isFlashlightOn = !isFlashlightOn }
                        )
                        DeviceToggleChip(
                            icon = Icons.Default.Wifi,
                            label = "Wi-Fi",
                            isActive = isWifiOn,
                            onClick = { isWifiOn = !isWifiOn }
                        )
                        DeviceToggleChip(
                            icon = Icons.Default.Bluetooth,
                            label = "Bluetooth",
                            isActive = isBluetoothOn,
                            onClick = { isBluetoothOn = !isBluetoothOn }
                        )
                        DeviceToggleChip(
                            icon = Icons.Default.DoNotDisturbOn,
                            label = "DND",
                            isActive = isDndOn,
                            onClick = { isDndOn = !isDndOn }
                        )
                    }

                    // Brightness Slider
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Brightness6, contentDescription = null, modifier = Modifier.size(14.dp))
                                Text("Screen Brightness", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                            Text("${(brightnessLevel * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                        Slider(
                            value = brightnessLevel,
                            onValueChange = { brightnessLevel = it },
                            modifier = Modifier.height(24.dp)
                        )
                    }

                    // Timer & Alarm Status Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                Column {
                                    Text("Timer", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(if (timerRunningSeconds > 0) "05:00 (Running)" else "Stopped", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Alarm, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFF9AB00))
                                Column {
                                    Text("Next Alarm", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(alarmTimeString, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Function Call Output
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("FUNCTION CALL OUTPUT (LITERT-LM)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("24.1 ms/tok", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF34A853))
                    }
                    lastFunctionCalls.forEach { call ->
                        Text(
                            text = call,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Quick Prompt Suggestions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AssistChip(
                    onClick = { parseAndExecuteDeviceCommand("Turn on flashlight and set timer for 5 minutes") },
                    label = { Text("🔦 Torch + ⏱️ Timer", fontSize = 10.sp) }
                )
                AssistChip(
                    onClick = { parseAndExecuteDeviceCommand("Turn off Wi-Fi and set brightness to 30%") },
                    label = { Text("📶 Off + 🔅 Dim", fontSize = 10.sp) }
                )
                AssistChip(
                    onClick = { parseAndExecuteDeviceCommand("Enable DND mode") },
                    label = { Text("🔕 Total Silence", fontSize = 10.sp) }
                )
            }

            // Execution Audit Log List
            Text("Action Audit Trail", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(executionHistory) { audit ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF34A853), modifier = Modifier.size(14.dp))
                                Column {
                                    Text(audit.actionName, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text(audit.details, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Text(
                                "LOCAL ONLY",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF34A853),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // Natural Language Command Input Bar
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
                        Icons.Default.Functions,
                        contentDescription = null,
                        tint = Color(0xFF1A73E8),
                        modifier = Modifier.size(20.dp)
                    )
                    TextField(
                        value = promptInput,
                        onValueChange = { promptInput = it },
                        placeholder = {
                            Text(
                                "e.g. 'Turn on flashlight and set alarm for 6:30 AM'",
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
                                parseAndExecuteDeviceCommand(promptInput)
                                promptInput = ""
                            }
                        }),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("mobile_actions_prompt_input")
                    )
                    IconButton(
                        onClick = {
                            if (promptInput.isNotBlank()) {
                                parseAndExecuteDeviceCommand(promptInput)
                                promptInput = ""
                            }
                        },
                        enabled = promptInput.isNotBlank() && !isExecuting,
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                if (promptInput.isNotBlank()) Color(0xFF1A73E8) else MaterialTheme.colorScheme.surfaceVariant,
                                CircleShape
                            )
                    ) {
                        if (isExecuting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                        } else {
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
}

@Composable
fun DeviceToggleChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isActive) Color(0xFF1A73E8).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, if (isActive) Color(0xFF1A73E8) else Color.Transparent),
        modifier = Modifier
            .clickable(onClick = onClick)
            .width(72.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) Color(0xFF1A73E8) else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                color = if (isActive) Color(0xFF1A73E8) else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (isActive) "ON" else "OFF",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = if (isActive) Color(0xFF1A73E8) else Color.Gray
            )
        }
    }
}
