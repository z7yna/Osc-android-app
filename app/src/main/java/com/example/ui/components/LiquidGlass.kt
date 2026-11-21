package com.example.ui.components

import android.graphics.RenderEffect as AndroidRenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

// ---------------------------------------------------------------------------
// AGSL Optical Surface Shader (API 33+)
// ---------------------------------------------------------------------------

private const val LIQUID_GLASS_SURFACE_SHADER_SRC = """
    uniform float2 uResolution;
    uniform float2 uTouch;
    uniform float uTouchGlow;
    uniform float uHighlight;
    uniform float uTime;

    half4 main(float2 fragCoord) {
        float2 size = max(uResolution, float2(1.0));
        float2 uv = fragCoord / size;

        // Edge proximity for curved lens refraction & Fresnel rim
        float edgeX = min(uv.x, 1.0 - uv.x);
        float edgeY = min(uv.y, 1.0 - uv.y);
        float edgeDist = min(edgeX * (size.x / max(size.y, 1.0)), edgeY);
        float fresnel = exp(-edgeDist * 14.0);

        // Specular light source reacting smoothly to touch coordinates
        float2 touchNorm = uTouch / size;
        float aspect = size.x / max(size.y, 1.0);
        float2 delta = float2((uv.x - touchNorm.x) * aspect, uv.y - touchNorm.y);
        float distTouch = length(delta);
        float touchSpec = exp(-distTouch * distTouch * (5.5 - uTouchGlow * 2.0));

        // Ambient optical sheen sweep across curved glass
        float sheenAxis = uv.x * 0.65 + uv.y * 0.35 - 0.18 + 0.06 * sin(uTime * 0.8);
        float sheen = exp(-sheenAxis * sheenAxis * 16.0);

        float3 white = float3(1.0, 1.0, 1.0);
        float3 liveRed = float3(1.0, 0.09, 0.267);
        float3 crimson = float3(0.69, 0.0, 0.125);

        float chromaticBias = clamp((uv.y - uv.x * 0.4) * 0.8 + 0.2, 0.0, 1.0);
        float3 rimColor = mix(white * 0.9, mix(crimson, liveRed, uHighlight), chromaticBias);

        float topLight = smoothstep(0.50, 0.0, uv.y);
        float3 col = float3(0.0);
        float alpha = 0.0;

        col += white * (0.04 + 0.05 * topLight + 0.055 * sheen);
        alpha += 0.045 + 0.05 * topLight + 0.045 * sheen;

        float touchStrength = 0.12 + 0.24 * uTouchGlow;
        float3 touchTint = mix(white, liveRed, 0.28 + 0.32 * uHighlight);
        col += touchTint * (touchSpec * touchStrength);
        alpha += touchSpec * touchStrength * 0.68;

        float edgeStrength = (0.11 + 0.14 * uHighlight + 0.16 * uTouchGlow * touchSpec);
        col += rimColor * (fresnel * edgeStrength);
        alpha += fresnel * edgeStrength * 0.82;

        alpha = clamp(alpha, 0.0, 0.52);
        return half4(col * alpha, alpha);
    }
"""

// ---------------------------------------------------------------------------
// 1. Pure AMOLED Black (#000000) Background with Moving Live Red (#FF1744) Roots
// ---------------------------------------------------------------------------

private data class RootBranchSpec(
    val startNorm: Offset,
    val dirAngleRad: Float,
    val lengthNorm: Float,
    val amplitudeDp: Float,
    val waveFreq: Float,
    val speed: Float,
    val phase: Float,
    val baseStrokeDp: Float,
    val baseAlpha: Float,
    val subBranches: List<SubRootSpec>
)

private data class SubRootSpec(
    val attachT: Float,
    val forkAngleOffsetRad: Float,
    val lengthFraction: Float,
    val phaseOffset: Float
)

