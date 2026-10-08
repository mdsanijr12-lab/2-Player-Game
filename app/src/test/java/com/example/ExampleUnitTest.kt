package com.example

import com.example.data.GameCatalog
import com.example.model.AppLanguage
import com.example.model.BotDifficulty
import com.example.model.ControlScheme
import com.example.model.GameCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun catalog_contains105UniqueGames_withValidSpecsAndContextControls() {
        val all = GameCatalog.allGames
        assertEquals(105, all.size)

        val uniqueIds = all.map { it.id }.toSet()
        assertEquals(105, uniqueIds.size)

        val uniqueNamesEn = all.map { it.nameEn }.toSet()
        assertEquals(105, uniqueNamesEn.size)

        val uniqueNamesBn = all.map { it.nameBn }.toSet()
        assertEquals(105, uniqueNamesBn.size)

        val contextSchemesSeen = mutableSetOf<ControlScheme>()

        all.forEach { game ->
            assertTrue(game.durationSeconds in 30..120)
            assertTrue(game.supportedPvpCounts.isNotEmpty())
            assertTrue(game.supportedBotCounts.isNotEmpty())
            assertTrue(game.descEn.isNotBlank())
            assertTrue(game.descBn.isNotBlank())
            assertTrue(game.objectiveEn.isNotBlank())
            assertTrue(game.objectiveBn.isNotBlank())

            // Part 1 verification: Every game defines what player controls, brief instruction, and context controls
            assertTrue(game.playerControlsEntityDescription(AppLanguage.ENGLISH).isNotBlank())
            assertTrue(game.playerControlsEntityDescription(AppLanguage.BANGLA).isNotBlank())
            assertTrue(game.briefInstruction(AppLanguage.ENGLISH).isNotBlank())
            assertTrue(game.briefInstruction(AppLanguage.BANGLA).isNotBlank())
            assertTrue(game.effectiveControlScheme.howToPlayText(AppLanguage.ENGLISH).isNotBlank())
            assertTrue(game.effectiveControlScheme.howToPlayText(AppLanguage.BANGLA).isNotBlank())

            contextSchemesSeen.add(game.effectiveControlScheme)
        }

        // Verify multiple genre-specific control schemes are actively used across games
        assertTrue(contextSchemesSeen.contains(ControlScheme.FIGHTING_CONTROLS))
        assertTrue(contextSchemesSeen.contains(ControlScheme.PUSH_ARENA_CONTROLS))
        assertTrue(contextSchemesSeen.contains(ControlScheme.RACING_CONTROLS))
        assertTrue(contextSchemesSeen.contains(ControlScheme.SPORTS_CONTROLS))
        assertTrue(contextSchemesSeen.contains(ControlScheme.TANK_CONTROLS))
        assertTrue(contextSchemesSeen.contains(ControlScheme.JUMP_DODGE_CONTROLS))
        assertTrue(contextSchemesSeen.contains(ControlScheme.PUZZLE_TACTICAL_CONTROLS))
        assertTrue(contextSchemesSeen.contains(ControlScheme.TAP_REACTION))

        val racingGames = GameCatalog.filterGames(
            query = "",
            category = GameCategory.RACING,
            favoritesOnly = false,
            favoriteIds = emptySet(),
            lang = AppLanguage.ENGLISH
        )
        assertTrue(racingGames.size >= 10)
    }

    @Test
    fun botDifficulty_enforcesFairSpeedAndReactionDelays() {
        // Easy Bot: 75%–90% speed, 500ms–900ms delay
        assertEquals(0.75f, BotDifficulty.EASY.minSpeedScale, 0.001f)
        assertEquals(0.90f, BotDifficulty.EASY.maxSpeedScale, 0.001f)
        assertEquals(0.500f, BotDifficulty.EASY.minReactionDelaySec, 0.001f)
        assertEquals(0.900f, BotDifficulty.EASY.maxReactionDelaySec, 0.001f)

        // Normal Bot: 90%–105% speed, 250ms–500ms delay
        assertEquals(0.90f, BotDifficulty.NORMAL.minSpeedScale, 0.001f)
        assertEquals(1.05f, BotDifficulty.NORMAL.maxSpeedScale, 0.001f)
        assertEquals(0.250f, BotDifficulty.NORMAL.minReactionDelaySec, 0.001f)
        assertEquals(0.500f, BotDifficulty.NORMAL.maxReactionDelaySec, 0.001f)

        // Hard Bot: 100%–115% speed, 120ms–300ms delay
        assertEquals(1.00f, BotDifficulty.HARD.minSpeedScale, 0.001f)
        assertEquals(1.15f, BotDifficulty.HARD.maxSpeedScale, 0.001f)
        assertEquals(0.120f, BotDifficulty.HARD.minReactionDelaySec, 0.001f)
        assertEquals(0.300f, BotDifficulty.HARD.maxReactionDelaySec, 0.001f)
    }
}
