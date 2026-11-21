package com.aistudio.kbeditor.r7v4qx.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aistudio.kbeditor.r7v4qx.model.KbEditorProject
import com.aistudio.kbeditor.r7v4qx.storage.ProAccountState
import com.aistudio.kbeditor.r7v4qx.ui.theme.*

@Composable
fun StudioHomeScreen(
    project: KbEditorProject,
    account: ProAccountState,
    onNewVideo: () -> Unit,
    onOpenEditorWithSheet: (ActiveStudioSheet) -> Unit,
    onOpenEditor: () -> Unit,
    onOpenAccount: () -> Unit,
    onApplyTemplate: (String) -> Unit = {},
    onPhotoToVideo: () -> Unit = {},
    savedProjects: List<KbEditorProject> = emptyList(),
    onNewDraft: () -> Unit = {},
    onOpenProject: (String) -> Unit = {},
    onDeleteProject: (String) -> Unit = {}
) {
    Scaffold(
        containerColor = Color(0xFF090B10),
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF0E1118),
                contentColor = Color.White
            ) {
                NavigationBarItem(
                    selected = true,
                    onClick = { onOpenEditor() },
                    icon = { Icon(Icons.Default.ContentCut, contentDescription = "Edit") },
                    label = { Text("Edit", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = VioletClip,
                        selectedTextColor = VioletClip,
                        indicatorColor = VioletClip.copy(alpha = 0.18f),
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    )
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { onOpenEditorWithSheet(ActiveStudioSheet.AUTO_CREATOR) },
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "AI Studio") },
                    label = { Text("AI Studio", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    )
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onOpenAccount,
                    icon = { Icon(Icons.Default.PersonOutline, contentDescription = "Pro") },
                    label = { Text("Pro (${account.dailyCoins}C)", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        unselectedIconColor = WarmAmber,
                        unselectedTextColor = WarmAmber
                    )
                )
            }
        }
    ) { innerPad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPad)
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
        // Top Bar: Profile Icon on left, Pro Coins Pill + Settings on right
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.clickable { onOpenAccount() }
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1A1E29))
                        .border(1.dp, VioletClip.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PersonOutline, contentDescription = "Account", tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Column {
                    Text(
                        text = if (account.isLoggedIn) account.userName else "KB Studio",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = if (account.isLoggedIn) "Pro • ${account.dailyCoins}/50 Coins" else "Tap to Login (50 Coins)",
                        fontSize = 11.sp,
                        color = WarmAmber
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onOpenAccount) {
                    Icon(Icons.Default.Stars, contentDescription = "Pro Plan", tint = WarmAmber)
                }
                IconButton(onClick = { onOpenEditorWithSheet(ActiveStudioSheet.EXPORT_PRO) }) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = TextPrimary)
                }
            }
        }

        // Two Hero Gradient Cards (Original Obsidian + Electric Violet / Amber Theme)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Left Hero Card: New Video
            Box(
                modifier = Modifier
                    .weight(1.25f)
                    .height(112.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF5B21B6), Color(0xFF7C3AED), Color(0xFFEC4899))
                        )
                    )
                    .clickable { onNewVideo() }
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF5B21B6), modifier = Modifier.size(22.dp))
                    }
                    Text("New video", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            // Right Hero Card: AI Studio
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(112.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF0F766E), Color(0xFF0D9488), Color(0xFFF59E0B))
                        )
                    )
                    .clickable { onOpenEditorWithSheet(ActiveStudioSheet.AUTO_CREATOR) }
                    .padding(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("Gemini 2.5", fontSize = 9.sp, color = WarmAmber, fontWeight = FontWeight.Bold)
                }
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                    Text("AI Studio", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        // 4x2 Quick Tools Grid (just like the reference screenshot, in dark obsidian theme)
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                HomeQuickTool("Photo Video", Icons.Default.Collections, Modifier.weight(1f)) {
                    onPhotoToVideo()
                }
                HomeQuickTool("Enhance", Icons.Default.HighQuality, Modifier.weight(1f)) {
                    onOpenEditorWithSheet(ActiveStudioSheet.ENHANCER)
                }
                HomeQuickTool("Auto Cap", Icons.Default.ClosedCaption, Modifier.weight(1f)) {
                    onOpenEditorWithSheet(ActiveStudioSheet.CAPTION_3D)
                }
                HomeQuickTool("Speed", Icons.Default.Speed, Modifier.weight(1f)) {
                    onOpenEditorWithSheet(ActiveStudioSheet.SPEED_RAMP)
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                HomeQuickTool("Remove BG", Icons.Default.PersonRemove, Modifier.weight(1f)) {
                    onOpenEditorWithSheet(ActiveStudioSheet.VFX_CHROMA_MASK)
                }
                HomeQuickTool("Audio Pro", Icons.Default.MusicNote, Modifier.weight(1f)) {
                    onOpenEditorWithSheet(ActiveStudioSheet.AUDIO_PRO)
                }
                HomeQuickTool("Keyframe", Icons.Default.Diamond, Modifier.weight(1f)) {
                    onOpenEditorWithSheet(ActiveStudioSheet.KEYFRAME_GRAPH)
                }
                HomeQuickTool("All tools", Icons.Default.GridView, Modifier.weight(1f)) {
                    onOpenEditor()
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                HomeQuickTool("Audio Dubbing", Icons.Default.RecordVoiceOver, Modifier.weight(1f)) {
                    onOpenEditorWithSheet(ActiveStudioSheet.AUDIO_DUBBING)
                }
                HomeQuickTool("Face Swap", Icons.Default.Face, Modifier.weight(1f)) {
                    onOpenEditorWithSheet(ActiveStudioSheet.FACE_SWAP)
                }
                HomeQuickTool("Effects", Icons.Default.Flare, Modifier.weight(1f)) {
                    onOpenEditorWithSheet(ActiveStudioSheet.TRANSITION_FX)
                }
                HomeQuickTool("Export 4K", Icons.Default.FileDownload, Modifier.weight(1f)) {
                    onOpenEditorWithSheet(ActiveStudioSheet.EXPORT_PRO)
                }
            }
        }

        // Pro Studio One-Tap Templates
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Studio Templates", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("1-Tap Look", fontSize = 11.sp, color = ElectricCyan)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            listOf(
                Triple("REELS_BEAT", "Reels Beat Sync", listOf(Color(0xFF7C3AED), Color(0xFFEC4899))),
                Triple("THUNDER_ACTION", "Thunder Storm", listOf(Color(0xFF0F172A), Color(0xFF0284C7))),
                Triple("FLOWER_DREAM", "Flower Petals", listOf(Color(0xFFBE185D), Color(0xFFF472B6))),
                Triple("BLINK_STAR", "Blink Star Glow", listOf(Color(0xFFB45309), Color(0xFFFACC15))),
                Triple("BOLLYWOOD_SLO", "Slow-Mo Dream", listOf(Color(0xFF9333EA), Color(0xFFF59E0B))),
                Triple("WEDDING_GOLD", "Wedding Gold", listOf(Color(0xFF92400E), Color(0xFFFBBF24))),
                Triple("GAMING_PHONK", "Gaming Phonk", listOf(Color(0xFF991B1B), Color(0xFF1E1B4B))),
                Triple("VLOG_FILM", "Vlog Warm Film", listOf(Color(0xFFD97706), Color(0xFF92400E))),
                Triple("PODCAST_PRO", "Podcast Clarity", listOf(Color(0xFF0284C7), Color(0xFF0F766E))),
                Triple("CYBER_NEON", "Cyber Neon", listOf(Color(0xFF06B6D4), Color(0xFF6D28D9))),
                Triple("VHS_90S", "90s VHS Retro", listOf(Color(0xFF374151), Color(0xFF7C2D12))),
                Triple("WINTER_SNOW", "Snowfall Vibe", listOf(Color(0xFF1E3A8A), Color(0xFF38BDF8))),
                Triple("FIRE_ENERGY", "Fire & Embers", listOf(Color(0xFFDC2626), Color(0xFFF97316))),
                Triple("LOVE_HEARTS", "Romantic Hearts", listOf(Color(0xFFE11D48), Color(0xFFFB7185))),
                Triple("PRISM_AURORA", "Prism Rainbow", listOf(Color(0xFF059669), Color(0xFF8B5CF6))),
                Triple("NOIR_CINEMA", "Noir Cinema", listOf(Color(0xFF111827), Color(0xFF4B5563)))
            ).forEach { (id, title, grad) ->
                Box(
                    modifier = Modifier
                        .width(136.dp)
                        .height(64.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.linearGradient(grad))
                        .clickable { onApplyTemplate(id) }
                        .padding(10.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        // Multi-Project Drafts Section (Up to 20 Saved Drafts)
        val displayProjects = savedProjects.ifEmpty { listOf(project) }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Projects (${displayProjects.size}/20)", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "+ New Draft",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = ElectricCyan,
                    modifier = Modifier.clickable { onNewDraft() }
                )
                Text(
                    text = "Open Editor →",
                    fontSize = 12.sp,
                    color = VioletClip,
                    modifier = Modifier.clickable { onOpenEditor() }
                )
            }
        }

        displayProjects.forEach { projItem ->
            val thumbUri = projItem.videoClips.firstOrNull { it.mediaUri.isNotBlank() }?.mediaUri.orEmpty()
            val durSec = (projItem.totalDurationMs / 1000L).coerceAtLeast(1L)
            val isActive = projItem.projectId == project.projectId
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF13161F))
                    .border(1.dp, if (isActive) VioletClip else Color(0xFF232838), RoundedCornerShape(14.dp))
                    .clickable {
                        onOpenProject(projItem.projectId)
                        onOpenEditor()
                    }
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF1E2230)),
                    contentAlignment = Alignment.Center
                ) {
                    if (thumbUri.isNotBlank()) {
                        AsyncImage(
                            model = thumbUri,
                            contentDescription = projItem.projectName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = VioletClip, modifier = Modifier.size(26.dp))
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(projItem.projectName, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White, maxLines = 1)
                    Text(
                        text = "${projItem.videoClips.size} clips  |  00:${"%02d".format(durSec)}  |  ${projItem.aspectRatio.label}",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
                IconButton(onClick = { onDeleteProject(projItem.projectId) }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Draft", tint = TextMuted, modifier = Modifier.size(18.dp))
                }
            }
        }
        }
    }
}

@Composable
private fun HomeQuickTool(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF151923))
                .border(1.dp, Color(0xFF232838), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Text(label, fontSize = 11.sp, color = TextPrimary, maxLines = 1)
    }
}
