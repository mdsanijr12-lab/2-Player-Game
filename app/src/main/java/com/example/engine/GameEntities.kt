package com.example.engine

import androidx.compose.ui.graphics.Color
import com.example.model.PlayerId

const val ARENA_SIZE = 1000f
const val ARENA_CENTER = 500f

data class PlayerInputState(
    var moveX: Float = 0f,
    var moveY: Float = 0f,
    var primaryPressed: Boolean = false,
    var secondaryPressed: Boolean = false,
    var primaryJustTriggered: Boolean = false,
    var secondaryJustTriggered: Boolean = false
)

enum class MorphShape {
    CIRCLE,   // Beats TRIANGLE
    TRIANGLE, // Beats SQUARE
    SQUARE;   // Beats CIRCLE

    fun beats(other: MorphShape): Boolean =
        (this == CIRCLE && other == TRIANGLE) ||
        (this == TRIANGLE && other == SQUARE) ||
        (this == SQUARE && other == CIRCLE)
}

data class PlayerEntity(
    val id: PlayerId,
    val isBot: Boolean,
    var x: Float,
    var y: Float,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var angleRad: Float = 0f,
    var turretAngleRad: Float = 0f,
    var radius: Float = 30f,
    var score: Int = 0,
    var progressSteps: Int = 0,
    var racePosition: Int = id.number,
    var eliminated: Boolean = false,
    var surviveTimeSec: Float = 0f,
    var actionCooldown: Float = 0f,
    var dashCooldown: Float = 0f,
    var dashTimer: Float = 0f,
    var dodgeTimer: Float = 0f,
    var attackAnimTimer: Float = 0f,
    var fallingTimer: Float = 0f,
    var shotCharge: Float = 0f,
    var jumpZ: Float = 0f,
    var jumpVz: Float = 0f,
    var carryingItem: Int = 0,
    var isTaggedOrCursed: Boolean = false,
    var morphShape: MorphShape = MorphShape.entries[id.index % 3],
    var respawnTimer: Float = 0f,
    var botSpeedFactor: Float = 1.0f,
    val basePos: Pair<Float, Float>
)

enum class EntityKind {
    CRYSTAL,
    TREASURE_CHEST,
    FLAG,
    BOMB,
    TARGET_DRONE,
    BUBBLE,
    METEOR_WARNING,
    LASER_WALL,
    BALL_SOCCER,
    BALL_BASKET,
    PUCK_HOCKEY,
    BALL_TENNIS,
    BALL_BOWLING,
    BOWLING_PIN,
    BALL_GOLF,
    GOLF_HOLE,
    DISC_NEON,
    DODGEBALL,
    CARNIVAL_RING,
    CARNIVAL_PEG,
    FRUIT,
    BALLOON,
    SEEKER_ORB,
    HIGHWAY_CAR,
    BRIDGE_PLANK,
    CHECKPOINT_GATE,
    CAPTURE_ZONE,
    SAFE_ZONE_RING,
    SHOCKWAVE_RING,
    PROJECTILE,
    WALL_BLOCK,
    QUIZ_PAD,
    CRATE_BOX,
    SOCKET_PAD
}

data class WorldEntity(
    val uid: Int,
    val kind: EntityKind,
    var x: Float,
    var y: Float,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var radius: Float = 24f,
    var width: Float = 48f,
    var height: Float = 48f,
    var ownerId: Int = -1,
    var value: Int = 1,
    var timer: Float = 0f,
    var maxTimer: Float = 3f,
    var label: String = "",
    var color: Color = Color.White,
    var active: Boolean = true
)

data class GridCell(
    val row: Int,
    val col: Int,
    var ownerId: Int = -1,
    var state: Int = 0, // 0 normal, 1 highlighted/safe, 2 hazard/collapsed, 3 wall, 4 hidden pair
    var symbol: Int = 0,
    var heat: Float = 0f,
    var label: String = ""
)

data class ParticleEffect(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var radius: Float,
    var color: Color,
    var alpha: Float = 1f,
    var life: Float = 0.6f,
    val maxLife: Float = 0.6f
)
