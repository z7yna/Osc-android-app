package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NorthWest
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.CannonOriginVariant
import com.example.domain.Coordinates
import com.example.domain.FireMode
import com.example.domain.FiringHistoryEntry
import com.example.domain.SFC_PAYLOAD_SIZES
import com.example.ui.FireControlUiState
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun InputScreen(
    state: FireControlUiState,
    onSelectField: (ActiveField) -> Unit,
    onSetCannonVariant: (CannonOriginVariant) -> Unit,
    onSetFireMode: (FireMode) -> Unit,
    onSetOriginPreset: (Coordinates) -> Unit,
    onCalculate: () -> Unit,
    onOpenHistory: () -> Unit,
    onApplyHistoryEntry: (FiringHistoryEntry) -> Unit,
    modifier: Modifier = Modifier,
    onScrollProgressChanged: (Float) -> Unit = {}
) {
    val scrollState = rememberScrollState()

    val scrollProgress by remember {
        derivedStateOf {
            if (scrollState.maxValue > 0) {
                (scrollState.value.toFloat() / scrollState.maxValue.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }
        }
    }
    LaunchedEffect(scrollProgress) {
        onScrollProgressChanged(scrollProgress)
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = if (state.activeField != null) 320.dp else 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Optical Header with Top-Right Circular PLAY Calculate Button & History Trigger
            AppHeader(
                selectedVariant = state.cannonVariant,
                canCalculate = state.validationResult.isValid,
                historyCount = state.history.size,
                onSelectVariant = onSetCannonVariant,
                onCalculate = onCalculate,
                onOpenHistory = onOpenHistory
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Cannon Origin Card
            CannonOriginCard(
                state = state,
                onSelectField = onSelectField,
                onSetPreset = onSetOriginPreset
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Target Coordinates Card
            TargetCard(
                state = state,
                onSelectField = onSelectField,
                onSetFireMode = onSetFireMode
            )

            // Animated Validation Warnings Glass Banner
            AnimatedVisibility(
                visible = !state.validationResult.isValid,
                enter = expandVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                exit = shrinkVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(14.dp))
                    ValidationErrorPanel(errors = state.validationResult.errors)
                }
            }

            // Recent Firing History Quick-Access Strip (up to 5 stored)
            Spacer(modifier = Modifier.height(16.dp))
            RecentHistoryStripCard(
                history = state.history,
                onOpenHistory = onOpenHistory,
                onApplyHistoryEntry = onApplyHistoryEntry
            )
        }
    }
}

