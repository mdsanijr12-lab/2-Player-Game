package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppLanguage
import com.example.model.BotDifficulty
import com.example.model.GameSpec
import com.example.model.MatchConfig
import com.example.model.MatchMode
import com.example.model.PlayerId
import com.example.model.UiStrings
import com.example.ui.components.GameThumbnailView

@Composable
fun GameSelectionScreen(
    game: GameSpec,
    lang: AppLanguage,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onBack: () -> Unit,
    onStartMatch: (MatchConfig) -> Unit
) {
    BackHandler { onBack() }

    var selectedMode by remember(game.id) { mutableStateOf(MatchMode.VS_BOT) }
    var selectedPvpPlayers by remember(game.id) {
        mutableIntStateOf(game.supportedPvpCounts.last())
    }
    var selectedBotCount by remember(game.id) {
        mutableIntStateOf(game.supportedBotCounts.last())
    }
    var selectedDifficulty by remember(game.id) {
        mutableStateOf(BotDifficulty.NORMAL)
    }

    val currentConfig = MatchConfig(
        game = game,
        mode = selectedMode,
        pvpPlayerCount = selectedPvpPlayers,
        botCount = selectedBotCount,
        botDifficulty = selectedDifficulty
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF090D16), Color(0xFF0F172A), Color(0xFF111827))
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag("game_selection_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Top Navigation Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E293B))
                        .testTag("back_to_home_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = UiStrings.get(lang, "back"),
                        tint = Color.White
                    )
                }

                Text(
                    text = game.title(lang),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )

                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E293B))
                        .testTag("detail_favorite_button")
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = UiStrings.get(lang, "favorites"),
                        tint = if (isFavorite) Color(0xFFEF4444) else Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Game Hero Card (Thumbnail + Description + Specs)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, game.category.accentColor.copy(alpha = 0.45f), RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    GameThumbnailView(
                        game = game,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(175.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = game.category.accentColor.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = game.category.label(lang),
                                color = game.category.accentColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = Color(0xFFFACC15),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${game.durationSeconds} ${UiStrings.get(lang, "seconds_short")}",
                                color = Color(0xFFFACC15),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = game.description(lang),
                        color = Color(0xFFE2E8F0),
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Objective, Win Rule, and Controls Summary
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0F172A))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = Color(0xFFFACC15),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${UiStrings.get(lang, "win_condition")}: ${game.winRule.label(lang)}",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Gamepad,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${UiStrings.get(lang, "controls")}: ${game.controlScheme.description(lang)}",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Mode Selection: Player vs Player OR VS Bot
            Text(
                text = UiStrings.get(lang, "select_mode"),
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MatchMode.entries.forEach { mode ->
                    val isSelected = selectedMode == mode
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { selectedMode = mode }
                            .border(
                                width = 2.dp,
                                color = if (isSelected) Color(0xFF38BDF8) else Color(0xFF334155),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .testTag("mode_btn_${mode.name.lowercase()}"),
                        color = if (isSelected) Color(0xFF1E3A8A) else Color(0xFF1E293B)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 14.dp, horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (mode == MatchMode.PVP) Icons.Default.People else Icons.Default.SmartToy,
                                contentDescription = null,
                                tint = if (isSelected) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = mode.label(lang),
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Mode Configuration: Only show valid player counts / bot counts for this specific game!
            if (selectedMode == MatchMode.PVP) {
                Text(
                    text = UiStrings.get(lang, "how_Many_players"),
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    game.supportedPvpCounts.forEach { count ->
                        val isSelected = selectedPvpPlayers == count
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedPvpPlayers = count }
                                .border(
                                    width = 2.dp,
                                    color = if (isSelected) Color(0xFF22C55E) else Color(0xFF334155),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .testTag("pvp_count_$count"),
                            color = if (isSelected) Color(0xFF14532D) else Color(0xFF1E293B)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "$count ${UiStrings.get(lang, "players")}",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            } else {
                // VS BOT Configuration: Number of Bots (1..maxBots) + Difficulty (Easy, Normal, Hard)
                Text(
                    text = UiStrings.get(lang, "how_many_bots"),
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    game.supportedBotCounts.forEach { bCount ->
                        val isSelected = selectedBotCount == bCount
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedBotCount = bCount }
                                .border(
                                    width = 2.dp,
                                    color = if (isSelected) Color(0xFF38BDF8) else Color(0xFF334155),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .testTag("bot_count_$bCount"),
                            color = if (isSelected) Color(0xFF1E3A8A) else Color(0xFF1E293B)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                val label = if (bCount == 1) {
                                    "1 ${UiStrings.get(lang, "bot")}"
                                } else {
                                    "$bCount ${UiStrings.get(lang, "bots")}"
                                }
                                Text(
                                    text = label,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = UiStrings.get(lang, "bot_difficulty"),
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BotDifficulty.entries.forEach { diff ->
                        val isSelected = selectedDifficulty == diff
                        val diffColor = when (diff) {
                            BotDifficulty.EASY -> Color(0xFF22C55E)
                            BotDifficulty.NORMAL -> Color(0xFFFACC15)
                            BotDifficulty.HARD -> Color(0xFFEF4444)
                        }
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedDifficulty = diff }
                                .border(
                                    width = 2.dp,
                                    color = if (isSelected) diffColor else Color(0xFF334155),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .testTag("difficulty_${diff.name.lowercase()}"),
                            color = if (isSelected) diffColor.copy(alpha = 0.2f) else Color(0xFF1E293B)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = diff.label(lang),
                                    color = if (isSelected) diffColor else Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Active Player Colors & Identity Preview
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = UiStrings.get(lang, "player_colors"),
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        val totalP = currentConfig.totalActivePlayers
                        for (i in 0 until totalP) {
                            val pid = PlayerId.entries[i]
                            val isBotSlot = currentConfig.isBot(i)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = pid.displayBadge(lang),
                                    color = pid.lightColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = if (isBotSlot) UiStrings.get(lang, "bot") else UiStrings.get(lang, "player"),
                                    color = Color(0xFF94A3B8),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // START MATCH Button
            Button(
                onClick = { onStartMatch(currentConfig) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .testTag("start_match_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E))
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color(0xFF090D16),
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = UiStrings.get(lang, "start"),
                    color = Color(0xFF090D16),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
