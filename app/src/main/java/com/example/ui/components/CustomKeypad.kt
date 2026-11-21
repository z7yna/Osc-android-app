package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.haptics.rememberHapticFeedbackManager
import com.example.ui.theme.*
import kotlin.math.PI
import kotlin.math.sin

enum class ActiveField(val label: String, val allowsNegative: Boolean, val allowsDecimal: Boolean) {
    ORIGIN_X("Origin X", true, true),
    ORIGIN_Y("Origin Y", true, true),
    ORIGIN_Z("Origin Z", true, true),
    TARGET_X("Target X", true, true),
    TARGET_Y("Target Y", true, true),
    TARGET_Z("Target Z", true, true),
    PASSCODE("Passcode", false, false),
    NUKE_SIZE("Nuke Size", false, false),
    STAB_DEPTH("Stab Depth", false, false),
    MAGAZINE_SLOT("Magazine Slot", false, false)
}

@Composable
fun CustomKeypad(
    activeField: ActiveField?,
    currentValue: String,
    onDigitPress: (String) -> Unit,
    onToggleSign: () -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onPreviousField: () -> Unit,
    onNextField: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hapticManager = rememberHapticFeedbackManager()

    var traySize by remember { mutableStateOf(IntSize.Zero) }
    var trayPointer by remember { mutableStateOf<Offset?>(null) }
    var isTrayTouched by remember { mutableStateOf(false) }

    val restingSpot = remember(traySize) {
        Offset(traySize.width * 0.5f, traySize.height * 0.12f)
    }
    val animatedTrayTouch by animateOffsetAsState(
        targetValue = trayPointer ?: restingSpot,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = if (isTrayTouched) Spring.StiffnessMedium else Spring.StiffnessLow
        ),
        label = "KeypadTrayTouch"
    )
    val trayGlow by animateFloatAsState(
        targetValue = if (isTrayTouched) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "KeypadTrayGlow"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "KeypadOptics")
    val horizonPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(7000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "KeypadHorizon"
    )
    val liveDotPulse by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "KeypadLiveDot"
    )

    AnimatedVisibility(
        visible = activeField != null,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        ) + fadeIn(),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = spring(stiffness = Spring.StiffnessMedium)
        ) + fadeOut(),
        modifier = modifier
    ) {
        if (activeField == null) return@AnimatedVisibility

        val trayShape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { traySize = it }
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val active = event.changes.firstOrNull { it.pressed }
                            if (active != null) {
                                trayPointer = active.position
                                isTrayTouched = true
                            } else {
                                isTrayTouched = false
                                trayPointer = null
                            }
                        }
                    }
                }
                .shadow(
                    elevation = 36.dp,
                    shape = trayShape,
                    ambientColor = AmoledBlack,
                    spotColor = LiveRed.copy(alpha = 0.65f)
                )
                .clip(trayShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xEE0C0C11),
                            Color(0xF7040406),
                            Color(0xFC000000)
                        )
                    )
                )
                .drawBehind {
                    val w = size.width
                    val h = size.height

                    // Live touch-reactive internal glass reflection field
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                LiveRed.copy(alpha = 0.14f + 0.16f * trayGlow),
                                Crimson.copy(alpha = 0.08f + 0.06f * trayGlow),
                                Color.Transparent
                            ),
                            center = animatedTrayTouch,
                            radius = w * 0.68f
                        ),
                        radius = w * 0.68f,
                        center = animatedTrayTouch
                    )

                    // Animated gliding top optical horizon glint + touch-following catchlight
                    val shimmerX = w * (0.5f + 0.34f * sin(horizonPhase))
                    val glintCenterX = if (isTrayTouched) animatedTrayTouch.x else shimmerX
                    drawLine(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Crimson.copy(alpha = 0.6f),
                                HighlightWhite.copy(alpha = 0.85f + 0.15f * trayGlow),
                                LiveRed.copy(alpha = 0.9f),
                                HighlightWhite.copy(alpha = 0.75f),
                                Color.Transparent
                            ),
                            startX = glintCenterX - w * 0.45f,
                            endX = glintCenterX + w * 0.45f
                        ),
                        start = Offset(24.dp.toPx(), 1.2.dp.toPx()),
                        end = Offset(w - 24.dp.toPx(), 1.2.dp.toPx()),
                        strokeWidth = 1.8.dp.toPx()
                    )

                    // Subtle side chromatic edge refraction
                    drawLine(
                        brush = Brush.verticalGradient(
                            listOf(HighlightWhite.copy(alpha = 0.3f), LiveRed.copy(alpha = 0.4f), Color.Transparent)
                        ),
                        start = Offset(1.dp.toPx(), 24.dp.toPx()),
                        end = Offset(1.dp.toPx(), h),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawLine(
                        brush = Brush.verticalGradient(
                            listOf(HighlightWhite.copy(alpha = 0.3f), LiveRed.copy(alpha = 0.4f), Color.Transparent)
                        ),
                        start = Offset(w - 1.dp.toPx(), 24.dp.toPx()),
                        end = Offset(w - 1.dp.toPx(), h),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            HighlightWhite.copy(alpha = 0.46f),
                            LiveRed.copy(alpha = 0.55f),
                            Color.Transparent
                        )
                    ),
                    shape = trayShape
                )
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Sleek Top Drag / Optical Handle
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 10.dp)
                        .width(42.dp)
                        .height(3.5.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0x33FFFFFF),
                                    HighlightWhite.copy(alpha = 0.7f),
                                    LiveRed.copy(alpha = 0.7f),
                                    Color(0x33FFFFFF)
                                )
                            )
                        )
                )

                // Recessed Obsidian Header Readout Capsule + Navigation Controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xCC030305))
                            .border(0.8.dp, Color(0x26FFFFFF), RoundedCornerShape(14.dp))
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(LiveRed.copy(alpha = liveDotPulse))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = activeField.label.uppercase(),
                                style = OscTextStyles.MicroLabel,
                                fontSize = 9.sp,
                                color = LiveRed
                            )
                            Text(
                                text = if (currentValue.isEmpty()) "0" else currentValue,
                                style = OscTextStyles.NumericHero,
                                fontSize = 20.sp,
                                color = HighlightWhite,
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LiquidKeySmall(
                            modifier = Modifier.testTag("keypad_prev_field"),
                            onClick = {
                                hapticManager.performKeyTap()
                                onPreviousField()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = "Previous Field",
                                tint = HighlightWhite,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        LiquidKeySmall(
                            modifier = Modifier.testTag("keypad_next_field"),
                            onClick = {
                                hapticManager.performKeyTap()
                                onNextField()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Next Field",
                                tint = HighlightWhite,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        LiquidDoneButton(
                            onClick = {
                                hapticManager.performKeyTap()
                                onDismiss()
                            }
                        )
                    }
                }

                // Optical Key Deck Container
                val rowHeight = 50.dp
                val spacing = 8.dp

                // Row 1: 1, 2, 3, +/-
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    LiquidNumKey("1", Modifier.weight(1f).height(rowHeight).testTag("keypad_1")) {
                        hapticManager.performKeyTap()
                        onDigitPress("1")
                    }
                    LiquidNumKey("2", Modifier.weight(1f).height(rowHeight).testTag("keypad_2")) {
                        hapticManager.performKeyTap()
                        onDigitPress("2")
                    }
                    LiquidNumKey("3", Modifier.weight(1f).height(rowHeight).testTag("keypad_3")) {
                        hapticManager.performKeyTap()
                        onDigitPress("3")
                    }
                    LiquidNumKey(
                        text = "+ / −",
                        modifier = Modifier.weight(1f).height(rowHeight).testTag("keypad_sign"),
                        isAction = true,
                        enabled = activeField.allowsNegative
                    ) {
                        hapticManager.performKeyTap()
                        onToggleSign()
                    }
                }

                Spacer(modifier = Modifier.height(spacing))

                // Row 2: 4, 5, 6, Backspace
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    LiquidNumKey("4", Modifier.weight(1f).height(rowHeight).testTag("keypad_4")) {
                        hapticManager.performKeyTap()
                        onDigitPress("4")
                    }
                    LiquidNumKey("5", Modifier.weight(1f).height(rowHeight).testTag("keypad_5")) {
                        hapticManager.performKeyTap()
                        onDigitPress("5")
                    }
                    LiquidNumKey("6", Modifier.weight(1f).height(rowHeight).testTag("keypad_6")) {
                        hapticManager.performKeyTap()
                        onDigitPress("6")
                    }
                    LiquidIconKey(
                        icon = Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = "Backspace",
                        modifier = Modifier.weight(1f).height(rowHeight).testTag("keypad_backspace"),
                        isAction = true
                    ) {
                        hapticManager.performKeyTap()
                        onBackspace()
                    }
                }

                Spacer(modifier = Modifier.height(spacing))

                // Row 3: 7, 8, 9, CLR
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    LiquidNumKey("7", Modifier.weight(1f).height(rowHeight).testTag("keypad_7")) {
                        hapticManager.performKeyTap()
                        onDigitPress("7")
                    }
                    LiquidNumKey("8", Modifier.weight(1f).height(rowHeight).testTag("keypad_8")) {
                        hapticManager.performKeyTap()
                        onDigitPress("8")
                    }
                    LiquidNumKey("9", Modifier.weight(1f).height(rowHeight).testTag("keypad_9")) {
                        hapticManager.performKeyTap()
                        onDigitPress("9")
                    }
                    LiquidNumKey(
                        text = "CLR",
                        modifier = Modifier.weight(1f).height(rowHeight).testTag("keypad_clear"),
                        isAction = true
                    ) {
                        hapticManager.performKeyTap()
                        onClear()
                    }
                }

                Spacer(modifier = Modifier.height(spacing))

                // Row 4: ., 0, 00, ±16
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    LiquidNumKey(
                        text = ".",
                        modifier = Modifier.weight(1f).height(rowHeight).testTag("keypad_dot"),
                        enabled = activeField.allowsDecimal
                    ) {
                        hapticManager.performKeyTap()
                        onDigitPress(".")
                    }
                    LiquidNumKey("0", Modifier.weight(1f).height(rowHeight).testTag("keypad_0")) {
                        hapticManager.performKeyTap()
                        onDigitPress("0")
                    }
                    LiquidNumKey("00", Modifier.weight(1f).height(rowHeight).testTag("keypad_00")) {
                        hapticManager.performKeyTap()
                        onDigitPress("00")
                    }
                    LiquidNumKey(
                        text = "±16",
                        modifier = Modifier.weight(1f).height(rowHeight).testTag("keypad_snap16"),
                        isAction = true
                    ) {
                        hapticManager.performKeyTap()
                        val num = currentValue.toIntOrNull() ?: 0
                        val rounded = if (num % 16 == 0) num + 16 else Math.round(num / 16.0f) * 16
                        onClear()
                        onDigitPress(rounded.toString())
                    }
                }
            }
        }
    }
}