@Composable
fun AmoledRedAtmosphereBackground(
    scrollProgress: Float = 0f,
    @Suppress("UNUSED_PARAMETER") centerpieceGlowYFraction: Float = 0.15f,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "LiveRedRoots")
    val animTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat() * 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 56000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RootsTime"
    )

    val roots = remember {
        listOf(
            // 1. Upper-left descending main root
            RootBranchSpec(
                startNorm = Offset(-0.04f, 0.06f),
                dirAngleRad = 0.72f,
                lengthNorm = 0.64f,
                amplitudeDp = 18f,
                waveFreq = 2.6f,
                speed = 0.55f,
                phase = 0.2f,
                baseStrokeDp = 1.9f,
                baseAlpha = 0.76f,
                subBranches = listOf(
                    SubRootSpec(0.30f, -0.48f, 0.45f, 1.1f),
                    SubRootSpec(0.58f, 0.44f, 0.40f, 2.4f),
                    SubRootSpec(0.78f, -0.35f, 0.30f, 3.6f)
                )
            ),
            // 2. Top-center descending taproot
            RootBranchSpec(
                startNorm = Offset(0.46f, -0.04f),
                dirAngleRad = 1.48f,
                lengthNorm = 0.46f,
                amplitudeDp = 14f,
                waveFreq = 2.8f,
                speed = 0.50f,
                phase = 2.8f,
                baseStrokeDp = 1.55f,
                baseAlpha = 0.64f,
                subBranches = listOf(
                    SubRootSpec(0.35f, -0.52f, 0.44f, 0.6f),
                    SubRootSpec(0.68f, 0.48f, 0.36f, 2.1f)
                )
            ),
            // 3. Upper-right sweeping root
            RootBranchSpec(
                startNorm = Offset(1.04f, 0.14f),
                dirAngleRad = 2.48f,
                lengthNorm = 0.60f,
                amplitudeDp = 16f,
                waveFreq = 2.9f,
                speed = 0.48f,
                phase = 1.7f,
                baseStrokeDp = 1.75f,
                baseAlpha = 0.72f,
                subBranches = listOf(
                    SubRootSpec(0.28f, 0.46f, 0.44f, 0.8f),
                    SubRootSpec(0.62f, -0.42f, 0.38f, 2.9f),
                    SubRootSpec(0.80f, 0.34f, 0.26f, 4.3f)
                )
            ),
            // 4. Upper-left secondary tendril
            RootBranchSpec(
                startNorm = Offset(-0.04f, 0.28f),
                dirAngleRad = 0.42f,
                lengthNorm = 0.48f,
                amplitudeDp = 14f,
                waveFreq = 3.2f,
                speed = 0.65f,
                phase = 4.2f,
                baseStrokeDp = 1.35f,
                baseAlpha = 0.58f,
                subBranches = listOf(
                    SubRootSpec(0.40f, -0.45f, 0.42f, 1.9f),
                    SubRootSpec(0.72f, 0.38f, 0.30f, 3.5f)
                )
            ),
            // 5. Mid-left creeping root
            RootBranchSpec(
                startNorm = Offset(-0.04f, 0.50f),
                dirAngleRad = 0.26f,
                lengthNorm = 0.58f,
                amplitudeDp = 15f,
                waveFreq = 3.1f,
                speed = 0.62f,
                phase = 3.4f,
                baseStrokeDp = 1.65f,
                baseAlpha = 0.66f,
                subBranches = listOf(
                    SubRootSpec(0.35f, 0.52f, 0.44f, 1.5f),
                    SubRootSpec(0.66f, -0.45f, 0.34f, 4.1f)
                )
            ),
            // 6. Mid-right high tendril
            RootBranchSpec(
                startNorm = Offset(1.04f, 0.36f),
                dirAngleRad = 2.86f,
                lengthNorm = 0.50f,
                amplitudeDp = 14f,
                waveFreq = 3.0f,
                speed = 0.60f,
                phase = 0.9f,
                baseStrokeDp = 1.4f,
                baseAlpha = 0.60f,
                subBranches = listOf(
                    SubRootSpec(0.38f, 0.48f, 0.40f, 2.7f),
                    SubRootSpec(0.70f, -0.38f, 0.32f, 4.8f)
                )
            ),
            // 7. Mid-right descending root
            RootBranchSpec(
                startNorm = Offset(1.03f, 0.58f),
                dirAngleRad = 2.66f,
                lengthNorm = 0.62f,
                amplitudeDp = 17f,
                waveFreq = 2.7f,
                speed = 0.50f,
                phase = 4.8f,
                baseStrokeDp = 1.8f,
                baseAlpha = 0.70f,
                subBranches = listOf(
                    SubRootSpec(0.32f, -0.50f, 0.45f, 2.0f),
                    SubRootSpec(0.64f, 0.38f, 0.34f, 0.4f)
                )
            ),
            // 8. Lower-left lateral root
            RootBranchSpec(
                startNorm = Offset(-0.04f, 0.78f),
                dirAngleRad = -0.34f,
                lengthNorm = 0.52f,
                amplitudeDp = 15f,
                waveFreq = 2.9f,
                speed = 0.56f,
                phase = 1.3f,
                baseStrokeDp = 1.5f,
                baseAlpha = 0.62f,
                subBranches = listOf(
                    SubRootSpec(0.34f, -0.44f, 0.42f, 3.0f),
                    SubRootSpec(0.68f, 0.40f, 0.32f, 5.1f)
                )
            ),
            // 9. Bottom-left rising root
            RootBranchSpec(
                startNorm = Offset(0.14f, 1.04f),
                dirAngleRad = -1.18f,
                lengthNorm = 0.58f,
                amplitudeDp = 16f,
                waveFreq = 2.8f,
                speed = 0.58f,
                phase = 2.2f,
                baseStrokeDp = 1.85f,
                baseAlpha = 0.74f,
                subBranches = listOf(
                    SubRootSpec(0.34f, 0.45f, 0.42f, 3.3f),
                    SubRootSpec(0.68f, -0.42f, 0.33f, 1.2f)
                )
            ),
            // 10. Bottom-center ascending root
            RootBranchSpec(
                startNorm = Offset(0.52f, 1.04f),
                dirAngleRad = -1.62f,
                lengthNorm = 0.48f,
                amplitudeDp = 14f,
                waveFreq = 3.1f,
                speed = 0.54f,
                phase = 3.9f,
                baseStrokeDp = 1.55f,
                baseAlpha = 0.66f,
                subBranches = listOf(
                    SubRootSpec(0.32f, -0.46f, 0.44f, 1.4f),
                    SubRootSpec(0.65f, 0.46f, 0.35f, 4.4f)
                )
            ),
            // 11. Bottom-right rising root
            RootBranchSpec(
                startNorm = Offset(0.84f, 1.04f),
                dirAngleRad = -1.96f,
                lengthNorm = 0.56f,
                amplitudeDp = 15f,
                waveFreq = 3.0f,
                speed = 0.52f,
                phase = 5.5f,
                baseStrokeDp = 1.75f,
                baseAlpha = 0.70f,
                subBranches = listOf(
                    SubRootSpec(0.30f, -0.44f, 0.44f, 0.9f),
                    SubRootSpec(0.65f, 0.40f, 0.35f, 3.8f)
                )
            )
        )
    }

    // Pure #000000 AMOLED void with ONLY moving Live Red (#FF1744) roots
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(AmoledBlack)
    ) {
        val w = size.width
        val h = size.height
        val diag = min(w, h) * 1.15f
        val parallaxY = (scrollProgress.coerceIn(0f, 1.5f) - 0.5f) * h * 0.08f

        roots.forEach { root ->
            drawMovingLiveRedRoot(
                root = root,
                canvasW = w,
                canvasH = h,
                scaleLength = diag,
                parallaxY = parallaxY,
                animTime = animTime
            )
        }
    }
}

