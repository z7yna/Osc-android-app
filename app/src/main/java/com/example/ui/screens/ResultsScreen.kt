package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.CalculationResult
import com.example.domain.CannonOriginVariant
import com.example.domain.FireMode
import com.example.ui.FireControlUiState
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun ResultsScreen(
    state: FireControlUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onScrollProgressChanged: (Float) -> Unit = {}
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val result = state.calculationResult ?: return

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

    // Staggered optical reveal state
    var revealedStep by remember(result) { mutableIntStateOf(0) }
    LaunchedEffect(result) {
        for (step in 1..6) {
            revealedStep = step
            delay(45L)
        }
    }

    BackHandler {
        onBack()
    }

    val copySolutionToClipboard = {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val isSfc = state.cannonVariant == CannonOriginVariant.OSC_SFC
        val text = if (isSfc && result.sfcTerminal != null) {
            """
            Target: ${state.targetName}
            Coordinates: X:${state.target.x.toInt()} Y:${state.target.y.toInt()} Z:${state.target.z.toInt()}
            Cannon: OSC SFC
            Payload Size: ${state.nukeSize}
            Orange Channel: ${result.sfcTerminal.orange}
            Blue Channel: ${result.sfcTerminal.blue}
            Distance: ${"%.1f".format(result.distance)} blocks
            Azimuth: ${"%.1f".format(result.azimuthDegrees)}°
            Total Time: ${result.timeEstimate}
            -------------------------
            Payload TNT: ${result.tntRequirements.payloadTnt} (${result.tntRequirements.payloadDescription})
            Acceleration TNT: ${result.tntRequirements.accelTnt} (X:${result.tntRequirements.accelX} Y:${result.tntRequirements.accelY} Z:${result.tntRequirements.accelZ})
            Total Mission TNT: ${result.tntRequirements.totalTnt} (${result.tntRequirements.stacksBreakdown})
            """.trimIndent()
        } else {
            val sizeOrDepth = if (state.fireMode == FireMode.NUKE)
                "Nuke Size: ${state.nukeSize}"
            else
                "Stab Depth: ${state.stabDepth}"
            val magazineLine = if (state.cannonVariant == CannonOriginVariant.OSC_MK6) {
                "\n            Magazine Slot: ${state.magazineSlot}"
            } else {
                ""
            }

            """
            Target: ${state.targetName}
            Coordinates: X:${state.target.x.toInt()} Y:${state.target.y.toInt()} Z:${state.target.z.toInt()}
            Cannon: ${state.cannonVariant.displayName}
            Fire Mode: ${state.fireMode.displayName}
            $sizeOrDepth$magazineLine
            Coarse: X:${result.coarse.x} Y:${result.coarse.y} Z:${result.coarse.z}
            Fine: X:${result.fine.x} Y:${result.fine.y} Z:${result.fine.z}
            Passcode: ${state.passcode}
            Nether Coords: X:${result.netherPos.x.toInt()} Y:${result.netherPos.y.toInt()} Z:${result.netherPos.z.toInt()}
            Distance: ${"%.1f".format(result.distance)} blocks
            Azimuth: ${"%.1f".format(result.azimuthDegrees)}°
            Total Time: ${result.timeEstimate}
            -------------------------
            Payload TNT: ${result.tntRequirements.payloadTnt} (${result.tntRequirements.payloadDescription})
            Acceleration TNT: ${result.tntRequirements.accelTnt} (X:${result.tntRequirements.accelX} Y:${result.tntRequirements.accelY} Z:${result.tntRequirements.accelZ})
            Total Mission TNT: ${result.tntRequirements.totalTnt} (${result.tntRequirements.stacksBreakdown})
            """.trimIndent()
        }

        val clip = ClipData.newPlainText("OSC Firing Solution", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Firing solution copied to clipboard!", Toast.LENGTH_SHORT).show()
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
                .padding(top = 12.dp, bottom = 44.dp)
        ) {
            // Optical Glass Top Action Bar
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
                        .weight(0.95f)
                        .testTag("back_to_inputs_button"),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Return to Inputs",
                            tint = HighlightWhite,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )

                GlassActionButton(
                    text = "COPY SOLUTION",
                    onClick = copySolutionToClipboard,
                    isPrimaryRed = true,
                    modifier = Modifier
                        .weight(1.35f)
                        .testTag("copy_solution_button"),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Solution",
                            tint = HighlightWhite,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Azimuth & Alignment Radar Schematic
            StaggeredGlassBlock(visible = revealedStep >= 1) {
                OscSchematicCanvas(
                    origin = state.origin,
                    target = state.target,
                    result = result
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. TNT Munitions Requirements (Payload + Accel)
            StaggeredGlassBlock(visible = revealedStep >= 2) {
                TntMunitionsCard(
                    result = result,
                    cannonVariant = state.cannonVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Mechanical Adjustments (Mk6 & MS)
            if (state.cannonVariant != CannonOriginVariant.OSC_SFC) {
                StaggeredGlassBlock(visible = revealedStep >= 3) {
                    FiringParametersCard(result = result)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 4. Punch Card LED Matrix / SFC Dual-Channel Terminal
            StaggeredGlassBlock(visible = revealedStep >= 4) {
                LedMatrixDisplay(
                    binaryGrid = result.binaryGrid,
                    sfcTerminal = result.sfcTerminal,
                    cannonVariant = state.cannonVariant,
                    isValid = state.validationResult.isValid
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 5. Flight Timing Breakdown
            StaggeredGlassBlock(visible = revealedStep >= 5) {
                FlightTimingCard(result = result)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 6. Telemetry & Nether Link + New Calculation Control
            StaggeredGlassBlock(visible = revealedStep >= 6) {
                Column {
                    TelemetryCard(result = result)

                    Spacer(modifier = Modifier.height(22.dp))

                    GlassActionButton(
                        text = "NEW CALCULATION",
                        onClick = onBack,
                        isPrimaryRed = true,
                        modifier = Modifier.testTag("new_calculation_button"),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = HighlightWhite,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun StaggeredGlassBlock(
    visible: Boolean,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) +
            slideInVertically(
                initialOffsetY = { it / 6 },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
    ) {
        content()
    }
}

@Composable
private fun TntMunitionsCard(
    result: CalculationResult,
    cannonVariant: CannonOriginVariant
) {
    val tnt = result.tntRequirements
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        isHighlighted = true
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = null,
                    tint = LiveRed,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "TNT MUNITIONS REQUIRED",
                    style = OscTextStyles.SectionTitle,
                    color = HighlightWhite
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0x38FF1744))
                    .border(0.9.dp, LiveRed.copy(alpha = 0.85f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${"%,d".format(tnt.totalTnt)} TNT",
                    style = OscTextStyles.NumericCompact,
                    fontSize = 11.sp,
                    color = HighlightWhite
                )
            }
        }

        // Payload TNT & Acceleration Propellant TNT
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Payload TNT Recessed Lens
            RecessedMetricCard(
                title = "PAYLOAD TNT",
                value = "%,d".format(tnt.payloadTnt),
                subtitle = tnt.payloadDescription,
                accentColor = HighlightWhite,
                modifier = Modifier.weight(1f)
            )

            // Acceleration TNT Recessed Lens
            RecessedMetricCard(
                title = "PROPEL / ACCEL",
                value = "%,d".format(tnt.accelTnt),
                subtitle = if (cannonVariant == CannonOriginVariant.OSC_SFC) {
                    "O: ${tnt.accelX} · B: ${tnt.accelZ}"
                } else {
                    "X:${tnt.accelX}  Y:${tnt.accelY}  Z:${tnt.accelZ}"
                },
                accentColor = LiveRed,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Inventory Stacks Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xCC040406))
                .border(0.8.dp, Color(0x22FFFFFF), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "INVENTORY STACKS",
                style = OscTextStyles.MicroLabel,
                color = SecondaryText
            )
            Text(
                text = tnt.stacksBreakdown,
                style = OscTextStyles.NumericCompact,
                fontSize = 12.sp,
                color = HighlightWhite
            )
        }
    }
}

@Composable
private fun RecessedMetricCard(
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(14.dp), ambientColor = AmoledBlack)
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xEB040406), Color(0xDF0A0A0E))
                )
            )
            .border(0.9.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Column {
            Text(
                text = title,
                style = OscTextStyles.MicroLabel,
                fontSize = 9.sp,
                color = accentColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = OscTextStyles.NumericHero,
                color = HighlightWhite
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = subtitle,
                style = OscTextStyles.NumericCompact,
                fontSize = 10.sp,
                color = SecondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FiringParametersCard(result: CalculationResult) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "MECHANICAL ADJUSTMENTS",
            style = OscTextStyles.SectionTitle,
            color = HighlightWhite,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Coarse Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xCC040406))
                .border(0.9.dp, HighlightWhite.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "COARSE",
                style = OscTextStyles.MicroLabel,
                color = HighlightWhite
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                AxisValue("X", result.coarse.x.toString())
                AxisValue("Y", result.coarse.y.toString())
                AxisValue("Z", result.coarse.z.toString())
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Fine Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xCC040406))
                .border(0.9.dp, LiveRed.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "FINE",
                style = OscTextStyles.MicroLabel,
                color = LiveRed
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                AxisValue("X", result.fine.x.toString())
                AxisValue("Y", result.fine.y.toString())
                AxisValue("Z", result.fine.z.toString())
            }
        }
    }
}

@Composable
private fun AxisValue(axis: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "$axis ",
            style = OscTextStyles.MicroLabel,
            color = SecondaryText
        )
        Text(
            text = value,
            style = OscTextStyles.NumericInput.copy(fontSize = 15.sp),
            color = HighlightWhite
        )
    }
}