@Composable
private fun LiquidDoneButton(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.91f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "DoneBtnScale"
    )
    val shape = RoundedCornerShape(999.dp)

    Box(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .testTag("keypad_done")
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = LiveRed),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .heightIn(min = 38.dp)
                .shadow(12.dp, shape, spotColor = LiveRed.copy(alpha = 0.75f))
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xEB66091E), Color(0xF22B020B))
                    )
                )
                .drawBehind {
                    drawLine(
                        brush = Brush.horizontalGradient(
                            listOf(Color.Transparent, HighlightWhite.copy(alpha = 0.7f), Color.Transparent)
                        ),
                        start = Offset(10.dp.toPx(), 1.dp.toPx()),
                        end = Offset(size.width - 10.dp.toPx(), 1.dp.toPx()),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(HighlightWhite.copy(alpha = 0.65f), LiveRed)
                    ),
                    shape = shape
                )
                .padding(horizontal = 14.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Done",
                    tint = HighlightWhite,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "DONE",
                    style = OscTextStyles.MicroLabel,
                    color = HighlightWhite
                )
            }
        }
    }
}

@Composable
private fun LiquidNumKey(
    text: String,
    modifier: Modifier = Modifier,
    isAction: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.90f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "KeyScale"
    )
    val glow by animateFloatAsState(
        targetValue = if (isPressed && enabled) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "KeyGlow"
    )

    val shape = RoundedCornerShape(16.dp)
    val textColor = when {
        !enabled -> MutedText.copy(alpha = 0.42f)
        isAction -> LiveRed
        else -> HighlightWhite
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = if (enabled) 6.dp else 0.dp,
                shape = shape,
                ambientColor = AmoledBlack,
                spotColor = if (isAction) LiveRed.copy(alpha = 0.48f) else AmoledBlack
            )
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = when {
                        !enabled -> listOf(Color(0x3D09090B), Color(0x55040406))
                        isAction -> listOf(Color(0xE628050E), Color(0xEE130206), Color(0xF5070103))
                        else -> listOf(Color(0xD91A1A22), Color(0xE60D0D12), Color(0xF0050507))
                    }
                )
            )
            .drawBehind {
                val w = size.width
                val h = size.height
                val r = 16.dp.toPx()

                if (enabled) {
                    // Curved upper liquid glass meniscus highlight
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                HighlightWhite.copy(alpha = 0.16f + 0.16f * glow),
                                HighlightWhite.copy(alpha = 0.03f),
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = h * 0.50f
                        ),
                        cornerRadius = CornerRadius(r, r)
                    )

                    // Inner depth bottom shadow
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, AmoledBlack.copy(alpha = 0.65f)),
                            startY = h * 0.55f,
                            endY = h
                        ),
                        cornerRadius = CornerRadius(r, r)
                    )

                    // Radial specular press flash
                    if (glow > 0.01f) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    (if (isAction) LiveRed else HighlightWhite).copy(alpha = 0.34f * glow),
                                    LiveRed.copy(alpha = 0.16f * glow),
                                    Color.Transparent
                                ),
                                center = Offset(w * 0.5f, h * 0.5f),
                                radius = w * 0.68f
                            ),
                            radius = w * 0.68f,
                            center = Offset(w * 0.5f, h * 0.5f)
                        )
                    }

                    // Top optical catchlight hairline
                    drawLine(
                        brush = Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                HighlightWhite.copy(alpha = 0.55f + 0.35f * glow),
                                Color.Transparent
                            )
                        ),
                        start = Offset(10.dp.toPx(), 1.dp.toPx()),
                        end = Offset(w - 10.dp.toPx(), 1.dp.toPx()),
                        strokeWidth = 1.dp.toPx()
                    )

                    // Fresnel & chromatic rim
                    drawRoundRect(
                        brush = Brush.linearGradient(
                            colors = if (isAction) {
                                listOf(
                                    HighlightWhite.copy(alpha = 0.45f + 0.3f * glow),
                                    LiveRed.copy(alpha = 0.78f),
                                    DeepRed
                                )
                            } else {
                                listOf(
                                    HighlightWhite.copy(alpha = 0.38f + 0.32f * glow),
                                    ChromaticCyanEdge.copy(alpha = 0.22f),
                                    Crimson.copy(alpha = 0.30f + 0.35f * glow)
                                )
                            }
                        ),
                        cornerRadius = CornerRadius(r, r),
                        style = Stroke(width = (0.9f + 0.3f * glow).dp.toPx())
                    )
                }
            }
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = ripple(color = if (isAction) LiveRed else HighlightWhite),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = OscTextStyles.NumericInput.copy(
                fontSize = if (text.length > 2) 14.sp else 19.sp
            ),
            color = textColor
        )
    }
}

