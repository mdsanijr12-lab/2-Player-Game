package com.example.engine

import androidx.compose.ui.graphics.Color
import com.example.audio.SfxType
import com.example.audio.SoundEngine
import com.example.model.ArenaTheme
import com.example.model.ControlScheme
import com.example.model.MatchConfig
import com.example.model.MatchResult
import com.example.model.MechanicGroup
import com.example.model.PlayerId
import com.example.model.PlayerResult
import com.example.model.WinRule
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

class GameEngine(
    val config: MatchConfig,
    private val soundEngine: SoundEngine,
    private val controlSensitivity: Float,
    private val particleMultiplier: Float
) {
    val game = config.game
    private val botAi = BotAiController(config.botDifficulty)
    private var nextUidCounter = 1

    val players: List<PlayerEntity> = GameLevelBuilder.createInitialPlayers(config)
    val entities: MutableList<WorldEntity> = mutableListOf()
    val grid: MutableList<GridCell> = mutableListOf()
    val particles: MutableList<ParticleEffect> = mutableListOf()
    var gridSize: Int = 0
        private set

    private val humanInputs = Array(4) { PlayerInputState() }

    var remainingSeconds: Float = game.durationSeconds.toFloat()
        private set
    var elapsedSeconds: Float = 0f
        private set

    var hudBannerEn: String = ""
        private set
    var hudBannerBn: String = ""
        private set

    var promptTargetValue: Int = 0
        private set
    var reactionSignalActive: Boolean = false
        private set

    private var waveTimer: Float = 0f
    private var secondaryTimer: Float = 0f
    var matchResult: MatchResult? = null
        private set

    init {
        gridSize = GameLevelBuilder.populateInitialWorld(
            game = game,
            playerCount = players.size,
            entities = entities,
            grid = grid,
            nextUid = { nextUidCounter++ }
        )
        setupInitialPrompts()
    }

    private fun nextUid(): Int = nextUidCounter++

    private fun setupInitialPrompts() {
        when (game.mechanicGroup) {
            MechanicGroup.MATH_PATTERN_QUIZ -> generateNextQuizPrompt()
            MechanicGroup.SHAPE_MORPH_CHASE -> {
                hudBannerEn = "● beats ▲ | ▲ beats ■ | ■ beats ●"
                hudBannerBn = "● হারায় ▲ | ▲ হারায় ■ | ■ হারায় ●"
            }
            MechanicGroup.REACTION_TAP -> {
                hudBannerEn = "WAIT FOR GREEN SIGNAL, THEN STRIKE!"
                hudBannerBn = "সবুজ সিগন্যাল দেখে মুহূর্তেই STRIKE চাপুন!"
            }
            else -> {
                hudBannerEn = game.briefInstruction(com.example.model.AppLanguage.ENGLISH)
                hudBannerBn = game.briefInstruction(com.example.model.AppLanguage.BANGLA)
            }
        }
    }

    private fun generateNextQuizPrompt() {
        val pads = entities.filter { it.kind == EntityKind.QUIZ_PAD }
        if (pads.isEmpty()) return
        when (game.variantIndex) {
            0 -> {
                val base = Random.nextInt(1, 50)
                val order = pads.indices.shuffled()
                val minVal = base
                promptTargetValue = minVal
                pads.forEachIndexed { idx, pad ->
                    val v = base + order[idx]
                    pad.value = v
                    pad.label = "$v"
                }
                hudBannerEn = "Claim Lowest Number: $minVal"
                hudBannerBn = "সর্বনিম্ন সংখ্যাটি নিন: $minVal"
            }
            1 -> {
                val shapes = listOf("●", "▲", "◆", "■")
                val targetIdx = Random.nextInt(shapes.size)
                promptTargetValue = targetIdx
                pads.forEachIndexed { idx, pad ->
                    pad.value = idx
                    pad.label = shapes[idx]
                }
                hudBannerEn = "Match Target Shape: ${shapes[targetIdx]}"
                hudBannerBn = "টার্গেট শেপ মেলান: ${shapes[targetIdx]}"
            }
            2 -> {
                val a = Random.nextInt(3, 12)
                val b = Random.nextInt(3, 10)
                val ans = a * b
                promptTargetValue = ans
                val wrong = listOf(ans + 4, ans - 3, ans + 9)
                val options = (listOf(ans) + wrong).shuffled()
                pads.forEachIndexed { idx, pad ->
                    val v = options[idx % options.size]
                    pad.value = v
                    pad.label = "$v"
                }
                hudBannerEn = "Solve: $a × $b = ?"
                hudBannerBn = "সমাধান করুন: $a × $b = ?"
            }
            else -> {
                val arrows = listOf("⬆ N", "➡ E", "⬇ S", "⬅ W")
                val targetIdx = Random.nextInt(arrows.size)
                promptTargetValue = targetIdx
                pads.forEachIndexed { idx, pad ->
                    pad.value = idx
                    pad.label = arrows[idx]
                }
                hudBannerEn = "Rush to Gate: ${arrows[targetIdx]}"
                hudBannerBn = "নির্দেশিত গেটে যান: ${arrows[targetIdx]}"
            }
        }
    }

    fun setHumanMovement(playerIndex: Int, mx: Float, my: Float) {
        if (playerIndex !in 0..3) return
        humanInputs[playerIndex].moveX = (mx * controlSensitivity).coerceIn(-1f, 1f)
        humanInputs[playerIndex].moveY = (my * controlSensitivity).coerceIn(-1f, 1f)
    }

    fun setHumanPrimaryButton(playerIndex: Int, pressed: Boolean) {
        if (playerIndex !in 0..3) return
        val inp = humanInputs[playerIndex]
        if (pressed && !inp.primaryPressed) {
            inp.primaryJustTriggered = true
        }
        inp.primaryPressed = pressed
    }

    fun setHumanSecondaryButton(playerIndex: Int, pressed: Boolean) {
        if (playerIndex !in 0..3) return
        val inp = humanInputs[playerIndex]
        if (pressed && !inp.secondaryPressed) {
            inp.secondaryJustTriggered = true
        }
        inp.secondaryPressed = pressed
    }

    fun step(rawDt: Float) {
        if (matchResult != null) return
        val dt = rawDt.coerceIn(0.001f, 0.05f)
        elapsedSeconds += dt
        remainingSeconds = (remainingSeconds - dt).coerceAtLeast(0f)
        waveTimer += dt
        secondaryTimer += dt

        val frameInputs = players.map { p ->
            if (p.isBot) {
                botAi.computeBotInput(
                    bot = p,
                    allPlayers = players,
                    entities = entities,
                    grid = grid,
                    gridSize = gridSize,
                    game = game,
                    elapsedSec = elapsedSeconds,
                    dt = dt,
                    promptTargetValue = promptTargetValue,
                    reactionSignalActive = reactionSignalActive
                )
            } else {
                val h = humanInputs[p.id.index]
                val copy = h.copy()
                h.primaryJustTriggered = false
                h.secondaryJustTriggered = false
                copy
            }
        }

        updatePlayers(dt, frameInputs)
        updateGameMechanics(dt)
        updateParticles(dt)
        checkMatchCompletion()
    }

    private fun updatePlayers(dt: Float, inputs: List<PlayerInputState>) {
        val isIce = game.arenaTheme == ArenaTheme.ICE_RINK
        val scheme = game.effectiveControlScheme
        val baseSpeed = when (game.mechanicGroup) {
            MechanicGroup.CIRCUIT_RACING, MechanicGroup.HIGHWAY_DODGE -> 315f
            MechanicGroup.REACTION_TAP -> 0f
            else -> 275f
        }

        players.forEachIndexed { idx, p ->
            if (p.eliminated) return@forEachIndexed

            // Handle fall-off-platform animation in Push/Fall Arena
            if (p.fallingTimer > 0f) {
                p.fallingTimer -= dt
                p.x += p.vx * dt * 0.4f
                p.y += p.vy * dt * 0.4f
                if (p.fallingTimer <= 0f) {
                    p.fallingTimer = 0f
                    if (game.winRule == WinRule.LAST_SURVIVING) {
                        p.eliminated = true
                    } else {
                        // Respawn back near center platform
                        p.x = ARENA_CENTER + (Random.nextFloat() - 0.5f) * 140f
                        p.y = ARENA_CENTER + (Random.nextFloat() - 0.5f) * 140f
                        p.vx = 0f
                        p.vy = 0f
                        p.respawnTimer = 0.85f
                    }
                }
                return@forEachIndexed
            }

            p.surviveTimeSec += dt
            if (p.respawnTimer > 0f) p.respawnTimer = (p.respawnTimer - dt).coerceAtLeast(0f)
            if (p.actionCooldown > 0f) p.actionCooldown = (p.actionCooldown - dt).coerceAtLeast(0f)
            if (p.dashCooldown > 0f) p.dashCooldown = (p.dashCooldown - dt).coerceAtLeast(0f)
            if (p.dashTimer > 0f) p.dashTimer = (p.dashTimer - dt).coerceAtLeast(0f)
            if (p.dodgeTimer > 0f) p.dodgeTimer = (p.dodgeTimer - dt).coerceAtLeast(0f)
            if (p.attackAnimTimer > 0f) p.attackAnimTimer = (p.attackAnimTimer - dt).coerceAtLeast(0f)
            if (p.shotCharge > 0f) p.shotCharge = (p.shotCharge - dt * 1.8f).coerceAtLeast(0f)

            // Jump Z-axis gravity physics
            if (p.jumpZ > 0f || p.jumpVz != 0f) {
                p.jumpZ += p.jumpVz * dt
                p.jumpVz -= 1850f * dt
                if (p.jumpZ <= 0f) {
                    p.jumpZ = 0f
                    p.jumpVz = 0f
                }
            }

            val inp = inputs[idx]
            val carrySlowdown = if (p.carryingItem > 0 && game.variantIndex == 1) 0.78f else 1.0f
            val dashMultiplier = if (p.dashTimer > 0f) 2.05f else 1.0f
            // Fair Bot Speed Scaling: Easy (75%-90%), Normal (90%-105%), Hard (100%-115%)
            val speedScale = if (p.isBot) p.botSpeedFactor else 1.0f
            val effectiveSpeed = baseSpeed * speedScale * carrySlowdown * dashMultiplier

            if (scheme == ControlScheme.RACING_CONTROLS) {
                val inputMag = hypot(inp.moveX, inp.moveY)
                val gas = if (inp.primaryPressed) 1.05f else if (inputMag > 0.2f) 0.85f else 0.1f
                val brake = if (inp.secondaryPressed) 0.35f else 1.0f
                if (inputMag > 0.12f) {
                    val targetAngle = atan2(inp.moveY, inp.moveX)
                    p.angleRad = targetAngle
                }
                val throttle = if (inp.primaryPressed || inputMag > 0.15f) inputMag.coerceAtLeast(0.55f) else 0f
                val targetVx = cos(p.angleRad) * effectiveSpeed * gas * brake * throttle
                val targetVy = sin(p.angleRad) * effectiveSpeed * gas * brake * throttle
                val lerp = if (isIce) 3.0f * dt else 9.0f * dt
                p.vx += (targetVx - p.vx) * lerp.coerceIn(0f, 1f)
                p.vy += (targetVy - p.vy) * lerp.coerceIn(0f, 1f)
            } else if (baseSpeed > 0f) {
                val targetVx = inp.moveX * effectiveSpeed
                val targetVy = inp.moveY * effectiveSpeed
                val lerp = if (isIce) 2.8f * dt else 11.0f * dt
                p.vx += (targetVx - p.vx) * lerp.coerceIn(0f, 1f)
                p.vy += (targetVy - p.vy) * lerp.coerceIn(0f, 1f)
                if (hypot(inp.moveX, inp.moveY) > 0.14f) {
                    p.angleRad = atan2(inp.moveY, inp.moveX)
                    if (scheme != ControlScheme.TANK_CONTROLS) {
                        p.turretAngleRad = p.angleRad
                    } else if (!inp.secondaryPressed) {
                        p.turretAngleRad = p.angleRad
                    }
                }
            }

            // Secondary Button Handling per Genre ControlScheme
            if (inp.secondaryJustTriggered) {
                handlePlayerSecondaryAction(p, scheme)
            }

            // Primary Button Handling per Genre ControlScheme
            if (inp.primaryJustTriggered) {
                handlePlayerPrimaryAction(p, scheme)
            }

            var nextX = (p.x + p.vx * dt).coerceIn(72f, 928f)
            var nextY = (p.y + p.vy * dt).coerceIn(72f, 928f)

            // Circuit Racing Inner Island Boundary Collision (so cars must drive around the track!)
            if (game.mechanicGroup == MechanicGroup.CIRCUIT_RACING) {
                val inInnerIsland = nextX in 325f..675f && nextY in 325f..675f
                if (inInnerIsland) {
                    val dxLeft = abs(nextX - 325f)
                    val dxRight = abs(675f - nextX)
                    val dyTop = abs(nextY - 325f)
                    val dyBottom = abs(675f - nextY)
                    val minEdge = minOf(dxLeft, dxRight, dyTop, dyBottom)
                    when (minEdge) {
                        dxLeft -> { nextX = 322f; p.vx = -abs(p.vx) * 0.45f }
                        dxRight -> { nextX = 678f; p.vx = abs(p.vx) * 0.45f }
                        dyTop -> { nextY = 322f; p.vy = -abs(p.vy) * 0.45f }
                        else -> { nextY = 678f; p.vy = abs(p.vy) * 0.45f }
                    }
                }
            }

            // Maze Wall Collisions
            if (gridSize > 0 && game.mechanicGroup == MechanicGroup.MAZE_RUNNER) {
                val cellStep = ARENA_SIZE / gridSize
                val c = (nextX / cellStep).toInt().coerceIn(0, gridSize - 1)
                val r = (nextY / cellStep).toInt().coerceIn(0, gridSize - 1)
                val cell = grid.getOrNull(r * gridSize + c)
                if (cell != null && cell.state == 3) {
                    p.vx = -p.vx * 0.5f
                    p.vy = -p.vy * 0.5f
                } else {
                    p.x = nextX
                    p.y = nextY
                }
            } else {
                p.x = nextX
                p.y = nextY
            }
        }

        // Player-to-Player Collisions & Momentum Knockback
        for (i in 0 until players.size) {
            for (j in i + 1 until players.size) {
                val a = players[i]
                val b = players[j]
                if (a.eliminated || b.eliminated || a.fallingTimer > 0f || b.fallingTimer > 0f) continue
                val dx = b.x - a.x
                val dy = b.y - a.y
                val dist = hypot(dx, dy)
                val minDist = a.radius + b.radius
                if (dist < minDist && dist > 0.001f) {
                    val nx = dx / dist
                    val ny = dy / dist
                    val overlap = (minDist - dist) * 0.5f
                    a.x = (a.x - nx * overlap).coerceIn(65f, 935f)
                    a.y = (a.y - ny * overlap).coerceIn(65f, 935f)
                    b.x = (b.x + nx * overlap).coerceIn(65f, 935f)
                    b.y = (b.y + ny * overlap).coerceIn(65f, 935f)

                    val bumpForce = if (game.mechanicGroup == MechanicGroup.BRAWL_KNOCKBACK) 390f else 175f
                    val aBoost = if (a.dashTimer > 0f || a.attackAnimTimer > 0f) 1.85f else 1f
                    val bBoost = if (b.dashTimer > 0f || b.attackAnimTimer > 0f) 1.85f else 1f
                    a.vx -= nx * bumpForce * bBoost
                    a.vy -= ny * bumpForce * bBoost
                    b.vx += nx * bumpForce * aBoost
                    b.vy += ny * bumpForce * aBoost

                    onPlayersCollide(a, b)
                }
            }
        }
    }

    private fun handlePlayerSecondaryAction(p: PlayerEntity, scheme: ControlScheme) {
        when (scheme) {
            ControlScheme.TANK_CONTROLS -> {
                // AIM: Lock turret onto nearest target drone or enemy tank!
                val nearestTarget = entities.filter {
                    it.active && (it.kind == EntityKind.TARGET_DRONE ||
                        it.kind == EntityKind.BUBBLE ||
                        it.kind == EntityKind.BALLOON)
                }.minByOrNull { hypot(it.x - p.x, it.y - p.y) }
                val nearestRival = players.filter { it.id != p.id && !it.eliminated }
                    .minByOrNull { hypot(it.x - p.x, it.y - p.y) }

                val tx = nearestTarget?.x ?: nearestRival?.x
                val ty = nearestTarget?.y ?: nearestRival?.y
                if (tx != null && ty != null) {
                    p.turretAngleRad = atan2(ty - p.y, tx - p.x)
                } else {
                    p.turretAngleRad += (PI / 4).toFloat()
                }
                soundEngine.playSfx(SfxType.CLICK)
            }

            ControlScheme.FIGHTING_CONTROLS -> {
                // DODGE: Evasive sidestep roll with brief invulnerability
                if (p.dashCooldown <= 0f) {
                    p.dodgeTimer = 0.30f
                    p.dashTimer = 0.20f
                    p.dashCooldown = 1.0f
                    soundEngine.playSfx(SfxType.JUMP)
                    spawnBurst(p.x, p.y, Color.White, 6)
                }
            }

            ControlScheme.RACING_CONTROLS -> {
                // BRAKE / DRIFT tap
                p.vx *= 0.72f
                p.vy *= 0.72f
            }

            else -> {
                // DASH / SPRINT / BOOST
                if (p.dashCooldown <= 0f) {
                    p.dashTimer = 0.24f
                    p.dashCooldown = 1.05f
                    soundEngine.playSfx(SfxType.JUMP)
                    spawnBurst(p.x, p.y, p.id.color, 6)
                }
            }
        }
    }

    private fun handlePlayerPrimaryAction(p: PlayerEntity, scheme: ControlScheme) {
        if (p.actionCooldown > 0f && game.mechanicGroup != MechanicGroup.REACTION_TAP) return

        when (scheme) {
            ControlScheme.JUMP_DODGE_CONTROLS, ControlScheme.JOYSTICK_JUMP -> {
                if (p.jumpZ <= 1f) {
                    p.jumpVz = 640f
                    p.jumpZ = 2f
                    p.actionCooldown = 0.42f
                    soundEngine.playSfx(SfxType.JUMP)
                    spawnBurst(p.x, p.y, p.id.color, 6)
                }
            }

            ControlScheme.TANK_CONTROLS, ControlScheme.JOYSTICK_SHOOT -> {
                p.actionCooldown = 0.34f
                p.attackAnimTimer = 0.18f
                soundEngine.playSfx(SfxType.SHOOT)
                val fireAngle = p.turretAngleRad
                // Tank chassis recoil
                p.vx -= cos(fireAngle) * 75f
                p.vy -= sin(fireAngle) * 75f
                entities.add(
                    WorldEntity(
                        uid = nextUid(),
                        kind = EntityKind.PROJECTILE,
                        x = p.x + cos(fireAngle) * (p.radius + 16f),
                        y = p.y + sin(fireAngle) * (p.radius + 16f),
                        vx = cos(fireAngle) * 650f,
                        vy = sin(fireAngle) * 650f,
                        radius = 12f,
                        ownerId = p.id.index,
                        timer = 1.4f,
                        color = p.id.color
                    )
                )
            }

            ControlScheme.SPORTS_CONTROLS -> {
                p.actionCooldown = 0.32f
                p.attackAnimTimer = 0.22f
                p.shotCharge = 1.0f
                soundEngine.playSfx(SfxType.SHOOT)

                var struckBall = false
                entities.filter {
                    it.active && (it.kind == EntityKind.BALL_SOCCER ||
                        it.kind == EntityKind.BALL_BASKET ||
                        it.kind == EntityKind.PUCK_HOCKEY ||
                        it.kind == EntityKind.BALL_TENNIS ||
                        it.kind == EntityKind.DISC_NEON ||
                        it.kind == EntityKind.DODGEBALL)
                }.forEach { ball ->
                    if (hypot(ball.x - p.x, ball.y - p.y) < p.radius + ball.radius + 54f) {
                        ball.vx = cos(p.angleRad) * 610f
                        ball.vy = sin(p.angleRad) * 610f
                        ball.ownerId = p.id.index
                        struckBall = true
                        spawnBurst(ball.x, ball.y, p.id.color, 10)
                    }
                }

                if (!struckBall) {
                    entities.add(
                        WorldEntity(
                            uid = nextUid(),
                            kind = EntityKind.PROJECTILE,
                            x = p.x + cos(p.angleRad) * (p.radius + 14f),
                            y = p.y + sin(p.angleRad) * (p.radius + 14f),
                            vx = cos(p.angleRad) * 580f,
                            vy = sin(p.angleRad) * 580f,
                            radius = 12f,
                            ownerId = p.id.index,
                            timer = 1.4f,
                            color = p.id.color
                        )
                    )
                }
            }

            ControlScheme.FIGHTING_CONTROLS,
            ControlScheme.PUSH_ARENA_CONTROLS,
            ControlScheme.PUZZLE_TACTICAL_CONTROLS,
            ControlScheme.JOYSTICK_ATTACK_DASH,
            ControlScheme.JOYSTICK_ACTION -> {
                p.actionCooldown = 0.38f
                p.attackAnimTimer = 0.24f
                soundEngine.playSfx(SfxType.HIT)
                spawnBurst(
                    p.x + cos(p.angleRad) * 28f,
                    p.y + sin(p.angleRad) * 28f,
                    p.id.color,
                    9
                )

                // Melee / Push Knockback Hit Detection against opponents
                val knockForce = if (scheme == ControlScheme.PUSH_ARENA_CONTROLS) 580f else 480f
                players.forEach { other ->
                    if (other.id != p.id && !other.eliminated && other.fallingTimer <= 0f && other.dodgeTimer <= 0f) {
                        val d = hypot(other.x - p.x, other.y - p.y)
                        if (d < 135f && d > 1f) {
                            val dirSign = if (game.variantIndex == 3 && game.mechanicGroup == MechanicGroup.BRAWL_KNOCKBACK) -1f else 1f
                            other.vx += ((other.x - p.x) / d) * knockForce * dirSign
                            other.vy += ((other.y - p.y) / d) * knockForce * dirSign
                            spawnBurst(other.x, other.y, Color.White, 8)
                        }
                    }
                }

                // Kick bombs away
                entities.filter { it.active && it.kind == EntityKind.BOMB }.forEach { bomb ->
                    val d = hypot(bomb.x - p.x, bomb.y - p.y)
                    if (d < 130f && d > 1f) {
                        bomb.vx = ((bomb.x - p.x) / d) * 520f
                        bomb.vy = ((bomb.y - p.y) / d) * 520f
                    }
                }

                // Territory / Puzzle board interaction
                if ((game.mechanicGroup == MechanicGroup.TERRITORY_PAINT || game.mechanicGroup == MechanicGroup.TILE_PUZZLE) && gridSize > 0) {
                    val step = ARENA_SIZE / gridSize
                    val pc = (p.x / step).toInt().coerceIn(0, gridSize - 1)
                    val pr = (p.y / step).toInt().coerceIn(0, gridSize - 1)
                    for (dr in -1..1) {
                        for (dc in -1..1) {
                            val nr = pr + dr
                            val nc = pc + dc
                            if (nr in 0 until gridSize && nc in 0 until gridSize) {
                                val cell = grid[nr * gridSize + nc]
                                if (cell.ownerId != p.id.index) {
                                    cell.ownerId = p.id.index
                                    if (game.mechanicGroup == MechanicGroup.TILE_PUZZLE) {
                                        p.score += 1
                                    }
                                }
                            }
                        }
                    }
                    if (game.mechanicGroup == MechanicGroup.TERRITORY_PAINT) {
                        recalculateTerritoryScores()
                    }
                }
            }

            ControlScheme.TAP_REACTION -> {
                if (reactionSignalActive) {
                    p.score += 2
                    soundEngine.playSfx(SfxType.SCORE)
                    spawnBurst(p.x, p.y, p.id.color, 12)
                    if (game.variantIndex in listOf(1, 3)) {
                        reactionSignalActive = false
                        entities.firstOrNull { it.kind == EntityKind.SAFE_ZONE_RING }?.let {
                            it.timer = 1.4f + Random.nextFloat() * 1.2f
                            it.color = Color(0xFFF59E0B)
                            it.label = "WAIT..."
                        }
                    }
                } else if (game.variantIndex in listOf(1, 3)) {
                    p.score = (p.score - 1).coerceAtLeast(0)
                    soundEngine.playSfx(SfxType.HIT)
                } else {
                    p.score += 1
                    soundEngine.playSfx(SfxType.CLICK)
                }
            }

            else -> {}
        }
    }

    private fun onPlayersCollide(a: PlayerEntity, b: PlayerEntity) {
        soundEngine.playSfx(SfxType.COLLISION)
        spawnBurst((a.x + b.x) * 0.5f, (a.y + b.y) * 0.5f, Color.White, 5)

        if (game.mechanicGroup == MechanicGroup.TAG_PASS_CURSE && a.respawnTimer <= 0f && b.respawnTimer <= 0f) {
            if (a.isTaggedOrCursed != b.isTaggedOrCursed) {
                a.isTaggedOrCursed = !a.isTaggedOrCursed
                b.isTaggedOrCursed = !b.isTaggedOrCursed
                a.respawnTimer = 0.75f
                b.respawnTimer = 0.75f
                soundEngine.playSfx(SfxType.SCORE)
            }
        } else if (game.mechanicGroup == MechanicGroup.SHAPE_MORPH_CHASE) {
            if (a.morphShape.beats(b.morphShape) && b.respawnTimer <= 0f) {
                a.score += 2
                b.respawnTimer = 1.0f
                soundEngine.playSfx(SfxType.SCORE)
                spawnBurst(b.x, b.y, a.id.color, 12)
            } else if (b.morphShape.beats(a.morphShape) && a.respawnTimer <= 0f) {
                b.score += 2
                a.respawnTimer = 1.0f
                soundEngine.playSfx(SfxType.SCORE)
                spawnBurst(a.x, a.y, b.id.color, 12)
            }
        }
    }

    private fun updateGameMechanics(dt: Float) {
        val iter = entities.iterator()
        while (iter.hasNext()) {
            val e = iter.next()
            if (!e.active) {
                iter.remove()
                continue
            }

            e.x += e.vx * dt
            e.y += e.vy * dt

            if (e.kind == EntityKind.PROJECTILE) {
                e.timer -= dt
                if (e.timer <= 0f || e.x !in 40f..960f || e.y !in 40f..960f) {
                    iter.remove()
                    continue
                }
                var hitSomething = false
                entities.filter {
                    it.active && (it.kind == EntityKind.TARGET_DRONE ||
                        it.kind == EntityKind.BUBBLE ||
                        it.kind == EntityKind.FRUIT ||
                        it.kind == EntityKind.BALLOON ||
                        it.kind == EntityKind.BOWLING_PIN ||
                        it.kind == EntityKind.GOLF_HOLE ||
                        it.kind == EntityKind.CARNIVAL_PEG)
                }.forEach { target ->
                    if (!hitSomething && hypot(target.x - e.x, target.y - e.y) < target.radius + e.radius + 10f) {
                        hitSomething = true
                        players.getOrNull(e.ownerId)?.let { it.score += target.value.coerceAtLeast(1) }
                        soundEngine.playSfx(SfxType.SCORE)
                        spawnBurst(target.x, target.y, e.color, 10)
                        if (target.kind != EntityKind.GOLF_HOLE && target.kind != EntityKind.CARNIVAL_PEG) {
                            target.x = Random.nextInt(180, 820).toFloat()
                            target.y = Random.nextInt(180, 820).toFloat()
                        }
                    }
                }
                if (!hitSomething) {
                    players.forEach { p ->
                        if (p.id.index != e.ownerId && !p.eliminated && p.dodgeTimer <= 0f &&
                            hypot(p.x - e.x, p.y - e.y) < p.radius + e.radius
                        ) {
                            hitSomething = true
                            players.getOrNull(e.ownerId)?.let { it.score += 1 }
                            p.vx += e.vx * 0.42f
                            p.vy += e.vy * 0.42f
                            soundEngine.playSfx(SfxType.HIT)
                            spawnBurst(p.x, p.y, e.color, 8)
                        }
                    }
                }
                if (hitSomething) {
                    iter.remove()
                    continue
                }
            } else if (e.kind == EntityKind.HIGHWAY_CAR) {
                if (e.y > 940f) {
                    e.y = 70f
                    players.filter { !it.eliminated }.forEach { it.score += 1 }
                }
                players.forEach { p ->
                    if (!p.eliminated && hypot(p.x - e.x, p.y - e.y) < p.radius + e.radius) {
                        p.score = (p.score - 2).coerceAtLeast(0)
                        p.y = 850f
                        soundEngine.playSfx(SfxType.COLLISION)
                        spawnBurst(p.x, p.y, Color(0xFFEF4444), 10)
                    }
                }
            } else {
                // Friction for sports balls
                if (e.kind == EntityKind.BALL_SOCCER || e.kind == EntityKind.BALL_BASKET) {
                    e.vx *= (1f - 0.45f * dt)
                    e.vy *= (1f - 0.45f * dt)
                }
                if (e.x < 90f || e.x > 910f) {
                    e.vx = -e.vx
                    e.x = e.x.coerceIn(90f, 910f)
                }
                if (e.y < 90f || e.y > 910f) {
                    e.vy = -e.vy
                    e.y = e.y.coerceIn(90f, 910f)
                }
            }
        }

        when (game.mechanicGroup) {
            MechanicGroup.BRAWL_KNOCKBACK -> {
                val ring = entities.firstOrNull { it.kind == EntityKind.SAFE_ZONE_RING }
                val ringRadius = ring?.radius ?: 360f
                players.forEach { p ->
                    if (p.eliminated || p.fallingTimer > 0f) return@forEach
                    val d = hypot(p.x - ARENA_CENTER, p.y - ARENA_CENTER)
                    if (d > ringRadius) {
                        // Start visible fall into the abyss!
                        val credit = players.filter { it.id != p.id && !it.eliminated && it.fallingTimer <= 0f }
                            .minByOrNull { hypot(it.x - p.x, it.y - p.y) }
                        credit?.let { it.score += 2 }
                        p.fallingTimer = 0.55f
                        soundEngine.playSfx(SfxType.EXPLOSION)
                        spawnBurst(p.x, p.y, p.id.color, 14)
                    }
                }
                entities.filter { it.kind == EntityKind.CRYSTAL }.forEach { c ->
                    players.forEach { p ->
                        if (!p.eliminated && p.fallingTimer <= 0f && hypot(p.x - c.x, p.y - c.y) < p.radius + c.radius) {
                            p.score += 2
                            c.x = Random.nextInt(220, 780).toFloat()
                            c.y = Random.nextInt(220, 780).toFloat()
                            soundEngine.playSfx(SfxType.SCORE)
                        }
                    }
                }
            }

            MechanicGroup.COLLECT_AND_RETURN -> {
                entities.filter {
                    it.kind == EntityKind.CRYSTAL ||
                        it.kind == EntityKind.TREASURE_CHEST ||
                        it.kind == EntityKind.FLAG ||
                        it.kind == EntityKind.CRATE_BOX
                }.forEach { item ->
                    players.forEach { p ->
                        if (!p.eliminated && p.carryingItem == 0 && hypot(p.x - item.x, p.y - item.y) < p.radius + item.radius + 8f) {
                            p.carryingItem = 1
                            item.x = Random.nextInt(260, 740).toFloat()
                            item.y = Random.nextInt(260, 740).toFloat()
                            soundEngine.playSfx(SfxType.CLICK)
                            spawnBurst(p.x, p.y, Color(0xFFFACC15), 8)
                        }
                    }
                }
                players.forEach { p ->
                    if (!p.eliminated && p.carryingItem > 0) {
                        val distBase = hypot(p.x - p.basePos.first, p.y - p.basePos.second)
                        if (distBase < 95f) {
                            p.score += 2
                            p.carryingItem = 0
                            soundEngine.playSfx(SfxType.SCORE)
                            spawnBurst(p.x, p.y, p.id.color, 14)
                        }
                    }
                }
            }

            MechanicGroup.BOMB_DODGE_KICK -> {
                entities.filter { it.kind == EntityKind.BOMB }.forEach { bomb ->
                    bomb.timer -= dt
                    if (bomb.timer <= 0f) {
                        soundEngine.playSfx(SfxType.EXPLOSION)
                        spawnBurst(bomb.x, bomb.y, Color(0xFFF97316), 18)
                        players.forEach { p ->
                            if (!p.eliminated && p.dodgeTimer <= 0f && hypot(p.x - bomb.x, p.y - bomb.y) < 165f) {
                                p.eliminated = true
                            } else if (!p.eliminated) {
                                p.score += 1
                            }
                        }
                        bomb.x = Random.nextInt(200, 800).toFloat()
                        bomb.y = Random.nextInt(200, 800).toFloat()
                        bomb.timer = bomb.maxTimer
                    }
                }
            }

            MechanicGroup.SAFE_ZONE_SURVIVAL -> {
                val safeRing = entities.firstOrNull { it.kind == EntityKind.SAFE_ZONE_RING }
                if (safeRing != null) {
                    if (game.variantIndex == 7) {
                        safeRing.radius = (safeRing.radius - 3.2f * dt).coerceAtLeast(110f)
                    }
                    safeRing.timer -= dt
                    if (safeRing.timer <= 0f) {
                        soundEngine.playSfx(SfxType.EXPLOSION)
                        players.forEach { p ->
                            if (!p.eliminated) {
                                val inside = hypot(p.x - safeRing.x, p.y - safeRing.y) <= safeRing.radius + p.radius * 0.5f
                                if (inside || p.jumpZ > 5f) {
                                    p.score += 3
                                    spawnBurst(p.x, p.y, Color(0xFF4ADE80), 8)
                                } else if (players.count { !it.eliminated } > 1) {
                                    p.eliminated = true
                                    spawnBurst(p.x, p.y, Color(0xFFEF4444), 14)
                                }
                            }
                        }
                        safeRing.x = Random.nextInt(240, 760).toFloat()
                        safeRing.y = Random.nextInt(240, 760).toFloat()
                        safeRing.timer = safeRing.maxTimer
                    }
                }
            }

            MechanicGroup.HAZARD_DODGE -> {
                entities.filter { it.kind == EntityKind.METEOR_WARNING || it.kind == EntityKind.SEEKER_ORB }.forEach { haz ->
                    haz.timer -= dt
                    if (haz.timer <= 0f) {
                        soundEngine.playSfx(SfxType.EXPLOSION)
                        spawnBurst(haz.x, haz.y, Color(0xFFEF4444), 14)
                        players.forEach { p ->
                            if (!p.eliminated && p.jumpZ <= 5f && p.dodgeTimer <= 0f &&
                                hypot(p.x - haz.x, p.y - haz.y) < haz.radius + p.radius
                            ) {
                                if (players.count { !it.eliminated } > 1) {
                                    p.eliminated = true
                                }
                            } else if (!p.eliminated) {
                                p.score += 1
                            }
                        }
                        haz.x = Random.nextInt(180, 820).toFloat()
                        haz.y = Random.nextInt(180, 820).toFloat()
                        haz.timer = haz.maxTimer
                    }
                }
                entities.filter { it.kind == EntityKind.CRYSTAL }.forEach { star ->
                    players.forEach { p ->
                        if (!p.eliminated && hypot(p.x - star.x, p.y - star.y) < p.radius + star.radius) {
                            p.score += 2
                            star.x = Random.nextInt(180, 820).toFloat()
                            star.y = Random.nextInt(180, 820).toFloat()
                            soundEngine.playSfx(SfxType.SCORE)
                        }
                    }
                }
            }

            MechanicGroup.TAG_PASS_CURSE -> {
                if (secondaryTimer >= 1.0f) {
                    secondaryTimer = 0f
                    players.forEach { p ->
                        if (!p.eliminated) {
                            if (game.variantIndex == 1) {
                                if (p.isTaggedOrCursed) p.score += 4
                            } else {
                                if (!p.isTaggedOrCursed) p.score += 2
                            }
                        }
                    }
                }
                entities.filter { it.kind == EntityKind.CRYSTAL }.forEach { orb ->
                    players.forEach { p ->
                        if (!p.eliminated && hypot(p.x - orb.x, p.y - orb.y) < p.radius + orb.radius) {
                            p.score += 2
                            orb.x = Random.nextInt(200, 800).toFloat()
                            orb.y = Random.nextInt(200, 800).toFloat()
                            soundEngine.playSfx(SfxType.SCORE)
                        }
                    }
                }
            }

            MechanicGroup.CIRCUIT_RACING, MechanicGroup.CHECKPOINT_RUSH -> {
                val gates = entities.filter { it.kind == EntityKind.CHECKPOINT_GATE }
                players.forEach { p ->
                    val targetGateIdx = p.progressSteps % gates.size.coerceAtLeast(1)
                    val gate = gates.firstOrNull { it.value == targetGateIdx } ?: gates.firstOrNull()
                    if (gate != null && hypot(p.x - gate.x, p.y - gate.y) < p.radius + gate.radius) {
                        p.progressSteps += 1
                        if (game.mechanicGroup == MechanicGroup.CIRCUIT_RACING) {
                            p.score = p.progressSteps / 2
                        } else {
                            p.score += 1
                            gate.x = Random.nextInt(180, 820).toFloat()
                            gate.y = Random.nextInt(180, 820).toFloat()
                        }
                        soundEngine.playSfx(SfxType.SCORE)
                        spawnBurst(gate.x, gate.y, p.id.color, 8)
                    }
                }
                // Live position tracking (1st, 2nd, 3rd, 4th)
                val ranked = players.sortedByDescending { p ->
                    val targetGateIdx = p.progressSteps % gates.size.coerceAtLeast(1)
                    val gate = gates.firstOrNull { it.value == targetGateIdx }
                    val distToNext = if (gate != null) hypot(p.x - gate.x, p.y - gate.y) else 0f
                    p.progressSteps * 10000f - distToNext
                }
                ranked.forEachIndexed { rIdx, p ->
                    p.racePosition = rIdx + 1
                }
            }

            MechanicGroup.BRIDGE_BUILDER -> {
                entities.filter { it.kind == EntityKind.BRIDGE_PLANK }.forEach { plank ->
                    players.forEach { p ->
                        if ((plank.ownerId == -1 || plank.ownerId == p.id.index) &&
                            hypot(p.x - plank.x, p.y - plank.y) < p.radius + plank.radius + 10f
                        ) {
                            p.score += 1
                            plank.x = Random.nextInt(160, 840).toFloat()
                            plank.y = Random.nextInt(220, 840).toFloat()
                            soundEngine.playSfx(SfxType.SCORE)
                            spawnBurst(p.x, p.y, p.id.color, 8)
                        }
                    }
                }
            }

            MechanicGroup.BALL_SPORTS, MechanicGroup.PENALTY_DUEL, MechanicGroup.BASKET_SHOOT, MechanicGroup.PADDLE_DEFENSE -> {
                entities.filter {
                    it.kind == EntityKind.BALL_SOCCER ||
                        it.kind == EntityKind.BALL_BASKET ||
                        it.kind == EntityKind.PUCK_HOCKEY ||
                        it.kind == EntityKind.BALL_TENNIS
                }.forEach { ball ->
                    players.forEach { p ->
                        val d = hypot(ball.x - p.x, ball.y - p.y)
                        val minD = ball.radius + p.radius
                        if (d < minD && d > 0.01f) {
                            val nx = (ball.x - p.x) / d
                            val ny = (ball.y - p.y) / d
                            ball.x = p.x + nx * (minD + 4f)
                            ball.y = p.y + ny * (minD + 4f)
                            val kickMult = if (p.attackAnimTimer > 0f) 560f else 390f
                            ball.vx = nx * kickMult + p.vx * 0.45f
                            ball.vy = ny * kickMult + p.vy * 0.45f
                            ball.ownerId = p.id.index
                            soundEngine.playSfx(SfxType.COLLISION)
                        }
                    }

                    val hoop = entities.firstOrNull { it.kind == EntityKind.GOLF_HOLE }
                    if (hoop != null && hypot(ball.x - hoop.x, ball.y - hoop.y) < hoop.radius + ball.radius) {
                        val scorer = players.getOrNull(ball.ownerId) ?: players.first()
                        scorer.score += 2
                        ball.x = ARENA_CENTER
                        ball.y = 650f
                        ball.vx = 0f
                        ball.vy = 0f
                        soundEngine.playSfx(SfxType.GOAL)
                        spawnBurst(hoop.x, hoop.y, scorer.id.color, 18)
                    } else if ((ball.y <= 108f || ball.y >= 892f) && ball.x in 280f..720f) {
                        // Ball entered the Top or Bottom Goal Mouth!
                        val scorer = if (ball.ownerId in players.indices) {
                            players[ball.ownerId]
                        } else {
                            if (ball.y <= 108f) players.first() else players.last()
                        }
                        scorer.score += 1
                        ball.x = ARENA_CENTER
                        ball.y = ARENA_CENTER
                        ball.vx = (Random.nextFloat() - 0.5f) * 260f
                        ball.vy = if (ball.y <= 108f) 240f else -240f
                        soundEngine.playSfx(SfxType.GOAL)
                        spawnBurst(ARENA_CENTER, ARENA_CENTER, scorer.id.color, 16)
                    }
                }
            }

            MechanicGroup.TILE_PUZZLE, MechanicGroup.TERRITORY_PAINT, MechanicGroup.WALL_TRAIL_TRAP -> {
                if (gridSize > 0) {
                    val step = ARENA_SIZE / gridSize
                    players.forEach { p ->
                        if (p.eliminated) return@forEach
                        val col = (p.x / step).toInt().coerceIn(0, gridSize - 1)
                        val row = (p.y / step).toInt().coerceIn(0, gridSize - 1)
                        val cell = grid[row * gridSize + col]
                        if (game.mechanicGroup == MechanicGroup.WALL_TRAIL_TRAP) {
                            cell.heat += dt * 1.4f
                            if (cell.heat > 1.3f && p.jumpZ <= 5f) {
                                if (players.count { !it.eliminated } > 1) {
                                    p.eliminated = true
                                    soundEngine.playSfx(SfxType.EXPLOSION)
                                    spawnBurst(p.x, p.y, Color(0xFFEF4444), 14)
                                }
                            } else {
                                cell.ownerId = p.id.index
                            }
                        } else if (cell.ownerId != p.id.index) {
                            cell.ownerId = p.id.index
                            if (game.mechanicGroup == MechanicGroup.TILE_PUZZLE) {
                                p.score += 1
                            }
                        }
                    }
                    if (game.mechanicGroup == MechanicGroup.TERRITORY_PAINT) {
                        recalculateTerritoryScores()
                    }
                }
            }

            MechanicGroup.MAZE_RUNNER -> {
                val goal = entities.firstOrNull { it.kind == EntityKind.CHECKPOINT_GATE }
                if (goal != null) {
                    players.forEach { p ->
                        if (hypot(p.x - goal.x, p.y - goal.y) < p.radius + goal.radius) {
                            p.score += 1
                            goal.x = Random.nextInt(180, 820).toFloat()
                            goal.y = Random.nextInt(180, 820).toFloat()
                            soundEngine.playSfx(SfxType.SCORE)
                            spawnBurst(p.x, p.y, p.id.color, 12)
                        }
                    }
                }
            }

            MechanicGroup.MATH_PATTERN_QUIZ -> {
                entities.filter { it.kind == EntityKind.QUIZ_PAD }.forEach { pad ->
                    players.forEach { p ->
                        if (hypot(p.x - pad.x, p.y - pad.y) < p.radius + pad.radius) {
                            if (pad.value == promptTargetValue) {
                                p.score += 2
                                soundEngine.playSfx(SfxType.SCORE)
                                spawnBurst(pad.x, pad.y, p.id.color, 14)
                                generateNextQuizPrompt()
                            }
                        }
                    }
                }
            }

            MechanicGroup.ZONE_CAPTURE -> {
                if (secondaryTimer >= 0.45f) {
                    secondaryTimer = 0f
                    entities.filter { it.kind == EntityKind.CAPTURE_ZONE }.forEach { zone ->
                        val occupants = players.filter { !it.eliminated && hypot(it.x - zone.x, it.y - zone.y) <= zone.radius }
                        if (occupants.size == 1) {
                            val owner = occupants.first()
                            owner.score += 3
                            zone.ownerId = owner.id.index
                            zone.color = owner.id.color
                        }
                    }
                }
            }

            MechanicGroup.REACTION_TAP -> {
                val ring = entities.firstOrNull { it.kind == EntityKind.SAFE_ZONE_RING }
                if (ring != null) {
                    ring.timer -= dt
                    if (ring.timer <= 0f) {
                        reactionSignalActive = !reactionSignalActive
                        ring.timer = if (reactionSignalActive) 1.1f else (1.2f + Random.nextFloat() * 1.3f)
                        ring.color = if (reactionSignalActive) Color(0xFF22C55E) else Color(0xFFF59E0B)
                        ring.label = if (reactionSignalActive) "STRIKE!" else "WAIT..."
                        if (reactionSignalActive) {
                            soundEngine.playSfx(SfxType.COUNTDOWN_GO)
                        }
                    }
                }
            }

            MechanicGroup.SHAPE_MORPH_CHASE -> {
                if (waveTimer >= 6.0f) {
                    waveTimer = 0f
                    players.forEach { p ->
                        p.morphShape = MorphShape.entries[(p.morphShape.ordinal + 1) % 3]
                    }
                    soundEngine.playSfx(SfxType.COUNTDOWN_TICK)
                }
                entities.filter { it.kind == EntityKind.CRYSTAL }.forEach { gem ->
                    players.forEach { p ->
                        if (hypot(p.x - gem.x, p.y - gem.y) < p.radius + gem.radius) {
                            p.score += 1
                            gem.x = Random.nextInt(200, 800).toFloat()
                            gem.y = Random.nextInt(200, 800).toFloat()
                            soundEngine.playSfx(SfxType.SCORE)
                        }
                    }
                }
            }

            else -> {}
        }
    }

    private fun recalculateTerritoryScores() {
        val counts = IntArray(4)
        grid.forEach { cell ->
            if (cell.ownerId in 0..3) counts[cell.ownerId]++
        }
        players.forEach { p ->
            p.score = counts[p.id.index]
        }
    }

    private fun spawnBurst(x: Float, y: Float, color: Color, baseCount: Int) {
        val count = (baseCount * particleMultiplier).toInt().coerceAtLeast(2)
        if (particles.size > 120) return
        repeat(count) { i ->
            val ang = (2 * PI * i / count) + Random.nextFloat() * 0.5f
            val spd = 90f + Random.nextFloat() * 180f
            particles.add(
                ParticleEffect(
                    x = x,
                    y = y,
                    vx = cos(ang).toFloat() * spd,
                    vy = sin(ang).toFloat() * spd,
                    radius = 5f + Random.nextFloat() * 5f,
                    color = color,
                    life = 0.45f,
                    maxLife = 0.45f
                )
            )
        }
    }

    private fun updateParticles(dt: Float) {
        val iter = particles.iterator()
        while (iter.hasNext()) {
            val pt = iter.next()
            pt.life -= dt
            if (pt.life <= 0f) {
                iter.remove()
            } else {
                pt.x += pt.vx * dt
                pt.y += pt.vy * dt
                pt.alpha = (pt.life / pt.maxLife).coerceIn(0f, 1f)
            }
        }
    }

    private fun checkMatchCompletion() {
        val alivePlayers = players.filter { !it.eliminated }

        // 1. Target score reached?
        if (game.targetScore > 0) {
            val winnerByTarget = players.firstOrNull { it.score >= game.targetScore }
            if (winnerByTarget != null) {
                val pEn = "${winnerByTarget.id.nameEn} ${winnerByTarget.id.emoji}"
                val pBn = "${winnerByTarget.id.nameBn} ${winnerByTarget.id.emoji}"
                val (reasonEn, reasonBn) = buildExplicitVictoryReason(winnerByTarget, pEn, pBn, reachedTarget = true)
                finalizeMatch(reasonEn = reasonEn, reasonBn = reasonBn)
                return
            }
        }

        // 2. Last surviving player in survival mode?
        if (game.winRule == WinRule.LAST_SURVIVING && alivePlayers.size <= 1 && players.size > 1) {
            val survivor = alivePlayers.firstOrNull()
            val reasonEn = if (survivor != null) {
                "${survivor.id.nameEn} ${survivor.id.emoji} knocked out all rivals as Last Survivor!"
            } else {
                "All players eliminated simultaneously!"
            }
            val reasonBn = if (survivor != null) {
                "${survivor.id.nameBn} ${survivor.id.emoji} সবাইকে পরাস্ত করে শেষ পর্যন্ত টিকে জয়ী হয়েছেন!"
            } else {
                "সব প্লেয়ার একই সাথে আউট হয়েছেন!"
            }
            finalizeMatch(reasonEn = reasonEn, reasonBn = reasonBn)
            return
        }

        // 3. Time expired?
        if (remainingSeconds <= 0f) {
            val top = players.maxByOrNull { it.score } ?: players.first()
            val pEn = "${top.id.nameEn} ${top.id.emoji}"
            val pBn = "${top.id.nameBn} ${top.id.emoji}"
            val (reasonEn, reasonBn) = buildExplicitVictoryReason(top, pEn, pBn, reachedTarget = false)
            finalizeMatch(reasonEn = reasonEn, reasonBn = reasonBn)
        }
    }

    private fun buildExplicitVictoryReason(
        winner: PlayerEntity,
        pEn: String,
        pBn: String,
        reachedTarget: Boolean
    ): Pair<String, String> = when (game.winRule) {
        WinRule.MOST_GOALS ->
            "$pEn scored ${winner.score} goals to win the match!" to
                "$pBn সর্বোচ্চ ${winner.score}টি গোল করে ম্যাচ জিতেছেন!"
        WinRule.FIRST_TO_FINISH ->
            if (reachedTarget) {
                "$pEn crossed the finish line first (${winner.score} laps/stages)!" to
                    "$pBn সবার আগে ফিনিশ লাইন অতিক্রম করেছেন (${winner.score} ধাপ)!"
            } else {
                "$pEn led the race at time up (${winner.score} laps/stages)!" to
                    "$pBn সময় শেষে রেসে এগিয়ে থেকে জয়ী হয়েছেন (${winner.score} ধাপ)!"
            }
        WinRule.MOST_TERRITORY ->
            "$pEn captured the largest territory (${winner.score} tiles)!" to
                "$pBn সবচেয়ে বেশি এলাকা (${winner.score}টি টাইল) দখল করে জিতেছেন!"
        WinRule.MOST_COLLECTED ->
            "$pEn collected and secured ${winner.score} items!" to
                "$pBn সর্বোচ্চ ${winner.score}টি অবজেক্ট সংগ্রহ করে জিতেছেন!"
        WinRule.BEST_PUZZLE_RESULT ->
            "$pEn solved the most puzzles (${winner.score} pts)!" to
                "$pBn সবচেয়ে বেশি পাজল সমাধান করে (${winner.score} পয়েন্ট) জিতেছেন!"
        WinRule.KING_TIME ->
            "$pEn controlled the objective zone longest (${winner.score} pts)!" to
                "$pBn সবচেয়ে বেশি সময় জোন নিয়ন্ত্রণে রেখে (${winner.score} পয়েন্ট) জিতেছেন!"
        WinRule.LAST_SURVIVING ->
            "$pEn survived the arena hazards longest (${winner.surviveTimeSec.toInt()}s)!" to
                "$pBn সবচেয়ে বেশি সময় (${winner.surviveTimeSec.toInt()} সে.) টিকে থেকে জিতেছেন!"
        else ->
            "$pEn won with ${winner.score} points!" to
                "$pBn সর্বোচ্চ ${winner.score} পয়েন্ট অর্জন করে বিজয়ী হয়েছেন!"
    }

    private fun finalizeMatch(reasonEn: String, reasonBn: String) {
        if (matchResult != null) return
        val sorted = when (game.winRule) {
            WinRule.LAST_SURVIVING -> players.sortedWith(
                compareByDescending<PlayerEntity> { !it.eliminated }
                    .thenByDescending { it.surviveTimeSec }
                    .thenByDescending { it.score }
            )
            else -> players.sortedWith(
                compareByDescending<PlayerEntity> { it.score }
                    .thenByDescending { !it.eliminated }
                    .thenByDescending { it.progressSteps }
            )
        }

        val top = sorted.first()
        val second = sorted.getOrNull(1)
        val isDraw = second != null &&
            top.score == second.score &&
            top.eliminated == second.eliminated &&
            abs(top.surviveTimeSec - second.surviveTimeSec) < 0.5f

        val winnerId: PlayerId? = if (isDraw) null else top.id

        val results = sorted.mapIndexed { idx, p ->
            val statEn = when (game.winRule) {
                WinRule.LAST_SURVIVING -> if (!p.eliminated) "Survived (${p.surviveTimeSec.toInt()}s)" else "Eliminated at ${p.surviveTimeSec.toInt()}s"
                WinRule.MOST_GOALS -> "${p.score} Goals Scored"
                WinRule.MOST_TERRITORY -> "${p.score} Tiles Captured"
                WinRule.FIRST_TO_FINISH -> "Position #${p.racePosition} • ${p.score} Stages"
                else -> "${p.score} Points"
            }
            val statBn = when (game.winRule) {
                WinRule.LAST_SURVIVING -> if (!p.eliminated) "টিকে ছিল (${p.surviveTimeSec.toInt()} সে.)" else "${p.surviveTimeSec.toInt()} সেকেন্ডে আউট"
                WinRule.MOST_GOALS -> "${p.score}টি গোল"
                WinRule.MOST_TERRITORY -> "${p.score}টি টাইল দখল"
                WinRule.FIRST_TO_FINISH -> "পজিশন #${p.racePosition} • ${p.score} ধাপ"
                else -> "${p.score} পয়েন্ট"
            }
            PlayerResult(
                playerId = p.id,
                isBot = p.isBot,
                score = p.score,
                survived = !p.eliminated,
                rank = idx + 1,
                statNoteEn = statEn,
                statNoteBn = statBn
            )
        }

        soundEngine.playSfx(if (winnerId != null) SfxType.WIN else SfxType.GAME_OVER)
        matchResult = MatchResult(
            config = config,
            winner = if (isDraw) null else top.id,
            playerResults = results,
            durationPlayedSeconds = elapsedSeconds.toInt().coerceAtLeast(1),
            finishReasonEn = if (isDraw) "Match ended in a tie!" else reasonEn,
            finishReasonBn = if (isDraw) "সমান স্কোর হওয়ায় ম্যাচ ড্র হয়েছে!" else reasonBn
        )
    }
}
