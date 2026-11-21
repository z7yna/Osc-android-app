package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

/**
 * Primary Optical Liquid Glass Card backed by LiquidGlassSurface.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    borderColor: Color = Crimson,
    isHighlighted: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    LiquidGlassSurface(
        modifier = modifier,
        shape = shape,
        cornerRadius = 22.dp,
        isHighlighted = isHighlighted,
        accentRimColor = borderColor,
        contentPadding = PaddingValues(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}

/**
 * Touch-reactive Optical Glass Pill / Action Button (used for New Calculation, etc.)
 */
@Composable
fun GlassActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isPrimaryRed: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    var touchPos by remember { mutableStateOf<Offset?>(null) }
    var btnSize by remember { mutableStateOf(IntSize.Zero) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.965f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "GlassBtnScale"
    )
    val touchGlow by animateFloatAsState(
        targetValue = if (isPressed && enabled) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "GlassBtnGlow"
    )
    val animatedTouch by animateOffsetAsState(
        targetValue = touchPos ?: Offset(btnSize.width * 0.5f, btnSize.height * 0.2f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "GlassBtnTouch"
    )

    val shape = RoundedCornerShape(999.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp)
            .minimumInteractiveComponentSize()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .onSizeChanged { btnSize = it }
            .pointerInput(enabled) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val active = event.changes.firstOrNull { it.pressed }
                        touchPos = if (enabled && active != null) active.position else null
                    }
                }
            }
            .shadow(
                elevation = if (enabled && isPrimaryRed) 18.dp else 8.dp,
                shape = shape,
                ambientColor = AmoledBlack,
                spotColor = if (enabled && isPrimaryRed) LiveRed.copy(alpha = 0.55f) else Crimson.copy(alpha = 0.2f)
            )
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = when {
                        !enabled -> listOf(Color(0x99121216), Color(0xB309090B))
                        isPrimaryRed -> listOf(Color(0xE63D0612), Color(0xE61F0208), Color(0xF20D0104))
                        else -> listOf(Color(0xD918181D), Color(0xE60B0B0F))
                    }
                )
            )
            .drawBehind {
                val w = size.width
                val h = size.height
                val radius = h / 2f

                // Diffused internal red luminescence
                if (enabled && isPrimaryRed) {
                    drawRoundRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                LiveRed.copy(alpha = 0.26f + 0.22f * touchGlow),
                                Crimson.copy(alpha = 0.14f),
                                Color.Transparent
                            ),
                            center = animatedTouch,
                            radius = w * 0.55f
                        ),
                        cornerRadius = CornerRadius(radius, radius)
                    )
                }

                // Upper meniscus sheen
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            HighlightWhite.copy(alpha = if (enabled) 0.22f else 0.06f),
                            Color.Transparent
                        ),
                        startY = 0f,
                        endY = h * 0.52f
                    ),
                    cornerRadius = CornerRadius(radius, radius)
                )

                // Touch-reactive specular hotspot
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            HighlightWhite.copy(alpha = if (enabled) 0.24f * touchGlow else 0f),
                            Color.Transparent
                        ),
                        center = animatedTouch,
                        radius = h * 1.4f
                    ),
                    radius = h * 1.4f,
                    center = animatedTouch
                )

                // Fresnel edge rim
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            HighlightWhite.copy(alpha = if (enabled) 0.55f else 0.18f),
                            if (enabled && isPrimaryRed) LiveRed.copy(alpha = 0.78f) else GlassHairlineWhite,
                            if (enabled && isPrimaryRed) DeepRed else Color(0x33FFFFFF)
                        )
                    ),
                    cornerRadius = CornerRadius(radius, radius),
                    style = Stroke(width = 1.2.dp.toPx())
                )
            }
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = ripple(color = if (isPrimaryRed) LiveRed else HighlightWhite),
                role = Role.Button,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                }
            )
            .padding(horizontal = 20.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (leadingIcon != null) {
                leadingIcon()
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = OscTextStyles.SectionTitle,
                color = if (enabled) HighlightWhite else MutedText
            )
        }
    }
}

/**
 * Recessed optical smoked-obsidian glass lens well for coordinate and parameter entry.
 */
