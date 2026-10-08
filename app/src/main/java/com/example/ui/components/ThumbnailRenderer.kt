package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ArenaTheme
import com.example.model.GameSpec
import com.example.model.MechanicGroup
import com.example.model.PlayerId
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun GameThumbnailView(
    game: GameSpec,
    modifier: Modifier = Modifier,
    showBadge: Boolean = true
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF0F172A))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawUniqueGameThumbnail(game)
        }
        if (showBadge) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .background(
                        color = Color(0xCC0F172A),
                        shape = RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "#${game.id.toString().padStart(2, '0')}",
                    color = game.category.accentColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

private fun DrawScope.drawUniqueGameThumbnail(game: GameSpec) {
    val w = size.width
    val h = size.height
    val cx = w * 0.5f
    val cy = h * 0.5f
    val minDim = minOf(w, h)
    val seed = game.id * 37 + game.variantIndex * 13

    // 1. Distinct Theme Background Palette
    val (bgTop, bgBottom, accent) = when (game.arenaTheme) {
        ArenaTheme.CYBER_ARENA -> Triple(Color(0xFF0F172A), Color(0xFF1E1B4B), Color(0xFF38BDF8))
        ArenaTheme.CRYSTAL_CAVERN -> Triple(Color(0xFF1E1B4B), Color(0xFF311042), Color(0xFFE879F9))
        ArenaTheme.BOMB_FACTORY -> Triple(Color(0xFF27171A), Color(0xFF18181B), Color(0xFFF97316))
        ArenaTheme.TARGET_RANGE -> Triple(Color(0xFF0B2528), Color(0xFF0F172A), Color(0xFF2DD4BF))
        ArenaTheme.STORM_ZONE -> Triple(Color(0xFF172554), Color(0xFF090D16), Color(0xFF60A5FA))
        ArenaTheme.METEOR_CRATER -> Triple(Color(0xFF2A1215), Color(0xFF111827), Color(0xFFFB7185))
        ArenaTheme.LASER_GRID -> Triple(Color(0xFF090D16), Color(0xFF2E1065), Color(0xFFF43F5E))
        ArenaTheme.SHADOW_DUNGEON -> Triple(Color(0xFF09090B), Color(0xFF1F2937), Color(0xFFA855F7))
        ArenaTheme.TREASURE_TEMPLE -> Triple(Color(0xFF2E1F0F), Color(0xFF1C1917), Color(0xFFFACC15))
        ArenaTheme.CASTLE_FORTRESS -> Triple(Color(0xFF1E293B), Color(0xFF0F172A), Color(0xFF94A3B8))
        ArenaTheme.ASPHALT_TRACK -> Triple(Color(0xFF18181B), Color(0xFF27272A), Color(0xFFF59E0B))
        ArenaTheme.OVAL_SPEEDWAY -> Triple(Color(0xFF1E293B), Color(0xFF0F172A), Color(0xFF38BDF8))
        ArenaTheme.HIGHWAY_TRAFFIC -> Triple(Color(0xFF111827), Color(0xFF1F2937), Color(0xFFFBBF24))
        ArenaTheme.TURBO_STRIP -> Triple(Color(0xFF2E1065), Color(0xFF0F172A), Color(0xFF22D3EE))
        ArenaTheme.ICE_RINK -> Triple(Color(0xFF0C4A6E), Color(0xFF082F49), Color(0xFF7DD3FC))
        ArenaTheme.DESERT_CANYON -> Triple(Color(0xFF451A03), Color(0xFF292524), Color(0xFFFB923C))
        ArenaTheme.NEON_BRIDGE -> Triple(Color(0xFF090D16), Color(0xFF1E1B4B), Color(0xFF06B6D4))
        ArenaTheme.CITY_CORNERS -> Triple(Color(0xFF1E293B), Color(0xFF111827), Color(0xFFA3E635))
        ArenaTheme.WATER_BRIDGES -> Triple(Color(0xFF0369A1), Color(0xFF0C4A6E), Color(0xFF38BDF8))
        ArenaTheme.CHECKPOINT_CITY -> Triple(Color(0xFF1F2937), Color(0xFF111827), Color(0xFFFDE047))
        ArenaTheme.SOCCER_PITCH -> Triple(Color(0xFF14532D), Color(0xFF064E3B), Color(0xFF4ADE80))
        ArenaTheme.BASKETBALL_COURT -> Triple(Color(0xFF7C2D12), Color(0xFF431407), Color(0xFFFB923C))
        ArenaTheme.AIR_HOCKEY_TABLE -> Triple(Color(0xFF0F172A), Color(0xFF1E3A8A), Color(0xFF60A5FA))
        ArenaTheme.TENNIS_COURT -> Triple(Color(0xFF1E3A8A), Color(0xFF14532D), Color(0xFFA3E635))
        ArenaTheme.PING_PONG_TABLE -> Triple(Color(0xFF1E40AF), Color(0xFF172554), Color(0xFFFFFFFF))
        ArenaTheme.BOWLING_LANE -> Triple(Color(0xFF451A03), Color(0xFF1C1917), Color(0xFFFBBF24))
        ArenaTheme.PUZZLE_MATRIX -> Triple(Color(0xFF1E1B4B), Color(0xFF0F172A), Color(0xFFC084FC))
        ArenaTheme.MAZE_LABYRINTH -> Triple(Color(0xFF064E3B), Color(0xFF0F172A), Color(0xFF34D399))
        ArenaTheme.LAVA_VOLCANO -> Triple(Color(0xFF450A0A), Color(0xFF18181B), Color(0xFFEF4444))
        ArenaTheme.MAGNET_LAB -> Triple(Color(0xFF1E1B4B), Color(0xFF0F172A), Color(0xFFF43F5E))
        ArenaTheme.GOLF_GREEN -> Triple(Color(0xFF15803D), Color(0xFF14532D), Color(0xFF86EFAC))
        ArenaTheme.CARNIVAL_RING -> Triple(Color(0xFF4C0519), Color(0xFF1E1B4B), Color(0xFFFBBF24))
        ArenaTheme.MIRROR_HALL -> Triple(Color(0xFF0F172A), Color(0xFF164E63), Color(0xFF67E8F9))
    }

    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(bgTop, bgBottom),
            start = Offset.Zero,
            end = Offset(w, h)
        )
    )

    // Subtle geometric grid with unique spacing per game
    val gridSteps = 4 + (game.id % 4)
    val stepX = w / gridSteps
    val stepY = h / gridSteps
    for (i in 1 until gridSteps) {
        drawLine(
            color = accent.copy(alpha = 0.10f),
            start = Offset(i * stepX, 0f),
            end = Offset(i * stepX, h),
            strokeWidth = 1.5f
        )
        drawLine(
            color = accent.copy(alpha = 0.10f),
            start = Offset(0f, i * stepY),
            end = Offset(w, i * stepY),
            strokeWidth = 1.5f
        )
    }

    // 2. Mechanic-Specific Centerpiece Illustration
    when (game.mechanicGroup) {
        MechanicGroup.BRAWL_KNOCKBACK -> {
            val ringR = minDim * (0.34f + (game.variantIndex % 3) * 0.03f)
            drawCircle(
                color = accent.copy(alpha = 0.22f),
                radius = ringR,
                center = Offset(cx, cy)
            )
            drawCircle(
                color = accent,
                radius = ringR,
                center = Offset(cx, cy),
                style = Stroke(width = 4f)
            )
            // Shockwave lines
            for (k in 0 until 6) {
                val ang = (k * 60 + seed) * PI / 180.0
                drawLine(
                    color = Color.White.copy(alpha = 0.5f),
                    start = Offset(cx + cos(ang).toFloat() * ringR * 0.3f, cy + sin(ang).toFloat() * ringR * 0.3f),
                    end = Offset(cx + cos(ang).toFloat() * ringR * 0.7f, cy + sin(ang).toFloat() * ringR * 0.7f),
                    strokeWidth = 3f
                )
            }
        }

        MechanicGroup.COLLECT_AND_RETURN -> {
            // Corner bases + central crystals/chests/flags
            val corners = listOf(
                Offset(w * 0.18f, h * 0.22f) to PlayerId.P1.color,
                Offset(w * 0.82f, h * 0.22f) to PlayerId.P2.color,
                Offset(w * 0.18f, h * 0.78f) to PlayerId.P3.color,
                Offset(w * 0.82f, h * 0.78f) to PlayerId.P4.color
            )
            corners.forEach { (pos, col) ->
                drawRoundRect(
                    color = col.copy(alpha = 0.35f),
                    topLeft = Offset(pos.x - 14f, pos.y - 14f),
                    size = Size(28f, 28f),
                    cornerRadius = CornerRadius(6f, 6f)
                )
            }
            // Central diamond/chest
            val diamond = Path().apply {
                moveTo(cx, cy - minDim * 0.18f)
                lineTo(cx + minDim * 0.16f, cy)
                lineTo(cx, cy + minDim * 0.18f)
                lineTo(cx - minDim * 0.16f, cy)
                close()
            }
            drawPath(diamond, color = Color(0xFFFACC15))
            drawPath(diamond, color = Color.White, style = Stroke(width = 2.5f))
        }

        MechanicGroup.BOMB_DODGE_KICK -> {
            // Blast danger rings + ticking bomb
            drawCircle(
                color = Color(0xFFEF4444).copy(alpha = 0.25f),
                radius = minDim * 0.34f,
                center = Offset(cx, cy)
            )
            drawCircle(
                color = Color(0xFF18181B),
                radius = minDim * 0.17f,
                center = Offset(cx, cy)
            )
            drawCircle(
                color = Color(0xFFF97316),
                radius = minDim * 0.06f,
                center = Offset(cx + minDim * 0.12f, cy - minDim * 0.14f)
            )
        }

        MechanicGroup.SHOOT_TARGETS -> {
            // Concentric bullseye + laser projectiles
            val radii = listOf(0.30f, 0.21f, 0.12f, 0.05f)
            radii.forEachIndexed { idx, r ->
                drawCircle(
                    color = if (idx % 2 == 0) accent else Color.White,
                    radius = minDim * r,
                    center = Offset(cx, cy),
                    style = if (idx < 3) Stroke(width = 3.5f) else androidx.compose.ui.graphics.drawscope.Fill
                )
            }
        }

        MechanicGroup.SAFE_ZONE_SURVIVAL -> {
            val safeX = cx + ((game.id % 3) - 1) * w * 0.12f
            val safeY = cy + (((game.id / 2) % 3) - 1) * h * 0.10f
            drawCircle(
                color = Color(0xFF22C55E).copy(alpha = 0.30f),
                radius = minDim * 0.28f,
                center = Offset(safeX, safeY)
            )
            drawCircle(
                color = Color(0xFF4ADE80),
                radius = minDim * 0.28f,
                center = Offset(safeX, safeY),
                style = Stroke(width = 4f)
            )
        }

        MechanicGroup.HAZARD_DODGE -> {
            // Rotating laser / meteor trails
            rotate(degrees = (game.id * 25f) % 360f, pivot = Offset(cx, cy)) {
                drawLine(
                    color = Color(0xFFF43F5E),
                    start = Offset(cx - w * 0.42f, cy),
                    end = Offset(cx + w * 0.42f, cy),
                    strokeWidth = 6f
                )
                drawLine(
                    color = Color(0xFFF43F5E),
                    start = Offset(cx, cy - h * 0.42f),
                    end = Offset(cx, cy + h * 0.42f),
                    strokeWidth = 6f
                )
            }
            drawCircle(color = Color.White, radius = minDim * 0.08f, center = Offset(cx, cy))
        }

        MechanicGroup.TAG_PASS_CURSE -> {
            // Golden/Shadow Crown Halo
            drawCircle(
                color = Color(0xFFFACC15),
                radius = minDim * 0.24f,
                center = Offset(cx, cy),
                style = Stroke(width = 7f)
            )
            drawCircle(
                color = accent.copy(alpha = 0.35f),
                radius = minDim * 0.15f,
                center = Offset(cx, cy)
            )
        }

        MechanicGroup.CASTLE_SIEGE -> {
            // Turret bastions
            val fortSize = minDim * 0.28f
            drawRoundRect(
                color = accent.copy(alpha = 0.3f),
                topLeft = Offset(cx - fortSize / 2, cy - fortSize / 2),
                size = Size(fortSize, fortSize),
                cornerRadius = CornerRadius(8f, 8f)
            )
            drawRoundRect(
                color = accent,
                topLeft = Offset(cx - fortSize / 2, cy - fortSize / 2),
                size = Size(fortSize, fortSize),
                cornerRadius = CornerRadius(8f, 8f),
                style = Stroke(width = 4f)
            )
        }

        MechanicGroup.CIRCUIT_RACING -> {
            // Oval / Circuit Road
            val padX = w * 0.16f
            val padY = h * 0.20f
            drawRoundRect(
                color = Color(0xFF334155),
                topLeft = Offset(padX, padY),
                size = Size(w - padX * 2, h - padY * 2),
                cornerRadius = CornerRadius(minDim * 0.25f, minDim * 0.25f),
                style = Stroke(width = minDim * 0.16f)
            )
            drawRoundRect(
                color = accent,
                topLeft = Offset(padX, padY),
                size = Size(w - padX * 2, h - padY * 2),
                cornerRadius = CornerRadius(minDim * 0.25f, minDim * 0.25f),
                style = Stroke(width = 2.5f)
            )
        }

        MechanicGroup.HIGHWAY_DODGE -> {
            // 4 Highway Lanes
            for (lane in 1..3) {
                val lx = w * (lane / 4f)
                drawLine(
                    color = Color.White.copy(alpha = 0.45f),
                    start = Offset(lx, 0f),
                    end = Offset(lx, h),
                    strokeWidth = 3f
                )
            }
        }

        MechanicGroup.BRIDGE_BUILDER -> {
            // River across middle + 4 colored bridges
            drawRect(
                color = Color(0xFF0284C7).copy(alpha = 0.65f),
                topLeft = Offset(0f, h * 0.36f),
                size = Size(w, h * 0.28f)
            )
            PlayerId.entries.forEachIndexed { idx, pid ->
                val bx = w * (0.18f + idx * 0.21f)
                drawRoundRect(
                    color = pid.color,
                    topLeft = Offset(bx - 8f, h * 0.32f),
                    size = Size(16f, h * 0.36f),
                    cornerRadius = CornerRadius(4f, 4f)
                )
            }
        }

        MechanicGroup.CHECKPOINT_RUSH -> {
            for (i in 0 until 3) {
                val gx = w * (0.25f + i * 0.25f)
                val gy = h * (0.30f + ((i + game.id) % 2) * 0.35f)
                drawCircle(
                    color = Color(0xFFFACC15),
                    radius = minDim * 0.11f,
                    center = Offset(gx, gy),
                    style = Stroke(width = 4f)
                )
            }
        }

        MechanicGroup.BALL_SPORTS, MechanicGroup.PENALTY_DUEL -> {
            // Pitch lines + center circle + soccer/hockey ball
            drawCircle(
                color = Color.White.copy(alpha = 0.45f),
                radius = minDim * 0.22f,
                center = Offset(cx, cy),
                style = Stroke(width = 3f)
            )
            drawLine(
                color = Color.White.copy(alpha = 0.45f),
                start = Offset(0f, cy),
                end = Offset(w, cy),
                strokeWidth = 3f
            )
            drawCircle(color = Color.White, radius = minDim * 0.08f, center = Offset(cx, cy))
        }

        MechanicGroup.BASKET_SHOOT -> {
            // Basketball key & orange hoop
            drawCircle(
                color = Color(0xFFF97316),
                radius = minDim * 0.14f,
                center = Offset(cx, h * 0.28f),
                style = Stroke(width = 5f)
            )
            drawCircle(
                color = Color(0xFFEA580C),
                radius = minDim * 0.09f,
                center = Offset(cx, h * 0.62f)
            )
        }

        MechanicGroup.PADDLE_DEFENSE -> {
            // Table divider + paddles + puck
            drawLine(
                color = accent.copy(alpha = 0.6f),
                start = Offset(w * 0.1f, cy),
                end = Offset(w * 0.9f, cy),
                strokeWidth = 4f
            )
            drawCircle(color = Color.White, radius = minDim * 0.07f, center = Offset(cx, cy))
        }

        MechanicGroup.BOWLING_ROLL -> {
            // Pin triangle formation
            val pinPositions = listOf(
                Offset(cx, h * 0.34f),
                Offset(cx - 14f, h * 0.25f), Offset(cx + 14f, h * 0.25f),
                Offset(cx - 28f, h * 0.17f), Offset(cx, h * 0.17f), Offset(cx + 28f, h * 0.17f)
            )
            pinPositions.forEach { pos ->
                drawCircle(color = Color.White, radius = 6f, center = pos)
                drawCircle(color = Color(0xFFEF4444), radius = 3f, center = pos)
            }
        }

        MechanicGroup.TILE_PUZZLE, MechanicGroup.MATH_PATTERN_QUIZ, MechanicGroup.TERRITORY_PAINT -> {
            // 3x3 Colorful Matrix Grid
            val cell = minDim * 0.17f
            val startX = cx - cell * 1.6f
            val startY = cy - cell * 1.6f
            val palette = listOf(PlayerId.P1.color, PlayerId.P2.color, PlayerId.P3.color, PlayerId.P4.color, accent)
            for (r in 0..2) {
                for (c in 0..2) {
                    val col = palette[(r * 3 + c + game.id) % palette.size]
                    drawRoundRect(
                        color = col.copy(alpha = 0.65f),
                        topLeft = Offset(startX + c * cell * 1.1f, startY + r * cell * 1.1f),
                        size = Size(cell, cell),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                }
            }
        }

        MechanicGroup.MAZE_RUNNER -> {
            // Concentric labyrinth corridors
            for (layer in 1..3) {
                val s = minDim * (0.20f * layer)
                drawRect(
                    color = accent.copy(alpha = 0.7f),
                    topLeft = Offset(cx - s, cy - s),
                    size = Size(s * 2, s * 2),
                    style = Stroke(width = 3.5f)
                )
            }
        }

        MechanicGroup.ZONE_CAPTURE -> {
            // 3 Capture Beacons
            val pts = listOf(
                Offset(cx, h * 0.28f),
                Offset(w * 0.30f, h * 0.68f),
                Offset(w * 0.70f, h * 0.68f)
            )
            pts.forEachIndexed { idx, p ->
                val c = PlayerId.entries[idx].color
                drawCircle(color = c.copy(alpha = 0.35f), radius = minDim * 0.14f, center = p)
                drawCircle(color = c, radius = minDim * 0.14f, center = p, style = Stroke(width = 3f))
            }
        }

        MechanicGroup.WALL_TRAIL_TRAP -> {
            // Light-cycle right-angle trails
            PlayerId.entries.forEachIndexed { i, pid ->
                val y = h * (0.25f + i * 0.16f)
                drawLine(
                    color = pid.color,
                    start = Offset(w * 0.15f, y),
                    end = Offset(w * (0.55f + (i % 2) * 0.25f), y),
                    strokeWidth = 5f
                )
            }
        }

        MechanicGroup.REACTION_TAP -> {
            // Lightning / Pulse rings
            drawCircle(
                color = accent.copy(alpha = 0.25f),
                radius = minDim * 0.32f,
                center = Offset(cx, cy)
            )
            drawCircle(
                color = Color(0xFF4ADE80),
                radius = minDim * 0.16f,
                center = Offset(cx, cy)
            )
        }

        MechanicGroup.GOLF_PUTT -> {
            // Putting green + flagstick
            drawCircle(
                color = Color(0xFF22C55E).copy(alpha = 0.35f),
                radius = minDim * 0.28f,
                center = Offset(cx, cy)
            )
            drawCircle(color = Color(0xFF090D16), radius = 7f, center = Offset(cx, cy))
            drawLine(
                color = Color.White,
                start = Offset(cx, cy),
                end = Offset(cx, cy - minDim * 0.24f),
                strokeWidth = 3f
            )
            val flag = Path().apply {
                moveTo(cx, cy - minDim * 0.24f)
                lineTo(cx + minDim * 0.16f, cy - minDim * 0.18f)
                lineTo(cx, cy - minDim * 0.12f)
                close()
            }
            drawPath(flag, color = Color(0xFFEF4444))
        }

        MechanicGroup.DISC_DODGEBALL -> {
            // Ricocheting neon discs
            for (i in 0..2) {
                val dx = w * (0.30f + i * 0.20f)
                val dy = h * (0.35f + (i % 2) * 0.25f)
                drawCircle(color = accent, radius = minDim * 0.09f, center = Offset(dx, dy), style = Stroke(width = 4f))
            }
        }

        MechanicGroup.SHAPE_MORPH_CHASE -> {
            // Circle, Triangle, Square morph emblems
            drawCircle(color = PlayerId.P1.color, radius = minDim * 0.11f, center = Offset(w * 0.30f, cy))
            drawRect(
                color = PlayerId.P2.color,
                topLeft = Offset(cx - minDim * 0.10f, cy - minDim * 0.10f),
                size = Size(minDim * 0.20f, minDim * 0.20f)
            )
            val tri = Path().apply {
                moveTo(w * 0.72f, cy - minDim * 0.11f)
                lineTo(w * 0.82f, cy + minDim * 0.10f)
                lineTo(w * 0.62f, cy + minDim * 0.10f)
                close()
            }
            drawPath(tri, color = PlayerId.P3.color)
        }
    }

    // 3. Draw the supported Player Avatars in their signature colors (Red, Blue, Yellow, Green)
    val maxP = game.maxPlayers
    val playerSpots = listOf(
        Offset(w * 0.22f, h * 0.74f),
        Offset(w * 0.78f, h * 0.26f),
        Offset(w * 0.22f, h * 0.26f),
        Offset(w * 0.78f, h * 0.74f)
    )
    for (i in 0 until maxP) {
        val pid = PlayerId.entries[i]
        val basePos = playerSpots[i]
        val jitterX = (((game.id + i * 7) % 5) - 2) * 3.5f
        val jitterY = (((game.id + i * 11) % 5) - 2) * 3.5f
        val pos = Offset(basePos.x + jitterX, basePos.y + jitterY)
        drawCircle(color = Color.Black.copy(alpha = 0.45f), radius = minDim * 0.085f, center = pos + Offset(2f, 3f))
        drawCircle(color = pid.color, radius = minDim * 0.08f, center = pos)
        drawCircle(color = Color.White, radius = minDim * 0.08f, center = pos, style = Stroke(width = 2f))
    }
}
