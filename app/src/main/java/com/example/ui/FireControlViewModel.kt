package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.data.MK6Memory
import com.example.data.MSMemory
import com.example.data.ModePreferencesManager
import com.example.data.SFMemory
import com.example.domain.*
import com.example.ui.components.ActiveField
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class ScreenMode {
    INPUT,
    RESULTS,
    HISTORY
}

enum class HapticEvent {
    CALCULATION_SUCCESS,
    HISTORY_SELECTED
}

data class FireControlUiState(
    val screenMode: ScreenMode = ScreenMode.INPUT,
    val activeField: ActiveField? = null,
    val cannonVariant: CannonOriginVariant = CannonOriginVariant.OSC_MK6,
    val fireMode: FireMode = FireMode.NUKE,
    val targetName: String = "Target 1",
    val originXStr: String = "0",
    val originYStr: String = "64",
    val originZStr: String = "0",
    val targetXStr: String = "1024",
    val targetYStr: String = "68",
    val targetZStr: String = "-2048",
    val passcodeStr: String = "940",
    val nukeSizeStr: String = "10",
    val stabDepthStr: String = "10",
    val magazineSlotStr: String = "0",
    val calculationResult: CalculationResult? = null,
    val validationResult: ValidationResult = ValidationResult(true, emptyList()),
    val errorMessage: String? = null,
    val history: List<FiringHistoryEntry> = emptyList()
) {
    val origin: Coordinates
        get() = Coordinates(
            x = originXStr.toDoubleOrNull() ?: 0.0,
            y = originYStr.toDoubleOrNull() ?: 64.0,
            z = originZStr.toDoubleOrNull() ?: 0.0
        )

    val target: Coordinates
        get() = Coordinates(
            x = targetXStr.toDoubleOrNull() ?: 0.0,
            y = targetYStr.toDoubleOrNull() ?: 68.0,
            z = targetZStr.toDoubleOrNull() ?: 0.0
        )

    val passcode: Int
        get() = passcodeStr.toIntOrNull() ?: 940

    val nukeSize: Int
        get() = nukeSizeStr.toIntOrNull() ?: 10

    val stabDepth: Int
        get() = stabDepthStr.toIntOrNull() ?: 10

    val magazineSlot: Int
        get() = magazineSlotStr.toIntOrNull() ?: 0

    val effectiveMagazineSlot: Int
        get() = if (cannonVariant == CannonOriginVariant.OSC_MK6) magazineSlot else 0

    fun getFieldValue(field: ActiveField): String {
        return when (field) {
            ActiveField.ORIGIN_X -> originXStr
            ActiveField.ORIGIN_Y -> originYStr
            ActiveField.ORIGIN_Z -> originZStr
            ActiveField.TARGET_X -> targetXStr
            ActiveField.TARGET_Y -> targetYStr
            ActiveField.TARGET_Z -> targetZStr
            ActiveField.PASSCODE -> passcodeStr
            ActiveField.NUKE_SIZE -> nukeSizeStr
            ActiveField.STAB_DEPTH -> stabDepthStr
            ActiveField.MAGAZINE_SLOT -> magazineSlotStr
        }
    }
}

class FireControlViewModel(application: Application) : AndroidViewModel(application) {
    private val mk6Memory = MK6Memory(application)
    private val msMemory = MSMemory(application)
    private val sfMemory = SFMemory(application)

    private fun getMemoryForVariant(variant: CannonOriginVariant): ModePreferencesManager {
        return when (variant) {
            CannonOriginVariant.OSC_MK6 -> mk6Memory
            CannonOriginVariant.OSC_MS -> msMemory
            CannonOriginVariant.OSC_SFC -> sfMemory
        }
    }

    private val _uiState = MutableStateFlow(loadStateForVariant(CannonOriginVariant.OSC_MK6))
    val uiState: StateFlow<FireControlUiState> = _uiState.asStateFlow()

    private val _hapticEvents = MutableSharedFlow<HapticEvent>(extraBufferCapacity = 4)
    val hapticEvents: SharedFlow<HapticEvent> = _hapticEvents.asSharedFlow()

    init {
        recomputeValidation()
    }