@Composable
private fun LiquidIconKey(
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    isAction: Boolean = false,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "IconKeyScale"
    )
    val glow by animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "IconKeyGlow"
    )
    val shape = RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(6.dp, shape, spotColor = LiveRed.copy(alpha = 0.45f))
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = if (isAction) {
                        listOf(Color(0xE628050E), Color(0xEE130206), Color(0xF5070103))
                    } else {
                        listOf(Color(0xD91A1A22), Color(0xE60D0D12))
                    }
                )
            )
            .drawBehind {
                drawLine(
                    brush = Brush.horizontalGradient(
                        listOf(Color.Transparent, HighlightWhite.copy(alpha = 0.55f + 0.35f * glow), Color.Transparent)
                    ),
                    start = Offset(10.dp.toPx(), 1.dp.toPx()),
                    end = Offset(size.width - 10.dp.toPx(), 1.dp.toPx()),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .border(
                width = (0.9f + 0.3f * glow).dp,
                brush = Brush.verticalGradient(
                    colors = listOf(HighlightWhite.copy(alpha = 0.45f + 0.3f * glow), LiveRed.copy(alpha = 0.75f))
                ),
                shape = shape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = LiveRed),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = LiveRed,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun LiquidKeySmall(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "SmallKeyScale"
    )
    val shape = RoundedCornerShape(12.dp)

    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = HighlightWhite),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(shape)
                .background(Brush.verticalGradient(listOf(Color(0xD91A1A22), Color(0xEE09090D))))
                .border(
                    width = 0.9.dp,
                    brush = Brush.verticalGradient(
                        listOf(HighlightWhite.copy(alpha = 0.42f), Crimson.copy(alpha = 0.45f))
                    ),
                    shape = shape
                ),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}
