package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.GameCatalog
import com.example.model.AppLanguage
import com.example.model.GameCategory
import com.example.model.GameSpec
import com.example.model.PlayerId
import com.example.model.UiStrings
import com.example.ui.components.GameThumbnailView

@Composable
fun HomeScreen(
    lang: AppLanguage,
    searchQuery: String,
    selectedCategory: GameCategory,
    favoritesOnly: Boolean,
    favoriteIds: Set<Int>,
    onSearchQueryChanged: (String) -> Unit,
    onCategorySelected: (GameCategory) -> Unit,
    onToggleFavoritesOnly: () -> Unit,
    onToggleFavoriteGame: (Int) -> Unit,
    onToggleLanguage: () -> Unit,
    onOpenSettings: () -> Unit,
    onSelectGame: (GameSpec) -> Unit
) {
    val filteredGames = GameCatalog.filterGames(
        query = searchQuery,
        category = selectedCategory,
        favoritesOnly = favoritesOnly,
        favoriteIds = favoriteIds,
        lang = lang
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
            .testTag("home_screen")
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 158.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. Top Header Bar + Branding Banner + Search + Category Filter
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Top Action Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = R.drawable.img_brand_splash),
                                contentDescription = stringResource(id = R.string.splash_logo_desc),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(1.5.dp, Color(0xFF38BDF8), RoundedCornerShape(10.dp))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = UiStrings.get(lang, "app_name"),
                                    color = Color.White,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    PlayerId.entries.forEach { pid ->
                                        Text(
                                            text = pid.emoji,
                                            fontSize = 10.sp
                                        )
                                    }
                                    Text(
                                        text = "${GameCatalog.allGames.size} ${UiStrings.get(lang, "games")}",
                                        color = Color(0xFF38BDF8),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Language Quick Switch (EN / বাংলা)
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable { onToggleLanguage() }
                                    .testTag("language_switch_button"),
                                color = Color(0xFF1E293B),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Language,
                                        contentDescription = UiStrings.get(lang, "language"),
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (lang == AppLanguage.ENGLISH) "বাংলা" else "EN",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Favorites Filter Button
                            IconButton(
                                onClick = onToggleFavoritesOnly,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(if (favoritesOnly) Color(0xFFEF4444) else Color(0xFF1E293B))
                                    .testTag("favorites_filter_button")
                            ) {
                                Icon(
                                    imageVector = if (favoritesOnly) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = UiStrings.get(lang, "favorites"),
                                    tint = Color.White
                                )
                            }

                            // Settings Button
                            IconButton(
                                onClick = onOpenSettings,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1E293B))
                                    .testTag("settings_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = UiStrings.get(lang, "settings"),
                                    tint = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Home Screen Top Branding Banner Image (compact, balanced height)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(108.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .border(
                                width = 1.5.dp,
                                color = Color(0xFF334155),
                                shape = RoundedCornerShape(18.dp)
                            )
                            .testTag("home_branding_banner")
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_brand_banner),
                            contentDescription = stringResource(id = R.string.home_banner_desc),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xCC090D16),
                                            Color(0x44090D16),
                                            Color(0xAA090D16)
                                        )
                                    )
                                )
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            contentAlignment = Alignment.BottomStart
                        ) {
                            Column {
                                Text(
                                    text = UiStrings.get(lang, "app_name"),
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = UiStrings.get(lang, "app_subtitle"),
                                    color = Color(0xFFE2E8F0),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChanged,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_input"),
                        placeholder = {
                            Text(
                                text = UiStrings.get(lang, "search_hint"),
                                color = Color(0xFF94A3B8),
                                fontSize = 13.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchQueryChanged("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = UiStrings.get(lang, "clear_filters"),
                                        tint = Color(0xFF94A3B8)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF1E293B),
                            unfocusedContainerColor = Color(0xFF1E293B),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF334155)
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Category Filter Row
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("category_row")
                    ) {
                        items(GameCategory.entries) { cat ->
                            val isSelected = selectedCategory == cat
                            val bg by animateColorAsState(
                                targetValue = if (isSelected) cat.accentColor else Color(0xFF1E293B),
                                label = "cat_bg"
                            )
                            val count = if (cat == GameCategory.ALL) {
                                GameCatalog.allGames.size
                            } else {
                                GameCatalog.allGames.count { it.category == cat }
                            }

                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onCategorySelected(cat) }
                                    .testTag("category_chip_${cat.id}"),
                                color = bg,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = cat.label(lang),
                                        color = if (isSelected) Color(0xFF090D16) else Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "$count",
                                        color = if (isSelected) Color(0xFF090D16).copy(alpha = 0.8f) else cat.accentColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Empty State if no games match
            if (filteredGames.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (favoritesOnly && favoriteIds.isEmpty()) {
                                UiStrings.get(lang, "no_favorites_yet")
                            } else {
                                UiStrings.get(lang, "no_games_found")
                            },
                            color = Color(0xFF94A3B8),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                onSearchQueryChanged("")
                                onCategorySelected(GameCategory.ALL)
                                if (favoritesOnly) onToggleFavoritesOnly()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8))
                        ) {
                            Text(
                                text = UiStrings.get(lang, "clear_filters"),
                                color = Color(0xFF090D16),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                // 3. Large Responsive Game Cards
                items(
                    items = filteredGames,
                    key = { it.id }
                ) { game ->
                    GameCardItem(
                        game = game,
                        lang = lang,
                        isFavorite = game.id in favoriteIds,
                        onToggleFavorite = { onToggleFavoriteGame(game.id) },
                        onClick = { onSelectGame(game) }
                    )
                }
            }
        }
    }
}

@Composable
private fun GameCardItem(
    game: GameSpec,
    lang: AppLanguage,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .border(
                width = 1.dp,
                color = game.category.accentColor.copy(alpha = 0.35f),
                shape = RoundedCornerShape(18.dp)
            )
            .testTag("game_card_${game.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(114.dp)
            ) {
                GameThumbnailView(
                    game = game,
                    modifier = Modifier.fillMaxSize()
                )

                // Favorite Heart Button in Top-Right
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xAA090D16))
                        .testTag("fav_btn_${game.id}")
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = UiStrings.get(lang, "favorites"),
                        tint = if (isFavorite) Color(0xFFEF4444) else Color.White,
                        modifier = Modifier.size(17.dp)
                    )
                }

                // Category Pill in Bottom-Start
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                        .background(
                            color = Color(0xCC090D16),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = game.category.label(lang),
                        color = game.category.accentColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = game.title(lang),
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = game.description(lang),
                color = Color(0xFF94A3B8),
                fontSize = 11.sp,
                maxLines = 2,
                lineHeight = 14.sp,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Footer Metadata: Supported Players + Match Timer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val playerRange = if (game.supportedPvpCounts.size == 1) {
                    "${game.supportedPvpCounts.first()}P"
                } else {
                    "${game.supportedPvpCounts.first()}–${game.supportedPvpCounts.last()}P"
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.People,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = playerRange,
                        color = Color(0xFFE2E8F0),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = Color(0xFFFACC15),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${game.durationSeconds}s",
                        color = Color(0xFFFACC15),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
