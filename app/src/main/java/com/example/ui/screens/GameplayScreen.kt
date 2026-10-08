package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.SfxType
import com.example.audio.SoundEngine
import com.example.engine.ArenaRenderer.renderArena
import com.example.engine.GameEngine
import com.example.model.AppLanguage
import com.example.model.GraphicsQuality
import com.example.model.MatchConfig
import com.example.model.MatchMode
import com.example.model.MatchResult
import com.example.model.PlayerId
import com.example.model.UiStrings
import com.example.ui.components.PlayerControlDeck
import kotlinx.coroutines.delay
import kotlin.math.ceil

@Composable
fun GameplayScreen(
    config: MatchConfig,
    lang: AppLanguage,
    soundEngine: SoundEngine,
    controlSensitivity: Float,
    graphicsQuality: GraphicsQuality,
    onMatchFinished: (MatchResult) -> Unit,
    onOpenSettingsFromPause: () -> Unit,
    onExitToSelection: () -> Unit
) {
    var restartGeneration by remember { mutableIntStateOf(0) }
    var countdownStep by remember(restartGeneration) { mutableIntStateOf(3) } // 3, 2, 1, 0(GO!), -1(Playing)
    var isPaused by remember(restartGeneration) { mutableStateOf(false) }
    var frameTick by remember { mutableLongStateOf(0L) }

    val engine = remember(config, restartGeneration) {
        GameEngine(
            config = config,
            soundEngine = soundEngine,
            controlSensitivity = controlSensitivity,
            particleMultiplier = graphicsQuality.particleMultiplier
        )
    }

    // Back button behavior: in VS_BOT, opens pause menu; in PVP, exits to selection
    BackHandler {
        if (config.mode == MatchMode.VS_BOT) {
            isPaused = !isPaused
        } else {
            onExitToSelection()
        }
    }

    // 3 -> 2 -> 1 -> GO! Countdown sequence
    LaunchedEffect(restartGeneration) {
        countdownStep = 3
        soundEngine.playSfx(SfxType.COUNTDOWN_TICK)
        delay(750)
        countdownStep = 2
        soundEngine.playSfx(SfxType.COUNTDOWN_TICK)
        delay(750)
        countdownStep = 1
        soundEngine.playSfx(SfxType.COUNTDOWN_TICK)
        delay(750)
        countdownStep = 0
        soundEngine.playSfx(SfxType.COUNTDOWN_GO)
        delay(550)
        countdownStep = -1
    }

    // 60 FPS Real-Time Game Loop (runs only after countdown completes and when not paused)
    LaunchedEffect(countdownStep, isPaused, restartGeneration) {
        if (countdownStep == -1 && !isPaused) {
            var lastNanos = 0L
            while (true) {
                withFrameNanos { nanos ->
                    if (lastNanos == 0L) lastNanos = nanos
                    val dt = ((nanos - lastNanos) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                    lastNanos = nanos
                    engine.step(dt)
                    frameTick = nanos
                }
                val result = engine.matchResult
                if (result != null) {
                    onMatchFinished(result)
                    break
                }
            }
        }
    }

    // Read frameTick to trigger Canvas and HUD recomposition smoothly
    val currentTick = frameTick
    val remainingSecInt = ceil(engine.remainingSeconds).toInt().coerceAtLeast(0)
    val mins = remainingSecInt / 60
    val secs = remainingSecInt % 60
    val timerText = "${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}"
    val totalPlayers = config.totalActivePlayers
    val textMeasurer = rememberTextMeasurer()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090D16))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag("gameplay_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ==================== TOP PLAYER CONTROLS (P2 & P3) ====================
            if (totalPlayers == 2) {
                val p2 = engine.players[1]
                PlayerControlDeck(
                    playerId = PlayerId.P2,
                    score = p2.score,
                    eliminated = p2.eliminated,
                    isBot = p2.isBot,
                    controlScheme = config.game.controlScheme,
                    lang = lang,
                    invertedTopPlayer = true,
                    compactCornerMode = false,
                    onMoveChanged = { mx, my -> engine.setHumanMovement(1, mx, my) },
                    onPrimaryChanged = { pressed -> engine.setHumanPrimaryButton(1, pressed) },
                    onSecondaryChanged = { pressed -> engine.setHumanSecondaryButton(1, pressed) },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                // 3 or 4 players: Top-Left (P3 Yellow) and Top-Right (P2 Blue)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val p3 = engine.players.getOrNull(2)
                    if (p3 != null) {
                        PlayerControlDeck(
                            playerId = PlayerId.P3,
                            score = p3.score,
                            eliminated = p3.eliminated,
                            isBot = p3.isBot,
                            controlScheme = config.game.controlScheme,
                            lang = lang,
                            invertedTopPlayer = true,
                            compactCornerMode = true,
                            onMoveChanged = { mx, my -> engine.setHumanMovement(2, mx, my) },
                            onPrimaryChanged = { pressed -> engine.setHumanPrimaryButton(2, pressed) },
                            onSecondaryChanged = { pressed -> engine.setHumanSecondaryButton(2, pressed) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    val p2 = engine.players[1]
                    PlayerControlDeck(
                        playerId = PlayerId.P2,
                        score = p2.score,
                        eliminated = p2.eliminated,
                        isBot = p2.isBot,
                        controlScheme = config.game.controlScheme,
                        lang = lang,
                        invertedTopPlayer = true,
                        compactCornerMode = true,
                        onMoveChanged = { mx, my -> engine.setHumanMovement(1, mx, my) },
                        onPrimaryChanged = { pressed -> engine.setHumanPrimaryButton(1, pressed) },
                        onSecondaryChanged = { pressed -> engine.setHumanSecondaryButton(1, pressed) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ==================== HUD BAR (TIMER + OBJECTIVE + VS BOT PAUSE) ====================
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                color = Color(0xFF1E293B),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Fixed Match Timer Pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0F172A))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("match_timer_display")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = if (remainingSecInt <= 10) Color(0xFFEF4444) else Color(0xFFFACC15),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = timerText,
                            color = if (remainingSecInt <= 10) Color(0xFFEF4444) else Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    // Game Name & Active Prompt Banner
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = config.game.title(lang),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val banner = if (lang == AppLanguage.BANGLA) engine.hudBannerBn else engine.hudBannerEn
                        Text(
                            text = banner,
                            color = Color(0xFF38BDF8),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // PAUSE BUTTON: Visible ONLY in VS BOT mode! (Strictly no pause button in PVP)
                    if (config.mode == MatchMode.VS_BOT) {
                        IconButton(
                            onClick = { isPaused = true },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0F172A))
                                .testTag("pause_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Pause,
                                contentDescription = UiStrings.get(lang, "pause"),
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else {
                        // In PVP mode, show compact PVP badge without any pause button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF0F172A))
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "${totalPlayers}P",
                                color = Color(0xFF4ADE80),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }

            // ==================== CENTRAL 2D TOP-DOWN ARENA CANVAS ====================
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .aspectRatio(1f, matchHeightConstraintsFirst = true)
                        .testTag("arena_canvas")
                ) {
                    // Reference currentTick so Compose redraws every frame
                    if (currentTick >= 0L) {
                        renderArena(engine = engine, textMeasurer = textMeasurer)
                    }
                }
            }

            // ==================== BOTTOM PLAYER CONTROLS (P1 & P4) ====================
            if (totalPlayers < 4 || config.mode == MatchMode.VS_BOT) {
                val p1 = engine.players[0]
                PlayerControlDeck(
                    playerId = PlayerId.P1,
                    score = p1.score,
                    eliminated = p1.eliminated,
                    isBot = false,
                    controlScheme = config.game.controlScheme,
                    lang = lang,
                    invertedTopPlayer = false,
                    compactCornerMode = false,
                    onMoveChanged = { mx, my -> engine.setHumanMovement(0, mx, my) },
                    onPrimaryChanged = { pressed -> engine.setHumanPrimaryButton(0, pressed) },
                    onSecondaryChanged = { pressed -> engine.setHumanSecondaryButton(0, pressed) },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                // 4 Local Human Players: Bottom-Left (P1 Red) and Bottom-Right (P4 Green)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val p1 = engine.players[0]
                    PlayerControlDeck(
                        playerId = PlayerId.P1,
                        score = p1.score,
                        eliminated = p1.eliminated,
                        isBot = false,
                        controlScheme = config.game.controlScheme,
                        lang = lang,
                        invertedTopPlayer = false,
                        compactCornerMode = true,
                        onMoveChanged = { mx, my -> engine.setHumanMovement(0, mx, my) },
                        onPrimaryChanged = { pressed -> engine.setHumanPrimaryButton(0, pressed) },
                        onSecondaryChanged = { pressed -> engine.setHumanSecondaryButton(0, pressed) },
                        modifier = Modifier.weight(1f)
                    )
                    val p4 = engine.players[3]
                    PlayerControlDeck(
                        playerId = PlayerId.P4,
                        score = p4.score,
                        eliminated = p4.eliminated,
                        isBot = p4.isBot,
                        controlScheme = config.game.controlScheme,
                        lang = lang,
                        invertedTopPlayer = false,
                        compactCornerMode = true,
                        onMoveChanged = { mx, my -> engine.setHumanMovement(3, mx, my) },
                        onPrimaryChanged = { pressed -> engine.setHumanPrimaryButton(3, pressed) },
                        onSecondaryChanged = { pressed -> engine.setHumanSecondaryButton(3, pressed) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // ==================== 3 - 2 - 1 - GO! COUNTDOWN OVERLAY ====================
        AnimatedVisibility(
            visible = countdownStep >= 0,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            val pulseScale by animateFloatAsState(
                targetValue = if (countdownStep == 0) 1.2f else 1.0f,
                animationSpec = tween(250),
                label = "countdown_pulse"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xAA090D16))
                    .testTag("countdown_overlay"),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .scale(pulseScale)
                        .border(3.dp, Color(0xFF38BDF8), CircleShape),
                    shape = CircleShape,
                    color = Color(0xFF1E293B)
                ) {
                    Box(
                        modifier = Modifier.size(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (countdownStep > 0) "$countdownStep" else UiStrings.get(lang, "ready_go"),
                            color = if (countdownStep == 0) Color(0xFF4ADE80) else Color.White,
                            fontSize = 46.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }

        // ==================== PAUSE MENU OVERLAY (VS BOT MODE ONLY) ====================
        if (isPaused && config.mode == MatchMode.VS_BOT) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xCC090D16))
                    .testTag("pause_menu_overlay"),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.84f)
                        .border(1.5.dp, Color(0xFF38BDF8), RoundedCornerShape(22.dp)),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = UiStrings.get(lang, "paused"),
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = { isPaused = false },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("resume_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(
                                text = UiStrings.get(lang, "resume"),
                                color = Color(0xFF090D16),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp
                            )
                        }

                        Button(
                            onClick = {
                                isPaused = false
                                restartGeneration++
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("restart_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(
                                text = UiStrings.get(lang, "restart"),
                                color = Color(0xFF090D16),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp
                            )
                        }

                        Button(
                            onClick = onOpenSettingsFromPause,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("pause_settings_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(
                                text = UiStrings.get(lang, "settings"),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }

                        Button(
                            onClick = onExitToSelection,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("exit_match_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(
                                text = UiStrings.get(lang, "exit"),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
