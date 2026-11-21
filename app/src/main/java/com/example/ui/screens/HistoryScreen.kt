package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NorthWest
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.CannonOriginVariant
import com.example.domain.FiringHistoryEntry
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    history: List<FiringHistoryEntry>,
    onSelectEntry: (FiringHistoryEntry) -> Unit,
    onClearHistory: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onScrollProgressChanged: (Float) -> Unit = {}
) {
    val context = LocalContext.current
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

    var revealedCount by remember(history.size) { mutableIntStateOf(0) }
    LaunchedEffect(history.size) {
        for (i in 1..history.size.coerceAtLeast(1)) {
            revealedCount = i
            delay(40L)
        }
    }

    BackHandler {
        onBack()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("history_view"),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp, bottom = 44.dp)
        ) {
            // Top Action Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassActionButton(
                    text = "INPUTS",
                    onClick = onBack,
                    isPrimaryRed = false,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("history_back_button"),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Inputs",
                            tint = HighlightWhite,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )

                GlassActionButton(
                    text = "CLEAR",
                    onClick = onClearHistory,
                    enabled = history.isNotEmpty(),
                    isPrimaryRed = true,
                    modifier = Modifier
                        .weight(0.9f)
                        .testTag("history_clear_button"),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear History",
                            tint = if (history.isNotEmpty()) HighlightWhite else MutedText,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Header Title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "FIRING HISTORY",
                        style = MaterialTheme.typography.headlineMedium,
                        color = HighlightWhite,
                        letterSpacing = 1.4.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "LAST 5 SOLUTIONS · TAP TO RE-POPULATE INPUTS",
                        style = OscTextStyles.MicroLabel,
                        color = LiveRed
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0x28FF1744))
                        .border(0.8.dp, LiveRed.copy(alpha = 0.6f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${history.size} / 5",
                        style = OscTextStyles.NumericCompact,
                        fontSize = 11.sp,
                        color = HighlightWhite
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (history.isEmpty()) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    isHighlighted = false
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 28.dp, horizontal = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(Color(0xCC060608))
                                .border(0.9.dp, Color(0x33FFFFFF), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "Empty History",
                                tint = LiveRed,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "NO FIRING SOLUTIONS STORED",
                            style = OscTextStyles.SectionTitle,
                            color = HighlightWhite,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Calculate a firing solution to log up to 5 recent configurations here for instant recall.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SecondaryText,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                history.forEachIndexed { index, entry ->
                    AnimatedVisibility(
                        visible = revealedCount > index,
                        enter = fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) +
                            slideInVertically(
                                initialOffsetY = { it / 5 },
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            )
                    ) {
                        HistoryEntryCard(
                            index = index,
                            entry = entry,
                            modifier = Modifier
                                .padding(bottom = 12.dp)
                                .testTag("history_entry_$index"),
                            onClick = {
                                onSelectEntry(entry)
                                Toast.makeText(
                                    context,
                                    "Loaded target (${entry.targetX.toInt()}, ${entry.targetY.toInt()}, ${entry.targetZ.toInt()})",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryEntryCard(
    index: Int,
    entry: FiringHistoryEntry,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "HistoryCardScale"
    )

    val timeLabel = remember(entry.id) {
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(entry.id))
    }

    val variantModeLabel = if (entry.cannonVariant == CannonOriginVariant.OSC_SFC) {
        "${entry.cannonVariant.displayName.uppercase()} · ${entry.nukeSize}B"
    } else {
        val modeShort = if (entry.fireMode.id == "nuke") "NUKE ${entry.nukeSize}" else "STAB ${entry.stabDepth}"
        "${entry.cannonVariant.displayName.uppercase()} · $modeShort"
    }

    LiquidGlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = LiveRed),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                }
            ),
        shape = RoundedCornerShape(20.dp),
        cornerRadius = 20.dp,
        isHighlighted = index == 0,
        accentRimColor = if (index == 0) LiveRed else Crimson,
        contentPadding = PaddingValues(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top Row: Index + Variant/Mode + Timestamp + Restore Cue
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (index == 0) Color(0x38FF1744) else Color(0xCC060608))
                            .border(
                                0.8.dp,
                                if (index == 0) LiveRed else Color(0x2EFFFFFF),
                                RoundedCornerShape(999.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "#${index + 1}",
                            style = OscTextStyles.NumericCompact,
                            fontSize = 10.sp,
                            color = if (index == 0) LiveRed else HighlightWhite
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = variantModeLabel,
                        style = OscTextStyles.MicroLabel,
                        color = HighlightWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = timeLabel,
                        style = OscTextStyles.NumericCompact,
                        fontSize = 10.sp,
                        color = SecondaryText
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.NorthWest,
                        contentDescription = "Restore inputs",
                        tint = LiveRed,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Recessed Target & Origin Coordinates Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xCC040406))
                    .border(0.8.dp, Color(0x20FFFFFF), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "TARGET",
                        style = OscTextStyles.MicroLabel,
                        fontSize = 9.sp,
                        color = LiveRed
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${entry.targetX.toInt()}, ${entry.targetY.toInt()}, ${entry.targetZ.toInt()}",
                        style = OscTextStyles.NumericCompact,
                        fontSize = 14.sp,
                        color = HighlightWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.padding(start = 12.dp)
                ) {
                    Text(
                        text = "ORIGIN",
                        style = OscTextStyles.MicroLabel,
                        fontSize = 9.sp,
                        color = SecondaryText
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${entry.originX.toInt()}, ${entry.originY.toInt()}, ${entry.originZ.toInt()}",
                        style = OscTextStyles.NumericCompact,
                        fontSize = 12.sp,
                        color = SecondaryText
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Telemetry Footer Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                HistoryMetricChip("DST", "${"%,.0f".format(entry.distance)}m")
                HistoryMetricChip("AZM", "${"%.1f".format(entry.azimuthDegrees)}°")
                HistoryMetricChip("TNT", "%,d".format(entry.totalTnt), highlight = true)
                HistoryMetricChip("TIME", entry.timeEstimate)
            }
        }
    }
}

@Composable
private fun HistoryMetricChip(
    label: String,
    value: String,
    highlight: Boolean = false
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "$label ",
            style = OscTextStyles.MicroLabel,
            fontSize = 9.sp,
            color = MutedText
        )
        Text(
            text = value,
            style = OscTextStyles.NumericCompact,
            fontSize = 11.sp,
            color = if (highlight) LiveRed else HighlightWhite
        )
    }
}