@Composable
fun LiquidGlassWell(
    label: String,
    value: String,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    sublabel: String? = null,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.965f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "WellScale"
    )
    val activeProgress by animateFloatAsState(
        targetValue = if (isActive) 1f else 0f,
        animationSpec = tween(220),
        label = "WellActive"
    )
    val labelColor by animateColorAsState(
        targetValue = if (isActive) LiveRed else SecondaryText,
        animationSpec = tween(200),
        label = "WellLabelColor"
    )

    val shape = RoundedCornerShape(14.dp)

    Box(
        modifier = modifier
            .heightIn(min = 58.dp)
            .minimumInteractiveComponentSize()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = if (isActive) 12.dp else 2.dp,
                shape = shape,
                ambientColor = AmoledBlack,
                spotColor = if (isActive) LiveRed.copy(alpha = 0.55f) else AmoledBlack
            )
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xEB040406),
                        Color(0xDF09090D),
                        Color(0xD4101016)
                    )
                )
            )
            .drawBehind {
                val w = size.width
                val h = size.height
                val r = 14.dp.toPx()

                // Active internal crimson glow
                if (activeProgress > 0.01f) {
                    drawRoundRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                LiveRed.copy(alpha = 0.24f * activeProgress),
                                DeepRed.copy(alpha = 0.14f * activeProgress),
                                Color.Transparent
                            ),
                            center = Offset(w * 0.5f, h * 0.65f),
                            radius = maxOf(w, h) * 0.78f
                        ),
                        cornerRadius = CornerRadius(r, r)
                    )
                }

                // Recessed inner top shadow + subtle bottom catchlight (authentic carved lens depth)
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            AmoledBlack.copy(alpha = 0.7f),
                            Color.Transparent,
                            HighlightWhite.copy(alpha = 0.05f)
                        )
                    ),
                    cornerRadius = CornerRadius(r, r)
                )

                // Top recessed highlight rim
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            if (isActive) LiveRed.copy(alpha = 0.85f) else HighlightWhite.copy(alpha = 0.25f),
                            Color.Transparent
                        )
                    ),
                    start = Offset(10.dp.toPx(), 1.dp.toPx()),
                    end = Offset(w - 10.dp.toPx(), 1.dp.toPx()),
                    strokeWidth = 1.dp.toPx()
                )

                // Outer edge rim
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = if (isActive) {
                            listOf(
                                HighlightWhite.copy(alpha = 0.55f),
                                LiveRed,
                                Crimson.copy(alpha = 0.65f)
                            )
                        } else {
                            listOf(
                                Color(0x22FFFFFF),
                                Color(0x14FFFFFF),
                                Color(0x2EFFFFFF)
                            )
                        }
                    ),
                    cornerRadius = CornerRadius(r, r),
                    style = Stroke(width = if (isActive) 1.35.dp.toPx() else 0.9.dp.toPx())
                )
            }
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = LiveRed),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                }
            )
            .padding(horizontal = 12.dp, vertical = 9.dp)
    ) {
        Column(verticalArrangement = Arrangement.Center) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = OscTextStyles.MicroLabel,
                    fontSize = 9.5.sp,
                    color = labelColor,
                    maxLines = 1
                )
                if (sublabel != null) {
                    Text(
                        text = sublabel,
                        style = OscTextStyles.MicroLabel,
                        fontSize = 8.5.sp,
                        color = MutedText,
                        maxLines = 1
                    )
                }
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = if (value.isEmpty()) "0" else value,
                style = OscTextStyles.NumericInput,
                color = HighlightWhite,
                maxLines = 1
            )
        }
    }
}

/**
 * Compact touch-reactive optical glass chip/pill with >=48dp minimum touch target compliance.
 */
@Composable
fun LiquidGlassPill(
    label: String,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "PillScale"
    )

    val shape = RoundedCornerShape(999.dp)

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
                indication = ripple(color = LiveRed),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .heightIn(min = 32.dp)
                .shadow(
                    elevation = if (isSelected) 10.dp else 0.dp,
                    shape = shape,
                    spotColor = LiveRed.copy(alpha = 0.6f)
                )
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        colors = if (isSelected) {
                            listOf(Color(0xE64D0817), Color(0xEB220209))
                        } else {
                            listOf(Color(0xB30F0F13), Color(0xCC060608))
                        }
                    )
                )
                .border(
                    width = if (isSelected) 1.1.dp else 0.8.dp,
                    brush = Brush.verticalGradient(
                        colors = if (isSelected) {
                            listOf(HighlightWhite.copy(alpha = 0.6f), LiveRed)
                        } else {
                            listOf(Color(0x33FFFFFF), Color(0x14FFFFFF))
                        }
                    ),
                    shape = shape
                )
                .padding(horizontal = 11.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = OscTextStyles.MicroLabel,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) HighlightWhite else SecondaryText,
                maxLines = 1
            )
        }
    }
}

/**
 * Optical Status Capsule for READY / ERROR indicator
 */
@Composable
fun StatusBadge(
    isValid: Boolean,
    modifier: Modifier = Modifier
) {
    val accent = if (isValid) StatusReadyGreen else LiveRed
    val bg = if (isValid) StatusReadyBg else StatusErrorBg
    val text = if (isValid) "READY" else "ERROR"

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .border(0.9.dp, accent.copy(alpha = 0.65f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(accent)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            style = OscTextStyles.MicroLabel,
            fontSize = 10.sp,
            color = HighlightWhite
        )
    }
}