@Composable
private fun AppHeader(
    selectedVariant: CannonOriginVariant,
    canCalculate: Boolean,
    historyCount: Int,
    onSelectVariant: (CannonOriginVariant) -> Unit,
    onCalculate: () -> Unit,
    onOpenHistory: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Row: Brand Identity (left) + History & Calculate Play Orb (top-right corner)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .shadow(14.dp, CircleShape, spotColor = LiveRed.copy(alpha = 0.65f))
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xE63D0612),
                                    Color(0xE6140307),
                                    Color(0xF5050507)
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                colors = listOf(HighlightWhite.copy(alpha = 0.72f), LiveRed, DeepRed)
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.GpsFixed,
                        contentDescription = "OSC Fire Control Emblem",
                        tint = HighlightWhite,
                        modifier = Modifier.size(19.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = "OSC FIRE CONTROL",
                        style = MaterialTheme.typography.titleLarge,
                        color = HighlightWhite,
                        letterSpacing = 1.2.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "ORBITAL STRIKE · MK 6",
                        style = OscTextStyles.MicroLabel,
                        fontSize = 9.sp,
                        color = LiveRed
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // History Button
                HeaderHistoryButton(
                    count = historyCount,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onOpenHistory()
                    }
                )

                // Top-Right Corner Circular PLAY Calculate Button
                LiquidCircularPlayButton(
                    onClick = onCalculate,
                    enabled = canCalculate,
                    orbDiameter = 54.dp,
                    caption = null
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Optical Glass Segmented Bar for Cannon Variant
        LiquidGlassSurface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(999.dp),
            cornerRadius = 999.dp,
            accentRimColor = Crimson,
            contentPadding = PaddingValues(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                CannonOriginVariant.entries.forEach { variant ->
                    val isSelected = selectedVariant == variant
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    val scale by animateFloatAsState(
                        targetValue = if (isPressed) 0.95f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "VariantScale"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 40.dp)
                            .minimumInteractiveComponentSize()
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                            .testTag("variant_${variant.id}")
                            .shadow(
                                elevation = if (isSelected) 12.dp else 0.dp,
                                shape = RoundedCornerShape(999.dp),
                                spotColor = LiveRed.copy(alpha = 0.7f)
                            )
                            .clip(RoundedCornerShape(999.dp))
                            .then(
                                if (isSelected) {
                                    Modifier.background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color(0xE6520718),
                                                Color(0xEB27020A)
                                            )
                                        )
                                    )
                                } else {
                                    Modifier.background(Color.Transparent)
                                }
                            )
                            .drawBehind {
                                if (isSelected) {
                                    drawLine(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                HighlightWhite.copy(alpha = 0.65f),
                                                Color.Transparent
                                            )
                                        ),
                                        start = Offset(12.dp.toPx(), 1.dp.toPx()),
                                        end = Offset(size.width - 12.dp.toPx(), 1.dp.toPx()),
                                        strokeWidth = 1.2.dp.toPx()
                                    )
                                }
                            }
                            .border(
                                width = if (isSelected) 1.dp else 0.dp,
                                brush = Brush.verticalGradient(
                                    colors = if (isSelected) {
                                        listOf(HighlightWhite.copy(alpha = 0.58f), LiveRed)
                                    } else {
                                        listOf(Color.Transparent, Color.Transparent)
                                    }
                                ),
                                shape = RoundedCornerShape(999.dp)
                            )
                            .clickable(
                                interactionSource = interactionSource,
                                indication = ripple(color = LiveRed),
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSelectVariant(variant)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = variant.displayName.uppercase(),
                            style = OscTextStyles.MicroLabel,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) HighlightWhite else SecondaryText
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderHistoryButton(
    count: Int,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "HistoryBtnScale"
    )

    Box(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .testTag("history_button"),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xD916161C), Color(0xEB070709))
                    )
                )
                .border(
                    width = 0.9.dp,
                    brush = Brush.verticalGradient(
                        listOf(HighlightWhite.copy(alpha = 0.45f), Crimson.copy(alpha = 0.6f))
                    ),
                    shape = CircleShape
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(color = LiveRed),
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = "Open Firing History ($count)",
                tint = HighlightWhite,
                modifier = Modifier.size(20.dp)
            )
        }

        if (count > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 2.dp, end = 2.dp)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(LiveRed)
                    .border(0.8.dp, HighlightWhite.copy(alpha = 0.8f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = count.toString(),
                    style = OscTextStyles.NumericCompact,
                    fontSize = 9.sp,
                    color = HighlightWhite
                )
            }
        }
    }
}