    private fun loadStateForVariant(variant: CannonOriginVariant): FireControlUiState {
        val mem = getMemoryForVariant(variant)
        return FireControlUiState(
            cannonVariant = variant,
            fireMode = mem.fireMode,
            targetName = mem.targetName,
            originXStr = mem.originX.toInt().toString(),
            originYStr = mem.originY.toInt().toString(),
            originZStr = mem.originZ.toInt().toString(),
            targetXStr = mem.targetX.toInt().toString(),
            targetYStr = mem.targetY.toInt().toString(),
            targetZStr = mem.targetZ.toInt().toString(),
            passcodeStr = mem.passcode.toString(),
            nukeSizeStr = mem.nukeSize.toString(),
            stabDepthStr = mem.stabDepth.toString(),
            magazineSlotStr = mem.magazineSlot.toString(),
            screenMode = mem.screenMode,
            history = mem.getHistory()
        )
    }

    private fun saveCurrentStateToMemory() {
        val state = _uiState.value
        val mem = getMemoryForVariant(state.cannonVariant)
        mem.fireMode = state.fireMode
        mem.targetName = state.targetName
        mem.originX = state.origin.x
        mem.originY = state.origin.y
        mem.originZ = state.origin.z
        mem.targetX = state.target.x
        mem.targetY = state.target.y
        mem.targetZ = state.target.z
        mem.passcode = state.passcode
        mem.nukeSize = state.nukeSize
        mem.stabDepth = state.stabDepth
        mem.magazineSlot = state.magazineSlot
        mem.screenMode = state.screenMode
    }

    fun selectField(field: ActiveField?) {
        _uiState.update { it.copy(activeField = field) }
    }

    fun dismissKeypad() {
        _uiState.update { it.copy(activeField = null) }
    }

    private fun visibleFields(state: FireControlUiState): List<ActiveField> {
        return when (state.cannonVariant) {
            CannonOriginVariant.OSC_SFC -> listOf(
                ActiveField.ORIGIN_X, ActiveField.ORIGIN_Y, ActiveField.ORIGIN_Z,
                ActiveField.TARGET_X, ActiveField.TARGET_Y, ActiveField.TARGET_Z,
                ActiveField.NUKE_SIZE
            )
            CannonOriginVariant.OSC_MS -> listOf(
                ActiveField.ORIGIN_X, ActiveField.ORIGIN_Y, ActiveField.ORIGIN_Z,
                ActiveField.PASSCODE,
                ActiveField.TARGET_X, ActiveField.TARGET_Y, ActiveField.TARGET_Z,
                if (state.fireMode == FireMode.NUKE) ActiveField.NUKE_SIZE else ActiveField.STAB_DEPTH
            )
            CannonOriginVariant.OSC_MK6 -> listOf(
                ActiveField.ORIGIN_X, ActiveField.ORIGIN_Y, ActiveField.ORIGIN_Z,
                ActiveField.PASSCODE,
                ActiveField.TARGET_X, ActiveField.TARGET_Y, ActiveField.TARGET_Z,
                if (state.fireMode == FireMode.NUKE) ActiveField.NUKE_SIZE else ActiveField.STAB_DEPTH,
                ActiveField.MAGAZINE_SLOT
            )
        }
    }

    fun nextField() {
        val state = _uiState.value
        val current = state.activeField ?: return
        val fields = visibleFields(state)
        val idx = fields.indexOf(current)
        val nextIndex = if (idx == -1) 0 else (idx + 1) % fields.size
        _uiState.update { it.copy(activeField = fields[nextIndex]) }
    }

    fun previousField() {
        val state = _uiState.value
        val current = state.activeField ?: return
        val fields = visibleFields(state)
        val idx = fields.indexOf(current)
        val prevIndex = if (idx <= 0) fields.size - 1 else idx - 1
        _uiState.update { it.copy(activeField = fields[prevIndex]) }
    }

    fun onDigitPress(digit: String) {
        val currentField = _uiState.value.activeField ?: return
        val currentVal = _uiState.value.getFieldValue(currentField)

        val newVal = if (currentVal == "0" && digit != ".") {
            digit
        } else if (currentVal == "-0" && digit != ".") {
            "-$digit"
        } else {
            if (digit == "." && currentVal.contains(".")) currentVal else currentVal + digit
        }

        updateFieldValue(currentField, newVal)
    }

    fun onToggleSign() {
        val currentField = _uiState.value.activeField ?: return
        if (!currentField.allowsNegative) return

        val currentVal = _uiState.value.getFieldValue(currentField)
        val newVal = if (currentVal.startsWith("-")) {
            currentVal.removePrefix("-")
        } else {
            "-$currentVal"
        }

        updateFieldValue(currentField, newVal)
    }

