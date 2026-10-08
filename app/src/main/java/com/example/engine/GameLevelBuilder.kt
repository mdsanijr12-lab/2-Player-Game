package com.example.engine

import androidx.compose.ui.graphics.Color
import com.example.model.GameSpec
import com.example.model.MatchConfig
import com.example.model.MechanicGroup
import com.example.model.PlayerId
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

object GameLevelBuilder {

    fun createInitialPlayers(config: MatchConfig): List<PlayerEntity> {
        val count = config.totalActivePlayers.coerceIn(2, 4)
        val spawnPositions = when (config.game.mechanicGroup) {
            MechanicGroup.PADDLE_DEFENSE -> {
                if (count == 2) {
                    listOf(
                        500f to 840f, // P1 bottom
                        500f to 160f  // P2 top
                    )
                } else {
                    listOf(
                        500f to 850f,
                        500f to 150f,
                        150f to 500f,
                        850f to 500f
                    )
                }
            }
            MechanicGroup.HIGHWAY_DODGE, MechanicGroup.BRIDGE_BUILDER, MechanicGroup.BOWLING_ROLL -> {
                listOf(
                    200f to 820f,
                    400f to 820f,
                    600f to 820f,
                    800f to 820f
                )
            }
            MechanicGroup.CIRCUIT_RACING -> {
                listOf(
                    240f to 760f,
                    310f to 760f,
                    240f to 830f,
                    310f to 830f
                )
            }
            else -> {
                listOf(
                    200f to 800f, // P1 Bottom-Left (Red)
                    800f to 200f, // P2 Top-Right (Blue)
                    200f to 200f, // P3 Top-Left (Yellow)
                    800f to 800f  // P4 Bottom-Right (Green)
                )
            }
        }

        return (0 until count).map { idx ->
            val pid = PlayerId.entries[idx]
            val pos = spawnPositions[idx]
            PlayerEntity(
                id = pid,
                isBot = config.isBot(idx),
                x = pos.first,
                y = pos.second,
                angleRad = if (pos.second > 500f) (-PI / 2).toFloat() else (PI / 2).toFloat(),
                isTaggedOrCursed = (config.game.mechanicGroup == MechanicGroup.TAG_PASS_CURSE && idx == 0),
                morphShape = MorphShape.entries[idx % 3],
                basePos = pos
            )
        }
    }

