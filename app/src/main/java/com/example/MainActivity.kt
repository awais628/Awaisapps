package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.db.AIState
import com.example.db.MoodEntry
import com.example.db.ZenViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: ZenViewModel = viewModel()) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Reflections, 1: Breath Space

    Scaffold(
        modifier = Modifier.fillMaxSize().testTag("main_scaffold"),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        ) {
                            // Render local JPG generated
                            Icon(
                                painter = painterResource(id = R.drawable.zenmind_logo_1782937583859),
                                contentDescription = "ZenMind logo",
                                tint = Color.Unspecified,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Column {
                            Text(
                                text = "ZenMind",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                color = ZenTextWhite
                            )
                            Text(
                                text = "AI Reflection Companion",
                                style = MaterialTheme.typography.labelSmall,
                                color = ZenTeal
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ZenDeepNight,
                    titleContentColor = ZenTextWhite
                ),
                actions = {
                    IconButton(
                        onClick = { viewModel.clearAllEntries() },
                        modifier = Modifier.testTag("clear_history_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear all reflections",
                            tint = ZenSoftGray
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = ZenSurface,
                tonalElevation = 8.dp,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 0) Icons.Default.AutoAwesome else Icons.Outlined.AutoAwesome,
                            contentDescription = "Journal & Insights"
                        )
                    },
                    label = { Text("Insights") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ZenDeepNight,
                        selectedTextColor = ZenTeal,
                        indicatorColor = ZenTeal,
                        unselectedIconColor = ZenSoftGray,
                        unselectedTextColor = ZenSoftGray
                    ),
                    modifier = Modifier.testTag("tab_reflections")
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 1) Icons.Default.SelfImprovement else Icons.Outlined.SelfImprovement,
                            contentDescription = "Breathing trainer"
                        )
                    },
                    label = { Text("Breathing") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ZenDeepNight,
                        selectedTextColor = ZenTeal,
                        indicatorColor = ZenTeal,
                        unselectedIconColor = ZenSoftGray,
                        unselectedTextColor = ZenSoftGray
                    ),
                    modifier = Modifier.testTag("tab_breathing")
                )
            }
        },
        containerColor = ZenDeepNight
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(ZenDeepNight, Color(0xFF0F1216))
                    )
                )
        ) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(250)) togetherWith fadeOut(animationSpec = tween(250))
                },
                label = "tab_transition"
            ) { targetTab ->
                when (targetTab) {
                    0 -> ReflectionsTab(viewModel)
                    1 -> BreathingTab()
                }
            }
        }
    }
}

