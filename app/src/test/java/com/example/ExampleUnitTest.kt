package com.example

import com.example.data.GameCatalog
import com.example.model.AppLanguage
import com.example.model.GameCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun catalog_contains105UniqueGames_withValidSpecs() {
        val all = GameCatalog.allGames
        assertEquals(105, all.size)

        val uniqueIds = all.map { it.id }.toSet()
        assertEquals(105, uniqueIds.size)

        val uniqueNamesEn = all.map { it.nameEn }.toSet()
        assertEquals(105, uniqueNamesEn.size)

        val uniqueNamesBn = all.map { it.nameBn }.toSet()
        assertEquals(105, uniqueNamesBn.size)

        all.forEach { game ->
            assertTrue(game.durationSeconds in 30..120)
            assertTrue(game.supportedPvpCounts.isNotEmpty())
            assertTrue(game.supportedBotCounts.isNotEmpty())
            assertTrue(game.descEn.isNotBlank())
            assertTrue(game.descBn.isNotBlank())
            assertTrue(game.objectiveEn.isNotBlank())
            assertTrue(game.objectiveBn.isNotBlank())
        }

        val racingGames = GameCatalog.filterGames(
            query = "",
            category = GameCategory.RACING,
            favoritesOnly = false,
            favoriteIds = emptySet(),
            lang = AppLanguage.ENGLISH
        )
        assertTrue(racingGames.size >= 10)
    }
}
