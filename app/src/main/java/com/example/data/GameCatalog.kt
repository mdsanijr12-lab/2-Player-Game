package com.example.data

import com.example.model.AppLanguage
import com.example.model.GameCategory
import com.example.model.GameSpec

object GameCatalog {
    val allGames: List<GameSpec> by lazy {
        (GameCatalogPart1.games1To55 + GameCatalogPart2.games56To105).sortedBy { it.id }
    }

    fun getById(id: Int): GameSpec =
        allGames.firstOrNull { it.id == id } ?: allGames.first()

    fun filterGames(
        query: String,
        category: GameCategory,
        favoritesOnly: Boolean,
        favoriteIds: Set<Int>,
        lang: AppLanguage
    ): List<GameSpec> {
        val trimmed = query.trim().lowercase()
        return allGames.filter { game ->
            if (favoritesOnly && game.id !in favoriteIds) return@filter false
            if (category != GameCategory.ALL && game.category != category) return@filter false
            if (trimmed.isEmpty()) return@filter true

            val matchNameEn = game.nameEn.lowercase().contains(trimmed)
            val matchNameBn = game.nameBn.lowercase().contains(trimmed)
            val matchDescEn = game.descEn.lowercase().contains(trimmed)
            val matchDescBn = game.descBn.lowercase().contains(trimmed)
            val matchCatEn = game.category.titleEn.lowercase().contains(trimmed)
            val matchCatBn = game.category.titleBn.lowercase().contains(trimmed)
            val matchKeyword = game.keywords.any { it.lowercase().contains(trimmed) }
            val matchId = game.id.toString() == trimmed || "#${game.id}" == trimmed

            matchNameEn || matchNameBn || matchDescEn || matchDescBn || matchCatEn || matchCatBn || matchKeyword || matchId
        }
    }
}
