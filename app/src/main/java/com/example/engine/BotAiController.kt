package com.example.engine

import com.example.model.BotDifficulty
import com.example.model.ControlScheme
import com.example.model.GameSpec
import com.example.model.MechanicGroup
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

class BotAiController(
    private val difficulty: BotDifficulty
) {
    // Periodic decision timers per bot slot (never 0ms!)
    private val decisionTimers = FloatArray(4) { idx ->
        sampleReactionDelay() * (0.5f + idx * 0.15f)
    }

    // Separate reaction delay timer when a sudden signal (e.g., GREEN light or close threat) appears
    private val signalReactionTimers = FloatArray(4) { -1f }
    private val wasSignalActive = BooleanArray(4) { false }

    private val cachedMoveX = FloatArray(4) { 0f }
    private val cachedMoveY = FloatArray(4) { 0f }
    private val cachedWantPrimary = BooleanArray(4) { false }
    private val cachedWantSecondary = BooleanArray(4) { false }

    private fun sampleReactionDelay(): Float {
        val minD = difficulty.minReactionDelaySec
        val maxD = difficulty.maxReactionDelaySec
        return minD + Random.nextFloat() * (maxD - minD)
    }

    fun sampleSpeedFactor(): Float {
        val minS = difficulty.minSpeedScale
        val maxS = difficulty.maxSpeedScale
        return minS + Random.nextFloat() * (maxS - minS)
    }

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
        if (bot.eliminated || bot.fallingTimer > 0f) return PlayerInputState()

        val aimJitterRad = when (difficulty) {
            BotDifficulty.EASY -> 0.50f
            BotDifficulty.NORMAL -> 0.22f
            BotDifficulty.HARD -> 0.05f
        }
        val mistakeProbability = when (difficulty) {
            BotDifficulty.EASY -> 0.30f
            BotDifficulty.NORMAL -> 0.14f
            BotDifficulty.HARD -> 0.03f
        }

        decisionTimers[idx] -= dt
        var triggerPrimary = false
        var triggerSecondary = false
        var holdPrimary = false
        var holdSecondary = false

        // Natural reaction delay for sudden signal games (Reaction Test, Quick Draw, Speed Tap, etc.)
        if (game.mechanicGroup == MechanicGroup.REACTION_TAP) {
            if (reactionSignalActive && !wasSignalActive[idx]) {
                wasSignalActive[idx] = true
                signalReactionTimers[idx] = sampleReactionDelay()
            } else if (!reactionSignalActive) {
                wasSignalActive[idx] = false
                signalReactionTimers[idx] = -1f
            }

            if (reactionSignalActive && signalReactionTimers[idx] >= 0f) {
                signalReactionTimers[idx] -= dt
                if (signalReactionTimers[idx] <= 0f) {
                    triggerPrimary = true
                    holdPrimary = true
                    // Reset for next tap after human-like cadence delay
                    signalReactionTimers[idx] = sampleReactionDelay()
                }
            } else if (!reactionSignalActive && difficulty == BotDifficulty.EASY && Random.nextFloat() < 0.004f) {
                // Easy bots can occasionally false-start!
                triggerPrimary = true
            }
            return PlayerInputState(
                moveX = 0f,
                moveY = 0f,
                primaryPressed = holdPrimary,
                secondaryPressed = false,
                primaryJustTriggered = triggerPrimary,
                secondaryJustTriggered = false
            )
        }

        // Periodic decision evaluation after natural reaction delay (500-900ms Easy, 250-500ms Normal, 120-300ms Hard)
        if (decisionTimers[idx] <= 0f) {
            decisionTimers[idx] = sampleReactionDelay()
            cachedWantPrimary[idx] = false
            cachedWantSecondary[idx] = false

            var targetX = ARENA_CENTER
            var targetY = ARENA_CENTER
            val makeMistake = Random.nextFloat() < mistakeProbability

            when (game.mechanicGroup) {
                MechanicGroup.BRAWL_KNOCKBACK -> {
                    val ring = entities.firstOrNull { it.active && it.kind == EntityKind.SAFE_ZONE_RING }
                    val ringRadius = ring?.radius ?: 360f
                    val distFromCenter = hypot(bot.x - ARENA_CENTER, bot.y - ARENA_CENTER)
                    val nearestRival = allPlayers
                        .filter { it.id != bot.id && !it.eliminated && it.fallingTimer <= 0f }
                        .minByOrNull { hypot(it.x - bot.x, it.y - bot.y) }

                    if (distFromCenter > ringRadius * 0.72f && !makeMistake) {
                        // Recover toward platform center so bot doesn't walk off edge
                        targetX = ARENA_CENTER
                        targetY = ARENA_CENTER
                        if (distFromCenter > ringRadius * 0.85f) {
                            cachedWantSecondary[idx] = true // Dash/Dodge back to safety
                        }
                    } else if (nearestRival != null) {
                        if (difficulty == BotDifficulty.HARD) {
                            // Flank slightly inside the rival to push them outward toward the ring edge!
                            val rivalToCenterDist = hypot(nearestRival.x - ARENA_CENTER, nearestRival.y - ARENA_CENTER).coerceAtLeast(1f)
                            val outX = (nearestRival.x - ARENA_CENTER) / rivalToCenterDist
                            val outY = (nearestRival.y - ARENA_CENTER) / rivalToCenterDist
                            targetX = nearestRival.x - outX * 24f
                            targetY = nearestRival.y - outY * 24f
                        } else {
                            targetX = nearestRival.x
                            targetY = nearestRival.y
                        }
                        val dRival = hypot(nearestRival.x - bot.x, nearestRival.y - bot.y)
                        if (dRival < 115f) {
                            cachedWantPrimary[idx] = true // Attack / Push!
                        } else if (dRival in 140f..250f && difficulty != BotDifficulty.EASY) {
                            cachedWantSecondary[idx] = Random.nextFloat() < 0.45f
                        }
                    }
                }

                MechanicGroup.COLLECT_AND_RETURN, MechanicGroup.BRIDGE_BUILDER -> {
                    if (bot.carryingItem > 0) {
                        if (game.mechanicGroup == MechanicGroup.BRIDGE_BUILDER) {
                            targetX = 200f + idx * 200f
                            targetY = 130f
                        } else {
                            targetX = bot.basePos.first
                            targetY = bot.basePos.second
                        }
                        cachedWantSecondary[idx] = !makeMistake
                    } else {
                        val candidates = entities.filter {
                            it.active && (it.kind == EntityKind.CRYSTAL ||
                                it.kind == EntityKind.TREASURE_CHEST ||
                                it.kind == EntityKind.FLAG ||
                                it.kind == EntityKind.BRIDGE_PLANK ||
                                it.kind == EntityKind.CRATE_BOX) &&
                                (it.ownerId == -1 || it.ownerId == idx)
                        }
                        val chosen = if (makeMistake && candidates.size > 1) {
                            candidates.random()
                        } else {
                            candidates.minByOrNull { hypot(it.x - bot.x, it.y - bot.y) }
                        }
                        if (chosen != null) {
                            targetX = chosen.x
                            targetY = chosen.y
                            if (hypot(chosen.x - bot.x, chosen.y - bot.y) < 90f) {
                                cachedWantPrimary[idx] = true
                            }
                        }
                    }
                }

                MechanicGroup.BOMB_DODGE_KICK -> {
                    val nearestBomb = entities.filter { it.active && it.kind == EntityKind.BOMB }
                        .minByOrNull { hypot(it.x - bot.x, it.y - bot.y) }
                    if (nearestBomb != null) {
                        val d = hypot(nearestBomb.x - bot.x, nearestBomb.y - bot.y)
                        if (d < 115f && nearestBomb.timer > 0.9f && !makeMistake) {
                            // Kick the bomb away!
                            cachedWantPrimary[idx] = true
                        }
                        if (d < 240f) {
                            val dx = bot.x - nearestBomb.x
                            val dy = bot.y - nearestBomb.y
                            targetX = (bot.x + dx * 1.8f).coerceIn(130f, 870f)
                            targetY = (bot.y + dy * 1.8f).coerceIn(130f, 870f)
                            if (d < 150f && nearestBomb.timer < 1.1f) {
                                cachedWantSecondary[idx] = true // Dodge roll!
                            }
                        } else {
                            targetX = ARENA_CENTER + cos(elapsedSec + idx) * 200f
                            targetY = ARENA_CENTER + sin(elapsedSec + idx) * 200f
                        }
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
                        val lead = if (difficulty == BotDifficulty.HARD && !makeMistake) 0.22f else 0f
                        targetX = target.x + target.vx * lead
                        targetY = target.y + target.vy * lead
                        cachedWantSecondary[idx] = true // Rotate/Lock turret aim
                        cachedWantPrimary[idx] = !makeMistake || Random.nextFloat() < 0.5f
                    } else {
                        val rival = allPlayers.filter { it.id != bot.id && !it.eliminated }
                            .minByOrNull { hypot(it.x - bot.x, it.y - bot.y) }
                        if (rival != null) {
                            targetX = rival.x
                            targetY = rival.y
                            cachedWantSecondary[idx] = true
                            cachedWantPrimary[idx] = true
                        }
                    }
                }

                MechanicGroup.SAFE_ZONE_SURVIVAL -> {
                    val safeRing = entities.firstOrNull { it.active && it.kind == EntityKind.SAFE_ZONE_RING }
                    if (safeRing != null) {
                        targetX = safeRing.x
                        targetY = safeRing.y
                        val dist = hypot(safeRing.x - bot.x, safeRing.y - bot.y)
                        if (dist > safeRing.radius * 0.85f && safeRing.timer < 1.5f && !makeMistake) {
                            cachedWantSecondary[idx] = true // Dash into safe zone
                        } else if (dist < safeRing.radius * 0.65f) {
                            // Inside safe zone: try to bump nearby rivals out
                            val rivalInside = allPlayers.firstOrNull {
                                it.id != bot.id && !it.eliminated && hypot(it.x - safeRing.x, it.y - safeRing.y) < safeRing.radius
                            }
                            if (rivalInside != null && !makeMistake) {
                                targetX = rivalInside.x
                                targetY = rivalInside.y
                                if (hypot(rivalInside.x - bot.x, rivalInside.y - bot.y) < 100f) {
                                    cachedWantPrimary[idx] = true
                                }
                            }
                        }
                    }
                }

                MechanicGroup.HAZARD_DODGE -> {
                    val hazard = entities.filter {
                        it.active && (it.kind == EntityKind.METEOR_WARNING ||
                            it.kind == EntityKind.SEEKER_ORB ||
                            it.kind == EntityKind.LASER_WALL ||
                            it.kind == EntityKind.SHOCKWAVE_RING)
                    }.minByOrNull { hypot(it.x - bot.x, it.y - bot.y) }
                    val star = entities.filter { it.active && it.kind == EntityKind.CRYSTAL }
                        .minByOrNull { hypot(it.x - bot.x, it.y - bot.y) }

                    if (hazard != null && hypot(hazard.x - bot.x, hazard.y - bot.y) < 220f && !makeMistake) {
                        targetX = (bot.x + (bot.x - hazard.x) * 1.7f).coerceIn(130f, 870f)
                        targetY = (bot.y + (bot.y - hazard.y) * 1.7f).coerceIn(130f, 870f)
                        if (hypot(hazard.x - bot.x, hazard.y - bot.y) < 140f) {
                            cachedWantPrimary[idx] = true // Jump over hazard!
                        }
                    } else if (star != null) {
                        targetX = star.x
                        targetY = star.y
                    } else {
                        targetX = ARENA_CENTER + cos(elapsedSec + idx) * 210f
                        targetY = ARENA_CENTER + sin(elapsedSec + idx) * 210f
                    }
                }

                MechanicGroup.TAG_PASS_CURSE -> {
                    val isRingKeepAway = game.variantIndex == 1
                    val isHolder = bot.isTaggedOrCursed
                    val shouldChase = (isHolder && !isRingKeepAway) || (!isHolder && isRingKeepAway)
                    if (shouldChase) {
                        val targetPlayer = if (isRingKeepAway) {
                            allPlayers.firstOrNull { it.isTaggedOrCursed && !it.eliminated }
                        } else {
                            allPlayers.filter { it.id != bot.id && !it.eliminated }
                                .minByOrNull { hypot(it.x - bot.x, it.y - bot.y) }
                        }
                        if (targetPlayer != null) {
                            targetX = targetPlayer.x
                            targetY = targetPlayer.y
                            if (hypot(targetPlayer.x - bot.x, targetPlayer.y - bot.y) < 150f && !makeMistake) {
                                cachedWantSecondary[idx] = true
                            }
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
                    val gates = entities.filter { it.active && it.kind == EntityKind.CHECKPOINT_GATE }
                    val gate = gates.firstOrNull { it.value == (bot.progressSteps % gates.size.coerceAtLeast(1)) }
                        ?: gates.firstOrNull()
                    if (gate != null) {
                        targetX = gate.x
                        targetY = gate.y
                        cachedWantPrimary[idx] = true // Hold Accelerator
                    }
                }

                MechanicGroup.HIGHWAY_DODGE -> {
                    val laneCenter = 180f + idx * 210f
                    val carAhead = entities.filter {
                        it.active && it.kind == EntityKind.HIGHWAY_CAR &&
                            abs(it.x - bot.x) < 92f && it.y < bot.y && (bot.y - it.y) < 270f
                    }.minByOrNull { bot.y - it.y }
                    targetX = if (carAhead != null && !makeMistake) {
                        if (bot.x > ARENA_CENTER) bot.x - 135f else bot.x + 135f
                    } else {
                        laneCenter
                    }
                    targetY = 260f
                    cachedWantPrimary[idx] = true
                }

                MechanicGroup.BALL_SPORTS, MechanicGroup.PENALTY_DUEL, MechanicGroup.BASKET_SHOOT -> {
                    val ball = entities.firstOrNull {
                        it.active && (it.kind == EntityKind.BALL_SOCCER ||
                            it.kind == EntityKind.BALL_BASKET ||
                            it.kind == EntityKind.PUCK_HOCKEY)
                    }
                    if (ball != null) {
                        if (difficulty == BotDifficulty.HARD && !makeMistake) {
                            // Position slightly behind the ball relative to target goal
                            val goalY = if (bot.basePos.second < ARENA_CENTER) 890f else 110f
                            val dirY = if (goalY > ball.y) -24f else 24f
                            targetX = ball.x
                            targetY = (ball.y + dirY).coerceIn(110f, 890f)
                        } else {
                            targetX = ball.x
                            targetY = ball.y
                        }
                        if (hypot(ball.x - bot.x, ball.y - bot.y) < 95f) {
                            cachedWantPrimary[idx] = true // Kick / Shoot!
                        }
                    }
                }

                MechanicGroup.PADDLE_DEFENSE -> {
                    val puck = entities.firstOrNull {
                        it.active && (it.kind == EntityKind.PUCK_HOCKEY ||
                            it.kind == EntityKind.BALL_TENNIS ||
                            it.kind == EntityKind.BALL_SOCCER)
                    }
                    if (puck != null) {
                        targetX = puck.x.coerceIn(170f, 830f)
                        targetY = bot.basePos.second
                        if (hypot(puck.x - bot.x, puck.y - bot.y) < 105f) {
                            cachedWantPrimary[idx] = true
                        }
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
                        cachedWantPrimary[idx] = true
                    }
                }

                MechanicGroup.TILE_PUZZLE, MechanicGroup.TERRITORY_PAINT -> {
                    if (grid.isNotEmpty()) {
                        val candidates = grid.filter { it.ownerId != idx && it.state != 2 && it.state != 3 }
                        val chosen = if (makeMistake && candidates.size > 2) {
                            candidates.random()
                        } else {
                            candidates.minByOrNull {
                                val cx = (it.col + 0.5f) * (ARENA_SIZE / gridSize)
                                val cy = (it.row + 0.5f) * (ARENA_SIZE / gridSize)
                                hypot(cx - bot.x, cy - bot.y)
                            }
                        }
                        if (chosen != null) {
                            targetX = (chosen.col + 0.5f) * (ARENA_SIZE / gridSize)
                            targetY = (chosen.row + 0.5f) * (ARENA_SIZE / gridSize)
                            cachedWantPrimary[idx] = true
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
                    val pads = entities.filter { it.active && it.kind == EntityKind.QUIZ_PAD }
                    val correctPad = pads.firstOrNull { it.value == promptTargetValue }
                    val chosen = if (makeMistake && pads.isNotEmpty()) {
                        pads.random()
                    } else {
                        correctPad ?: pads.firstOrNull()
                    }
                    if (chosen != null) {
                        targetX = chosen.x
                        targetY = chosen.y
                        if (hypot(chosen.x - bot.x, chosen.y - bot.y) < 75f) {
                            cachedWantPrimary[idx] = true
                        }
                    }
                }

                MechanicGroup.ZONE_CAPTURE -> {
                    val zone = entities.filter { it.active && it.kind == EntityKind.CAPTURE_ZONE }
                        .minByOrNull { hypot(it.x - bot.x, it.y - bot.y) }
                    if (zone != null) {
                        targetX = zone.x
                        targetY = zone.y
                        val rivalInZone = allPlayers.firstOrNull {
                            it.id != bot.id && !it.eliminated && hypot(it.x - zone.x, it.y - zone.y) <= zone.radius
                        }
                        if (rivalInZone != null && hypot(rivalInZone.x - bot.x, rivalInZone.y - bot.y) < 110f) {
                            cachedWantPrimary[idx] = true // Knock challenger out of zone
                        }
                    }
                }

                MechanicGroup.WALL_TRAIL_TRAP -> {
                    if (grid.isNotEmpty()) {
                        val cleanCell = grid.filter { it.state == 0 && it.heat < 0.35f }
                            .minByOrNull {
                                val cx = (it.col + 0.5f) * (ARENA_SIZE / gridSize)
                                val cy = (it.row + 0.5f) * (ARENA_SIZE / gridSize)
                                val d = hypot(cx - bot.x, cy - bot.y)
                                if (d < 65f) 9999f else d
                            }
                        if (cleanCell != null) {
                            targetX = (cleanCell.col + 0.5f) * (ARENA_SIZE / gridSize)
                            targetY = (cleanCell.row + 0.5f) * (ARENA_SIZE / gridSize)
                        }
                    }
                }

                MechanicGroup.SHAPE_MORPH_CHASE -> {
                    val prey = allPlayers.filter {
                        it.id != bot.id && !it.eliminated && bot.morphShape.beats(it.morphShape)
                    }.minByOrNull { hypot(it.x - bot.x, it.y - bot.y) }
                    val predator = allPlayers.filter {
                        it.id != bot.id && !it.eliminated && it.morphShape.beats(bot.morphShape)
                    }.minByOrNull { hypot(it.x - bot.x, it.y - bot.y) }

                    if (predator != null && hypot(predator.x - bot.x, predator.y - bot.y) < 220f && !makeMistake) {
                        targetX = (bot.x + (bot.x - predator.x) * 1.5f).coerceIn(130f, 870f)
                        targetY = (bot.y + (bot.y - predator.y) * 1.5f).coerceIn(130f, 870f)
                        cachedWantSecondary[idx] = true
                    } else if (prey != null) {
                        targetX = prey.x
                        targetY = prey.y
                        if (hypot(prey.x - bot.x, prey.y - bot.y) < 120f) {
                            cachedWantPrimary[idx] = true
                        }
                    }
                }

                MechanicGroup.REACTION_TAP -> {
                    targetX = bot.x
                    targetY = bot.y
                }
            }

            val dx = targetX - bot.x
            val dy = targetY - bot.y
            val dist = hypot(dx, dy)
            if (dist > 10f) {
                val baseAngle = atan2(dy, dx)
                val noisyAngle = baseAngle + (Random.nextFloat() - 0.5f) * 2f * aimJitterRad
                // Notice: speed scaling is applied via bot.botSpeedFactor in GameEngine so movement vector stays normalized [-1..1]
                cachedMoveX[idx] = cos(noisyAngle)
                cachedMoveY[idx] = sin(noisyAngle)
            } else {
                cachedMoveX[idx] = 0f
                cachedMoveY[idx] = 0f
            }

            triggerPrimary = cachedWantPrimary[idx]
            triggerSecondary = cachedWantSecondary[idx]
        }

        val scheme = game.effectiveControlScheme
        if (scheme == ControlScheme.RACING_CONTROLS) {
            holdPrimary = true
            if (abs(cachedMoveX[idx]) > 0.72f && abs(cachedMoveY[idx]) > 0.72f && difficulty == BotDifficulty.HARD) {
                holdSecondary = Random.nextFloat() < 0.16f
            }
        } else {
            holdPrimary = triggerPrimary
            holdSecondary = triggerSecondary
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
