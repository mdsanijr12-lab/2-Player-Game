package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.audio.SoundEngine
import com.example.data.GameCatalog
import com.example.data.SettingsRepository
import com.example.engine.GameEngine
import com.example.model.AppLanguage
import com.example.model.BotDifficulty
import com.example.model.MatchConfig
import com.example.model.MatchMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context and verify offline engine`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("SANI 4Player Game", appName)

        val repo = SettingsRepository(context)
        repo.setLanguage(AppLanguage.BANGLA)
        assertEquals(AppLanguage.BANGLA, repo.state.value.language)
        repo.toggleFavorite(1)
        assertTrue(1 in repo.state.value.favoriteGameIds)

        val soundEngine = SoundEngine(context)
        val firstGame = GameCatalog.getById(1)
        val config = MatchConfig(
            game = firstGame,
            mode = MatchMode.VS_BOT,
            botCount = 3,
            botDifficulty = BotDifficulty.HARD
        )
        val engine = GameEngine(
            config = config,
            soundEngine = soundEngine,
            controlSensitivity = 1.0f,
            particleMultiplier = 1.0f
        )
        engine.step(0.016f)
        assertEquals(4, engine.players.size)
        assertNotNull(engine.players.first())
    }
}
