package com.example.engine

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.model.ArenaTheme
import com.example.model.MechanicGroup
import com.example.model.PlayerId
import kotlin.math.cos
import kotlin.math.sin

object ArenaRenderer {

    fun DrawScope.renderArena(
        engine: GameEngine,
        textMeasurer: TextMeasurer
    ) {
        val boardSize = minOf(size.width, size.height)
        val offsetX = (size.width - boardSize) * 0.5f
        val offsetY = (size.height - boardSize) * 0.5f
        val scale = boardSize / ARENA_SIZE

        fun toScreen(vx: Float, vy: Float): Offset =
            Offset(offsetX + vx * scale, offsetY + vy * scale)

        // 1. Theme-Specific Arena Floor
        val (bg1, bg2, borderCol) = getArenaColors(engine.game.arenaTheme)
        drawRoundRect(
            brush = Brush.radialGradient(
                colors = listOf(bg1, bg2),
                center = Offset(size.width * 0.5f, size.height * 0.5f),
                radius = boardSize * 0.65f
            ),
            topLeft = Offset(offsetX, offsetY),
            size = Size(boardSize, boardSize),
            cornerRadius = CornerRadius(20f, 20f)
        )

        // Subtle tactical floor lines
        val stepPx = boardSize / 8f
        for (i in 1..7) {
            drawLine(
                color = borderCol.copy(alpha = 0.12f),
                start = Offset(offsetX + i * stepPx, offsetY),
                end = Offset(offsetX + i * stepPx, offsetY + boardSize),
                strokeWidth = 1.5f
            )
            drawLine(
                color = borderCol.copy(alpha = 0.12f),
                start = Offset(offsetX, offsetY + i * stepPx),
                end = Offset(offsetX + boardSize, offsetY + i * stepPx),
                strokeWidth = 1.5f
            )
        }

        // Theme-specific court/track markings
        when (engine.game.mechanicGroup) {
            MechanicGroup.CIRCUIT_RACING -> {
                val pad = 135f * scale
                drawRoundRect(
                    color = Color(0xFF334155).copy(alpha = 0.65f),
                    topLeft = Offset(offsetX + pad, offsetY + pad),
                    size = Size(boardSize - pad * 2, boardSize - pad * 2),
                    cornerRadius = CornerRadius(140f * scale, 140f * scale),
                    style = Stroke(width = 110f * scale)
                )
            }
            MechanicGroup.BALL_SPORTS, MechanicGroup.PENALTY_DUEL, MechanicGroup.PADDLE_DEFENSE -> {
                // Center line, circle, and top/bottom goal nets
                drawLine(
                    color = Color.White.copy(alpha = 0.45f),
                    start = Offset(offsetX, offsetY + boardSize * 0.5f),
                    end = Offset(offsetX + boardSize, offsetY + boardSize * 0.5f),
                    strokeWidth = 3f
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.45f),
                    radius = 120f * scale,
                    center = toScreen(ARENA_CENTER, ARENA_CENTER),
                    style = Stroke(width = 3f)
                )
                // Top and Bottom Goal Mouths
                drawRect(
                    color = PlayerId.P2.color.copy(alpha = 0.45f),
                    topLeft = Offset(offsetX + boardSize * 0.30f, offsetY),
                    size = Size(boardSize * 0.40f, 34f * scale)
                )
                drawRect(
                    color = PlayerId.P1.color.copy(alpha = 0.45f),
                    topLeft = Offset(offsetX + boardSize * 0.30f, offsetY + boardSize - 34f * scale),
                    size = Size(boardSize * 0.40f, 34f * scale)
                )
            }
            MechanicGroup.COLLECT_AND_RETURN -> {
                // Draw corner bases for active players
                engine.players.forEach { p ->
                    val bp = toScreen(p.basePos.first, p.basePos.second)
                    drawCircle(
                        color = p.id.color.copy(alpha = 0.25f),
                        radius = 75f * scale,
                        center = bp
                    )
                    drawCircle(
                        color = p.id.color,
                        radius = 75f * scale,
                        center = bp,
                        style = Stroke(width = 3.5f)
                    )
                }
            }
            else -> {}
        }