private fun DrawScope.drawMovingLiveRedRoot(
    root: RootBranchSpec,
    canvasW: Float,
    canvasH: Float,
    scaleLength: Float,
    parallaxY: Float,
    animTime: Float
) {
    val start = Offset(root.startNorm.x * canvasW, root.startNorm.y * canvasH - parallaxY)
    val totalLen = root.lengthNorm * scaleLength
    val cosA = cos(root.dirAngleRad)
    val sinA = sin(root.dirAngleRad)
    val perpX = -sinA
    val perpY = cosA
    val ampPx = root.amplitudeDp.dp.toPx()

    val steps = 18
    val trunkPoints = ArrayList<Offset>(steps + 1)

    for (i in 0..steps) {
        val t = i.toFloat() / steps.toFloat()
        val dist = totalLen * t
        // Taper sway at root base, amplify organically toward living tips
        val wave = sin(t * root.waveFreq * PI.toFloat() - animTime * root.speed + root.phase)
        val sway = ampPx * (0.25f + 0.95f * t) * wave
        val px = start.x + cosA * dist + perpX * sway
        val py = start.y + sinA * dist + perpY * sway
        trunkPoints.add(Offset(px, py))
    }

    val trunkPath = buildSmoothSplinePath(trunkPoints)
    val tipPoint = trunkPoints.last()
    val pulse = 0.72f + 0.28f * sin(animTime * root.speed * 1.3f + root.phase)

    // Tight Live Red (#FF1744) optical root glow confined strictly to the root vein
    drawPath(
        path = trunkPath,
        brush = Brush.linearGradient(
            colors = listOf(
                LiveRed.copy(alpha = root.baseAlpha * 0.28f * pulse),
                LiveRed.copy(alpha = root.baseAlpha * 0.16f * pulse),
                Color.Transparent
            ),
            start = start,
            end = tipPoint
        ),
        style = Stroke(width = (root.baseStrokeDp * 3.2f).dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    // Crisp Live Red (#FF1744) moving root core
    drawPath(
        path = trunkPath,
        brush = Brush.linearGradient(
            colors = listOf(
                LiveRed.copy(alpha = root.baseAlpha * 0.95f * pulse),
                LiveRed.copy(alpha = root.baseAlpha * 0.65f * pulse),
                LiveRed.copy(alpha = 0.08f)
            ),
            start = start,
            end = tipPoint
        ),
        style = Stroke(width = root.baseStrokeDp.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    // Forking secondary root tendrils
    root.subBranches.forEach { sub ->
        val attachIdx = (sub.attachT * steps).toInt().coerceIn(1, steps - 1)
        val branchStart = trunkPoints[attachIdx]
        val branchAngle = root.dirAngleRad + sub.forkAngleOffsetRad
        val bCos = cos(branchAngle)
        val bSin = sin(branchAngle)
        val bPerpX = -bSin
        val bPerpY = bCos
        val branchLen = totalLen * sub.lengthFraction

        val subSteps = 10
        val subPoints = ArrayList<Offset>(subSteps + 1)
        for (j in 0..subSteps) {
            val st = j.toFloat() / subSteps.toFloat()
            val bDist = branchLen * st
            val bWave = sin(st * 2.4f * PI.toFloat() - animTime * (root.speed * 1.15f) + sub.phaseOffset)
            val bSway = ampPx * 0.55f * st * bWave
            subPoints.add(
                Offset(
                    branchStart.x + bCos * bDist + bPerpX * bSway,
                    branchStart.y + bSin * bDist + bPerpY * bSway
                )
            )
        }

        val subPath = buildSmoothSplinePath(subPoints)
        val subTip = subPoints.last()
        val subStroke = root.baseStrokeDp * 0.62f

        drawPath(
            path = subPath,
            brush = Brush.linearGradient(
                colors = listOf(
                    LiveRed.copy(alpha = root.baseAlpha * 0.22f * pulse),
                    Color.Transparent
                ),
                start = branchStart,
                end = subTip
            ),
            style = Stroke(width = (subStroke * 3.0f).dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        drawPath(
            path = subPath,
            brush = Brush.linearGradient(
                colors = listOf(
                    LiveRed.copy(alpha = root.baseAlpha * 0.80f * pulse),
                    LiveRed.copy(alpha = 0.05f)
                ),
                start = branchStart,
                end = subTip
            ),
            style = Stroke(width = subStroke.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

private fun buildSmoothSplinePath(points: List<Offset>): Path {
    val path = Path()
    if (points.isEmpty()) return path
    path.moveTo(points.first().x, points.first().y)
    for (i in 1 until points.size - 1) {
        val current = points[i]
        val next = points[i + 1]
        val midX = (current.x + next.x) * 0.5f
        val midY = (current.y + next.y) * 0.5f
        path.quadraticTo(current.x, current.y, midX, midY)
    }
    path.lineTo(points.last().x, points.last().y)
    return path
}

// ---------------------------------------------------------------------------
// 2. Touch-Responsive Optical Liquid Glass Surface
// ---------------------------------------------------------------------------

@Composable
fun LiquidGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    cornerRadius: Dp = 22.dp,
    isHighlighted: Boolean = false,
    accentRimColor: Color = Crimson,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable BoxScope.() -> Unit
) {
    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    var pointerPos by remember { mutableStateOf<Offset?>(null) }
    var isTouched by remember { mutableStateOf(false) }

    val restingPos = remember(boxSize) {
        Offset(boxSize.width * 0.24f, boxSize.height * 0.14f)
    }
    val targetPos = pointerPos ?: restingPos

    val animatedTouchPos by animateOffsetAsState(
        targetValue = targetPos,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = if (isTouched) Spring.StiffnessMedium else Spring.StiffnessLow
        ),
        label = "GlassTouchPos"
    )
    val touchGlow by animateFloatAsState(
        targetValue = if (isTouched) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "GlassTouchGlow"
    )
    val highlightFactor by animateFloatAsState(
        targetValue = if (isHighlighted) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "GlassHighlight"
    )
    val animatedBorderColor by animateColorAsState(
        targetValue = if (isHighlighted) LiveRed.copy(alpha = 0.72f) else accentRimColor.copy(alpha = 0.42f),
        animationSpec = tween(260),
        label = "GlassRimColor"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "GlassSheen")
    val glassTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "GlassTime"
    )

    val glassShader = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            RuntimeShader(LIQUID_GLASS_SURFACE_SHADER_SRC)
        } else {
            null
        }
    }

    val density = LocalDensity.current
    val cornerPx = with(density) { cornerRadius.toPx() }

    Box(
        modifier = modifier
            .onSizeChanged { boxSize = it }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val activeChange = event.changes.firstOrNull { it.pressed }
                        if (activeChange != null) {
                            pointerPos = activeChange.position
                            isTouched = true
                        } else {
                            isTouched = false
                            pointerPos = null
                        }
                    }
                }
            }
            .shadow(
                elevation = if (isHighlighted) 22.dp else 14.dp,
                shape = shape,
                ambientColor = AmoledBlack,
                spotColor = if (isHighlighted) LiveRed.copy(alpha = 0.42f) else Crimson.copy(alpha = 0.22f)
            )
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xBA09090D),
                        Color(0xB3040406),
                        Color(0xC8010102)
                    )
                )
            )
            .drawWithContent {
                val w = size.width
                val h = size.height

                // Touch-reactive internal red light scattering
                val scatterAlpha = 0.04f + 0.08f * highlightFactor + 0.10f * touchGlow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            LiveRed.copy(alpha = scatterAlpha),
                            DeepRed.copy(alpha = scatterAlpha * 0.4f),
                            Color.Transparent
                        ),
                        center = animatedTouchPos,
                        radius = maxOf(w, h) * 0.65f
                    ),
                    radius = maxOf(w, h) * 0.65f,
                    center = animatedTouchPos
                )

                // Optical refraction & touch-reactive specular sheen
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && glassShader != null) {
                    glassShader.setFloatUniform("uResolution", w, h)
                    glassShader.setFloatUniform("uTouch", animatedTouchPos.x, animatedTouchPos.y)
                    glassShader.setFloatUniform("uTouchGlow", touchGlow)
                    glassShader.setFloatUniform("uHighlight", highlightFactor)
                    glassShader.setFloatUniform("uTime", glassTime)
                    drawRect(brush = ShaderBrush(glassShader))
                } else {
                    drawFallbackOpticalLayers(
                        touchPos = animatedTouchPos,
                        touchGlow = touchGlow,
                        highlightFactor = highlightFactor,
                        cornerPx = cornerPx
                    )
                }

                // Inner depth shadow rim
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Transparent,
                            AmoledBlack.copy(alpha = 0.6f)
                        )
                    ),
                    cornerRadius = CornerRadius(cornerPx, cornerPx),
                    style = Stroke(width = 3.dp.toPx())
                )

                drawContent()

                // Fresnel highlight + controlled chromatic edge
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            HighlightWhite.copy(alpha = 0.40f + 0.18f * touchGlow),
                            ChromaticCyanEdge.copy(alpha = 0.14f),
                            animatedBorderColor,
                            DeepRed.copy(alpha = 0.55f)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(w * 0.65f, h)
                    ),
                    cornerRadius = CornerRadius(cornerPx, cornerPx),
                    style = Stroke(width = (1.0f + 0.35f * highlightFactor).dp.toPx())
                )

                val glintCenterX = animatedTouchPos.x.coerceIn(w * 0.15f, w * 0.85f)
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            HighlightWhite.copy(alpha = 0.52f + 0.3f * touchGlow),
                            HighlightWhite.copy(alpha = 0.70f + 0.25f * touchGlow),
                            Color.Transparent
                        ),
                        startX = glintCenterX - w * 0.42f,
                        endX = glintCenterX + w * 0.42f
                    ),
                    start = Offset(cornerPx * 0.6f, 1.dp.toPx()),
                    end = Offset(w - cornerPx * 0.6f, 1.dp.toPx()),
                    strokeWidth = 1.35.dp.toPx()
                )
            }
            .padding(contentPadding),
        content = content
    )
}