@Composable
private fun RecentHistoryStripCard(
    history: List<FiringHistoryEntry>,
    onOpenHistory: () -> Unit,
    onApplyHistoryEntry: (FiringHistoryEntry) -> Unit
) {
    val context = LocalContext.current

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        isHighlighted = false
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = LiveRed,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "RECENT SOLUTIONS (${history.size}/5)",
                    style = OscTextStyles.SectionTitle,
                    color = HighlightWhite
                )
            }

            LiquidGlassPill(
                label = "VIEW ALL",
                isSelected = history.isNotEmpty(),
                modifier = Modifier.testTag("view_all_history_pill"),
                onClick = onOpenHistory
            )
        }

        if (history.isEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Tap the top-right play button to calculate and save solutions.",
                style = MaterialTheme.typography.bodyMedium,
                color = SecondaryText
            )
        } else {
            Spacer(modifier = Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                history.take(2).forEachIndexed { idx, entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("quick_history_item_$idx")
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xCC040406))
                            .border(0.8.dp, Color(0x26FFFFFF), RoundedCornerShape(12.dp))
                            .clickable {
                                onApplyHistoryEntry(entry)
                                Toast.makeText(
                                    context,
                                    "Loaded target (${entry.targetX.toInt()}, ${entry.targetY.toInt()}, ${entry.targetZ.toInt()})",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "TGT ${entry.targetX.toInt()}, ${entry.targetY.toInt()}, ${entry.targetZ.toInt()}",
                                style = OscTextStyles.NumericCompact,
                                color = HighlightWhite,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${entry.cannonVariant.displayName} · ${"%,d".format(entry.totalTnt)} TNT · ${"%,.0f".format(entry.distance)}m",
                                style = OscTextStyles.MicroLabel,
                                fontSize = 9.sp,
                                color = SecondaryText
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.NorthWest,
                            contentDescription = "Restore inputs",
                            tint = LiveRed,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CannonOriginCard(
    state: FireControlUiState,
    onSelectField: (ActiveField) -> Unit,
    onSetPreset: (Coordinates) -> Unit
) {
    val isSfc = state.cannonVariant == CannonOriginVariant.OSC_SFC
    val isHighlighted = state.activeField in listOf(
        ActiveField.ORIGIN_X, ActiveField.ORIGIN_Y, ActiveField.ORIGIN_Z, ActiveField.PASSCODE
    )

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        isHighlighted = isHighlighted
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "CANNON ORIGIN",
                style = OscTextStyles.SectionTitle,
                color = HighlightWhite
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                LiquidGlassPill(
                    label = "0, 64, 0",
                    modifier = Modifier.testTag("preset_origin_default"),
                    onClick = { onSetPreset(Coordinates(0.0, 64.0, 0.0)) }
                )
                LiquidGlassPill(
                    label = "SPAWN",
                    modifier = Modifier.testTag("preset_origin_spawn"),
                    onClick = { onSetPreset(Coordinates(0.0, 100.0, 0.0)) }
                )
            }
        }

        // Origin X / Y / Z Wells
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LiquidGlassWell(
                label = "ORIGIN X",
                value = state.originXStr,
                isActive = state.activeField == ActiveField.ORIGIN_X,
                modifier = Modifier
                    .weight(1f)
                    .testTag("origin_x_well"),
                onClick = { onSelectField(ActiveField.ORIGIN_X) }
            )
            LiquidGlassWell(
                label = "ORIGIN Y",
                value = state.originYStr,
                isActive = state.activeField == ActiveField.ORIGIN_Y,
                modifier = Modifier
                    .weight(1f)
                    .testTag("origin_y_well"),
                onClick = { onSelectField(ActiveField.ORIGIN_Y) }
            )
            LiquidGlassWell(
                label = "ORIGIN Z",
                value = state.originZStr,
                isActive = state.activeField == ActiveField.ORIGIN_Z,
                modifier = Modifier
                    .weight(1f)
                    .testTag("origin_z_well"),
                onClick = { onSelectField(ActiveField.ORIGIN_Z) }
            )
        }

        if (!isSfc) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Requires chunk alignment (÷ 16)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SecondaryText
                )

                val isPasscodeActive = state.activeField == ActiveField.PASSCODE
                LiquidGlassPill(
                    label = "CODE ${state.passcodeStr}",
                    isSelected = isPasscodeActive,
                    modifier = Modifier.testTag("passcode_well"),
                    onClick = { onSelectField(ActiveField.PASSCODE) }
                )
            }
        }
    }
}

