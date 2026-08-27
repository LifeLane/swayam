package com.example.edgeaicore.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.edgeaicore.ui.playground.PlaygroundMode
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.edgeaicore.EdgeAICore
import com.example.edgeaicore.core.explanation.ExplanationRecord
import com.example.edgeaicore.core.memory.MemoryEntity
import com.example.edgeaicore.ui.agent.AgentScreen
import com.example.edgeaicore.ui.automation.RoutinesScreen
import com.example.edgeaicore.ui.benchmark.BenchmarkScreen
import com.example.edgeaicore.ui.capture.CaptureScreen
import com.example.edgeaicore.ui.common.UniversalExplanationSheet
import com.example.edgeaicore.ui.document.DocumentIntelligenceScreen
import com.example.edgeaicore.ui.home.HomeScreen
import com.example.edgeaicore.ui.memory.MemoryDetailSheet
import com.example.edgeaicore.ui.memory.MemoryScreen
import com.example.edgeaicore.ui.models.ModelCenterScreen
import com.example.edgeaicore.ui.privacy.PrivacyCenterScreen
import com.example.edgeaicore.ui.profile.ProfileScreen
import com.example.edgeaicore.ui.setup.ProvisioningOverlay
import com.example.edgeaicore.ui.storage.StorageCenterScreen
import com.example.edgeaicore.ui.tools.ConnectedServicesScreen
import com.example.edgeaicore.ui.tools.ToolPlaygroundScreen
import com.example.edgeaicore.ui.tools.ToolsScreen
import com.example.edgeaicore.ui.voice.AudioJournalScreen
import com.example.edgeaicore.ui.playground.PlaygroundScreen
import com.example.edgeaicore.ui.playground.PlaygroundViewModel
import com.example.edgeaicore.ui.gallery.EdgeUseCaseType
import com.example.edgeaicore.ui.gallery.GoogleAiEdgeGalleryHomeScreen
import com.example.edgeaicore.ui.gallery.UseCaseDetailScreen
import com.example.edgeaicore.ui.gallery.AskImagePlayground
import com.example.edgeaicore.ui.gallery.AudioScribePlayground
import com.example.edgeaicore.ui.gallery.TinyGardenPlayground
import com.example.edgeaicore.ui.gallery.MobileActionsPlayground
import com.example.edgeaicore.ui.gallery.AgentSkillsPlayground
import com.example.edgeaicore.ui.gallery.AiChatPlayground
import com.example.edgeaicore.ui.gallery.PromptLabPlayground
import com.example.edgeaicore.ui.gallery.EdgeGalleryModelsScreen
import com.example.ui.theme.LocalAIGreen

enum class MainDestination(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    GALLERY("Gallery", Icons.Filled.GridView, Icons.Outlined.GridView),
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home),
    PLAYGROUND("Playground", Icons.Filled.Terminal, Icons.Outlined.Terminal),
    MEMORY("Memory", Icons.Filled.Psychology, Icons.Outlined.Psychology),
    AGENT("Agent", Icons.Filled.SmartToy, Icons.Outlined.SmartToy),
    TOOLS("Tools", Icons.Filled.Extension, Icons.Outlined.Extension),
    PROFILE("Profile", Icons.Filled.Person, Icons.Outlined.Person)
}

sealed class SubDestination {
    object Capture : SubDestination()
    object PrivacyCenter : SubDestination()
    object ModelCenter : SubDestination()
    object StorageCenter : SubDestination()
    object ConnectedServices : SubDestination()
    object DocumentIntelligence : SubDestination()
    object Benchmark : SubDestination()
    object AudioJournal : SubDestination()
    object Routines : SubDestination()
    object ToolPlayground : SubDestination()
    data class UseCaseDetail(val type: EdgeUseCaseType) : SubDestination()
    object AskImagePlayground : SubDestination()
    object AudioScribePlayground : SubDestination()
    object TinyGardenPlayground : SubDestination()
    object MobileActionsPlayground : SubDestination()
    object AgentSkillsPlayground : SubDestination()
    object AiChatPlayground : SubDestination()
    object PromptLabPlayground : SubDestination()
    object GalleryModels : SubDestination()
}