private fun DrawScope.drawFallbackOpticalLayers(
    touchPos: Offset,
    touchGlow: Float,
    highlightFactor: Float,
    cornerPx: Float
) {
    val w = size.width
    val h = size.height

    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                HighlightWhite.copy(alpha = 0.10f),
                HighlightWhite.copy(alpha = 0.03f),
                Color.Transparent
            ),
            startY = 0f,
            endY = h * 0.48f
        ),
        cornerRadius = CornerRadius(cornerPx, cornerPx)
    )

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                HighlightWhite.copy(alpha = 0.12f + 0.18f * touchGlow),
                HighlightWhite.copy(alpha = 0.04f + 0.06f * touchGlow),
                Color.Transparent
            ),
            center = touchPos,
            radius = min(w, h) * (0.55f + 0.2f * highlightFactor)
        ),
        radius = min(w, h) * (0.55f + 0.2f * highlightFactor),
        center = touchPos
    )
}

// ---------------------------------------------------------------------------
// 3. Circular PLAY Control (Adaptable for Top-Right Corner or Hero Placement)
// ---------------------------------------------------------------------------

@Composable
fun LiquidCircularPlayButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    orbDiameter: Dp = 56.dp,
    caption: String? = null
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    var touchOffset by remember { mutableStateOf<Offset?>(null) }
    var orbSize by remember { mutableStateOf(IntSize.Zero) }

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.90f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "PlayOrbScale"
    )
    val touchGlow by animateFloatAsState(
        targetValue = if (isPressed && enabled) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "PlayOrbTouchGlow"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "PlayOrbAmbient")
    val rimAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PlayOrbRimAngle"
    )
    val auraPulse by infiniteTransition.animateFloat(
        initialValue = 0.80f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PlayOrbAuraPulse"
    )

    val restingSpecular = remember(orbSize) {
        Offset(orbSize.width * 0.34f, orbSize.height * 0.24f)
    }
    val activeSpecular by animateOffsetAsState(
        targetValue = touchOffset ?: restingSpecular,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "PlayOrbSpecular"
    )

    val blurModifier = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Modifier.graphicsLayer {
                renderEffect = AndroidRenderEffect
                    .createBlurEffect(20f, 20f, Shader.TileMode.DECAL)
                    .asComposeRenderEffect()
            }
        } else {
            Modifier
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(orbDiameter + 12.dp),
            contentAlignment = Alignment.Center
        ) {
            // Tight localized crimson rim aura right around the orb (keeps AMOLED background pure black)
            Box(
                modifier = Modifier
                    .size(orbDiameter + 6.dp)
                    .then(blurModifier)
                    .drawBehind {
                        val glowAlpha = if (enabled) 0.40f * auraPulse + 0.20f * touchGlow else 0.08f
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    LiveRed.copy(alpha = glowAlpha),
                                    Crimson.copy(alpha = glowAlpha * 0.5f),
                                    Color.Transparent
                                )
                            )
                        )
                    }
            )

            // Main Circular Optical Glass Orb
            Box(
                modifier = Modifier
                    .size(orbDiameter)
                    .minimumInteractiveComponentSize()
                    .graphicsLayer {
                        scaleX = pressScale
                        scaleY = pressScale
                    }
                    .testTag("calculate_play_button")
                    .semantics {
                        contentDescription = if (enabled) {
                            "Calculate firing solution"
                        } else {
                            "Calculate disabled, resolve validation warnings first"
                        }
                    }
                    .onSizeChanged { orbSize = it }
                    .pointerInput(enabled) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                val down = event.changes.firstOrNull { it.pressed }
                                touchOffset = if (enabled && down != null) down.position else null
                            }
                        }
                    }
                    .shadow(
                        elevation = if (enabled) 18.dp else 4.dp,
                        shape = CircleShape,
                        ambientColor = AmoledBlack,
                        spotColor = if (enabled) LiveRed else DeepRed
                    )
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = if (enabled) {
                                listOf(
                                    Color(0xE622050C),
                                    Color(0xEE0D0406),
                                    Color(0xFA030304)
                                )
                            } else {
                                listOf(
                                    Color(0xCC111114),
                                    Color(0xE6060608)
                                )
                            }
                        )
                    )
                    .clickable(
                        enabled = enabled,
                        interactionSource = interactionSource,
                        indication = ripple(bounded = true, color = LiveRed),
                        role = Role.Button,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onClick()
                        }
                    )
                    .drawBehind {
                        val r = size.minDimension / 2f
                        val c = Offset(size.width / 2f, size.height / 2f)

                        if (enabled) {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        LiveRed.copy(alpha = 0.25f + 0.18f * touchGlow),
                                        Crimson.copy(alpha = 0.14f),
                                        Color.Transparent
                                    ),
                                    center = c,
                                    radius = r * 0.92f
                                ),
                                radius = r * 0.92f,
                                center = c
                            )
                        }

                        drawCircle(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    HighlightWhite.copy(alpha = if (enabled) 0.28f else 0.10f),
                                    HighlightWhite.copy(alpha = 0.05f),
                                    Color.Transparent
                                ),
                                startY = 0f,
                                endY = size.height * 0.56f
                            ),
                            radius = r * 0.94f,
                            center = c
                        )

                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    HighlightWhite.copy(alpha = if (enabled) 0.36f + 0.34f * touchGlow else 0.12f),
                                    LiveRed.copy(alpha = if (enabled) 0.16f * touchGlow else 0f),
                                    Color.Transparent
                                ),
                                center = activeSpecular,
                                radius = r * 0.72f
                            ),
                            radius = r * 0.72f,
                            center = activeSpecular
                        )

                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    AmoledBlack.copy(alpha = 0.65f)
                                ),
                                center = Offset(c.x, c.y - r * 0.08f),
                                radius = r
                            ),
                            radius = r,
                            center = c,
                            style = Stroke(width = 3.dp.toPx())
                        )

                        rotate(degrees = rimAngle, pivot = c) {
                            drawCircle(
                                brush = Brush.sweepGradient(
                                    colors = if (enabled) {
                                        listOf(
                                            HighlightWhite.copy(alpha = 0.85f),
                                            LiveRed.copy(alpha = 0.82f),
                                            DeepRed.copy(alpha = 0.35f),
                                            ChromaticCyanEdge.copy(alpha = 0.35f),
                                            HighlightWhite.copy(alpha = 0.85f)
                                        )
                                    } else {
                                        listOf(
                                            SecondaryText.copy(alpha = 0.25f),
                                            MutedText.copy(alpha = 0.12f),
                                            SecondaryText.copy(alpha = 0.25f)
                                        )
                                    },
                                    center = c
                                ),
                                radius = r - 1.dp.toPx(),
                                center = c,
                                style = Stroke(width = 1.4.dp.toPx())
                            )
                        }

                        // Sculpted Luminous Red Play Triangle Icon
                        val iconRadius = r * 0.40f
                        val shiftX = iconRadius * 0.14f
                        val p1 = Offset(c.x - iconRadius * 0.58f + shiftX, c.y - iconRadius * 0.74f)
                        val p2 = Offset(c.x + iconRadius * 0.92f + shiftX, c.y)
                        val p3 = Offset(c.x - iconRadius * 0.58f + shiftX, c.y + iconRadius * 0.74f)

                        val playPath = Path().apply {
                            moveTo(p1.x, p1.y)
                            lineTo(p2.x, p2.y)
                            lineTo(p3.x, p3.y)
                            close()
                        }

                        if (enabled) {
                            drawPath(
                                path = playPath,
                                color = LiveRed.copy(alpha = 0.40f + 0.22f * touchGlow),
                                style = Stroke(
                                    width = 4.5.dp.toPx(),
                                    join = StrokeJoin.Round
                                )
                            )
                        }

                        drawPath(
                            path = playPath,
                            brush = Brush.verticalGradient(
                                colors = if (enabled) {
                                    listOf(
                                        Color(0xFFFF5277),
                                        LiveRed,
                                        Crimson
                                    )
                                } else {
                                    listOf(
                                        SecondaryText.copy(alpha = 0.45f),
                                        MutedText.copy(alpha = 0.35f)
                                    )
                                }
                            )
                        )

                        drawPath(
                            path = playPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    HighlightWhite.copy(alpha = if (enabled) 0.92f else 0.25f),
                                    LiveRed.copy(alpha = if (enabled) 0.55f else 0.10f)
                                )
                            ),
                            style = Stroke(width = 1.dp.toPx(), join = StrokeJoin.Round)
                        )
                    },
                contentAlignment = Alignment.Center
            ) {}
        }

        if (!caption.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (enabled) caption else "INVALID",
                style = OscTextStyles.MicroLabel,
                color = if (enabled) HighlightWhite.copy(alpha = 0.85f) else LiveRed.copy(alpha = 0.85f)
            )
        }
    }
}
