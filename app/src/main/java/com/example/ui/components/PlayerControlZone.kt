package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppLanguage
import com.example.model.ControlScheme
import com.example.model.PlayerId
import kotlin.math.hypot

@Composable
fun PlayerControlDeck(
    playerId: PlayerId,
    score: Int,
    eliminated: Boolean,
    isBot: Boolean,
    controlScheme: ControlScheme,
    lang: AppLanguage,
    invertedTopPlayer: Boolean,
    compactCornerMode: Boolean,
    onMoveChanged: (Float, Float) -> Unit,
    onPrimaryChanged: (Boolean) -> Unit,
    onSecondaryChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val containerRotation = if (invertedTopPlayer && !isBot) 180f else 0f

    Surface(
        modifier = modifier
            .rotate(containerRotation)
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = 1.5.dp,
                color = playerId.color.copy(alpha = if (eliminated) 0.25f else 0.75f),
                shape = RoundedCornerShape(16.dp)
            )
            .testTag("control_zone_p${playerId.number}"),
        color = Color(0xFF0F172A).copy(alpha = 0.90f)
    ) {
        if (isBot) {
            // Compact Bot Status Indicator (only player identity + score, no touch controls needed)
            Row(
                modifier = Modifier
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(playerId.color)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = playerId.displayBadge(lang),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (eliminated) "OUT" else "$score",
                    color = playerId.lightColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        } else {
            // Human Local Multiplayer Touch Control Zone
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header: Player identity + Score
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = playerId.displayBadge(lang),
                        color = playerId.lightColor,
                        fontSize = if (compactCornerMode) 11.sp else 12.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = if (eliminated) "OUT" else "$score",
                        color = Color.White,
                        fontSize = if (compactCornerMode) 13.sp else 15.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Controls Row: Joystick and/or Buttons depending on game's ControlScheme
                val joySize = if (compactCornerMode) 74.dp else 90.dp
                val primarySize = if (compactCornerMode) 54.dp else 64.dp
                val secondarySize = if (compactCornerMode) 48.dp else 54.dp

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (controlScheme.usesJoystick) {
                        FixedTouchJoystick(
                            playerId = playerId,
                            sizeDp = joySize,
                            inverted = invertedTopPlayer,
                            onVectorChanged = onMoveChanged
                        )
                    }

                    val primaryLabel = controlScheme.primaryLabel(lang)
                    val secondaryLabel = controlScheme.secondaryLabel(lang)

                    if (primaryLabel.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ArcadeActionButton(
                                label = primaryLabel,
                                color = playerId.color,
                                sizeDp = if (!controlScheme.usesJoystick) 72.dp else primarySize,
                                testTag = "btn_primary_p${playerId.number}",
                                onPressedChanged = onPrimaryChanged
                            )
                            if (secondaryLabel != null) {
                                ArcadeActionButton(
                                    label = secondaryLabel,
                                    color = playerId.darkColor,
                                    sizeDp = secondarySize,
                                    testTag = "btn_secondary_p${playerId.number}",
                                    onPressedChanged = onSecondaryChanged
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FixedTouchJoystick(
    playerId: PlayerId,
    sizeDp: Dp,
    inverted: Boolean,
    onVectorChanged: (Float, Float) -> Unit
) {
    var knobOffset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .size(sizeDp)
            .testTag("joystick_p${playerId.number}")
            .pointerInput(playerId, inverted) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val maxRadius = (minOf(size.width, size.height) / 2f) * 0.78f

                    fun updateFromPoint(pt: Offset) {
                        val dx = pt.x - center.x
                        val dy = pt.y - center.y
                        val dist = hypot(dx, dy)
                        val clampedDist = dist.coerceAtMost(maxRadius)
                        val nx = if (dist > 0.01f) dx / dist else 0f
                        val ny = if (dist > 0.01f) dy / dist else 0f
                        knobOffset = Offset(nx * clampedDist, ny * clampedDist)
                        val normX = (nx * (clampedDist / maxRadius)).coerceIn(-1f, 1f)
                        val normY = (ny * (clampedDist / maxRadius)).coerceIn(-1f, 1f)
                        // If container is rotated 180 degrees for top players, flip vector so screen direction matches arena direction!
                        val worldX = if (inverted) -normX else normX
                        val worldY = if (inverted) -normY else normY
                        onVectorChanged(worldX, worldY)
                    }

                    updateFromPoint(down.position)
                    val pointerId = down.id
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                        if (!change.pressed) break
                        change.consume()
                        updateFromPoint(change.position)
                    }
                    knobOffset = Offset.Zero
                    onVectorChanged(0f, 0f)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val c = Offset(size.width / 2f, size.height / 2f)
            val baseR = (minOf(size.width, size.height) / 2f) * 0.90f
            drawCircle(
                color = playerId.color.copy(alpha = 0.16f),
                radius = baseR,
                center = c
            )
            drawCircle(
                color = playerId.color.copy(alpha = 0.65f),
                radius = baseR,
                center = c,
                style = Stroke(width = 3f)
            )
            // Crosshair guides
            drawLine(
                color = Color.White.copy(alpha = 0.15f),
                start = Offset(c.x - baseR * 0.7f, c.y),
                end = Offset(c.x + baseR * 0.7f, c.y),
                strokeWidth = 2f
            )
            drawLine(
                color = Color.White.copy(alpha = 0.15f),
                start = Offset(c.x, c.y - baseR * 0.7f),
                end = Offset(c.x, c.y + baseR * 0.7f),
                strokeWidth = 2f
            )
            // Movable Thumb Knob
            val knobPos = c + knobOffset
            drawCircle(
                color = playerId.color,
                radius = baseR * 0.42f,
                center = knobPos
            )
            drawCircle(
                color = Color.White,
                radius = baseR * 0.42f,
                center = knobPos,
                style = Stroke(width = 2.5f)
            )
        }
    }
}

@Composable
fun ArcadeActionButton(
    label: String,
    color: Color,
    sizeDp: Dp,
    testTag: String,
    onPressedChanged: (Boolean) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(sizeDp)
            .clip(CircleShape)
            .background(if (isPressed) Color.White else color)
            .border(2.5.dp, Color.White.copy(alpha = 0.85f), CircleShape)
            .testTag(testTag)
            .pointerInput(label) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    onPressedChanged(true)
                    val pointerId = down.id
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                        if (!change.pressed) break
                        change.consume()
                    }
                    isPressed = false
                    onPressedChanged(false)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isPressed) Color.Black else Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1
        )
    }
}