    fun onBackspace() {
        val currentField = _uiState.value.activeField ?: return
        val currentVal = _uiState.value.getFieldValue(currentField)

        val newVal = if (currentVal.length <= 1 || (currentVal.length == 2 && currentVal.startsWith("-"))) {
            "0"
        } else {
            currentVal.dropLast(1)
        }

        updateFieldValue(currentField, newVal)
    }

    fun onClear() {
        val currentField = _uiState.value.activeField ?: return
        updateFieldValue(currentField, "0")
    }

    private fun updateFieldValue(field: ActiveField, value: String) {
        _uiState.update { state ->
            when (field) {
                ActiveField.ORIGIN_X -> state.copy(originXStr = value)
                ActiveField.ORIGIN_Y -> state.copy(originYStr = value)
                ActiveField.ORIGIN_Z -> state.copy(originZStr = value)
                ActiveField.TARGET_X -> state.copy(targetXStr = value)
                ActiveField.TARGET_Y -> state.copy(targetYStr = value)
                ActiveField.TARGET_Z -> state.copy(targetZStr = value)
                ActiveField.PASSCODE -> state.copy(passcodeStr = value)
                ActiveField.NUKE_SIZE -> state.copy(nukeSizeStr = value)
                ActiveField.STAB_DEPTH -> state.copy(stabDepthStr = value)
                ActiveField.MAGAZINE_SLOT -> state.copy(magazineSlotStr = value)
            }
        }
        saveCurrentStateToMemory()
        recomputeValidation()
    }

    fun setCannonVariant(variant: CannonOriginVariant) {
        saveCurrentStateToMemory()
        val loadedState = loadStateForVariant(variant)
        _uiState.update { loadedState }
        recomputeValidation()
    }

    fun setFireMode(mode: FireMode) {
        _uiState.update { it.copy(fireMode = mode) }
        saveCurrentStateToMemory()
        recomputeValidation()
    }

    fun setPasscode(code: Int) {
        dismissKeypad()
        _uiState.update { it.copy(passcodeStr = code.toString()) }
        saveCurrentStateToMemory()
        _hapticEvents.tryEmit(HapticEvent.HISTORY_SELECTED)
        recomputeValidation()
    }

    fun setNukeSize(size: Int) {
        dismissKeypad()
        _uiState.update { it.copy(nukeSizeStr = size.toString()) }
        saveCurrentStateToMemory()
        _hapticEvents.tryEmit(HapticEvent.HISTORY_SELECTED)
        recomputeValidation()
    }

    fun setPasscode(code: Int) {
        dismissKeypad()
        _uiState.update { it.copy(passcodeStr = code.toString()) }
        prefs.passcode = code
        _hapticEvents.tryEmit(HapticEvent.HISTORY_SELECTED)
        recomputeValidation()
    }

    fun setNukeSize(size: Int) {
        dismissKeypad()
        _uiState.update { it.copy(nukeSizeStr = size.toString()) }
        prefs.nukeSize = size
        _hapticEvents.tryEmit(HapticEvent.HISTORY_SELECTED)
        recomputeValidation()
    }

    fun setTargetName(name: String) {
        _uiState.update { it.copy(targetName = name) }
        saveCurrentStateToMemory()
    }

    fun setOriginPreset(origin: Coordinates) {
        _uiState.update {
            it.copy(
                originXStr = origin.x.toInt().toString(),
                originYStr = origin.y.toInt().toString(),
                originZStr = origin.z.toInt().toString()
            )
        }
        saveCurrentStateToMemory()
        recomputeValidation()
    }

    private fun recomputeValidation() {
        val state = _uiState.value
        val validation = OscCalculatorEngine.validate(
            origin = state.origin,
            target = state.target,
            fireMode = state.fireMode,
            nukeSize = state.nukeSize,
            stabDepth = state.stabDepth,
            magazineSlot = state.effectiveMagazineSlot,
            passcode = state.passcode,
            cannonOrigin = state.cannonVariant
        )
        _uiState.update { it.copy(validationResult = validation) }
    }

