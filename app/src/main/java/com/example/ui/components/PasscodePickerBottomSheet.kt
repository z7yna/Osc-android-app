package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.ValidPasscodes
import com.example.ui.haptics.rememberHapticFeedbackManager
import com.example.ui.theme.*

@Composable
fun PasscodePickerBottomSheet(
    isVisible: Boolean,
    currentPasscode: Int,
    onSelectPasscode: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hapticManager = rememberHapticFeedbackManager()
    val passcodes = remember { ValidPasscodes.LIST.sorted() }

    AnimatedVisibility(
        visible = isVisible,
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
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AmoledBlack.copy(alpha = 0.78f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() },
            contentAlignment = Alignment.BottomCenter
        ) {
            val sheetShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.75f)
                    .shadow(
                        elevation = 40.dp,
                        shape = sheetShape,
                        ambientColor = AmoledBlack,
                        spotColor = LiveRed.copy(alpha = 0.7f)
                    )
                    .clip(sheetShape)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xF2120408),
                                Color(0xF8080204),
                                Color(0xFF000000)
                            )
                        )
                    )
                    .border(
                        width = 1.2.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                HighlightWhite.copy(alpha = 0.6f),
                                LiveRed.copy(alpha = 0.7f),
                                Color.Transparent
                            )
                        ),
                        shape = sheetShape
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {} // Consume clicks inside sheet
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Drag Handle
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(42.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(HighlightWhite.copy(alpha = 0.5f))
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x38FF1744))
                                    .border(1.dp, LiveRed, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VpnKey,
                                    contentDescription = null,
                                    tint = HighlightWhite,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "SELECT REDSTONE PASSCODE",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = HighlightWhite
                                )
                                Text(
                                    text = "222 VERIFIED ZERO-MISFIRE CODES",
                                    style = OscTextStyles.MicroLabel,
                                    color = LiveRed
                                )
                            }
                        }

                        // Close Button
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0x22FFFFFF))
                                .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                                .clickable {
                                    hapticManager.performKeyTap()
                                    onDismiss()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close passcodes drawer",
                                tint = HighlightWhite,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Grid of Passcodes
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(passcodes) { code ->
                            val isSelected = code == currentPasscode
                            PasscodeCell(
                                passcode = code,
                                isSelected = isSelected,
                                onClick = {
                                    hapticManager.performHistorySelection()
                                    onSelectPasscode(code)
                                    onDismiss()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PasscodeCell(
    passcode: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "PasscodeCellScale"
    )

    val shape = RoundedCornerShape(16.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .minimumInteractiveComponentSize()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = if (isSelected) 10.dp else 2.dp,
                shape = shape,
                spotColor = if (isSelected) LiveRed else AmoledBlack
            )
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = if (isSelected) {
                        listOf(Color(0xE659081B), Color(0xF22B020B))
                    } else {
                        listOf(Color(0xD918181F), Color(0xEB0C0C10))
                    }
                )
            )
            .border(
                width = if (isSelected) 1.3.dp else 0.8.dp,
                brush = Brush.verticalGradient(
                    colors = if (isSelected) {
                        listOf(HighlightWhite.copy(alpha = 0.75f), LiveRed)
                    } else {
                        listOf(Color(0x33FFFFFF), Color(0x18FFFFFF))
                    }
                ),
                shape = shape
            )
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 6.dp)
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = HighlightWhite,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = passcode.toString(),
                style = OscTextStyles.NumericInput,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                color = if (isSelected) HighlightWhite else SecondaryText,
                textAlign = TextAlign.Center
            )
        }
    }
}