@Composable
fun AppShell(
    edgeAI: EdgeAICore,
    modifier: Modifier = Modifier
) {
    var currentMainDestination by remember { mutableStateOf(MainDestination.GALLERY) }
    var currentSubDestination by remember { mutableStateOf<SubDestination?>(null) }

    var selectedMemoryForDetail by remember { mutableStateOf<MemoryEntity?>(null) }
    var activeExplanation by remember { mutableStateOf<ExplanationRecord?>(null) }
    var showDeveloperModal by remember { mutableStateOf(false) }

    val playgroundViewModel = remember { PlaygroundViewModel(edgeAI.context, edgeAI) }

    val openUnifiedConsole: (String?) -> Unit = { prompt ->
        if (!prompt.isNullOrBlank()) {
            playgroundViewModel.sendMessage(prompt)
        }
        currentMainDestination = MainDestination.PLAYGROUND
        currentSubDestination = null
    }

    var agentInitialGoal by remember { mutableStateOf<String?>(null) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWideScreen = maxWidth > 600.dp

        Row(modifier = Modifier.fillMaxSize()) {
            // Tablet / Foldable Navigation Rail
            if (isWideScreen && currentSubDestination == null) {
                NavigationRail(
                    modifier = Modifier.widthIn(min = 80.dp)
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    MainDestination.values().forEach { destination ->
                        NavigationRailItem(
                            selected = currentMainDestination == destination,
                            onClick = { currentMainDestination = destination },
                            icon = {
                                Icon(
                                    imageVector = if (currentMainDestination == destination) destination.selectedIcon else destination.unselectedIcon,
                                    contentDescription = destination.title
                                )
                            },
                            label = { Text(destination.title) },
                            modifier = Modifier.testTag("nav_rail_${destination.name.lowercase()}")
                        )
                    }
                }
            }

            // Main Content Area
            Scaffold(
                modifier = Modifier.weight(1f),
                bottomBar = {
                    if (currentSubDestination == null && !isWideScreen) {
                        NavigationBar(
                            modifier = Modifier.testTag("main_bottom_nav")
                        ) {
                            MainDestination.values().forEach { destination ->
                                NavigationBarItem(
                                    selected = currentMainDestination == destination,
                                    onClick = { currentMainDestination = destination },
                                    icon = {
                                        Icon(
                                            imageVector = if (currentMainDestination == destination) destination.selectedIcon else destination.unselectedIcon,
                                            contentDescription = destination.title
                                        )
                                    },
                                    label = { Text(destination.title) },
                                    modifier = Modifier.testTag("nav_item_${destination.name.lowercase()}")
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 1200.dp)
                    ) {
                        val sub = currentSubDestination
                        if (sub != null) {
                            when (sub) {
                                is SubDestination.Capture -> {
                                    CaptureScreen(
                                        edgeAI = edgeAI,
                                        onBack = { currentSubDestination = null },
                                        onMemorySaved = { currentSubDestination = null }
                                    )
                                }
                                is SubDestination.PrivacyCenter -> {
                                    PrivacyCenterScreen(
                                        edgeAI = edgeAI,
                                        onBack = { currentSubDestination = null },
                                        onNavigateToStorage = { currentSubDestination = SubDestination.StorageCenter }
                                    )
                                }
                                is SubDestination.ModelCenter -> {
                                    ModelCenterScreen(
                                        edgeAI = edgeAI,
                                        onBack = { currentSubDestination = null }
                                    )
                                }
                                is SubDestination.StorageCenter -> {
                                    StorageCenterScreen(
                                        edgeAI = edgeAI,
                                        onBack = { currentSubDestination = null },
                                        onNavigateToModels = { currentSubDestination = SubDestination.ModelCenter },
                                        onNavigateToMemories = {
                                            currentSubDestination = null
                                            currentMainDestination = MainDestination.MEMORY
                                        }
                                    )
                                }
                                is SubDestination.ConnectedServices -> {
                                    ConnectedServicesScreen(
                                        edgeAI = edgeAI,
                                        onBack = { currentSubDestination = null },
                                        onOpenDeveloperModal = { showDeveloperModal = true }
                                    )
                                }
                                is SubDestination.DocumentIntelligence -> {
                                    DocumentIntelligenceScreen(
                                        edgeAI = edgeAI,
                                        onBack = { currentSubDestination = null },
                                        onNavigateToAsk = { prompt -> openUnifiedConsole(prompt) }
                                    )
                                }
                                is SubDestination.Benchmark -> {
                                    BenchmarkScreen(
                                        edgeAI = edgeAI,
                                        onBack = { currentSubDestination = null }
                                    )
                                }
                                is SubDestination.AudioJournal -> {
                                    AudioJournalScreen(
                                        edgeAI = edgeAI,
                                        onBack = { currentSubDestination = null },
                                        onNavigateToAsk = { prompt -> openUnifiedConsole(prompt) }
                                    )
                                }
                                is SubDestination.Routines -> {
                                    RoutinesScreen(
                                        edgeAI = edgeAI,
                                        onBack = { currentSubDestination = null },
                                        onNavigateToAsk = { prompt -> openUnifiedConsole(prompt) }
                                    )
                                }
                                is SubDestination.ToolPlayground -> {
                                    ToolPlaygroundScreen(
                                        edgeAI = edgeAI,
                                        onBack = { currentSubDestination = null }
                                    )
                                }
                                is SubDestination.UseCaseDetail -> {
                                    UseCaseDetailScreen(
                                        useCaseType = sub.type,
                                        edgeAI = edgeAI,
                                        onBack = { currentSubDestination = null },
                                        onLaunchPlayground = { type, modelId ->
                                            when (type) {
                                                EdgeUseCaseType.ASK_IMAGE -> currentSubDestination = SubDestination.AskImagePlayground
                                                EdgeUseCaseType.AUDIO_SCRIBE -> currentSubDestination = SubDestination.AudioScribePlayground
                                                EdgeUseCaseType.TINY_GARDEN -> currentSubDestination = SubDestination.TinyGardenPlayground
                                                EdgeUseCaseType.MOBILE_ACTIONS -> currentSubDestination = SubDestination.MobileActionsPlayground
                                                EdgeUseCaseType.AI_CHAT -> currentSubDestination = SubDestination.AiChatPlayground
                                                EdgeUseCaseType.AGENT_SKILLS -> currentSubDestination = SubDestination.AgentSkillsPlayground
                                                EdgeUseCaseType.PROMPT_LAB -> currentSubDestination = SubDestination.PromptLabPlayground
                                            }
                                        }
                                    )
                                }
                                is SubDestination.AskImagePlayground -> {
                                    AskImagePlayground(
                                        edgeAI = edgeAI,
                                        onBack = { currentSubDestination = null }
                                    )
                                }
                                is SubDestination.AudioScribePlayground -> {
                                    AudioScribePlayground(
                                        edgeAI = edgeAI,
                                        onBack = { currentSubDestination = null }
                                    )
                                }
                                is SubDestination.TinyGardenPlayground -> {
                                    TinyGardenPlayground(
                                        edgeAI = edgeAI,
                                        onBack = { currentSubDestination = null }
                                    )
                                }
                                is SubDestination.MobileActionsPlayground -> {
                                    MobileActionsPlayground(
                                        edgeAI = edgeAI,
                                        onBack = { currentSubDestination = null }
                                    )
                                }
                                is SubDestination.AgentSkillsPlayground -> {
                                    AgentSkillsPlayground(
                                        edgeAI = edgeAI,
                                        onBack = { currentSubDestination = null }
                                    )
                                }
                                is SubDestination.AiChatPlayground -> {
                                    AiChatPlayground(
                                        edgeAI = edgeAI,
                                        onBack = { currentSubDestination = null }
                                    )
                                }
                                is SubDestination.PromptLabPlayground -> {
                                    PromptLabPlayground(
                                        edgeAI = edgeAI,
                                        onBack = { currentSubDestination = null }
                                    )
                                }
                                is SubDestination.GalleryModels -> {
                                    EdgeGalleryModelsScreen(
                                        edgeAI = edgeAI,
                                        onBack = { currentSubDestination = null },
                                        onLaunchModelUseCase = { model ->
                                            when (model.id) {
                                                "tinygarden-270m" -> currentSubDestination = SubDestination.TinyGardenPlayground
                                                "mobileactions-270m" -> currentSubDestination = SubDestination.MobileActionsPlayground
                                                else -> {
                                                    currentSubDestination = null
                                                    currentMainDestination = MainDestination.PLAYGROUND
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        } else {
                            when (currentMainDestination) {
                                MainDestination.GALLERY -> {
                                    GoogleAiEdgeGalleryHomeScreen(
                                        edgeAI = edgeAI,
                                        onSelectUseCase = { type ->
                                            currentSubDestination = SubDestination.UseCaseDetail(type)
                                        },
                                        onOpenModelsCatalog = {
                                            currentSubDestination = SubDestination.GalleryModels
                                        },
                                        onOpenSettings = {
                                            showDeveloperModal = true
                                        }
                                    )
                                }
                                MainDestination.HOME -> {
                                    HomeScreen(
                                        edgeAI = edgeAI,
                                        onNavigateToAsk = { prompt ->
                                            openUnifiedConsole(prompt)
                                        },
                                        onNavigateToCapture = { currentSubDestination = SubDestination.Capture },
                                        onNavigateToMemory = { currentMainDestination = MainDestination.MEMORY },
                                        onNavigateToAgent = { goal ->
                                            agentInitialGoal = goal
                                            currentMainDestination = MainDestination.AGENT
                                        },
                                        onNavigateToTools = { currentMainDestination = MainDestination.TOOLS },
                                        onNavigateToDocumentIntel = { currentSubDestination = SubDestination.DocumentIntelligence },
                                        onNavigateToBenchmark = { currentSubDestination = SubDestination.Benchmark },
                                        onNavigateToAudioJournal = { currentSubDestination = SubDestination.AudioJournal },
                                        onNavigateToRoutines = { currentSubDestination = SubDestination.Routines },
                                        onOpenOperatingCenter = { showDeveloperModal = true },
                                        onShowExplanation = { activeExplanation = it }
                                    )
                                }
                                MainDestination.PLAYGROUND -> {
                                    PlaygroundScreen(
                                        edgeAI = edgeAI,
                                        viewModel = playgroundViewModel,
                                        onShowExplanation = { activeExplanation = it }
                                    )
                                }
                                MainDestination.MEMORY -> {
                                    MemoryScreen(
                                        edgeAI = edgeAI,
                                        onNavigateToAskMemory = { prompt ->
                                            openUnifiedConsole(prompt)
                                        },
                                        onSelectMemory = { memory -> selectedMemoryForDetail = memory }
                                    )
                                }
                                MainDestination.AGENT -> {
                                    AgentScreen(
                                        edgeAI = edgeAI,
                                        initialGoal = agentInitialGoal,
                                        onShowExplanation = { activeExplanation = it }
                                    )
                                }
                                MainDestination.TOOLS -> {
                                    ToolsScreen(
                                        edgeAI = edgeAI,
                                        onNavigateToConnectedServices = { currentSubDestination = SubDestination.ConnectedServices },
                                        onNavigateToPlayground = {
                                            playgroundViewModel.selectMode(PlaygroundMode.TOOLS)
                                            currentMainDestination = MainDestination.PLAYGROUND
                                        }
                                    )
                                }
                                MainDestination.PROFILE -> {
                                    ProfileScreen(
                                        edgeAI = edgeAI,
                                        onNavigateToPrivacy = { currentSubDestination = SubDestination.PrivacyCenter },
                                        onNavigateToModels = { currentSubDestination = SubDestination.ModelCenter },
                                        onNavigateToStorage = { currentSubDestination = SubDestination.StorageCenter },
                                        onNavigateToServices = { currentSubDestination = SubDestination.ConnectedServices },
                                        onNavigateToDocumentIntel = { currentSubDestination = SubDestination.DocumentIntelligence },
                                        onNavigateToBenchmark = { currentSubDestination = SubDestination.Benchmark },
                                        onNavigateToAudioJournal = { currentSubDestination = SubDestination.AudioJournal },
                                        onNavigateToRoutines = { currentSubDestination = SubDestination.Routines },
                                        onNavigateToToolPlayground = {
                                            playgroundViewModel.selectMode(PlaygroundMode.TOOLS)
                                            currentMainDestination = MainDestination.PLAYGROUND
                                        },
                                        onOpenDeveloperModal = { showDeveloperModal = true }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Memory Detail Bottom Sheet
    selectedMemoryForDetail?.let { mem ->
        MemoryDetailSheet(
            memory = mem,
            edgeAI = edgeAI,
            onDismiss = { selectedMemoryForDetail = null },
            onAskAIAboutMemory = { prompt ->
                selectedMemoryForDetail = null
                if (prompt.isNotBlank()) playgroundViewModel.sendMessage(prompt)
                currentMainDestination = MainDestination.PLAYGROUND
            },
            onMemoryDeleted = {
                selectedMemoryForDetail = null
            }
        )
    }

    // Universal Explanation Modal Sheet
    if (activeExplanation != null) {
        UniversalExplanationSheet(
            record = activeExplanation,
            onDismiss = { activeExplanation = null }
        )
    }

    // Advanced Developer / Diagnostics Modal
    if (showDeveloperModal) {
        AiEngineModal(
            edgeAI = edgeAI,
            onDismiss = { showDeveloperModal = false }
        )
    }

    // First-Launch / Preparing Environment Overlay
    ProvisioningOverlay(
        edgeAI = edgeAI,
        onOpenModelCenter = { currentSubDestination = SubDestination.ModelCenter }
    )
}