    fun calculateSolution() {
        dismissKeypad()
        val state = _uiState.value
        val usedMagazineSlot = state.effectiveMagazineSlot
        val validation = OscCalculatorEngine.validate(
            origin = state.origin,
            target = state.target,
            fireMode = state.fireMode,
            nukeSize = state.nukeSize,
            stabDepth = state.stabDepth,
            magazineSlot = usedMagazineSlot,
            passcode = state.passcode,
            cannonOrigin = state.cannonVariant
        )

        if (!validation.isValid) {
            _uiState.update {
                it.copy(
                    validationResult = validation,
                    errorMessage = validation.errors.firstOrNull()
                )
            }
            return
        }

        try {
            val result = OscCalculatorEngine.calculate(
                origin = state.origin,
                targetCoords = state.target,
                fireMode = state.fireMode,
                nukeSize = state.nukeSize,
                stabDepth = state.stabDepth,
                magazineSlot = usedMagazineSlot,
                passcode = state.passcode,
                cannonOrigin = state.cannonVariant
            )

            saveCurrentStateToMemory()
            val mem = getMemoryForVariant(state.cannonVariant)

            val historyEntry = FiringHistoryEntry(
                id = System.currentTimeMillis(),
                cannonVariant = state.cannonVariant,
                fireMode = state.fireMode,
                originX = state.origin.x,
                originY = state.origin.y,
                originZ = state.origin.z,
                targetX = state.target.x,
                targetY = state.target.y,
                targetZ = state.target.z,
                passcode = state.passcode,
                nukeSize = state.nukeSize,
                stabDepth = state.stabDepth,
                magazineSlot = usedMagazineSlot,
                distance = result.distance,
                azimuthDegrees = result.azimuthDegrees,
                totalTnt = result.tntRequirements.totalTnt,
                timeEstimate = result.timeEstimate
            )
            val updatedHistory = mem.addHistoryEntry(historyEntry)

            _hapticEvents.tryEmit(HapticEvent.CALCULATION_SUCCESS)

            _uiState.update {
                it.copy(
                    calculationResult = result,
                    validationResult = validation,
                    errorMessage = null,
                    history = updatedHistory,
                    screenMode = ScreenMode.RESULTS
                )
            }
            mem.screenMode = ScreenMode.RESULTS
        } catch (e: Exception) {
            _uiState.update {
                it.copy(errorMessage = "Calculation failed: ${e.message}")
            }
        }
    }

    fun applyHistoryEntry(entry: FiringHistoryEntry) {
        dismissKeypad()
        val mem = getMemoryForVariant(entry.cannonVariant)
        mem.fireMode = entry.fireMode
        mem.originX = entry.originX
        mem.originY = entry.originY
        mem.originZ = entry.originZ
        mem.targetX = entry.targetX
        mem.targetY = entry.targetY
        mem.targetZ = entry.targetZ
        mem.passcode = entry.passcode
        mem.nukeSize = entry.nukeSize
        mem.stabDepth = entry.stabDepth
        mem.magazineSlot = entry.magazineSlot

        _hapticEvents.tryEmit(HapticEvent.HISTORY_SELECTED)

        if (entry.cannonVariant != _uiState.value.cannonVariant) {
            setCannonVariant(entry.cannonVariant)
        }

        _uiState.update {
            it.copy(
                activeField = null,
                fireMode = entry.fireMode,
                originXStr = formatCoordString(entry.originX),
                originYStr = formatCoordString(entry.originY),
                originZStr = formatCoordString(entry.originZ),
                targetXStr = formatCoordString(entry.targetX),
                targetYStr = formatCoordString(entry.targetY),
                targetZStr = formatCoordString(entry.targetZ),
                passcodeStr = entry.passcode.toString(),
                nukeSizeStr = entry.nukeSize.toString(),
                stabDepthStr = entry.stabDepth.toString(),
                magazineSlotStr = entry.magazineSlot.toString(),
                errorMessage = null,
                screenMode = ScreenMode.INPUT
            )
        }
        saveCurrentStateToMemory()
        recomputeValidation()
    }

    fun clearHistory() {
        val state = _uiState.value
        val mem = getMemoryForVariant(state.cannonVariant)
        val updated = mem.clearHistory()
        _uiState.update { it.copy(history = updated) }
    }

    private fun formatCoordString(value: Double): String {
        return if (value % 1.0 == 0.0) {
            value.toInt().toString()
        } else {
            value.toString()
        }
    }

    fun openHistory() {
        dismissKeypad()
        _uiState.update { it.copy(screenMode = ScreenMode.HISTORY) }
        saveCurrentStateToMemory()
    }

    fun returnToInputs() {
        _uiState.update { it.copy(screenMode = ScreenMode.INPUT) }
        saveCurrentStateToMemory()
    }
}