@Composable
fun ReflectionsTab(viewModel: ZenViewModel) {
    val entries by viewModel.entries.collectAsState()
    val selectedMood by viewModel.selectedMood.collectAsState()
    val journalText by viewModel.journalText.collectAsState()
    val aiState by viewModel.aiState.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var moodFilter by remember { mutableStateOf("ALL") } // ALL, RAD, HAPPY, NEUTRAL, SAD, AWFUL

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // AI State / Loading or Success Advice banner
        item {
            AIInsightBanner(
                aiState = aiState,
                onDismiss = { viewModel.resetInputs() }
            )
        }

        // New Entry Logger Form Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("new_reflection_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = ZenSurface),
                border = BorderStroke(1.dp, ZenVioletDark),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "How is your spirit today?",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = ZenTextWhite
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // Mood Selectors row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val moods = listOf(
                            "AWFUL" to "😭",
                            "SAD" to "😢",
                            "NEUTRAL" to "😐",
                            "HAPPY" to "🙂",
                            "RAD" to "🤩"
                        )

                        moods.forEach { (moodName, emoji) ->
                            val isSelected = selectedMood == moodName
                            val moodColor = getMoodColor(moodName)
                            
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) moodColor.copy(alpha = 0.15f) else Color.Transparent)
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) moodColor else ZenSoftGray.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { viewModel.selectMood(moodName) }
                                    .padding(vertical = 10.dp)
                                    .testTag("mood_$moodName")
                            ) {
                                Text(
                                    text = emoji,
                                    fontSize = 24.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = moodName,
                                    fontSize = 9.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) moodColor else ZenSoftGray
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Journal Text Input
                    OutlinedTextField(
                        value = journalText,
                        onValueChange = { viewModel.updateJournalText(it) },
                        placeholder = {
                            Text(
                                "Describe your thoughts, feelings, or what's on your mind...",
                                fontSize = 14.sp,
                                color = ZenSoftGray.copy(alpha = 0.7f)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 110.dp)
                            .testTag("journal_input"),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = ZenTextWhite,
                            unfocusedTextColor = ZenTextWhite,
                            focusedBorderColor = ZenTeal,
                            unfocusedBorderColor = ZenVioletDark,
                            focusedContainerColor = ZenDeepNight.copy(alpha = 0.5f),
                            unfocusedContainerColor = ZenDeepNight.copy(alpha = 0.5f)
                        ),
                        maxLines = 6
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Clear fields button
                        OutlinedButton(
                            onClick = { viewModel.resetInputs() },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("clear_button"),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = ZenSoftGray
                            ),
                            border = BorderStroke(
                                1.dp,
                                ZenVioletDark
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear fields", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Clear", fontSize = 14.sp)
                        }

                        // Call Gemini API Button
                        Button(
                            onClick = { viewModel.analyzeAndSaveEntry() },
                            modifier = Modifier
                                .weight(2f)
                                .height(48.dp)
                                .testTag("submit_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ZenTeal,
                                contentColor = ZenDeepNight
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (aiState is AIState.Loading) {
                                CircularProgressIndicator(
                                    color = ZenDeepNight,
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = "Analyze", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Consult ZenMind", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }

        // Historic Reflections Filter & Search
        item {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                Text(
                    text = "Reflection Logs",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = ZenTextWhite
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Search Box
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search logs...", color = ZenSoftGray, fontSize = 13.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = ZenTextWhite,
                        unfocusedTextColor = ZenTextWhite,
                        focusedBorderColor = ZenTeal.copy(alpha = 0.5f),
                        unfocusedBorderColor = ZenVioletDark,
                        focusedContainerColor = ZenSurface,
                        unfocusedContainerColor = ZenSurface
                    ),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = ZenSoftGray) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search", tint = ZenSoftGray)
                            }
                        }
                    },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Mood Filter Row (Scrollable or simple layout)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val filterOptions = listOf("ALL", "RAD", "HAPPY", "NEUTRAL", "SAD", "AWFUL")
                    filterOptions.forEach { opt ->
                        val isSelected = moodFilter == opt
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) ZenTeal else ZenSurface)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) Color.Transparent else ZenVioletDark,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { moodFilter = opt }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (opt == "ALL") "All" else getMoodEmoji(opt),
                                fontSize = if (opt == "ALL") 12.sp else 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) ZenDeepNight else ZenSoftGray
                            )
                        }
                    }
                }
            }
        }

        // Filter actual list
        val filteredEntries = entries.filter { entry ->
            val matchesSearch = entry.journalText.contains(searchQuery, ignoreCase = true) ||
                    (entry.aiReflection?.contains(searchQuery, ignoreCase = true) ?: false) ||
                    (entry.aiRecommendation?.contains(searchQuery, ignoreCase = true) ?: false)
            val matchesMood = moodFilter == "ALL" || entry.mood == moodFilter
            matchesSearch && matchesMood
        }

        if (filteredEntries.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Spa,
                            contentDescription = "Empty",
                            tint = ZenSoftGray.copy(alpha = 0.3f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty() || moodFilter != "ALL") "No matches found." else "A clean mirror awaits your reflection.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ZenSoftGray
                        )
                    }
                }
            }
        } else {
            items(filteredEntries, key = { it.id }) { entry ->
                ReflectionLogCard(entry = entry, onDelete = { viewModel.deleteEntry(entry.id) })
            }
        }
    }
}

@Composable
fun AIInsightBanner(
    aiState: AIState,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(
        visible = aiState is AIState.Success || aiState is AIState.Error,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        when (aiState) {
            is AIState.Success -> {
                val advice = aiState.advice
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, ZenViolet.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                        .testTag("ai_insight_banner"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = ZenSurface
                    )
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI Star",
                                tint = ZenViolet
                            )
                            Text(
                                text = "ZenMind Insight",
                                fontWeight = FontWeight.Bold,
                                color = ZenViolet,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ZenViolet.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = advice.tag,
                                    fontSize = 11.sp,
                                    color = ZenViolet,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = advice.reflection,
                            style = MaterialTheme.typography.bodyMedium,
                            color = ZenTextWhite,
                            lineHeight = 22.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Mindfulness Action Card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(ZenTeal.copy(alpha = 0.08f))
                                .border(1.dp, ZenTeal.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SelfImprovement,
                                        contentDescription = "Breathing Recommendation",
                                        tint = ZenTeal,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Mindful Action Step",
                                        fontWeight = FontWeight.Bold,
                                        color = ZenTeal,
                                        fontSize = 12.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = advice.recommendation,
                                    fontSize = 13.sp,
                                    color = ZenTextWhite.copy(alpha = 0.9f),
                                    lineHeight = 18.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth().height(42.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ZenViolet,
                                contentColor = ZenDeepNight
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Absorb Reflection", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            is AIState.Error -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF3B1D20))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = "Error", tint = Color(0xFFE57373))
                        Text(
                            text = aiState.message,
                            color = Color(0xFFF5C6C6),
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFFF5C6C6))
                        }
                    }
                }
            }
            else -> {}
        }
    }
}