@Composable
private fun FlightTimingCard(result: CalculationResult) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "MISSION TIMING",
            style = OscTextStyles.SectionTitle,
            color = HighlightWhite,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TimeMetricBox(
                label = "TOTAL TIME",
                time = result.timeEstimate,
                color = LiveRed,
                modifier = Modifier.weight(1f)
            )
            TimeMetricBox(
                label = "PAYLOAD BUILD",
                time = result.timeBreakdown.payloadBuilding,
                color = HighlightWhite,
                modifier = Modifier.weight(1f)
            )
            TimeMetricBox(
                label = "ACCELERATION",
                time = result.timeBreakdown.acceleration,
                color = SecondaryText,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TimeMetricBox(
    label: String,
    time: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xCC040406))
            .border(0.9.dp, color.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .padding(horizontal = 8.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                style = OscTextStyles.MicroLabel,
                fontSize = 8.5.sp,
                color = SecondaryText,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = time,
                style = OscTextStyles.NumericCompact,
                fontSize = 13.sp,
                color = color
            )
        }
    }
}

@Composable
private fun TelemetryCard(result: CalculationResult) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "TELEMETRY & NETHER LINK",
            style = OscTextStyles.SectionTitle,
            color = HighlightWhite,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "NETHER CANNON COORDS",
                    style = OscTextStyles.MicroLabel,
                    color = SecondaryText
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "X ${result.netherPos.x.toInt()} · Y ${result.netherPos.y.toInt()} · Z ${result.netherPos.z.toInt()}",
                    style = OscTextStyles.NumericCompact,
                    color = HighlightWhite
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "TARGET DISTANCE",
                    style = OscTextStyles.MicroLabel,
                    color = SecondaryText
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${"%,.1f".format(result.distance)} m",
                    style = OscTextStyles.NumericCompact,
                    color = LiveRed
                )
            }
        }
    }
}
