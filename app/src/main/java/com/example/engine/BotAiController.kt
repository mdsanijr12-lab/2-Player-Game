package com.example.engine

import com.example.model.BotDifficulty
import com.example.model.GameSpec
import com.example.model.MechanicGroup
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

class BotAiController(
    private val difficulty: BotDifficulty
) {
    private val decisionTimers = FloatArray(4) { 0f }
    private val cachedMoveX = FloatArray(4) { 0f }
    private val cachedMoveY = FloatArray(4) { 0f }

    fun computeBotInput(
        bot: PlayerEntity,
        allPlayers: List<PlayerEntity>,
        entities: List<WorldEntity>,
        grid: List<GridCell>,
        gridSize: Int,
        game: GameSpec,
        elapsedSec: Float,
        dt: Float,
        promptTargetValue: Int,
        reactionSignalActive: Boolean
    ): PlayerInputState {
        val idx = bot.id.index
        if (bot.eliminated) return PlayerInputState()

        val decisionInterval = when (difficulty) {
            BotDifficulty.EASY -> 0.34f
            BotDifficulty.NORMAL -> 0.16f
            BotDifficulty.HARD -> 0.06f
        }
        val aimJitterRad = when (difficulty) {
            BotDifficulty.EASY -> 0.52f
            BotDifficulty.NORMAL -> 0.20f
            BotDifficulty.HARD -> 0.04f
        }
        val throttleScale = when (difficulty) {
            BotDifficulty.EASY -> 0.78f
            BotDifficulty.NORMAL -> 0.92f
            BotDifficulty.HARD -> 1.0f
        }

        decisionTimers[idx] -= dt
        var triggerPrimary = false
        var triggerSecondary = false
        var holdPrimary = false
        var holdSecondary = false

        if (decisionTimers[idx] <= 0f) {
            decisionTimers[idx] = decisionInterval + Random.nextFloat() * 0.05f

            var targetX = ARENA_CENTER
            var targetY = ARENA_CENTER

            when (game.mechanicGroup) {
                MechanicGroup.BRAWL_KNOCKBACK -> {
                    val nearestRival = allPlayers
                        .filter { it.id != bot.id && !it.eliminated }
                        .minByOrNull { hypot(it.x - bot.x, it.y - bot.y) }
                    val distFromCenter = hypot(bot.x - ARENA_CENTER, bot.y - ARENA_CENTER)
                    if (distFromCenter > 320f && difficulty != BotDifficulty.EASY) {
                        // Priority: stay safely inside ring
                        targetX = ARENA_CENTER
                        targetY = ARENA_CENTER
                    } else if (nearestRival != null) {
                        targetX = nearestRival.x
                        targetY = nearestRival.y
                    }
                }

                MechanicGroup.COLLECT_AND_RETURN, MechanicGroup.BRIDGE_BUILDER -> {
                    if (bot.carryingItem > 0) {
                        if (game.mechanicGroup == MechanicGroup.BRIDGE_BUILDER) {
                            val bridgeX = 200f + idx * 200f
                            targetX = bridgeX
                            targetY = 120f
                        } else {
                            targetX = bot.basePos.first
                            targetY = bot.basePos.second
                        }
                    } else {
                        val item = entities.filter {
                            it.active && (it.kind == EntityKind.CRYSTAL ||
                                it.kind == EntityKind.TREASURE_CHEST ||
                                it.kind == EntityKind.FLAG ||
                                it.kind == EntityKind.BRIDGE_PLANK ||
                                it.kind == EntityKind.CRATE_BOX) &&
                                (it.ownerId == -1 || it.ownerId == idx)
                        }.minByOrNull { hypot(it.x - bot.x, it.y - bot.y) }
                        if (item != null) {
                            targetX = item.x
                            targetY = item.y
                        }
                    }
                }

                MechanicGroup.BOMB_DODGE_KICK -> {
                    val danger = entities.filter { it.active && it.kind == EntityKind.BOMB }
                        .minByOrNull { hypot(it.x - bot.x, it.y - bot.y) }
                    if (danger != null && hypot(danger.x - bot.x, danger.y - bot.y) < 260f) {
                        // Run away from bomb
                        val dx = bot.x - danger.x
                        val dy = bot.y - danger.y
                        targetX = (bot.x + dx * 2f).coerceIn(120f, 880f)
                        targetY = (bot.y + dy * 2f).coerceIn(120f, 880f)
                    } else {
                        targetX = ARENA_CENTER + cos(elapsedSec + idx) * 220f
                        targetY = ARENA_CENTER + sin(elapsedSec + idx) * 220f
                    }
                }

                MechanicGroup.SHOOT_TARGETS, MechanicGroup.CASTLE_SIEGE, MechanicGroup.DISC_DODGEBALL -> {
                    val target = entities.filter {
                        it.active && (it.kind == EntityKind.TARGET_DRONE ||
                            it.kind == EntityKind.BUBBLE ||
                            it.kind == EntityKind.FRUIT ||
                            it.kind == EntityKind.BALLOON ||
                            it.kind == EntityKind.DISC_NEON ||
                            it.kind == EntityKind.DODGEBALL)
                    }.minByOrNull { hypot(it.x - bot.x, it.y - bot.y) }

                    if (target != null) {
                        targetX = if (difficulty == BotDifficulty.HARD) target.x + target.vx * 0.25f else target.x
                        targetY = if (difficulty == BotDifficulty.HARD) target.y + target.vy * 0.25f else target.y
                    } else {
                        val rival = allPlayers.filter { it.id != bot.id && !it.eliminated }
                            .minByOrNull { hypot(it.x - bot.x, it.y - bot.y) }
                        if (rival != null) {
                            targetX = rival.x
                            targetY = rival.y
                        }
                    }
                }

                MechanicGroup.SAFE_ZONE_SURVIVAL -> {
                    val safeRing = entities.firstOrNull { it.active && it.kind == EntityKind.SAFE_ZONE_RING }
                    if (safeRing != null) {
                        targetX = safeRing.x
                        targetY = safeRing.y
                    } else if (grid.isNotEmpty()) {
                        val safeCell = grid.filter { it.state == 1 }
                            .minByOrNull {
                                val cx = (it.col + 0.5f) * (ARENA_SIZE / gridSize)
                                val cy = (it.row + 0.5f) * (ARENA_SIZE / gridSize)
                                hypot(cx - bot.x, cy - bot.y)
                            }
                        if (safeCell != null) {
                            targetX = (safeCell.col + 0.5f) * (ARENA_SIZE / gridSize)
                            targetY = (safeCell.row + 0.5f) * (ARENA_SIZE / gridSize)
                        }
                    }
                }

                MechanicGroup.HAZARD_DODGE -> {
                    // Collectible stars vs hazards
                    val star = entities.filter { it.active && it.kind == EntityKind.CRYSTAL }
                        .minByOrNull { hypot(it.x - bot.x, it.y - bot.y) }
                    val hazard = entities.filter {
                        it.active && (it.kind == EntityKind.METEOR_WARNING ||
                            it.kind == EntityKind.SEEKER_ORB ||
                            it.kind == EntityKind.LASER_WALL)
                    }.minByOrNull { hypot(it.x - bot.x, it.y - bot.y) }

                    if (hazard != null && hypot(hazard.x - bot.x, hazard.y - bot.y) < 230f) {
                        targetX = (bot.x + (bot.x - hazard.x) * 1.8f).coerceIn(130f, 870f)
                        targetY = (bot.y + (bot.y - hazard.y) * 1.8f).coerceIn(130f, 870f)
                    } else if (star != null) {
                        targetX = star.x
                        targetY = star.y
                    } else {
                        targetX = ARENA_CENTER + cos(elapsedSec * 1.3f + idx) * 240f
                        targetY = ARENA_CENTER + sin(elapsedSec * 1.3f + idx) * 240f
                    }
                }

                MechanicGroup.TAG_PASS_CURSE -> {
                    val isRingGame = game.variantIndex == 1 // Chase Ring: holder wants to flee!
                    val isHolder = bot.isTaggedOrCursed
                    val shouldChaseRival = (isHolder && !isRingGame) || (!isHolder && isRingGame)
                    if (shouldChaseRival) {
                        val targetPlayer = if (isRingGame) {
                            allPlayers.firstOrNull { it.isTaggedOrCursed && !it.eliminated }
                        } else {
                            allPlayers.filter { it.id != bot.id && !it.eliminated }
                                .minByOrNull { hypot(it.x - bot.x, it.y - bot.y) }
                        }
                        if (targetPlayer != null) {
                            targetX = targetPlayer.x
                            targetY = targetPlayer.y
                        }
                    } else {
                        val chaser = allPlayers.firstOrNull { it.isTaggedOrCursed && it.id != bot.id }
                            ?: allPlayers.filter { it.id != bot.id && !it.eliminated }
                                .minByOrNull { hypot(it.x - bot.x, it.y - bot.y) }
                        if (chaser != null) {
                            targetX = (bot.x + (bot.x - chaser.x) * 1.6f).coerceIn(140f, 860f)
                            targetY = (bot.y + (bot.y - chaser.y) * 1.6f).coerceIn(140f, 860f)
                        }
                    }
                }

                MechanicGroup.CIRCUIT_RACING, MechanicGroup.CHECKPOINT_RUSH -> {
                    val gate = entities.filter { it.active && it.kind == EntityKind.CHECKPOINT_GATE }
                        .firstOrNull { it.value == (bot.progressSteps % 4) }
                        ?: entities.firstOrNull { it.active && it.kind == EntityKind.CHECKPOINT_GATE }
                    if (gate != null) {
                        targetX = gate.x
                        targetY = gate.y
                    }
                }

                MechanicGroup.HIGHWAY_DODGE -> {
                    val laneCenter = 180f + idx * 210f
                    val carAhead = entities.filter {
                        it.active && it.kind == EntityKind.HIGHWAY_CAR &&
                            abs(it.x - bot.x) < 90f && it.y < bot.y && (bot.y - it.y) < 280f
                    }.minByOrNull { bot.y - it.y }
                    targetX = if (carAhead != null) {
                        if (bot.x > ARENA_CENTER) bot.x - 130f else bot.x + 130f
                    } else {
                        laneCenter
                    }
                    targetY = 260f
                }

                MechanicGroup.BALL_SPORTS, MechanicGroup.PENALTY_DUEL, MechanicGroup.BASKET_SHOOT -> {
                    val ball = entities.firstOrNull {
                        it.active && (it.kind == EntityKind.BALL_SOCCER ||
                            it.kind == EntityKind.BALL_BASKET ||
                            it.kind == EntityKind.PUCK_HOCKEY)
                    }
                    if (ball != null) {
                        targetX = ball.x
                        targetY = ball.y
                    }
                }

                MechanicGroup.PADDLE_DEFENSE -> {
                    val puck = entities.firstOrNull {
                        it.active && (it.kind == EntityKind.PUCK_HOCKEY ||
                            it.kind == EntityKind.BALL_TENNIS ||
                            it.kind == EntityKind.BALL_SOCCER)
                    }
                    if (puck != null) {
                        targetX = puck.x.coerceIn(160f, 840f)
                        targetY = bot.basePos.second
                    }
                }

                MechanicGroup.BOWLING_ROLL, MechanicGroup.GOLF_PUTT -> {
                    val goal = entities.firstOrNull {
                        it.active && (it.kind == EntityKind.BOWLING_PIN ||
                            it.kind == EntityKind.GOLF_HOLE ||
                            it.kind == EntityKind.CARNIVAL_PEG)
                    }
                    if (goal != null) {
                        targetX = goal.x
                        targetY = goal.y
                    }
                }

                MechanicGroup.TILE_PUZZLE, MechanicGroup.TERRITORY_PAINT -> {
                    if (grid.isNotEmpty()) {
                        val candidate = grid.filter { it.ownerId != idx && it.state != 2 && it.state != 3 }
                            .minByOrNull {
                                val cx = (it.col + 0.5f) * (ARENA_SIZE / gridSize)
                                val cy = (it.row + 0.5f) * (ARENA_SIZE / gridSize)
                                hypot(cx - bot.x, cy - bot.y)
                            }
                        if (candidate != null) {
                            targetX = (candidate.col + 0.5f) * (ARENA_SIZE / gridSize)
                            targetY = (candidate.row + 0.5f) * (ARENA_SIZE / gridSize)
                        }
                    }
                }

                MechanicGroup.MAZE_RUNNER -> {
                    val goal = entities.firstOrNull { it.active && it.kind == EntityKind.CHECKPOINT_GATE }
                        ?: entities.firstOrNull { it.active && it.kind == EntityKind.CRYSTAL }
                    if (goal != null) {
                        targetX = goal.x
                        targetY = goal.y
                    }
                }

                MechanicGroup.MATH_PATTERN_QUIZ -> {
                    val correctPad = entities.firstOrNull {
                        it.active && it.kind == EntityKind.QUIZ_PAD && it.value == promptTargetValue
                    }
                    val anyPad = entities.firstOrNull { it.active && it.kind == EntityKind.QUIZ_PAD }
                    val chosen = if (difficulty == BotDifficulty.EASY && Random.nextFloat() < 0.35f) anyPad else (correctPad ?: anyPad)
                    if (chosen != null) {
                        targetX = chosen.x
                        targetY = chosen.y
                    }
                }

                MechanicGroup.ZONE_CAPTURE -> {
                    val zone = entities.filter { it.active && it.kind == EntityKind.CAPTURE_ZONE }
                        .minByOrNull { hypot(it.x - bot.x, it.y - bot.y) }
                    if (zone != null) {
                        targetX = zone.x
                        targetY = zone.y
                    }
                }

                MechanicGroup.WALL_TRAIL_TRAP -> {
                    if (grid.isNotEmpty()) {
                        val cleanCell = grid.filter { it.state == 0 && it.ownerId == -1 }
                            .minByOrNull {
                                val cx = (it.col + 0.5f) * (ARENA_SIZE / gridSize)
                                val cy = (it.row + 0.5f) * (ARENA_SIZE / gridSize)
                                val dist = hypot(cx - bot.x, cy - bot.y)
                                if (dist < 60f) 9999f else dist
                            }
                        if (cleanCell != null) {
                            targetX = (cleanCell.col + 0.5f) * (ARENA_SIZE / gridSize)
                            targetY = (cleanCell.row + 0.5f) * (ARENA_SIZE / gridSize)
                        }
                    }
                }

                MechanicGroup.REACTION_TAP -> {
                    targetX = bot.x
                    targetY = bot.y
                }

                MechanicGroup.SHAPE_MORPH_CHASE -> {
                    val prey = allPlayers.filter {
                        it.id != bot.id && !it.eliminated && bot.morphShape.beats(it.morphShape)
                    }.minByOrNull { hypot(it.x - bot.x, it.y - bot.y) }
                    val predator = allPlayers.filter {
                        it.id != bot.id && !it.eliminated && it.morphShape.beats(bot.morphShape)
                    }.minByOrNull { hypot(it.x - bot.x, it.y - bot.y) }

                    if (predator != null && hypot(predator.x - bot.x, predator.y - bot.y) < 220f && difficulty != BotDifficulty.EASY) {
                        targetX = (bot.x + (bot.x - predator.x) * 1.5f).coerceIn(130f, 870f)
                        targetY = (bot.y + (bot.y - predator.y) * 1.5f).coerceIn(130f, 870f)
                    } else if (prey != null) {
                        targetX = prey.x
                        targetY = prey.y
                    }
                }
            }

            val dx = targetX - bot.x
            val dy = targetY - bot.y
            val dist = hypot(dx, dy)
            if (dist > 10f) {
                val baseAngle = atan2(dy, dx)
                val noisyAngle = baseAngle + (Random.nextFloat() - 0.5f) * 2f * aimJitterRad
                cachedMoveX[idx] = (cos(noisyAngle) * throttleScale).toFloat()
                cachedMoveY[idx] = (sin(noisyAngle) * throttleScale).toFloat()
            } else {
                cachedMoveX[idx] = 0f
                cachedMoveY[idx] = 0f
            }
        }

        // Action / Shoot / Jump / Reaction Tap triggering logic
        when (game.mechanicGroup) {
            MechanicGroup.REACTION_TAP -> {
                if (reactionSignalActive) {
                    val tapChance = when (difficulty) {
                        BotDifficulty.EASY -> 0.14f
                        BotDifficulty.NORMAL -> 0.28f
                        BotDifficulty.HARD -> 0.55f
                    }
                    if (Random.nextFloat() < tapChance) {
                        triggerPrimary = true
                        holdPrimary = true
                    }
                }
            }

            MechanicGroup.CIRCUIT_RACING, MechanicGroup.HIGHWAY_DODGE -> {
                holdPrimary = true // Gas pedal
                if (abs(cachedMoveX[idx]) > 0.75f && abs(cachedMoveY[idx]) > 0.75f && difficulty == BotDifficulty.HARD) {
                    holdSecondary = Random.nextFloat() < 0.15f // Light corner brake
                }
            }

            MechanicGroup.HAZARD_DODGE, MechanicGroup.SAFE_ZONE_SURVIVAL -> {
                // Jump if shockwave or laser is close
                val wave = entities.firstOrNull {
                    it.active && (it.kind == EntityKind.SHOCKWAVE_RING || it.kind == EntityKind.LASER_WALL)
                }
                if (wave != null && Random.nextFloat() < (if (difficulty == BotDifficulty.HARD) 0.45f else 0.22f)) {
                    triggerPrimary = true
                    holdPrimary = true
                }
            }

            else -> {
                val fireChance = when (difficulty) {
                    BotDifficulty.EASY -> 0.08f
                    BotDifficulty.NORMAL -> 0.16f
                    BotDifficulty.HARD -> 0.28f
                }
                if (Random.nextFloat() < fireChance) {
                    triggerPrimary = true
                    holdPrimary = true
                }
                if (Random.nextFloat() < fireChance * 0.4f) {
                    triggerSecondary = true
                    holdSecondary = true
                }
            }
        }

        return PlayerInputState(
            moveX = cachedMoveX[idx],
            moveY = cachedMoveY[idx],
            primaryPressed = holdPrimary,
            secondaryPressed = holdSecondary,
            primaryJustTriggered = triggerPrimary,
            secondaryJustTriggered = triggerSecondary
        )
    }
}
