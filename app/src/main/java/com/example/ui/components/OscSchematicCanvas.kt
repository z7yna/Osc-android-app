package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.CalculationResult
import com.example.domain.Coordinates
import com.example.ui.theme.*
import kotlin.math.*

@Composable
fun OscSchematicCanvas(
    origin: Coordinates,
    target: Coordinates,
    result: CalculationResult,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "OpticalRadar")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "TargetPulseAlpha"
    )



    LiquidGlassSurface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        cornerRadius = 24.dp,
        isHighlighted = true,
        accentRimColor = Crimson,
        contentPadding = PaddingValues(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(256.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF100306),
                            Color(0xFF040406),
                            AmoledBlack
                        )
                    )
                )
                .border(0.8.dp, Color(0x22FFFFFF), RoundedCornerShape(16.dp))
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = min(size.width, size.height) / 2f * 0.86f

                // 1. Subtle deep-red internal glow at center
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            DeepRed.copy(alpha = 0.36f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = radius
                    ),
                    radius = radius,
                    center = center
                )

                // 2. Concentric precision optical rings
                val ringFracs = listOf(0.25f, 0.5f, 0.75f, 1.0f)
                ringFracs.forEachIndexed { index, frac ->
                    drawCircle(
                        color = if (index == ringFracs.lastIndex) {
                            HighlightWhite.copy(alpha = 0.24f)
                        } else {
                            SecondaryText.copy(alpha = 0.16f)
                        },
                        radius = radius * frac,
                        center = center,
                        style = Stroke(
                            width = if (index == ringFracs.lastIndex) 1.dp.toPx() else 0.75.dp.toPx(),
                            pathEffect = if (index < ringFracs.lastIndex) {
                                PathEffect.dashPathEffect(floatArrayOf(5f, 7f))
                            } else {
                                null
                            }
                        )
                    )
                }

                // 3. Precision Degree Ticks on Outer Bezel (every 15°)
                for (deg in 0 until 360 step 15) {
                    val rad = Math.toRadians(deg.toDouble())
                    val isCardinal = deg % 90 == 0
                    val innerR = radius * if (isCardinal) 0.90f else 0.95f
                    val start = Offset(
                        center.x + (cos(rad) * innerR).toFloat(),
                        center.y + (sin(rad) * innerR).toFloat()
                    )
                    val end = Offset(
                        center.x + (cos(rad) * radius).toFloat(),
                        center.y + (sin(rad) * radius).toFloat()
                    )
                    drawLine(
                        color = if (isCardinal) HighlightWhite.copy(alpha = 0.45f) else SecondaryText.copy(alpha = 0.22f),
                        start = start,
                        end = end,
                        strokeWidth = if (isCardinal) 1.3.dp.toPx() else 0.8.dp.toPx()
                    )
                }

                // 4. Cardinal Crosshairs
                drawLine(
                    color = SecondaryText.copy(alpha = 0.16f),
                    start = Offset(center.x - radius, center.y),
                    end = Offset(center.x + radius, center.y),
                    strokeWidth = 0.8.dp.toPx()
                )
                drawLine(
                    color = SecondaryText.copy(alpha = 0.16f),
                    start = Offset(center.x, center.y - radius),
                    end = Offset(center.x, center.y + radius),
                    strokeWidth = 0.8.dp.toPx()
                )



                // 6. Vector to Target
                val dx = target.x - origin.x
                val dz = target.z - origin.z
                val distance = sqrt(dx * dx + dz * dz)

                val targetDistFraction = 0.78f
                val targetOffset = if (distance > 0.0) {
                    val unitX = (dx / distance).toFloat()
                    val unitZ = (dz / distance).toFloat()
                    Offset(
                        center.x + unitX * radius * targetDistFraction,
                        center.y + unitZ * radius * targetDistFraction
                    )
                } else {
                    center
                }

                // 7. Azimuth Sector Wedge
                val azimuthDeg = result.azimuthDegrees.toFloat()
                drawArc(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            LiveRed.copy(alpha = 0.28f),
                            Crimson.copy(alpha = 0.14f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = radius
                    ),
                    startAngle = -90f,
                    sweepAngle = azimuthDeg,
                    useCenter = true,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = androidx.compose.ui.geometry.Size(radius * 2f, radius * 2f)
                )

                // 8. Trajectory Line (Specular White -> Live Red)
                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(HighlightWhite, LiveRed),
                        start = center,
                        end = targetOffset
                    ),
                    start = center,
                    end = targetOffset,
                    strokeWidth = 2.2.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // 9. Target Reticle & Pulsing Lock Halo
                drawCircle(
                    color = LiveRed.copy(alpha = 0.25f * pulseAlpha),
                    radius = 12.dp.toPx(),
                    center = targetOffset
                )
                drawCircle(
                    color = LiveRed.copy(alpha = pulseAlpha),
                    radius = 6.5.dp.toPx(),
                    center = targetOffset,
                    style = Stroke(width = 1.6.dp.toPx())
                )
                drawCircle(
                    color = HighlightWhite,
                    radius = 2.8.dp.toPx(),
                    center = targetOffset
                )

                // 10. Cannon Origin Center Lens
                drawCircle(
                    color = HighlightWhite.copy(alpha = 0.35f),
                    radius = 7.dp.toPx(),
                    center = center,
                    style = Stroke(width = 1.dp.toPx())
                )
                drawCircle(
                    color = HighlightWhite,
                    radius = 3.dp.toPx(),
                    center = center
                )
            }

            // Cardinal Labels
            Text(
                text = "N · 0°",
                style = OscTextStyles.MicroLabel,
                fontSize = 9.sp,
                color = HighlightWhite.copy(alpha = 0.78f),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 6.dp)
            )
            Text(
                text = "S · 180°",
                style = OscTextStyles.MicroLabel,
                fontSize = 9.sp,
                color = SecondaryText.copy(alpha = 0.7f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 6.dp)
            )
            Text(
                text = "E +X",
                style = OscTextStyles.MicroLabel,
                fontSize = 9.sp,
                color = SecondaryText.copy(alpha = 0.7f),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp)
            )
            Text(
                text = "W −X",
                style = OscTextStyles.MicroLabel,
                fontSize = 9.sp,
                color = SecondaryText.copy(alpha = 0.7f),
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 8.dp)
            )

            // Frosted Glass HUD Telemetry Pill (Top Left)
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xB3070709))
                    .border(0.8.dp, Color(0x28FFFFFF), RoundedCornerShape(10.dp))
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "AZM ${"%.1f".format(result.azimuthDegrees)}°",
                    style = OscTextStyles.NumericCompact,
                    fontSize = 11.sp,
                    color = LiveRed
                )
                Text(
                    text = "DST ${"%,.0f".format(result.distance)}m",
                    style = OscTextStyles.NumericCompact,
                    fontSize = 10.sp,
                    color = HighlightWhite
                )
            }

            // Frosted Glass HUD Delta Pill (Bottom Right)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xB3070709))
                    .border(0.8.dp, Color(0x28FFFFFF), RoundedCornerShape(10.dp))
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "ΔX %+.0f".format(target.x - origin.x),
                    style = OscTextStyles.NumericCompact,
                    fontSize = 10.sp,
                    color = SecondaryText
                )
                Text(
                    text = "ΔZ %+.0f".format(target.z - origin.z),
                    style = OscTextStyles.NumericCompact,
                    fontSize = 10.sp,
                    color = SecondaryText
                )
                Text(
                    text = "ΔY %+.0f".format(target.y - origin.y),
                    style = OscTextStyles.NumericCompact,
                    fontSize = 10.sp,
                    color = HighlightWhite
                )
            }
        }
    }
}
