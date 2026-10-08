package com.example.engine

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.model.ArenaTheme
import com.example.model.ControlScheme
import com.example.model.MechanicGroup
import com.example.model.PlayerId
import kotlin.math.PI
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
                color = borderCol.copy(alpha = 0.11f),
                start = Offset(offsetX + i * stepPx, offsetY),
                end = Offset(offsetX + i * stepPx, offsetY + boardSize),
                strokeWidth = 1.5f
            )
            drawLine(
                color = borderCol.copy(alpha = 0.11f),
                start = Offset(offsetX, offsetY + i * stepPx),
                end = Offset(offsetX + boardSize, offsetY + i * stepPx),
                strokeWidth = 1.5f
            )
        }

        // 2. Genre-Specific Environment & Track/Court/Platform Markings
        when (engine.game.mechanicGroup) {
            MechanicGroup.BRAWL_KNOCKBACK -> {
                // Dark abyss pit around raised platform ring
                val ring = engine.entities.firstOrNull { it.kind == EntityKind.SAFE_ZONE_RING }
                val ringR = (ring?.radius ?: 365f) * scale
                val center = toScreen(ARENA_CENTER, ARENA_CENTER)
                drawRoundRect(
                    color = Color(0xFF05070B),
                    topLeft = Offset(offsetX, offsetY),
                    size = Size(boardSize, boardSize),
                    cornerRadius = CornerRadius(20f, 20f)
                )
                // Raised Arena Platform Shadow & Surface
                drawCircle(
                    color = Color.Black.copy(alpha = 0.65f),
                    radius = ringR + 10f * scale,
                    center = center + Offset(0f, 8f * scale)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(bg1, bg2),
                        center = center,
                        radius = ringR
                    ),
                    radius = ringR,
                    center = center
                )
                // Platform Edge Caution Ring
                drawCircle(
                    color = Color(0xFFFACC15),
                    radius = ringR,
                    center = center,
                    style = Stroke(width = 6f * scale)
                )
                drawCircle(
                    color = borderCol.copy(alpha = 0.45f),
                    radius = ringR * 0.65f,
                    center = center,
                    style = Stroke(width = 2.5f * scale)
                )
            }

            MechanicGroup.CIRCUIT_RACING -> {
                // Outer Track + Inner Island Barrier + Checkered Finish Line
                val trackOuterPad = 85f * scale
                val islandPad = 325f * scale
                drawRoundRect(
                    color = Color(0xFF1F2937),
                    topLeft = Offset(offsetX + trackOuterPad, offsetY + trackOuterPad),
                    size = Size(boardSize - trackOuterPad * 2, boardSize - trackOuterPad * 2),
                    cornerRadius = CornerRadius(90f * scale, 90f * scale)
                )
                drawRoundRect(
                    color = Color(0xFFEF4444),
                    topLeft = Offset(offsetX + trackOuterPad, offsetY + trackOuterPad),
                    size = Size(boardSize - trackOuterPad * 2, boardSize - trackOuterPad * 2),
                    cornerRadius = CornerRadius(90f * scale, 90f * scale),
                    style = Stroke(width = 5f * scale)
                )
                // Inner Island Barrier (cars collide with this!)
                drawRoundRect(
                    color = Color(0xFF064E3B),
                    topLeft = Offset(offsetX + islandPad, offsetY + islandPad),
                    size = Size(boardSize - islandPad * 2, boardSize - islandPad * 2),
                    cornerRadius = CornerRadius(48f * scale, 48f * scale)
                )
                drawRoundRect(
                    color = Color(0xFFFACC15),
                    topLeft = Offset(offsetX + islandPad, offsetY + islandPad),
                    size = Size(boardSize - islandPad * 2, boardSize - islandPad * 2),
                    cornerRadius = CornerRadius(48f * scale, 48f * scale),
                    style = Stroke(width = 5f * scale)
                )
                // Checkered Finish Line on Bottom-Left Straight
                val finStart = toScreen(160f, 800f)
                for (sq in 0 until 6) {
                    drawRect(
                        color = if (sq % 2 == 0) Color.White else Color.Black,
                        topLeft = Offset(finStart.x + sq * 18f * scale, finStart.y - 8f * scale),
                        size = Size(18f * scale, 16f * scale)
                    )
                }
            }

            MechanicGroup.HIGHWAY_DODGE -> {
                for (lane in 1..3) {
                    val lx = offsetX + boardSize * (lane / 4f)
                    for (dash in 0..9) {
                        drawLine(
                            color = Color(0xFFFACC15).copy(alpha = 0.6f),
                            start = Offset(lx, offsetY + dash * (boardSize / 10f) + 10f),
                            end = Offset(lx, offsetY + dash * (boardSize / 10f) + 42f),
                            strokeWidth = 3.5f
                        )
                    }
                }
            }

            MechanicGroup.BALL_SPORTS, MechanicGroup.PENALTY_DUEL, MechanicGroup.PADDLE_DEFENSE -> {
                // Pitch markings + Penalty boxes + Goal Posts
                drawLine(
                    color = Color.White.copy(alpha = 0.55f),
                    start = Offset(offsetX + 60f * scale, offsetY + boardSize * 0.5f),
                    end = Offset(offsetX + boardSize - 60f * scale, offsetY + boardSize * 0.5f),
                    strokeWidth = 3.5f
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.55f),
                    radius = 120f * scale,
                    center = toScreen(ARENA_CENTER, ARENA_CENTER),
                    style = Stroke(width = 3.5f)
                )
                // Top Goal Net (Player 2 Defends)
                val goalLeft = offsetX + boardSize * 0.28f
                val goalWidth = boardSize * 0.44f
                val goalDepth = 42f * scale
                drawRoundRect(
                    color = PlayerId.P2.color.copy(alpha = 0.35f),
                    topLeft = Offset(goalLeft, offsetY + 45f * scale),
                    size = Size(goalWidth, goalDepth),
                    cornerRadius = CornerRadius(6f, 6f)
                )
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(goalLeft, offsetY + 45f * scale),
                    size = Size(goalWidth, goalDepth),
                    cornerRadius = CornerRadius(6f, 6f),
                    style = Stroke(width = 4f)
                )
                // Bottom Goal Net (Player 1 Defends)
                drawRoundRect(
                    color = PlayerId.P1.color.copy(alpha = 0.35f),
                    topLeft = Offset(goalLeft, offsetY + boardSize - 87f * scale),
                    size = Size(goalWidth, goalDepth),
                    cornerRadius = CornerRadius(6f, 6f)
                )
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(goalLeft, offsetY + boardSize - 87f * scale),
                    size = Size(goalWidth, goalDepth),
                    cornerRadius = CornerRadius(6f, 6f),
                    style = Stroke(width = 4f)
                )
            }

            MechanicGroup.COLLECT_AND_RETURN -> {
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

        // 3. Grid Cells (for Territory, Puzzle, Maze, Hot Tile, Lava Floor)
        val gSize = engine.gridSize
        if (gSize > 0 && engine.grid.isNotEmpty()) {
            val cellPx = boardSize / gSize
            engine.grid.forEach { cell ->
                val cx = offsetX + cell.col * cellPx
                val cy = offsetY + cell.row * cellPx
                val tileColor = when {
                    cell.state == 3 -> Color(0xFF475569)
                    cell.heat > 1.0f -> Color(0xFFEF4444).copy(alpha = 0.80f)
                    cell.heat > 0.4f -> Color(0xFFF97316).copy(alpha = 0.58f)
                    cell.ownerId in 0..3 -> PlayerId.entries[cell.ownerId].color.copy(alpha = 0.58f)
                    cell.state == 1 -> Color(0xFF22C55E).copy(alpha = 0.42f)
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

        // 4. World Entities
        engine.entities.forEach { e ->
            if (!e.active) return@forEach
            val pos = toScreen(e.x, e.y)
            val rad = e.radius * scale
            when (e.kind) {
                EntityKind.SAFE_ZONE_RING, EntityKind.CAPTURE_ZONE, EntityKind.CHECKPOINT_GATE -> {
                    if (engine.game.mechanicGroup != MechanicGroup.BRAWL_KNOCKBACK || e.kind != EntityKind.SAFE_ZONE_RING) {
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
                    val warnRatio = (1f - (e.timer / e.maxTimer.coerceAtLeast(0.5f))).coerceIn(0.15f, 1f)
                    drawCircle(
                        color = Color(0xFFEF4444).copy(alpha = 0.24f * warnRatio),
                        radius = rad * 2.4f * warnRatio,
                        center = pos
                    )
                    drawCircle(color = Color(0xFF18181B), radius = rad, center = pos)
                    drawCircle(color = Color(0xFFEF4444), radius = rad, center = pos, style = Stroke(width = 3f))
                    drawCircle(color = Color(0xFFFDE047), radius = rad * 0.36f, center = pos)
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
                    drawRoundRect(
                        color = Color(0xFF0F172A),
                        topLeft = Offset(pos.x - wPx * 0.36f, pos.y - hPx * 0.22f),
                        size = Size(wPx * 0.72f, hPx * 0.38f),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                }

                EntityKind.BALL_SOCCER, EntityKind.BALL_BASKET, EntityKind.PUCK_HOCKEY, EntityKind.BALL_TENNIS -> {
                    drawCircle(color = Color.Black.copy(alpha = 0.38f), radius = rad, center = pos + Offset(3f, 5f))
                    drawCircle(color = e.color, radius = rad, center = pos)
                    drawCircle(color = Color(0xFF0F172A), radius = rad * 0.42f, center = pos)
                    drawCircle(color = Color.White, radius = rad, center = pos, style = Stroke(width = 2.5f))
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

        // 5. Particles
        engine.particles.forEach { pt ->
            val pos = toScreen(pt.x, pt.y)
            drawCircle(
                color = pt.color.copy(alpha = pt.alpha),
                radius = pt.radius * scale,
                center = pos
            )
        }

        // 6. Articulated Genre-Specific Player Sprites (Race Cars, Tanks, Brawlers, Athletes)
        val scheme = engine.game.effectiveControlScheme
        engine.players.forEach { p ->
            if (p.eliminated) return@forEach
            val groundPos = toScreen(p.x, p.y)
            val jumpOffsetPx = p.jumpZ * scale * 0.34f
            val fallScale = if (p.fallingTimer > 0f) (p.fallingTimer / 0.55f).coerceIn(0.2f, 1f) else 1f
            val bodyPos = Offset(groundPos.x, groundPos.y - jumpOffsetPx)
            val rad = (p.radius + if (p.jumpZ > 2f) 6f else 0f) * scale * fallScale

            // Ground shadow
            drawCircle(
                color = Color.Black.copy(alpha = 0.45f * fallScale),
                radius = p.radius * scale * 0.94f * fallScale,
                center = groundPos + Offset(3f, 6f)
            )

            // Cursed / Halo / Carrying / Dodge aura
            if (p.isTaggedOrCursed) {
                drawCircle(
                    color = Color(0xFFFACC15),
                    radius = rad * 1.50f,
                    center = bodyPos,
                    style = Stroke(width = 4.5f)
                )
            }
            if (p.carryingItem > 0) {
                drawCircle(
                    color = Color(0xFF4ADE80),
                    radius = rad * 1.38f,
                    center = bodyPos,
                    style = Stroke(width = 3.5f)
                )
            }
            if (p.dodgeTimer > 0f) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.75f),
                    radius = rad * 1.32f,
                    center = bodyPos,
                    style = Stroke(width = 3f)
                )
            }

            when (scheme) {
                ControlScheme.RACING_CONTROLS -> {
                    // Top-Down Race Car Sprite with 4 Tires, Chassis, Cockpit & Spoiler
                    val deg = (p.angleRad * 180f / PI.toFloat())
                    rotate(degrees = deg, pivot = bodyPos) {
                        val carL = rad * 2.1f
                        val carW = rad * 1.35f
                        // 4 Black Tires
                        val tireW = carL * 0.26f
                        val tireH = carW * 0.24f
                        drawRoundRect(
                            color = Color(0xFF090D16),
                            topLeft = Offset(bodyPos.x - carL * 0.36f, bodyPos.y - carW * 0.62f),
                            size = Size(tireW, tireH),
                            cornerRadius = CornerRadius(3f, 3f)
                        )
                        drawRoundRect(
                            color = Color(0xFF090D16),
                            topLeft = Offset(bodyPos.x + carL * 0.12f, bodyPos.y - carW * 0.62f),
                            size = Size(tireW, tireH),
                            cornerRadius = CornerRadius(3f, 3f)
                        )
                        drawRoundRect(
                            color = Color(0xFF090D16),
                            topLeft = Offset(bodyPos.x - carL * 0.36f, bodyPos.y + carW * 0.38f),
                            size = Size(tireW, tireH),
                            cornerRadius = CornerRadius(3f, 3f)
                        )
                        drawRoundRect(
                            color = Color(0xFF090D16),
                            topLeft = Offset(bodyPos.x + carL * 0.12f, bodyPos.y + carW * 0.38f),
                            size = Size(tireW, tireH),
                            cornerRadius = CornerRadius(3f, 3f)
                        )
                        // Car Body Chassis
                        drawRoundRect(
                            color = p.id.color,
                            topLeft = Offset(bodyPos.x - carL * 0.5f, bodyPos.y - carW * 0.5f),
                            size = Size(carL, carW),
                            cornerRadius = CornerRadius(rad * 0.45f, rad * 0.45f)
                        )
                        drawRoundRect(
                            color = Color.White,
                            topLeft = Offset(bodyPos.x - carL * 0.5f, bodyPos.y - carW * 0.5f),
                            size = Size(carL, carW),
                            cornerRadius = CornerRadius(rad * 0.45f, rad * 0.45f),
                            style = Stroke(width = 2.5f)
                        )
                        // Cockpit Windshield
                        drawRoundRect(
                            color = Color(0xFF0F172A),
                            topLeft = Offset(bodyPos.x - carL * 0.10f, bodyPos.y - carW * 0.34f),
                            size = Size(carL * 0.36f, carW * 0.68f),
                            cornerRadius = CornerRadius(4f, 4f)
                        )
                    }
                }

                ControlScheme.TANK_CONTROLS, ControlScheme.JOYSTICK_SHOOT -> {
                    // Top-Down Tank / Blaster Sprite with Treads + Rotating Turret Cannon
                    val hullDeg = (p.angleRad * 180f / PI.toFloat())
                    rotate(degrees = hullDeg, pivot = bodyPos) {
                        val hullS = rad * 1.75f
                        // Left & Right Tank Treads
                        drawRoundRect(
                            color = Color(0xFF1E293B),
                            topLeft = Offset(bodyPos.x - hullS * 0.52f, bodyPos.y - hullS * 0.58f),
                            size = Size(hullS * 1.04f, hullS * 0.24f),
                            cornerRadius = CornerRadius(4f, 4f)
                        )
                        drawRoundRect(
                            color = Color(0xFF1E293B),
                            topLeft = Offset(bodyPos.x - hullS * 0.52f, bodyPos.y + hullS * 0.34f),
                            size = Size(hullS * 1.04f, hullS * 0.24f),
                            cornerRadius = CornerRadius(4f, 4f)
                        )
                        // Armored Hull
                        drawRoundRect(
                            color = p.id.darkColor,
                            topLeft = Offset(bodyPos.x - hullS * 0.44f, bodyPos.y - hullS * 0.40f),
                            size = Size(hullS * 0.88f, hullS * 0.80f),
                            cornerRadius = CornerRadius(6f, 6f)
                        )
                        drawRoundRect(
                            color = Color.White,
                            topLeft = Offset(bodyPos.x - hullS * 0.44f, bodyPos.y - hullS * 0.40f),
                            size = Size(hullS * 0.88f, hullS * 0.80f),
                            cornerRadius = CornerRadius(6f, 6f),
                            style = Stroke(width = 2f)
                        )
                    }
                    // Independently Rotating Turret Cannon Barrel
                    val barrelEnd = Offset(
                        bodyPos.x + cos(p.turretAngleRad) * rad * 1.55f,
                        bodyPos.y + sin(p.turretAngleRad) * rad * 1.55f
                    )
                    drawLine(
                        color = Color.White,
                        start = bodyPos,
                        end = barrelEnd,
                        strokeWidth = 7f * scale * fallScale
                    )
                    drawCircle(color = p.id.color, radius = rad * 0.65f, center = bodyPos)
                    drawCircle(color = Color.White, radius = rad * 0.65f, center = bodyPos, style = Stroke(width = 2.5f))
                }

                else -> {
                    // Top-Down Fighter / Sumo / Athlete Sprite with Shoulders, Head & Animated Gloves/Cue
                    val forwardX = cos(p.angleRad)
                    val forwardY = sin(p.angleRad)
                    val rightX = -forwardY
                    val rightY = forwardX
                    val punchReach = if (p.attackAnimTimer > 0f) rad * 1.45f else rad * 0.72f

                    // Attack / Push Shockwave Swing Arc when striking!
                    if (p.attackAnimTimer > 0f) {
                        val swingCenter = Offset(
                            bodyPos.x + forwardX * rad * 0.95f,
                            bodyPos.y + forwardY * rad * 0.95f
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.55f),
                            radius = rad * 1.25f,
                            center = swingCenter,
                            style = Stroke(width = 5f)
                        )
                    }

                    // Left & Right Hands / Gloves / Cleats
                    val leftHand = Offset(
                        bodyPos.x + forwardX * punchReach - rightX * rad * 0.68f,
                        bodyPos.y + forwardY * punchReach - rightY * rad * 0.68f
                    )
                    val rightHand = Offset(
                        bodyPos.x + forwardX * punchReach + rightX * rad * 0.68f,
                        bodyPos.y + forwardY * punchReach + rightY * rad * 0.68f
                    )
                    drawCircle(color = p.id.lightColor, radius = rad * 0.32f, center = leftHand)
                    drawCircle(color = Color.White, radius = rad * 0.32f, center = leftHand, style = Stroke(width = 2f))
                    drawCircle(color = p.id.lightColor, radius = rad * 0.32f, center = rightHand)
                    drawCircle(color = Color.White, radius = rad * 0.32f, center = rightHand, style = Stroke(width = 2f))

                    // Torso / Shoulders
                    drawCircle(color = p.id.color, radius = rad, center = bodyPos)
                    drawCircle(color = Color.White, radius = rad, center = bodyPos, style = Stroke(width = 3f))

                    // Visor / Facing Nose
                    val nosePos = Offset(bodyPos.x + forwardX * rad * 0.62f, bodyPos.y + forwardY * rad * 0.62f)
                    drawCircle(color = Color.White, radius = rad * 0.25f, center = nosePos)
                }
            }

            // Player Identity Badge (P1..P4, Race Position, or Morph Shape)
            val badgeText = when {
                engine.game.mechanicGroup == MechanicGroup.SHAPE_MORPH_CHASE -> when (p.morphShape) {
                    MorphShape.CIRCLE -> "●"
                    MorphShape.TRIANGLE -> "▲"
                    MorphShape.SQUARE -> "■"
                }
                engine.game.mechanicGroup == MechanicGroup.CIRCUIT_RACING ->
                    "#${p.racePosition}"
                else ->
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

        // 7. Crisp Arena Border Frame
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
