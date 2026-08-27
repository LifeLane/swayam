package com.example.edgeaicore.ui.gallery

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
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
import androidx.compose.ui.graphics.Brush
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

enum class PlantType(val displayName: String, val emoji: String, val harvestEmoji: String) {
    SUNFLOWER("Sunflower", "🌻", "🌻"),
    STRAWBERRY("Strawberry", "🌱", "🍓"),
    TOMATO("Tomato", "🌿", "🍅"),
    ROSE("Rose", "🥀", "🌹"),
    CARROT("Carrot", "🌱", "🥕"),
    LAVENDER("Lavender", "🌿", "🪻")
}

enum class GrowthStage {
    EMPTY, SEED, SPROUT, BLOOMING, READY_TO_HARVEST
}

data class GardenPlot(
    val id: Int,
    val plantType: PlantType? = null,
    val stage: GrowthStage = GrowthStage.EMPTY,
    val moisturePercent: Int = 0,
    val isFertilized: Boolean = false,
    val hasWeeds: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TinyGardenPlayground(
    edgeAI: EdgeAICore,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    var plots by remember {
        mutableStateOf(
            listOf(
                GardenPlot(1, PlantType.SUNFLOWER, GrowthStage.BLOOMING, moisturePercent = 75),
                GardenPlot(2, PlantType.STRAWBERRY, GrowthStage.READY_TO_HARVEST, moisturePercent = 60),
                GardenPlot(3, PlantType.TOMATO, GrowthStage.SPROUT, moisturePercent = 40),
                GardenPlot(4, null, GrowthStage.EMPTY, moisturePercent = 10),
                GardenPlot(5, PlantType.CARROT, GrowthStage.SEED, moisturePercent = 80),
                GardenPlot(6, null, GrowthStage.EMPTY, moisturePercent = 15)
            )
        )
    }

    var harvestCount by remember { mutableIntStateOf(14) }
    var promptInput by remember { mutableStateOf("") }
    var isThinking by remember { mutableStateOf(false) }
    var lastFunctionCall by remember { mutableStateOf<String?>("FunctionGemma: {\"action\":\"init\",\"model\":\"TinyGarden-270M\"}") }
    var gardenNarratorMessage by remember { mutableStateOf("Welcome to your offline Tiny Garden! Type natural commands like 'Plant a sunflower in plot 4' or 'Water everything'.") }

    fun parseAndExecuteGardenCommand(command: String) {
        if (command.isBlank()) return
        val lower = command.lowercase()
        isThinking = true

        coroutineScope.launch {
            delay(350) // simulate super-fast 270M on-device inference
            isThinking = false

            when {
                lower.contains("water") -> {
                    plots = plots.map { if (it.stage != GrowthStage.EMPTY) it.copy(moisturePercent = 100) else it.copy(moisturePercent = 80) }
                    lastFunctionCall = """{"function": "water_garden", "target": "all_plots", "moisture": 100}"""
                    gardenNarratorMessage = "🌧️ FunctionGemma watered all garden plots! Plants are thriving."
                }
                lower.contains("harvest") -> {
                    var harvested = 0
                    plots = plots.map {
                        if (it.stage == GrowthStage.READY_TO_HARVEST || it.stage == GrowthStage.BLOOMING) {
                            harvested++
                            it.copy(plantType = null, stage = GrowthStage.EMPTY, moisturePercent = 20)
                        } else it
                    }
                    if (harvested > 0) {
                        harvestCount += harvested * 5
                        lastFunctionCall = """{"function": "harvest_crops", "yield_count": $harvested, "basket_points": ${harvested * 5}}"""
                        gardenNarratorMessage = "🧺 Harvested $harvested ripe crops! Added ${harvested * 5} points to your basket."
                    } else {
                        lastFunctionCall = """{"function": "harvest_crops", "status": "no_ripe_crops"}"""
                        gardenNarratorMessage = "🌾 No ripe crops ready to harvest yet. Try watering them first!"
                    }
                }
                lower.contains("sunflower") || lower.contains("rose") || lower.contains("strawberry") || lower.contains("tomato") || lower.contains("carrot") || lower.contains("plant") -> {
                    val plant = when {
                        lower.contains("rose") -> PlantType.ROSE
                        lower.contains("strawberry") -> PlantType.STRAWBERRY
                        lower.contains("tomato") -> PlantType.TOMATO
                        lower.contains("carrot") -> PlantType.CARROT
                        else -> PlantType.SUNFLOWER
                    }
                    // find plot number if mentioned
                    val targetPlotIndex = when {
                        lower.contains("1") -> 0
                        lower.contains("2") -> 1
                        lower.contains("3") -> 2
                        lower.contains("4") -> 3
                        lower.contains("5") -> 4
                        lower.contains("6") -> 5
                        else -> plots.indexOfFirst { it.stage == GrowthStage.EMPTY }.let { if (it != -1) it else 0 }
                    }

                    plots = plots.mapIndexed { idx, plot ->
                        if (idx == targetPlotIndex) {
                            plot.copy(plantType = plant, stage = GrowthStage.SPROUT, moisturePercent = 90)
                        } else plot
                    }
                    lastFunctionCall = """{"function": "plant_seed", "species": "${plant.displayName}", "plot_index": ${targetPlotIndex + 1}, "status": "planted"}"""
                    gardenNarratorMessage = "🌱 Planted ${plant.displayName} in Plot ${targetPlotIndex + 1}! It's sprouting nicely."
                }
                lower.contains("fertiliz") -> {
                    plots = plots.map {
                        if (it.stage != GrowthStage.EMPTY) it.copy(isFertilized = true, stage = GrowthStage.READY_TO_HARVEST) else it
                    }
                    lastFunctionCall = """{"function": "apply_fertilizer", "target": "active_plants", "growth_boost": "accelerated"}"""
                    gardenNarratorMessage = "✨ Organic fertilizer applied! All growing plants bloomed to full maturity."
                }
                lower.contains("weed") || lower.contains("clear") -> {
                    plots = plots.map { it.copy(hasWeeds = false) }
                    lastFunctionCall = """{"function": "pull_weeds", "status": "soil_cleared"}"""
                    gardenNarratorMessage = "🧹 Removed all weeds from the garden bed."
                }
                else -> {
                    lastFunctionCall = """{"function": "garden_advice", "intent": "${command.take(24)}", "confidence": 0.94}"""
                    gardenNarratorMessage = "🌻 TinyGarden-270M: 'Growing happily! Try asking me to plant sunflowers, water the beds, or harvest crops.'"
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Tiny Garden", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF34A853).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "EXPERIMENTAL",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E7E34),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = "FunctionGemma-270M • Offline Mini-Game",
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
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("🧺", fontSize = 14.sp)
                            Text(
                                "$harvestCount pts",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
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
            // Garden Narrator Banner
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFE8F5E9),
                border = BorderStroke(1.dp, Color(0xFFC8E6C9)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF34A853)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.LocalFlorist, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = gardenNarratorMessage,
                            fontSize = 13.sp,
                            color = Color(0xFF1B5E20),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Interactive Garden Plots Grid (6 Plots)
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                itemsIndexed(plots) { index, plot ->
                    GardenPlotCard(
                        plot = plot,
                        plotIndex = index + 1,
                        onClick = {
                            if (plot.stage == GrowthStage.READY_TO_HARVEST) {
                                parseAndExecuteGardenCommand("Harvest plot ${index + 1}")
                            } else if (plot.stage == GrowthStage.EMPTY) {
                                parseAndExecuteGardenCommand("Plant sunflower in plot ${index + 1}")
                            } else {
                                parseAndExecuteGardenCommand("Water plot ${index + 1}")
                            }
                        }
                    )
                }
            }

            // Function Call Inspector Sheet
            if (lastFunctionCall != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = lastFunctionCall!!,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }

            // Quick Garden Actions Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = { parseAndExecuteGardenCommand("Water all plants") },
                    label = { Text("🌧️ Water All", fontSize = 11.sp) }
                )
                AssistChip(
                    onClick = { parseAndExecuteGardenCommand("Harvest ripe crops") },
                    label = { Text("🧺 Harvest", fontSize = 11.sp) }
                )
                AssistChip(
                    onClick = { parseAndExecuteGardenCommand("Plant strawberries") },
                    label = { Text("🍓 Plant Berry", fontSize = 11.sp) }
                )
                AssistChip(
                    onClick = { parseAndExecuteGardenCommand("Add fertilizer") },
                    label = { Text("✨ Boost", fontSize = 11.sp) }
                )
            }

            // Natural Language Garden Command Input
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
                        Icons.Default.Spa,
                        contentDescription = null,
                        tint = Color(0xFF34A853),
                        modifier = Modifier.size(20.dp)
                    )
                    TextField(
                        value = promptInput,
                        onValueChange = { promptInput = it },
                        placeholder = {
                            Text(
                                "Ask FunctionGemma: e.g. 'Plant a rose in plot 6'",
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
                                parseAndExecuteGardenCommand(promptInput)
                                promptInput = ""
                            }
                        }),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tiny_garden_prompt_input")
                    )
                    IconButton(
                        onClick = {
                            if (promptInput.isNotBlank()) {
                                parseAndExecuteGardenCommand(promptInput)
                                promptInput = ""
                            }
                        },
                        enabled = promptInput.isNotBlank() && !isThinking,
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                if (promptInput.isNotBlank()) Color(0xFF34A853) else MaterialTheme.colorScheme.surfaceVariant,
                                CircleShape
                            )
                    ) {
                        if (isThinking) {
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
fun GardenPlotCard(
    plot: GardenPlot,
    plotIndex: Int,
    onClick: () -> Unit
) {
    val plotBackground = when (plot.stage) {
        GrowthStage.EMPTY -> Color(0xFFD7CCC8) // soil brown
        GrowthStage.SEED -> Color(0xFFC8E6C9)
        GrowthStage.SPROUT -> Color(0xFFA5D6A7)
        GrowthStage.BLOOMING -> Color(0xFFFFF9C4)
        GrowthStage.READY_TO_HARVEST -> Color(0xFFFFECB3)
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = plotBackground,
        border = BorderStroke(1.dp, if (plot.stage == GrowthStage.READY_TO_HARVEST) Color(0xFFF9AB00) else Color(0xFFBDBDBD)),
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.85f)
            .clickable(onClick = onClick)
            .testTag("garden_plot_$plotIndex")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Plot Number & Fertilizer badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "#$plotIndex",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.DarkGray
                )
                if (plot.isFertilized) {
                    Text("✨", fontSize = 10.sp)
                }
            }

            // Visual Plant Stage
            Box(
                modifier = Modifier.size(44.dp),
                contentAlignment = Alignment.Center
            ) {
                when (plot.stage) {
                    GrowthStage.EMPTY -> Text("🕳️", fontSize = 24.sp)
                    GrowthStage.SEED -> Text("🌱", fontSize = 24.sp)
                    GrowthStage.SPROUT -> Text("🌿", fontSize = 28.sp)
                    GrowthStage.BLOOMING -> Text(plot.plantType?.emoji ?: "🌸", fontSize = 32.sp)
                    GrowthStage.READY_TO_HARVEST -> Text(plot.plantType?.harvestEmoji ?: "🌟", fontSize = 36.sp)
                }
            }

            // Moisture & Name
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = plot.plantType?.displayName ?: "Empty Soil",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black.copy(alpha = 0.8f)
                )
                LinearProgressIndicator(
                    progress = { plot.moisturePercent / 100f },
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = Color(0xFF1976D2),
                    trackColor = Color.White.copy(alpha = 0.5f),
                )
            }
        }
    }
}