@Composable
fun ReflectionLogCard(
    entry: MoodEntry,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val moodColor = getMoodColor(entry.mood)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .testTag("reflection_card_${entry.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ZenSurface),
        border = BorderStroke(1.dp, ZenVioletDark)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(moodColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = getMoodEmoji(entry.mood),
                        fontSize = 18.sp
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.mood.lowercase().replaceFirstChar { it.uppercase() },
                        fontWeight = FontWeight.Bold,
                        color = moodColor,
                        fontSize = 14.sp
                    )
                    Text(
                        text = formatTimestamp(entry.timestamp),
                        fontSize = 11.sp,
                        color = ZenSoftGray
                    )
                }

                if (entry.tag != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(ZenViolet.copy(alpha = 0.1f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = entry.tag,
                            fontSize = 9.sp,
                            color = ZenViolet,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("delete_button_${entry.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Delete entry",
                        tint = ZenSoftGray.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Journal text
            Text(
                text = entry.journalText,
                style = MaterialTheme.typography.bodyMedium,
                color = ZenTextWhite,
                lineHeight = 20.sp,
                maxLines = if (expanded) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis
            )

            // Dynamic view expanding arrow
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = if (expanded) "Hide Insights" else "Read ZenMind Insights",
                    fontSize = 11.sp,
                    color = ZenTeal,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = ZenTeal,
                    modifier = Modifier.size(14.dp)
                )
            }

            // Expanded AI Section
            AnimatedVisibility(
                visible = expanded && (entry.aiReflection != null || entry.aiRecommendation != null),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(color = ZenSurfaceVariant, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    if (entry.aiReflection != null) {
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Spa,
                                contentDescription = "Lotus",
                                tint = ZenViolet,
                                modifier = Modifier.size(16.dp)
                            )
                            Column {
                                Text(
                                    text = "Reflection Feedback",
                                    fontSize = 11.sp,
                                    color = ZenViolet,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = entry.aiReflection,
                                    fontSize = 13.sp,
                                    color = ZenTextWhite.copy(alpha = 0.9f),
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }

                    if (entry.aiRecommendation != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SelfImprovement,
                                contentDescription = "Self Improvement",
                                tint = ZenTeal,
                                modifier = Modifier.size(16.dp)
                            )
                            Column {
                                Text(
                                    text = "Mindful Action Recommendation",
                                    fontSize = 11.sp,
                                    color = ZenTeal,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = entry.aiRecommendation,
                                    fontSize = 13.sp,
                                    color = ZenTextWhite.copy(alpha = 0.9f),
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BreathingTab() {
    var isActive by remember { mutableStateOf(false) }
    var breathingState by remember { mutableStateOf("Ready") } // Inhale, Hold, Exhale, Rest
    var progressAmount by remember { mutableStateOf(0f) }
    var completedCycles by remember { mutableStateOf(0) }

    LaunchedEffect(isActive) {
        if (isActive) {
            while (isActive) {
                // Inhale: 4s
                breathingState = "Inhale"
                val startInhale = System.currentTimeMillis()
                while (System.currentTimeMillis() - startInhale < 4000 && isActive) {
                    val elapsed = System.currentTimeMillis() - startInhale
                    progressAmount = elapsed.toFloat() / 4000f
                    delay(16)
                }
                if (!isActive) break
                progressAmount = 1f

                // Hold: 4s
                breathingState = "Hold"
                val startHold = System.currentTimeMillis()
                while (System.currentTimeMillis() - startHold < 4000 && isActive) {
                    progressAmount = 1f
                    delay(100)
                }
                if (!isActive) break

                // Exhale: 4s
                breathingState = "Exhale"
                val startExhale = System.currentTimeMillis()
                while (System.currentTimeMillis() - startExhale < 4000 && isActive) {
                    val elapsed = System.currentTimeMillis() - startExhale
                    progressAmount = 1f - (elapsed.toFloat() / 4000f)
                    delay(16)
                }
                if (!isActive) break
                progressAmount = 0f

                // Rest: 4s
                breathingState = "Hold & Rest"
                val startRest = System.currentTimeMillis()
                while (System.currentTimeMillis() - startRest < 4000 && isActive) {
                    progressAmount = 0f
                    delay(100)
                }
                if (!isActive) break

                completedCycles++
            }
        } else {
            progressAmount = 0f
            breathingState = "Ready"
        }
    }

    // Color interpolation for different phases
    val bubbleColor = when (breathingState) {
        "Inhale" -> ZenTeal
        "Hold" -> ZenAmber
        "Exhale" -> ZenViolet
        else -> ZenSoftGray
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Square Breathing Coach",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = ZenTextWhite,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Follow the bubble to balance your nervous system.",
            style = MaterialTheme.typography.bodyMedium,
            color = ZenSoftGray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        // Large glowing breathing bubble
        Box(
            modifier = Modifier
                .size(260.dp)
                .testTag("breathing_bubble_box"),
            contentAlignment = Alignment.Center
        ) {
            // Glowing background aura
            val glowRadius = 80.dp + (progressAmount * 120f).dp
            Box(
                modifier = Modifier
                    .size(glowRadius)
                    .clip(CircleShape)
                    .background(bubbleColor.copy(alpha = 0.05f))
                    .blur(30.dp)
            )

            // Primary moving canvas
            Canvas(modifier = Modifier.size(240.dp)) {
                val baseRadius = 50.dp.toPx()
                val addedRadius = progressAmount * 50.dp.toPx()
                val totalRadius = baseRadius + addedRadius

                // Pulsing central aura
                drawCircle(
                    color = bubbleColor.copy(alpha = 0.1f),
                    radius = totalRadius + 20f
                )

                // Filled circle
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(bubbleColor, bubbleColor.copy(alpha = 0.4f)),
                        center = center,
                        radius = totalRadius
                    ),
                    radius = totalRadius
                )

                // Thin outer ring
                drawCircle(
                    color = bubbleColor,
                    radius = totalRadius,
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            // Instructional text inside the bubble
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = when (breathingState) {
                        "Inhale" -> "BREATHE IN"
                        "Hold" -> "HOLD"
                        "Exhale" -> "BREATHE OUT"
                        "Hold & Rest" -> "REST"
                        else -> "PREPARE"
                    },
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = ZenDeepNight,
                    textAlign = TextAlign.Center
                )
                if (isActive) {
                    val secondsLeft = when (breathingState) {
                        "Inhale" -> (4 - (progressAmount * 4).toInt()).coerceIn(1, 4)
                        "Exhale" -> ((progressAmount * 4).toInt()).coerceIn(1, 4)
                        else -> 4
                    }
                    Text(
                        text = "$secondsLeft",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = ZenDeepNight.copy(alpha = 0.8f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // State Feedback
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = ZenSurface),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "Timer icon",
                    tint = ZenTeal
                )
                Text(
                    text = "Completed Cycles: $completedCycles",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = ZenTextWhite
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Control button
        Button(
            onClick = {
                isActive = !isActive
                if (!isActive) {
                    progressAmount = 0f
                    breathingState = "Ready"
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("breathing_start_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isActive) Color(0xFFE57373) else ZenTeal,
                contentColor = ZenDeepNight
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(
                imageVector = if (isActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isActive) "Pause" else "Start"
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isActive) "Pause Practice" else "Start Breathing Exercise",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

// Helpers for Mood logic
fun getMoodColor(mood: String): Color {
    return when (mood) {
        "RAD" -> ZenAmber
        "HAPPY" -> ZenTeal
        "NEUTRAL" -> Color(0xFF81C784)
        "SAD" -> ZenViolet
        "AWFUL" -> Color(0xFFE57373)
        else -> ZenSoftGray
    }
}

fun getMoodEmoji(mood: String): String {
    return when (mood) {
        "RAD" -> "🤩"
        "HAPPY" -> "🙂"
        "NEUTRAL" -> "😐"
        "SAD" -> "😢"
        "AWFUL" -> "😭"
        else -> "😐"
    }
}

fun formatTimestamp(timestamp: Long): String {
    val date = java.util.Date(timestamp)
    val format = java.text.SimpleDateFormat("MMM dd, yyyy · h:mm a", java.util.Locale.getDefault())
    return format.format(date)
}
