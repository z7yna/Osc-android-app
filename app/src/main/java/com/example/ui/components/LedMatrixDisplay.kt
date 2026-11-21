package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.CannonOriginVariant
import com.example.domain.SfcTerminal
import com.example.ui.theme.*

@Composable
fun LedMatrixDisplay(
    binaryGrid: List<List<Int>>,
    sfcTerminal: SfcTerminal?,
    cannonVariant: CannonOriginVariant,
    isValid: Boolean,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        borderColor = if (isValid) Crimson else LiveRed
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (cannonVariant == CannonOriginVariant.OSC_SFC) {
                    "SFC DUAL-CHANNEL TERMINAL"
                } else {
                    "BINARY PUNCH-CARD MATRIX (5×13)"
                },
                style = OscTextStyles.SectionTitle,
                color = HighlightWhite,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            StatusBadge(isValid = isValid)
        }

        if (cannonVariant == CannonOriginVariant.OSC_SFC && sfcTerminal != null) {
            SfcTerminalDisplay(sfcTerminal = sfcTerminal)
        } else {
            Mk6GridDisplay(binaryGrid = binaryGrid)
        }
    }
}

@Composable
private fun Mk6GridDisplay(binaryGrid: List<List<Int>>) {
    val columnColors = listOf(
        ColBlueDark, ColBlueDark, ColBlueDark,
        ColBlueLight,
        ColGreen, ColGreen,
        ColYellow,
        ColOrange, ColOrange, ColOrange,
        ColRed,
        ColBrown, ColBrown
    )

    val rowColors = listOf(Row1, Row2, Row3, Row4, Row5)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xF0030305), Color(0xFA060609))
                )
            )
            .border(0.9.dp, Color(0x24FFFFFF), RoundedCornerShape(14.dp))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header Colors
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(10.dp).background(rowColors[0], RoundedCornerShape(2.dp)))
            columnColors.forEach { colColor ->
                Box(modifier = Modifier.size(12.dp, 5.dp).background(colColor, RoundedCornerShape(2.dp)))
            }
            Box(modifier = Modifier.size(10.dp).background(rowColors[0], RoundedCornerShape(2.dp)))
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Matrix Rows
        binaryGrid.forEachIndexed { rowIdx, row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.5.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp, 16.dp)
                        .background(rowColors.getOrElse(rowIdx) { Row1 }, RoundedCornerShape(2.dp))
                )

                row.forEach { value ->
                    LedBulb(
                        isOn = value == 1,
                        activeColor = LedRedOn,
                        inactiveColor = LedRedOff
                    )
                }

                Box(
                    modifier = Modifier
                        .size(10.dp, 16.dp)
                        .background(rowColors.getOrElse(rowIdx) { Row1 }, RoundedCornerShape(2.dp))
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Bottom Header Colors
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(10.dp).background(rowColors[4], RoundedCornerShape(2.dp)))
            columnColors.forEach { colColor ->
                Box(modifier = Modifier.size(12.dp, 5.dp).background(colColor, RoundedCornerShape(2.dp)))
            }
            Box(modifier = Modifier.size(10.dp).background(rowColors[4], RoundedCornerShape(2.dp)))
        }
    }
}

@Composable
private fun SfcTerminalDisplay(sfcTerminal: SfcTerminal) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xF0030305), Color(0xFA060609))
                )
            )
            .border(0.9.dp, Color(0x24FFFFFF), RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        // Orange Channel
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ORANGE: ${sfcTerminal.orange}",
                style = OscTextStyles.NumericCompact,
                fontSize = 12.sp,
                color = LedOrangeOn
            )
            Text(
                text = if (sfcTerminal.orange < 0) "NEG (−)" else "POS (+)",
                style = OscTextStyles.MicroLabel,
                color = if (sfcTerminal.orange < 0) LiveRed else HighlightWhite
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            sfcTerminal.orangeBits.forEach { bit ->
                LedBulb(
                    isOn = bit == 1,
                    activeColor = LedOrangeOn,
                    inactiveColor = Color(0xFF1A1006)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Blue Channel
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "BLUE: ${sfcTerminal.blue}",
                style = OscTextStyles.NumericCompact,
                fontSize = 12.sp,
                color = LedBlueOn
            )
            Text(
                text = if (sfcTerminal.blue < 0) "NEG (−)" else "POS (+)",
                style = OscTextStyles.MicroLabel,
                color = if (sfcTerminal.blue < 0) LiveRed else HighlightWhite
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            sfcTerminal.blueBits.forEach { bit ->
                LedBulb(
                    isOn = bit == 1,
                    activeColor = LedBlueOn,
                    inactiveColor = Color(0xFF08121E)
                )
            }
        }
    }
}

@Composable
private fun LedBulb(
    isOn: Boolean,
    activeColor: Color,
    inactiveColor: Color
) {
    Box(
        modifier = Modifier
            .size(16.dp)
            .shadow(
                elevation = if (isOn) 8.dp else 0.dp,
                shape = CircleShape,
                spotColor = activeColor,
                ambientColor = activeColor
            )
            .clip(CircleShape)
            .background(
                if (isOn) {
                    Brush.radialGradient(
                        listOf(HighlightWhite, activeColor, activeColor.copy(alpha = 0.85f))
                    )
                } else {
                    Brush.radialGradient(
                        listOf(inactiveColor, AmoledBlack)
                    )
                }
            )
            .border(
                width = 0.9.dp,
                color = if (isOn) HighlightWhite.copy(alpha = 0.82f) else Color(0x26FFFFFF),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isOn) {
            Box(
                modifier = Modifier
                    .size(4.5.dp)
                    .offset(x = (-1.5).dp, y = (-1.5).dp)
                    .clip(CircleShape)
                    .background(HighlightWhite.copy(alpha = 0.9f))
            )
        }
    }
}