    fun populateInitialWorld(
        game: GameSpec,
        playerCount: Int,
        entities: MutableList<WorldEntity>,
        grid: MutableList<GridCell>,
        nextUid: () -> Int
    ): Int {
        entities.clear()
        grid.clear()
        var gridSize = 0

        when (game.mechanicGroup) {
            MechanicGroup.BRAWL_KNOCKBACK -> {
                // Safe ring radius indicator
                val ringRadius = when (game.variantIndex) {
                    1 -> 340f // Balance Ball
                    4 -> 350f // Push Arena Sumo
                    else -> 380f
                }
                entities.add(
                    WorldEntity(
                        uid = nextUid(),
                        kind = EntityKind.SAFE_ZONE_RING,
                        x = ARENA_CENTER,
                        y = ARENA_CENTER,
                        radius = ringRadius,
                        color = Color(0xFF38BDF8)
                    )
                )
                if (game.variantIndex == 3) {
                    // Magnet Arena: metallic orbs to pull
                    repeat(6) { i ->
                        val ang = i * PI / 3
                        entities.add(
                            WorldEntity(
                                uid = nextUid(),
                                kind = EntityKind.CRYSTAL,
                                x = ARENA_CENTER + cos(ang).toFloat() * 190f,
                                y = ARENA_CENTER + sin(ang).toFloat() * 190f,
                                radius = 20f,
                                color = Color(0xFFF43F5E)
                            )
                        )
                    }
                }
            }

            MechanicGroup.COLLECT_AND_RETURN -> {
                val itemKind = when (game.variantIndex) {
                    1 -> EntityKind.TREASURE_CHEST
                    2 -> EntityKind.CRATE_BOX
                    3 -> EntityKind.FLAG
                    else -> EntityKind.CRYSTAL
                }
                val itemCount = if (itemKind == EntityKind.FLAG) 3 else 7
                repeat(itemCount) { i ->
                    val ang = (2 * PI * i) / itemCount
                    val dist = if (i % 2 == 0) 130f else 230f
                    entities.add(
                        WorldEntity(
                            uid = nextUid(),
                            kind = itemKind,
                            x = ARENA_CENTER + cos(ang).toFloat() * dist,
                            y = ARENA_CENTER + sin(ang).toFloat() * dist,
                            radius = if (itemKind == EntityKind.TREASURE_CHEST) 26f else 22f,
                            color = Color(0xFFFACC15)
                        )
                    )
                }
            }

            MechanicGroup.BOMB_DODGE_KICK -> {
                repeat(4) { i ->
                    entities.add(
                        WorldEntity(
                            uid = nextUid(),
                            kind = EntityKind.BOMB,
                            x = 300f + (i % 2) * 400f,
                            y = 300f + (i / 2) * 400f,
                            vx = (Random.nextFloat() - 0.5f) * 140f,
                            vy = (Random.nextFloat() - 0.5f) * 140f,
                            radius = 26f,
                            timer = 2.6f + i * 0.7f,
                            maxTimer = 3.5f,
                            color = Color(0xFFF97316)
                        )
                    )
                }
            }

            MechanicGroup.SHOOT_TARGETS -> {
                val kind = when (game.variantIndex) {
                    3 -> EntityKind.BUBBLE
                    5 -> EntityKind.FRUIT
                    6 -> EntityKind.BALLOON
                    else -> EntityKind.TARGET_DRONE
                }
                repeat(6) { i ->
                    val ang = i * PI / 3
                    entities.add(
                        WorldEntity(
                            uid = nextUid(),
                            kind = kind,
                            x = ARENA_CENTER + cos(ang).toFloat() * 240f,
                            y = ARENA_CENTER + sin(ang).toFloat() * 240f,
                            vx = cos(ang + 1.1).toFloat() * 135f,
                            vy = sin(ang + 1.1).toFloat() * 135f,
                            radius = if (kind == EntityKind.BUBBLE) 34f else 24f,
                            value = if (i % 2 == 0) 2 else 1,
                            color = if (i % 2 == 0) Color(0xFFFACC15) else Color(0xFF38BDF8)
                        )
                    )
                }
            }

            MechanicGroup.SAFE_ZONE_SURVIVAL -> {
                if (game.variantIndex in listOf(3, 4, 5)) {
                    // Grid-based survival (Lava Floor, Last Square, Safe Tile)
                    gridSize = 6
                    val step = 1000f / gridSize
                    for (r in 0 until gridSize) {
                        for (c in 0 until gridSize) {
                            val sym = (r + c) % 4
                            grid.add(
                                GridCell(
                                    row = r,
                                    col = c,
                                    state = if ((r + c) % 3 == 0) 1 else 0,
                                    symbol = sym,
                                    label = listOf("★", "▲", "●", "■")[sym]
                                )
                            )
                        }
                    }
                    // Keep spawn cells safe initially
                    entities.add(
                        WorldEntity(
                            uid = nextUid(),
                            kind = EntityKind.SAFE_ZONE_RING,
                            x = ARENA_CENTER,
                            y = ARENA_CENTER,
                            radius = step * 1.1f,
                            timer = 4.0f,
                            maxTimer = 4.0f,
                            color = Color(0xFF22C55E)
                        )
                    )
                } else {
                    entities.add(
                        WorldEntity(
                            uid = nextUid(),
                            kind = EntityKind.SAFE_ZONE_RING,
                            x = ARENA_CENTER,
                            y = ARENA_CENTER,
                            radius = 230f,
                            timer = 4.5f,
                            maxTimer = 4.5f,
                            color = Color(0xFF22C55E)
                        )
                    )
                }
            }

            MechanicGroup.HAZARD_DODGE -> {
                repeat(4) { i ->
                    entities.add(
                        WorldEntity(
                            uid = nextUid(),
                            kind = if (game.variantIndex == 6) EntityKind.SEEKER_ORB else EntityKind.METEOR_WARNING,
                            x = 250f + (i % 2) * 500f,
                            y = 250f + (i / 2) * 500f,
                            vx = (Random.nextFloat() - 0.5f) * 180f,
                            vy = (Random.nextFloat() - 0.5f) * 180f,
                            radius = 42f,
                            timer = 2.0f + i * 0.5f,
                            maxTimer = 2.5f,
                            color = Color(0xFFEF4444)
                        )
                    )
                }
                // Bonus stars to collect while dodging
                repeat(4) { i ->
                    entities.add(
                        WorldEntity(
                            uid = nextUid(),
                            kind = EntityKind.CRYSTAL,
                            x = 320f + (i % 2) * 360f,
                            y = 320f + (i / 2) * 360f,
                            radius = 20f,
                            color = Color(0xFFFACC15)
                        )
                    )
                }
            }

            MechanicGroup.TAG_PASS_CURSE -> {
                // Spawn light orbs for non-cursed players to collect
                repeat(5) { i ->
                    val ang = i * 2 * PI / 5
                    entities.add(
                        WorldEntity(
                            uid = nextUid(),
                            kind = EntityKind.CRYSTAL,
                            x = ARENA_CENTER + cos(ang).toFloat() * 220f,
                            y = ARENA_CENTER + sin(ang).toFloat() * 220f,
                            radius = 20f,
                            color = Color(0xFFFDE047)
                        )
                    )
                }
            }

            MechanicGroup.CASTLE_SIEGE -> {
                repeat(5) { i ->
                    val ang = i * 2 * PI / 5
                    entities.add(
                        WorldEntity(
                            uid = nextUid(),
                            kind = EntityKind.TARGET_DRONE,
                            x = ARENA_CENTER + cos(ang).toFloat() * 180f,
                            y = ARENA_CENTER + sin(ang).toFloat() * 180f,
                            vx = cos(ang).toFloat() * 95f,
                            vy = sin(ang).toFloat() * 95f,
                            radius = 28f,
                            color = Color(0xFFF97316)
                        )
                    )
                }
            }

            MechanicGroup.CIRCUIT_RACING, MechanicGroup.CHECKPOINT_RUSH -> {
                // 4 track checkpoints around the circuit
                val gates = listOf(
                    220f to 220f,
                    780f to 220f,
                    780f to 780f,
                    220f to 780f
                )
                gates.forEachIndexed { idx, (gx, gy) ->
                    entities.add(
                        WorldEntity(
                            uid = nextUid(),
                            kind = EntityKind.CHECKPOINT_GATE,
                            x = gx,
                            y = gy,
                            radius = 65f,
                            value = idx,
                            label = "CP ${idx + 1}",
                            color = Color(0xFFFACC15)
                        )
                    )
                }
            }

            MechanicGroup.HIGHWAY_DODGE -> {
                repeat(6) { i ->
                    val laneX = 180f + (i % 4) * 210f
                    entities.add(
                        WorldEntity(
                            uid = nextUid(),
                            kind = EntityKind.HIGHWAY_CAR,
                            x = laneX,
                            y = 120f + (i * 140f) % 700f,
                            vx = 0f,
                            vy = 240f + (i % 3) * 55f,
                            radius = 32f,
                            width = 52f,
                            height = 84f,
                            color = Color(0xFFF59E0B)
                        )
                    )
                }
            }

            MechanicGroup.BRIDGE_BUILDER -> {
                repeat(10) { i ->
                    val owner = i % playerCount
                    entities.add(
                        WorldEntity(
                            uid = nextUid(),
                            kind = EntityKind.BRIDGE_PLANK,
                            x = 160f + (i * 75f) % 680f,
                            y = 660f + (i % 3) * 90f,
                            radius = 22f,
                            ownerId = owner,
                            color = PlayerId.entries[owner].color
                        )
                    )
                }
            }

            MechanicGroup.BALL_SPORTS, MechanicGroup.PENALTY_DUEL -> {
                val ballKind = if (game.variantIndex == 1) EntityKind.PUCK_HOCKEY else EntityKind.BALL_SOCCER
                entities.add(
                    WorldEntity(
                        uid = nextUid(),
                        kind = ballKind,
                        x = ARENA_CENTER,
                        y = ARENA_CENTER,
                        vx = 140f,
                        vy = -120f,
                        radius = 24f,
                        color = Color.White
                    )
                )
            }

            MechanicGroup.BASKET_SHOOT -> {
                // Target hoop at top-center + two basketballs
                entities.add(
                    WorldEntity(
                        uid = nextUid(),
                        kind = EntityKind.GOLF_HOLE, // Acts as hoop target
                        x = ARENA_CENTER,
                        y = 180f,
                        vx = if (game.variantIndex == 1) 140f else 0f,
                        radius = 48f,
                        color = Color(0xFFF97316)
                    )
                )
                repeat(3) { i ->
                    entities.add(
                        WorldEntity(
                            uid = nextUid(),
                            kind = EntityKind.BALL_BASKET,
                            x = 300f + i * 200f,
                            y = 560f,
                            radius = 22f,
                            color = Color(0xFFEA580C)
                        )
                    )
                }
            }

            MechanicGroup.PADDLE_DEFENSE -> {
                val puckCount = if (game.variantIndex == 3) 3 else 1 // Goal Keeper has multi-balls
                repeat(puckCount) { i ->
                    val ang = (i * 2.1f) + 0.7f
                    entities.add(
                        WorldEntity(
                            uid = nextUid(),
                            kind = if (game.variantIndex in listOf(1, 2)) EntityKind.BALL_TENNIS else EntityKind.PUCK_HOCKEY,
                            x = ARENA_CENTER,
                            y = ARENA_CENTER,
                            vx = cos(ang) * 320f,
                            vy = sin(ang) * 340f,
                            radius = 22f,
                            color = if (game.variantIndex == 1) Color(0xFFA3E635) else Color.White
                        )
                    )
                }
            }

            MechanicGroup.BOWLING_ROLL -> {
                // 10-pin rack near top
                val rows = listOf(1, 2, 3, 4)
                var pinIndex = 0
                rows.forEachIndexed { rIdx, count ->
                    val rowY = 260f - rIdx * 48f
                    val startX = ARENA_CENTER - (count - 1) * 30f
                    for (c in 0 until count) {
                        entities.add(
                            WorldEntity(
                                uid = nextUid(),
                                kind = EntityKind.BOWLING_PIN,
                                x = startX + c * 60f,
                                y = rowY,
                                radius = 18f,
                                value = ++pinIndex,
                                color = Color.White
                            )
                        )
                    }
                }
            }

            MechanicGroup.TILE_PUZZLE, MechanicGroup.TERRITORY_PAINT, MechanicGroup.WALL_TRAIL_TRAP -> {
                gridSize = 7
                for (r in 0 until gridSize) {
                    for (c in 0 until gridSize) {
                        val sym = (r * 3 + c) % 6
                        grid.add(
                            GridCell(
                                row = r,
                                col = c,
                                ownerId = -1,
                                state = 0,
                                symbol = sym,
                                label = listOf("◆", "★", "●", "▲", "■", "✦")[sym]
                            )
                        )
                    }
                }
            }

            MechanicGroup.MAZE_RUNNER -> {
                gridSize = 7
                for (r in 0 until gridSize) {
                    for (c in 0 until gridSize) {
                        val isWall = (r % 2 == 1 && c % 2 == 1 && !(r == 3 && c == 3))
                        grid.add(
                            GridCell(
                                row = r,
                                col = c,
                                state = if (isWall) 3 else 0
                            )
                        )
                    }
                }
                entities.add(
                    WorldEntity(
                        uid = nextUid(),
                        kind = EntityKind.CHECKPOINT_GATE,
                        x = ARENA_CENTER,
                        y = ARENA_CENTER,
                        radius = 45f,
                        label = "GOAL",
                        color = Color(0xFF34D399)
                    )
                )
            }

            MechanicGroup.MATH_PATTERN_QUIZ -> {
                val padPositions = listOf(
                    320f to 350f,
                    680f to 350f,
                    320f to 650f,
                    680f to 650f
                )
                padPositions.forEachIndexed { idx, (px, py) ->
                    entities.add(
                        WorldEntity(
                            uid = nextUid(),
                            kind = EntityKind.QUIZ_PAD,
                            x = px,
                            y = py,
                            radius = 58f,
                            value = idx,
                            label = "${(idx + 1) * 12}",
                            color = Color(0xFFC084FC)
                        )
                    )
                }
            }

            MechanicGroup.ZONE_CAPTURE -> {
                val zones = if (game.variantIndex == 0) {
                    listOf(500f to 260f, 280f to 680f, 720f to 680f)
                } else {
                    listOf(500f to 500f)
                }
                zones.forEachIndexed { i, (zx, zy) ->
                    entities.add(
                        WorldEntity(
                            uid = nextUid(),
                            kind = EntityKind.CAPTURE_ZONE,
                            x = zx,
                            y = zy,
                            vx = if (game.variantIndex == 2) 95f else 0f,
                            vy = if (game.variantIndex == 2) 75f else 0f,
                            radius = 110f,
                            label = listOf("ALPHA", "BETA", "GAMMA")[i % 3],
                            color = Color(0xFF38BDF8)
                        )
                    )
                }
            }

            MechanicGroup.REACTION_TAP -> {
                entities.add(
                    WorldEntity(
                        uid = nextUid(),
                        kind = EntityKind.SAFE_ZONE_RING,
                        x = ARENA_CENTER,
                        y = ARENA_CENTER,
                        radius = 150f,
                        timer = 1.6f,
                        maxTimer = 2.0f,
                        label = "WAIT...",
                        color = Color(0xFFF59E0B)
                    )
                )
            }

            MechanicGroup.GOLF_PUTT -> {
                entities.add(
                    WorldEntity(
                        uid = nextUid(),
                        kind = if (game.variantIndex == 2) EntityKind.CARNIVAL_PEG else EntityKind.GOLF_HOLE,
                        x = ARENA_CENTER,
                        y = 240f,
                        vx = if (game.variantIndex > 0) 110f else 0f,
                        radius = 42f,
                        color = Color(0xFF4ADE80)
                    )
                )
            }

            MechanicGroup.DISC_DODGEBALL -> {
                val kind = if (game.variantIndex == 0) EntityKind.DISC_NEON else EntityKind.DODGEBALL
                repeat(4) { i ->
                    val ang = i * PI / 2
                    entities.add(
                        WorldEntity(
                            uid = nextUid(),
                            kind = kind,
                            x = ARENA_CENTER + cos(ang).toFloat() * 200f,
                            y = ARENA_CENTER + sin(ang).toFloat() * 200f,
                            vx = cos(ang + 0.8).toFloat() * 260f,
                            vy = sin(ang + 0.8).toFloat() * 260f,
                            radius = 22f,
                            color = Color(0xFF22D3EE)
                        )
                    )
                }
            }

            MechanicGroup.SHAPE_MORPH_CHASE -> {
                repeat(4) { i ->
                    entities.add(
                        WorldEntity(
                            uid = nextUid(),
                            kind = EntityKind.CRYSTAL,
                            x = 300f + (i % 2) * 400f,
                            y = 300f + (i / 2) * 400f,
                            radius = 20f,
                            color = Color(0xFFFACC15)
                        )
                    )
                }
            }
        }

        return gridSize
    }
}
