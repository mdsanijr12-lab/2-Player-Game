package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.audio.SfxType
import com.example.audio.SoundEngine
import com.example.data.GameCatalog
import com.example.data.SettingsRepository
import com.example.model.GameCategory
import com.example.model.GameSpec
import com.example.model.MatchConfig
import com.example.model.MatchMode
import com.example.model.MatchResult
import com.example.ui.screens.GameSelectionScreen
import com.example.ui.screens.GameplayScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ResultScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.MyApplicationTheme

sealed interface AppDestination {
    data object Splash : AppDestination
    data object Home : AppDestination
    data class GameSelection(val game: GameSpec) : AppDestination
    data class Gameplay(val config: MatchConfig) : AppDestination
    data class Result(val matchResult: MatchResult) : AppDestination
    data class Settings(val returnTo: AppDestination) : AppDestination
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                SaniFourPlayerApp(
                    onUpdateFullscreen = { fullscreen ->
                        val controller = WindowCompat.getInsetsController(window, window.decorView)
                        if (fullscreen) {
                            controller.systemBarsBehavior =
                                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                            controller.hide(WindowInsetsCompat.Type.systemBars())
                        } else {
                            controller.show(WindowInsetsCompat.Type.systemBars())
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun SaniFourPlayerApp(
    skipSplashForTesting: Boolean = false,
    onUpdateFullscreen: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val settingsRepo = remember { SettingsRepository(context) }
    val soundEngine = remember { SoundEngine(context) }
    val settings by settingsRepo.state.collectAsState()

    var destination by remember {
        mutableStateOf<AppDestination>(
            if (skipSplashForTesting) AppDestination.Home else AppDestination.Splash
        )
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(GameCategory.ALL) }
    var favoritesOnly by remember { mutableStateOf(false) }

    // Keep SoundEngine & Fullscreen synced with Settings
    LaunchedEffect(
        settings.musicVolume,
        settings.sfxVolume,
        settings.isMuted,
        settings.vibrationEnabled,
        settings.fullscreenEnabled
    ) {
        soundEngine.updateSettings(
            musicVol = settings.musicVolume,
            sfxVol = settings.sfxVolume,
            muted = settings.isMuted,
            vibrate = settings.vibrationEnabled
        )
        onUpdateFullscreen(settings.fullscreenEnabled)
    }

    DisposableEffect(Unit) {
        soundEngine.startMusic()
        onDispose {
            soundEngine.stopMusic()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF090D16)
    ) {
        Crossfade(
            targetState = destination,
            animationSpec = tween(durationMillis = 240),
            label = "app_screen_transition"
        ) { screen ->
            when (screen) {
                is AppDestination.Splash -> {
                    SplashScreen(
                        lang = settings.language,
                        onSplashFinished = {
                            destination = AppDestination.Home
                        }
                    )
                }

                is AppDestination.Home -> {
                    HomeScreen(
                        lang = settings.language,
                        searchQuery = searchQuery,
                        selectedCategory = selectedCategory,
                        favoritesOnly = favoritesOnly,
                        favoriteIds = settings.favoriteGameIds,
                        onSearchQueryChanged = { searchQuery = it },
                        onCategorySelected = {
                            soundEngine.playSfx(SfxType.CLICK)
                            selectedCategory = it
                        },
                        onToggleFavoritesOnly = {
                            soundEngine.playSfx(SfxType.CLICK)
                            favoritesOnly = !favoritesOnly
                        },
                        onToggleFavoriteGame = { gameId ->
                            soundEngine.playSfx(SfxType.CLICK)
                            settingsRepo.toggleFavorite(gameId)
                        },
                        onToggleLanguage = {
                            soundEngine.playSfx(SfxType.CLICK)
                            settingsRepo.toggleLanguage()
                        },
                        onOpenSettings = {
                            soundEngine.playSfx(SfxType.CLICK)
                            destination = AppDestination.Settings(returnTo = AppDestination.Home)
                        },
                        onSelectGame = { game ->
                            soundEngine.playSfx(SfxType.CLICK)
                            destination = AppDestination.GameSelection(game)
                        }
                    )
                }

                is AppDestination.GameSelection -> {
                    val game = GameCatalog.getById(screen.game.id)
                    GameSelectionScreen(
                        game = game,
                        lang = settings.language,
                        isFavorite = game.id in settings.favoriteGameIds,
                        onToggleFavorite = {
                            soundEngine.playSfx(SfxType.CLICK)
                            settingsRepo.toggleFavorite(game.id)
                        },
                        onBack = {
                            soundEngine.playSfx(SfxType.CLICK)
                            destination = AppDestination.Home
                        },
                        onStartMatch = { matchConfig ->
                            soundEngine.playSfx(SfxType.CLICK)
                            destination = AppDestination.Gameplay(matchConfig)
                        }
                    )
                }

                is AppDestination.Gameplay -> {
                    GameplayScreen(
                        config = screen.config,
                        lang = settings.language,
                        soundEngine = soundEngine,
                        controlSensitivity = settings.controlSensitivity,
                        graphicsQuality = settings.graphicsQuality,
                        onMatchFinished = { result ->
                            destination = AppDestination.Result(result)
                        },
                        onOpenSettingsFromPause = {
                            soundEngine.playSfx(SfxType.CLICK)
                            destination = AppDestination.Settings(returnTo = screen)
                        },
                        onExitToSelection = {
                            soundEngine.playSfx(SfxType.CLICK)
                            destination = AppDestination.GameSelection(screen.config.game)
                        }
                    )
                }

                is AppDestination.Result -> {
                    ResultScreen(
                        result = screen.matchResult,
                        lang = settings.language,
                        onReplay = {
                            soundEngine.playSfx(SfxType.CLICK)
                            destination = AppDestination.Gameplay(screen.matchResult.config)
                        },
                        onBackToGames = {
                            soundEngine.playSfx(SfxType.CLICK)
                            destination = AppDestination.Home
                        }
                    )
                }

                is AppDestination.Settings -> {
                    SettingsScreen(
                        settings = settings,
                        onLanguageSelected = {
                            soundEngine.playSfx(SfxType.CLICK)
                            settingsRepo.setLanguage(it)
                        },
                        onMusicVolumeChanged = { settingsRepo.setMusicVolume(it) },
                        onSfxVolumeChanged = { settingsRepo.setSfxVolume(it) },
                        onMutedChanged = {
                            soundEngine.playSfx(SfxType.CLICK)
                            settingsRepo.setMuted(it)
                        },
                        onVibrationChanged = {
                            soundEngine.playSfx(SfxType.CLICK)
                            settingsRepo.setVibration(it)
                        },
                        onFullscreenChanged = {
                            soundEngine.playSfx(SfxType.CLICK)
                            settingsRepo.setFullscreen(it)
                        },
                        onGraphicsQualityChanged = {
                            soundEngine.playSfx(SfxType.CLICK)
                            settingsRepo.setGraphicsQuality(it)
                        },
                        onControlSensitivityChanged = { settingsRepo.setControlSensitivity(it) },
                        onResetFavorites = {
                            soundEngine.playSfx(SfxType.CLICK)
                            settingsRepo.resetFavorites()
                        },
                        onResetSettings = {
                            soundEngine.playSfx(SfxType.CLICK)
                            settingsRepo.resetSettings()
                        },
                        onBack = {
                            soundEngine.playSfx(SfxType.CLICK)
                            destination = screen.returnTo
                        }
                    )
                }
            }
        }
    }
}