@Composable
private fun TargetCard(
    state: FireControlUiState,
    onSelectField: (ActiveField) -> Unit,
    onSetFireMode: (FireMode) -> Unit
) {
    val isSfc = state.cannonVariant == CannonOriginVariant.OSC_SFC
    val isHighlighted = state.activeField in listOf(
        ActiveField.TARGET_X, ActiveField.TARGET_Y, ActiveField.TARGET_Z,
        ActiveField.NUKE_SIZE, ActiveField.STAB_DEPTH, ActiveField.MAGAZINE_SLOT
    )

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        isHighlighted = isHighlighted
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "TARGET COORDINATES",
                style = OscTextStyles.SectionTitle,
                color = HighlightWhite,
                modifier = Modifier.weight(1f)
            )

            // Fire Mode Toggle (Nuke vs Stab)
            if (!isSfc) {
                Spacer(modifier = Modifier.width(8.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0xCC040406))
                        .border(0.8.dp, Color(0x26FFFFFF), RoundedCornerShape(999.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    LiquidGlassPill(
                        label = "NUKE",
                        isSelected = state.fireMode == FireMode.NUKE,
                        modifier = Modifier.testTag("fire_mode_nuke"),
                        onClick = { onSetFireMode(FireMode.NUKE) }
                    )
                    LiquidGlassPill(
                        label = "STAB",
                        isSelected = state.fireMode == FireMode.STAB,
                        modifier = Modifier.testTag("fire_mode_stab"),
                        onClick = { onSetFireMode(FireMode.STAB) }
                    )
                }
            }
        }

        // Target X / Y / Z Wells
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LiquidGlassWell(
                label = "TARGET X",
                value = state.targetXStr,
                isActive = state.activeField == ActiveField.TARGET_X,
                modifier = Modifier
                    .weight(1f)
                    .testTag("target_x_well"),
                onClick = { onSelectField(ActiveField.TARGET_X) }
            )
            LiquidGlassWell(
                label = "TARGET Y",
                value = state.targetYStr,
                isActive = state.activeField == ActiveField.TARGET_Y,
                modifier = Modifier
                    .weight(1f)
                    .testTag("target_y_well"),
                onClick = { onSelectField(ActiveField.TARGET_Y) }
            )
            LiquidGlassWell(
                label = "TARGET Z",
                value = state.targetZStr,
                isActive = state.activeField == ActiveField.TARGET_Z,
                modifier = Modifier
                    .weight(1f)
                    .testTag("target_z_well"),
                onClick = { onSelectField(ActiveField.TARGET_Z) }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Secondary Payload & Magazine Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isSfc) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "PAYLOAD SIZE (BLOCKS)",
                        style = OscTextStyles.MicroLabel,
                        color = SecondaryText
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        SFC_PAYLOAD_SIZES.forEach { size ->
                            val isSelected = state.nukeSize == size
                            LiquidGlassPill(
                                label = size.toString(),
                                isSelected = isSelected,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("sfc_size_$size"),
                                onClick = { onSelectField(ActiveField.NUKE_SIZE) }
                            )
                        }
                    }
                }
            } else {
                val isNuke = state.fireMode == FireMode.NUKE
                val label = if (isNuke) "NUKE SIZE" else "STAB DEPTH"
                val nukeTntCount = (state.nukeSize * (state.nukeSize + 1)) / 2
                val sublabel = if (isNuke) "$nukeTntCount TNT" else "BLOCKS"
                val value = if (isNuke) state.nukeSizeStr else state.stabDepthStr
                val field = if (isNuke) ActiveField.NUKE_SIZE else ActiveField.STAB_DEPTH

                LiquidGlassWell(
                    label = label,
                    sublabel = sublabel,
                    value = value,
                    isActive = state.activeField == field,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("payload_well"),
                    onClick = { onSelectField(field) }
                )

                if (state.cannonVariant == CannonOriginVariant.OSC_MK6) {
                    LiquidGlassWell(
                        label = "MAGAZINE",
                        sublabel = "0 – 15",
                        value = state.magazineSlotStr,
                        isActive = state.activeField == ActiveField.MAGAZINE_SLOT,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("magazine_well"),
                        onClick = { onSelectField(ActiveField.MAGAZINE_SLOT) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ValidationErrorPanel(errors: List<String>) {
    LiquidGlassSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        cornerRadius = 18.dp,
        isHighlighted = true,
        accentRimColor = LiveRed,
        contentPadding = PaddingValues(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Validation Warning",
                    tint = LiveRed,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "VALIDATION WARNINGS",
                    style = OscTextStyles.MicroLabel,
                    color = LiveRed
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            errors.forEach { err ->
                Text(
                    text = "• $err",
                    style = MaterialTheme.typography.bodyMedium,
                    color = HighlightWhite.copy(alpha = 0.88f),
                    modifier = Modifier.padding(vertical = 1.5.dp)
                )
            }
        }
    }
}
