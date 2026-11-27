package com.example

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.FireControlViewModel
import com.example.ui.HapticEvent
import com.example.ui.ScreenMode
import com.example.ui.components.AmoledRedAtmosphereBackground
import com.example.ui.components.CustomKeypad
import com.example.ui.haptics.rememberHapticFeedbackManager
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.InputScreen
import com.example.ui.screens.ResultsScreen
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        )
        setContent {
            MyApplicationTheme {
                FireControlApp()
            }
        }
    }
}

@Composable
fun FireControlApp(
    viewModel: FireControlViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val hapticManager = rememberHapticFeedbackManager()

    LaunchedEffect(viewModel, hapticManager) {
        viewModel.hapticEvents.collect { event ->
            when (event) {
                HapticEvent.CALCULATION_SUCCESS -> hapticManager.performCalculationSuccess()
                HapticEvent.HISTORY_SELECTED -> hapticManager.performHistorySelection()
            }
        }
    }

    var inputScrollProgress by remember { mutableFloatStateOf(0f) }
    var resultsScrollProgress by remember { mutableFloatStateOf(0f) }
    var historyScrollProgress by remember { mutableFloatStateOf(0f) }

    val targetScrollProgress = when (state.screenMode) {
        ScreenMode.INPUT -> inputScrollProgress
        ScreenMode.RESULTS -> resultsScrollProgress + 0.35f
        ScreenMode.HISTORY -> historyScrollProgress + 0.2f
    }

    val animatedParallax by animateFloatAsState(
        targetValue = targetScrollProgress,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "RootsParallax"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBlack)
    ) {
        // Pure AMOLED Black (#000000) with ONLY Moving Live Red (#FF1744) Roots
        AmoledRedAtmosphereBackground(
            scrollProgress = animatedParallax
        )

        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.safeDrawing,
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Tap outside keypad to dismiss
                if (state.activeField != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                viewModel.dismissKeypad()
                            }
                    )
                }

                // Fluid Spring & Scale-Depth Transitions across Input, Results, and History
                AnimatedContent(
                    targetState = state.screenMode,
                    transitionSpec = {
                        val forward = targetState.ordinal > initialState.ordinal

                        val enter = slideInHorizontally(
                            animationSpec = tween(400)
                        ) { width ->
                            if (forward) width else -width
                        } + fadeIn(
                            animationSpec = tween(350)
                        ) + scaleIn(
                            initialScale = 0.965f,
                            animationSpec = tween(400)
                        )

                        val exit = slideOutHorizontally(
                            animationSpec = tween(320)
                        ) { width ->
                            if (forward) -width / 4 else width / 4
                        } + fadeOut(
                            animationSpec = tween(260)
                        ) + scaleOut(
                            targetScale = 0.97f,
                            animationSpec = tween(320)
                        )

                        enter.togetherWith(exit).using(
                            SizeTransform(clip = false)
                        )
                    },
                    label = "ScreenTransition"
                ) { screen ->
                    when (screen) {
                        ScreenMode.INPUT -> {
                            InputScreen(
                                state = state,
                                onSelectField = { viewModel.selectField(it) },
                                onSetCannonVariant = { viewModel.setCannonVariant(it) },
                                onSetFireMode = { viewModel.setFireMode(it) },
                                onSetOriginPreset = { viewModel.setOriginPreset(it) },
                                onSetPasscode = { viewModel.setPasscode(it) },
                                onSetNukeSize = { viewModel.setNukeSize(it) },
                                onCalculate = { viewModel.calculateSolution() },
                                onOpenHistory = { viewModel.openHistory() },
                                onApplyHistoryEntry = { viewModel.applyHistoryEntry(it) },
                                onScrollProgressChanged = { inputScrollProgress = it }
                            )
                        }
                        ScreenMode.RESULTS -> {
                            ResultsScreen(
                                state = state,
                                onBack = { viewModel.returnToInputs() },
                                onScrollProgressChanged = { resultsScrollProgress = it }
                            )
                        }
                        ScreenMode.HISTORY -> {
                            HistoryScreen(
                                history = state.history,
                                onSelectEntry = { viewModel.applyHistoryEntry(it) },
                                onClearHistory = { viewModel.clearHistory() },
                                onBack = { viewModel.returnToInputs() },
                                onScrollProgressChanged = { historyScrollProgress = it }
                            )
                        }
                    }
                }

                // Docked Optical Liquid Glass Keypad
                CustomKeypad(
                    activeField = state.activeField,
                    currentValue = state.activeField?.let { state.getFieldValue(it) } ?: "",
                    onDigitPress = { viewModel.onDigitPress(it) },
                    onToggleSign = { viewModel.onToggleSign() },
                    onBackspace = { viewModel.onBackspace() },
                    onClear = { viewModel.onClear() },
                    onPreviousField = { viewModel.previousField() },
                    onNextField = { viewModel.nextField() },
                    onDismiss = { viewModel.dismissKeypad() },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}
