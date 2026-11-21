package com.example.ui.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

/**
 * Dedicated tactile feedback manager for the Liquid Glass UI.
 * Uses VibrationEffect waveforms / predefined effects with safe fallbacks across API 24-36.
 */
class HapticFeedbackManager(
    context: Context,
    private val view: View? = null
) {
    private val vibrator: Vibrator? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    } catch (_: Exception) {
        null
    }

    /**
     * Two-stage optical resonance pulse for successful firing solution calculations.
     */
    fun performCalculationSuccess() {
        val handledByView = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view?.performHapticFeedback(HapticFeedbackConstants.CONFIRM) == true
        } else {
            false
        }

        if (vibrator?.hasVibrator() == true) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val timings = longArrayOf(0, 14, 38, 22)
                    val effect = if (vibrator.hasAmplitudeControl()) {
                        VibrationEffect.createWaveform(timings, intArrayOf(0, 145, 0, 235), -1)
                    } else {
                        VibrationEffect.createWaveform(timings, -1)
                    }
                    vibrator.vibrate(effect)
                    return
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(longArrayOf(0, 14, 38, 22), -1)
                    return
                }
            } catch (_: Exception) {
                // Fallback to view haptics below
            }
        }

        if (!handledByView) {
            view?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
    }

    /**
     * Crisp liquid-glass snap when restoring a saved solution from history.
     */
    fun performHistorySelection() {
        val handledByView = view?.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK) == true

        if (vibrator?.hasVibrator() == true) {
            try {
                when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                        vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                        return
                    }
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                        val amp = if (vibrator.hasAmplitudeControl()) 175 else VibrationEffect.DEFAULT_AMPLITUDE
                        vibrator.vibrate(VibrationEffect.createOneShot(12, amp))
                        return
                    }
                    else -> {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(12)
                        return
                    }
                }
            } catch (_: Exception) {
                // Fallback already attempted via View
            }
        }

        if (!handledByView) {
            view?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    /**
     * Subtle micro-tick for optical keypad keys.
     */
    fun performKeyTap() {
        if (vibrator?.hasVibrator() == true) {
            try {
                when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                        vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
                        return
                    }
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                        val amp = if (vibrator.hasAmplitudeControl()) 95 else VibrationEffect.DEFAULT_AMPLITUDE
                        vibrator.vibrate(VibrationEffect.createOneShot(6, amp))
                        return
                    }
                }
            } catch (_: Exception) {
                // Fallback below
            }
        }
        view?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }
}

@Composable
fun rememberHapticFeedbackManager(): HapticFeedbackManager {
    val context = LocalContext.current
    val view = LocalView.current
    return remember(context, view) {
        HapticFeedbackManager(context = context, view = view)
    }
}