        // 2. Grid Cells (for Territory, Puzzle, Maze, Hot Tile, Lava Floor)
        val gSize = engine.gridSize
        if (gSize > 0 && engine.grid.isNotEmpty()) {
            val cellPx = boardSize / gSize
            engine.grid.forEach { cell ->
                val cx = offsetX + cell.col * cellPx
                val cy = offsetY + cell.row * cellPx
                val tileColor = when {
                    cell.state == 3 -> Color(0xFF475569) // Solid Maze Wall
                    cell.heat > 1.0f -> Color(0xFFEF4444).copy(alpha = 0.78f)
                    cell.heat > 0.4f -> Color(0xFFF97316).copy(alpha = 0.55f)
                    cell.ownerId in 0..3 -> PlayerId.entries[cell.ownerId].color.copy(alpha = 0.55f)
                    cell.state == 1 -> Color(0xFF22C55E).copy(alpha = 0.40f)
                    else -> Color.White.copy(alpha = 0.06f)
                }
                drawRoundRect(
                    color = tileColor,
                    topLeft = Offset(cx + 3f, cy + 3f),
                    size = Size(cellPx - 6f, cellPx - 6f),
                    cornerRadius = CornerRadius(8f, 8f)
                )
            }
        }

        // 3. World Entities (Balls, Bombs, Crystals, Checkpoints, Safe Zones, Targets, Pads)
        engine.entities.forEach { e ->
            if (!e.active) return@forEach
            val pos = toScreen(e.x, e.y)
            val rad = e.radius * scale
            when (e.kind) {
                EntityKind.SAFE_ZONE_RING, EntityKind.CAPTURE_ZONE, EntityKind.CHECKPOINT_GATE -> {
                    drawCircle(
                        color = e.color.copy(alpha = 0.20f),
                        radius = rad,
                        center = pos
                    )
                    drawCircle(
                        color = e.color,
                        radius = rad,
                        center = pos,
                        style = Stroke(width = 4.5f)
                    )
                    if (e.label.isNotEmpty()) {
                        val layout = textMeasurer.measure(
                            text = e.label,
                            style = TextStyle(
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        )
                        drawText(
                            textLayoutResult = layout,
                            topLeft = Offset(pos.x - layout.size.width / 2f, pos.y - layout.size.height / 2f)
                        )
                    }
                }

                EntityKind.QUIZ_PAD -> {
                    drawRoundRect(
                        color = e.color.copy(alpha = 0.32f),
                        topLeft = Offset(pos.x - rad, pos.y - rad),
                        size = Size(rad * 2f, rad * 2f),
                        cornerRadius = CornerRadius(14f, 14f)
                    )
                    drawRoundRect(
                        color = e.color,
                        topLeft = Offset(pos.x - rad, pos.y - rad),
                        size = Size(rad * 2f, rad * 2f),
                        cornerRadius = CornerRadius(14f, 14f),
                        style = Stroke(width = 3.5f)
                    )
                    if (e.label.isNotEmpty()) {
                        val layout = textMeasurer.measure(
                            text = e.label,
                            style = TextStyle(
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        drawText(
                            textLayoutResult = layout,
                            topLeft = Offset(pos.x - layout.size.width / 2f, pos.y - layout.size.height / 2f)
                        )
                    }
                }

                EntityKind.BOMB, EntityKind.METEOR_WARNING -> {
                    // Pulsing danger blast radius
                    val warnRatio = (1f - (e.timer / e.maxTimer.coerceAtLeast(0.5f))).coerceIn(0.15f, 1f)
                    drawCircle(
                        color = Color(0xFFEF4444).copy(alpha = 0.22f * warnRatio),
                        radius = rad * 2.4f * warnRatio,
                        center = pos
                    )
                    drawCircle(
                        color = Color(0xFFEF4444),
                        radius = rad,
                        center = pos
                    )
                    drawCircle(
                        color = Color(0xFFFDE047),
                        radius = rad * 0.38f,
                        center = pos
                    )
                }

                EntityKind.HIGHWAY_CAR -> {
                    val wPx = e.width * scale
                    val hPx = e.height * scale
                    drawRoundRect(
                        color = e.color,
                        topLeft = Offset(pos.x - wPx * 0.5f, pos.y - hPx * 0.5f),
                        size = Size(wPx, hPx),
                        cornerRadius = CornerRadius(10f, 10f)
                    )
                }

                EntityKind.CRYSTAL, EntityKind.TREASURE_CHEST, EntityKind.FLAG -> {
                    val diamond = Path().apply {
                        moveTo(pos.x, pos.y - rad)
                        lineTo(pos.x + rad, pos.y)
                        lineTo(pos.x, pos.y + rad)
                        lineTo(pos.x - rad, pos.y)
                        close()
                    }
                    drawPath(diamond, color = e.color)
                    drawPath(diamond, color = Color.White, style = Stroke(width = 2f))
                }

                else -> {
                    drawCircle(color = Color.Black.copy(alpha = 0.35f), radius = rad, center = pos + Offset(2f, 4f))
                    drawCircle(color = e.color, radius = rad, center = pos)
                    drawCircle(color = Color.White, radius = rad, center = pos, style = Stroke(width = 2.5f))
                }
            }
        }

        // 4. Particles
        engine.particles.forEach { pt ->
            val pos = toScreen(pt.x, pt.y)
            drawCircle(
                color = pt.color.copy(alpha = pt.alpha),
                radius = pt.radius * scale,
                center = pos
            )
        }

        // 5. Players (with shadows, jump elevation, direction pointer, carrying ring, and shape badge)
        engine.players.forEach { p ->
            if (p.eliminated) return@forEach
            val groundPos = toScreen(p.x, p.y)
            val jumpOffsetPx = p.jumpZ * scale * 0.32f
            val bodyPos = Offset(groundPos.x, groundPos.y - jumpOffsetPx)
            val rad = (p.radius + if (p.jumpZ > 2f) 5f else 0f) * scale

            // Shadow on floor
            drawCircle(
                color = Color.Black.copy(alpha = 0.42f),
                radius = p.radius * scale * 0.92f,
                center = groundPos + Offset(3f, 5f)
            )

            // Cursed / Halo / Carrying aura
            if (p.isTaggedOrCursed) {
                drawCircle(
                    color = Color(0xFFFACC15),
                    radius = rad * 1.48f,
                    center = bodyPos,
                    style = Stroke(width = 4.5f)
                )
            }
            if (p.carryingItem > 0) {
                drawCircle(
                    color = Color(0xFF4ADE80),
                    radius = rad * 1.35f,
                    center = bodyPos,
                    style = Stroke(width = 3.5f)
                )
            }

            // Main colored player disc (Red / Blue / Yellow / Green)
            drawCircle(color = p.id.color, radius = rad, center = bodyPos)
            drawCircle(color = Color.White, radius = rad, center = bodyPos, style = Stroke(width = 3f))

            // Facing direction indicator nose
            val noseX = bodyPos.x + cos(p.angleRad) * rad * 0.78f
            val noseY = bodyPos.y + sin(p.angleRad) * rad * 0.78f
            drawCircle(color = Color.White, radius = rad * 0.26f, center = Offset(noseX, noseY))

            // Player label (P1, P2, P3, P4 or Morph Shape in Shape Chase)
            val badgeText = if (engine.game.mechanicGroup == MechanicGroup.SHAPE_MORPH_CHASE) {
                when (p.morphShape) {
                    MorphShape.CIRCLE -> "●"
                    MorphShape.TRIANGLE -> "▲"
                    MorphShape.SQUARE -> "■"
                }
            } else {
                "P${p.id.number}"
            }
            val layout = textMeasurer.measure(
                text = badgeText,
                style = TextStyle(
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black
                )
            )
            drawText(
                textLayoutResult = layout,
                topLeft = Offset(bodyPos.x - layout.size.width / 2f, bodyPos.y - layout.size.height / 2f)
            )
        }

        // 6. Crisp Arena Border Frame
        drawRoundRect(
            color = borderCol.copy(alpha = 0.75f),
            topLeft = Offset(offsetX, offsetY),
            size = Size(boardSize, boardSize),
            cornerRadius = CornerRadius(20f, 20f),
            style = Stroke(width = 4f)
        )
    }

    private fun getArenaColors(theme: ArenaTheme): Triple<Color, Color, Color> = when (theme) {
        ArenaTheme.SOCCER_PITCH, ArenaTheme.GOLF_GREEN ->
            Triple(Color(0xFF15803D), Color(0xFF064E3B), Color(0xFF4ADE80))
        ArenaTheme.BASKETBALL_COURT, ArenaTheme.BOWLING_LANE ->
            Triple(Color(0xFF7C2D12), Color(0xFF431407), Color(0xFFFB923C))
        ArenaTheme.ICE_RINK ->
            Triple(Color(0xFF0369A1), Color(0xFF0C4A6E), Color(0xFF7DD3FC))
        ArenaTheme.DESERT_CANYON, ArenaTheme.TREASURE_TEMPLE ->
            Triple(Color(0xFF78350F), Color(0xFF292524), Color(0xFFFBBF24))
        ArenaTheme.LAVA_VOLCANO, ArenaTheme.BOMB_FACTORY ->
            Triple(Color(0xFF450A0A), Color(0xFF18181B), Color(0xFFEF4444))
        ArenaTheme.CRYSTAL_CAVERN, ArenaTheme.PUZZLE_MATRIX ->
            Triple(Color(0xFF311042), Color(0xFF0F172A), Color(0xFFE879F9))
        ArenaTheme.ASPHALT_TRACK, ArenaTheme.HIGHWAY_TRAFFIC, ArenaTheme.OVAL_SPEEDWAY ->
            Triple(Color(0xFF27272A), Color(0xFF18181B), Color(0xFFFACC15))
        else ->
            Triple(Color(0xFF1E293B), Color(0xFF0F172A), Color(0xFF38BDF8))
    }
}
