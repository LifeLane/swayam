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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.edgeaicore.EdgeAICore
import com.example.edgeaicore.ui.common.OnDeviceModelStatusIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoogleAiEdgeGalleryHomeScreen(
    edgeAI: EdgeAICore,
    onSelectUseCase: (EdgeUseCaseType) -> Unit,
    onOpenModelsCatalog: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val diagnosticsMetrics by edgeAI.diagnostics.metrics.collectAsStateWithLifecycle()
    val specs = remember { edgeAI.diagnostics.specs() }

    var showDrawerOrMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 4-Color Google AI Edge / LiteRT Flower Icon
                        GoogleEdgeLogoBadge(modifier = Modifier.size(24.dp))

                        Text(
                            buildAnnotatedString {
                                append("Google ")
                                withStyle(SpanStyle(color = Color(0xFF1A73E8), fontWeight = FontWeight.Bold)) {
                                    append("AI Edge ")
                                }
                                append("Gallery")
                            },
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onOpenModelsCatalog,
                        modifier = Modifier.testTag("gallery_menu_btn")
                    ) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    IconButton(
                        onClick = onOpenModelsCatalog,
                        modifier = Modifier.testTag("gallery_models_btn")
                    ) {
                        Badge(containerColor = Color(0xFF1A73E8)) {
                            Text("9", fontSize = 10.sp, color = Color.White)
                        }
                    }
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.testTag("gallery_settings_btn")
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = "Engine Settings")
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
            contentPadding = PaddingValues(top = 10.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            // 1. HERO TITLE & SUBTITLE
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Google AI Edge Gallery",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = buildAnnotatedString {
                            append("Discover the power of on-device AI models from the ")
                            withStyle(SpanStyle(color = Color(0xFF1A73E8), textDecoration = TextDecoration.Underline)) {
                                append("LiteRT community")
                            }
                            append(", featuring the all-new ")
                            withStyle(SpanStyle(color = Color(0xFF1A73E8), textDecoration = TextDecoration.Underline)) {
                                append("Gemma 4")
                            }
                            append(".")
                        },
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
            }

            // Real-Time On-Device Model & Latency Status Indicator
            item {
                OnDeviceModelStatusIndicator(
                    modelName = diagnosticsMetrics.activeModelName,
                    msPerToken = diagnosticsMetrics.msPerToken,
                    tokensPerSecond = diagnosticsMetrics.tokensPerSecond,
                    backend = "${diagnosticsMetrics.activeBackend.name} • ${specs.totalRamMb / 1024}GB RAM",
                    isGenerating = false,
                    onClick = onOpenModelsCatalog
                )
            }

            // 2. "TRY GEMMA 4 TODAY" SECTION
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFF1A73E8),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Try Gemma 4 today",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Text(
                        text = "Gemma 4 E2B & E4B are here! Try them in AI Chat, Agent Skills, or the use cases below.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )

                    // Two Highlighted Cards: AI Chat & Agent Skills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // AI Chat Card
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFF1F6FD),
                            border = BorderStroke(1.dp, Color(0xFFD2E3FC)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onSelectUseCase(EdgeUseCaseType.AI_CHAT) }
                                .testTag("try_gemma_ai_chat")
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF1A73E8)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Forum,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Text("AI Chat", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF1A73E8))
                                Text(
                                    "Chat with the latest Gemma 4 model today",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 16.sp
                                )
                            }
                        }

                        // Agent Skills Card (with "New" badge)
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFFEF7E0),
                            border = BorderStroke(1.dp, Color(0xFFFEEFC3)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onSelectUseCase(EdgeUseCaseType.AGENT_SKILLS) }
                                .testTag("try_gemma_agent_skills")
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFF9AB00)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.RocketLaunch,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFF9334E6).copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            "New",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF9334E6),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text("Agent Skills", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFFB06000))
                                Text(
                                    "Have Gemma 4 complete agentic tasks for you",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }

            // 3. "EXPLORE OTHER USE CASES" SECTION
            item {
                Text(
                    text = "Explore other use cases",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Grid of Use Cases
            val useCases = EdgeUseCaseType.values()
            items(useCases.size / 2 + useCases.size % 2) { rowIndex ->
                val firstIndex = rowIndex * 2
                val secondIndex = firstIndex + 1

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    UseCaseGalleryCard(
                        useCase = useCases[firstIndex],
                        onClick = { onSelectUseCase(useCases[firstIndex]) },
                        modifier = Modifier.weight(1f)
                    )

                    if (secondIndex < useCases.size) {
                        UseCaseGalleryCard(
                            useCase = useCases[secondIndex],
                            onClick = { onSelectUseCase(useCases[secondIndex]) },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun UseCaseGalleryCard(
    useCase: EdgeUseCaseType,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("gallery_card_${useCase.name.lowercase()}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Icon squircle badge & Model count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(useCase.themeColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = useCase.icon,
                        contentDescription = null,
                        tint = useCase.themeColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Text(
                    text = "${useCase.availableModelsCount} Models",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }

            // Title
            Text(
                text = useCase.title,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Short Subtitle
            Text(
                text = useCase.shortSubtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp,
                maxLines = 2
            )
        }
    }
}

@Composable
fun GoogleEdgeLogoBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.SpaceEvenly) {
                Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFFEA4335))) // Red
                Spacer(modifier = Modifier.height(2.dp))
                Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFFFBBC05))) // Yellow
            }
            Spacer(modifier = Modifier.width(2.dp))
            Column(verticalArrangement = Arrangement.SpaceEvenly) {
                Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFF1A73E8))) // Blue
                Spacer(modifier = Modifier.height(2.dp))
                Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFF34A853))) // Green
            }
        }
    }
}
