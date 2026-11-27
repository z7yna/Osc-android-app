package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.domain.CannonOriginVariant
import com.example.domain.FireMode
import com.example.domain.FiringHistoryEntry
import com.example.domain.TargetPreset
import org.json.JSONArray
import org.json.JSONObject

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("osc_fire_control_prefs", Context.MODE_PRIVATE)

    var originX: Double
        get() = prefs.getFloat(KEY_ORIGIN_X, 0.0f).toDouble()
        set(value) = prefs.edit().putFloat(KEY_ORIGIN_X, value.toFloat()).apply()

    var originY: Double
        get() = prefs.getFloat(KEY_ORIGIN_Y, 64.0f).toDouble()
        set(value) = prefs.edit().putFloat(KEY_ORIGIN_Y, value.toFloat()).apply()

    var originZ: Double
        get() = prefs.getFloat(KEY_ORIGIN_Z, 0.0f).toDouble()
        set(value) = prefs.edit().putFloat(KEY_ORIGIN_Z, value.toFloat()).apply()

    var targetX: Double
        get() = prefs.getFloat(KEY_TARGET_X, 1024.0f).toDouble()
        set(value) = prefs.edit().putFloat(KEY_TARGET_X, value.toFloat()).apply()

    var targetY: Double
        get() = prefs.getFloat(KEY_TARGET_Y, 68.0f).toDouble()
        set(value) = prefs.edit().putFloat(KEY_TARGET_Y, value.toFloat()).apply()

    var targetZ: Double
        get() = prefs.getFloat(KEY_TARGET_Z, -2048.0f).toDouble()
        set(value) = prefs.edit().putFloat(KEY_TARGET_Z, value.toFloat()).apply()

    var targetName: String
        get() = prefs.getString(KEY_TARGET_NAME, "Target 1") ?: "Target 1"
        set(value) = prefs.edit().putString(KEY_TARGET_NAME, value).apply()

    var passcode: Int
        get() = prefs.getInt(KEY_PASSCODE, 940)
        set(value) = prefs.edit().putInt(KEY_PASSCODE, value).apply()

    var cannonVariant: CannonOriginVariant
        get() {
            val id = prefs.getString(KEY_CANNON_VARIANT, CannonOriginVariant.OSC_MK6.id)
            return CannonOriginVariant.entries.find { it.id == id } ?: CannonOriginVariant.OSC_MK6
        }
        set(value) = prefs.edit().putString(KEY_CANNON_VARIANT, value.id).apply()

    var fireMode: FireMode
        get() {
            val id = prefs.getString(KEY_FIRE_MODE, FireMode.NUKE.id)
            return FireMode.entries.find { it.id == id } ?: FireMode.NUKE
        }
        set(value) = prefs.edit().putString(KEY_FIRE_MODE, value.id).apply()

    var nukeSize: Int
        get() = prefs.getInt(KEY_NUKE_SIZE, 10)
        set(value) = prefs.edit().putInt(KEY_NUKE_SIZE, value).apply()

    var stabDepth: Int
        get() = prefs.getInt(KEY_STAB_DEPTH, 10)
        set(value) = prefs.edit().putInt(KEY_STAB_DEPTH, value).apply()

    var magazineSlot: Int
        get() = prefs.getInt(KEY_MAGAZINE_SLOT, 0)
        set(value) = prefs.edit().putInt(KEY_MAGAZINE_SLOT, value).apply()

    fun getTargetPresets(): List<TargetPreset> {
        val raw = prefs.getString(KEY_TARGET_PRESETS, null)
        if (raw == null) {
            // Default built-in presets
            val defaults = listOf(
                TargetPreset("Main Base", 1024.0, 68.0, -2048.0),
                TargetPreset("Enemy Spawner", 2048.0, 70.0, 1024.0),
                TargetPreset("Target Alpha", 0.0, 72.0, -5000.0)
            )
            saveTargetPresets(defaults)
            return defaults
        }
        return try {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    add(
                        TargetPreset(
                            name = obj.optString("name", "Preset"),
                            x = obj.optDouble("x", 0.0),
                            y = obj.optDouble("y", 64.0),
                            z = obj.optDouble("z", 0.0)
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun addTargetPreset(preset: TargetPreset): List<TargetPreset> {
        val current = getTargetPresets().toMutableList()
        // Replace if name exists, otherwise append
        val existingIdx = current.indexOfFirst { it.name.equals(preset.name, ignoreCase = true) }
        if (existingIdx != -1) {
            current[existingIdx] = preset
        } else {
            current.add(preset)
        }
        saveTargetPresets(current)
        return current
    }

    fun deleteTargetPreset(name: String): List<TargetPreset> {
        val current = getTargetPresets().filterNot { it.name.equals(name, ignoreCase = true) }
        saveTargetPresets(current)
        return current
    }

    private fun saveTargetPresets(presets: List<TargetPreset>) {
        val array = JSONArray()
        presets.forEach { p ->
            val obj = JSONObject().apply {
                put("name", p.name)
                put("x", p.x)
                put("y", p.y)
                put("z", p.z)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_TARGET_PRESETS, array.toString()).apply()
    }

    fun getHistory(): List<FiringHistoryEntry> {
        val raw = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val variant = CannonOriginVariant.entries.find { it.id == obj.optString("cannonVariant") }
                        ?: CannonOriginVariant.OSC_MK6
                    val mode = FireMode.entries.find { it.id == obj.optString("fireMode") }
                        ?: FireMode.NUKE
                    add(
                        FiringHistoryEntry(
                            id = obj.optLong("id", System.currentTimeMillis() - i),
                            cannonVariant = variant,
                            fireMode = mode,
                            originX = obj.optDouble("originX", 0.0),
                            originY = obj.optDouble("originY", 64.0),
                            originZ = obj.optDouble("originZ", 0.0),
                            targetX = obj.optDouble("targetX", 0.0),
                            targetY = obj.optDouble("targetY", 68.0),
                            targetZ = obj.optDouble("targetZ", -2048.0),
                            passcode = obj.optInt("passcode", 940),
                            nukeSize = obj.optInt("nukeSize", 10),
                            stabDepth = obj.optInt("stabDepth", 10),
                            magazineSlot = obj.optInt("magazineSlot", 0),
                            distance = obj.optDouble("distance", 0.0),
                            azimuthDegrees = obj.optDouble("azimuthDegrees", 0.0),
                            totalTnt = obj.optInt("totalTnt", 0),
                            timeEstimate = obj.optString("timeEstimate", "00:00:00")
                        )
                    )
                }
            }.take(MAX_HISTORY_SIZE)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun addHistoryEntry(entry: FiringHistoryEntry): List<FiringHistoryEntry> {
        val existing = getHistory().filterNot { prev ->
            prev.cannonVariant == entry.cannonVariant &&
                prev.fireMode == entry.fireMode &&
                prev.originX == entry.originX &&
                prev.originY == entry.originY &&
                prev.originZ == entry.originZ &&
                prev.targetX == entry.targetX &&
                prev.targetY == entry.targetY &&
                prev.targetZ == entry.targetZ &&
                prev.passcode == entry.passcode &&
                prev.nukeSize == entry.nukeSize &&
                prev.stabDepth == entry.stabDepth &&
                prev.magazineSlot == entry.magazineSlot
        }
        val updated = (listOf(entry) + existing).take(MAX_HISTORY_SIZE)
        saveHistory(updated)
        return updated
    }

    fun clearHistory(): List<FiringHistoryEntry> {
        prefs.edit().remove(KEY_HISTORY).apply()
        return emptyList()
    }

    private fun saveHistory(entries: List<FiringHistoryEntry>) {
        val array = JSONArray()
        entries.take(MAX_HISTORY_SIZE).forEach { e ->
            val obj = JSONObject().apply {
                put("id", e.id)
                put("cannonVariant", e.cannonVariant.id)
                put("fireMode", e.fireMode.id)
                put("originX", e.originX)
                put("originY", e.originY)
                put("originZ", e.originZ)
                put("targetX", e.targetX)
                put("targetY", e.targetY)
                put("targetZ", e.targetZ)
                put("passcode", e.passcode)
                put("nukeSize", e.nukeSize)
                put("stabDepth", e.stabDepth)
                put("magazineSlot", e.magazineSlot)
                put("distance", e.distance)
                put("azimuthDegrees", e.azimuthDegrees)
                put("totalTnt", e.totalTnt)
                put("timeEstimate", e.timeEstimate)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_HISTORY, array.toString()).apply()
    }

    companion object {
        const val MAX_HISTORY_SIZE = 5
        private const val KEY_ORIGIN_X = "origin_x"
        private const val KEY_ORIGIN_Y = "origin_y"
        private const val KEY_ORIGIN_Z = "origin_z"
        private const val KEY_TARGET_X = "target_x"
        private const val KEY_TARGET_Y = "target_y"
        private const val KEY_TARGET_Z = "target_z"
        private const val KEY_TARGET_NAME = "target_name"
        private const val KEY_PASSCODE = "passcode"
        private const val KEY_CANNON_VARIANT = "cannon_variant"
        private const val KEY_FIRE_MODE = "fire_mode"
        private const val KEY_NUKE_SIZE = "nuke_size"
        private const val KEY_STAB_DEPTH = "stab_depth"
        private const val KEY_MAGAZINE_SLOT = "magazine_slot"
        private const val KEY_HISTORY = "firing_history"
        private const val KEY_TARGET_PRESETS = "target_presets"
    }
}
