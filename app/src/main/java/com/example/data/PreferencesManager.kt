package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.domain.CannonOriginVariant
import com.example.domain.FireMode
import com.example.domain.FiringHistoryEntry
import com.example.ui.ScreenMode
import org.json.JSONArray
import org.json.JSONObject

open class ModePreferencesManager(context: Context, private val variant: CannonOriginVariant) {
    private val fileName = when (variant) {
        CannonOriginVariant.OSC_MK6 -> "osc_mk6_prefs"
        CannonOriginVariant.OSC_MS -> "osc_ms_prefs"
        CannonOriginVariant.OSC_SFC -> "osc_sf_prefs"
    }
    private val prefs: SharedPreferences = context.getSharedPreferences(fileName, Context.MODE_PRIVATE)

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
        get() = prefs.getFloat(KEY_TARGET_X, if (variant == CannonOriginVariant.OSC_SFC) 100.0f else 1024.0f).toDouble()
        set(value) = prefs.edit().putFloat(KEY_TARGET_X, value.toFloat()).apply()

    var targetY: Double
        get() = prefs.getFloat(KEY_TARGET_Y, 68.0f).toDouble()
        set(value) = prefs.edit().putFloat(KEY_TARGET_Y, value.toFloat()).apply()

    var targetZ: Double
        get() = prefs.getFloat(KEY_TARGET_Z, if (variant == CannonOriginVariant.OSC_SFC) 100.0f else -2048.0f).toDouble()
        set(value) = prefs.edit().putFloat(KEY_TARGET_Z, value.toFloat()).apply()

    var targetName: String
        get() = prefs.getString(KEY_TARGET_NAME, "Target 1") ?: "Target 1"
        set(value) = prefs.edit().putString(KEY_TARGET_NAME, value).apply()

    var passcode: Int
        get() = prefs.getInt(KEY_PASSCODE, 940)
        set(value) = prefs.edit().putInt(KEY_PASSCODE, value).apply()

    var fireMode: FireMode
        get() {
            val id = prefs.getString(KEY_FIRE_MODE, FireMode.NUKE.id)
            return FireMode.entries.find { it.id == id } ?: FireMode.NUKE
        }
        set(value) = prefs.edit().putString(KEY_FIRE_MODE, value.id).apply()

    var nukeSize: Int
        get() = prefs.getInt(KEY_NUKE_SIZE, if (variant == CannonOriginVariant.OSC_SFC) 16 else 10)
        set(value) = prefs.edit().putInt(KEY_NUKE_SIZE, value).apply()

    var stabDepth: Int
        get() = prefs.getInt(KEY_STAB_DEPTH, 10)
        set(value) = prefs.edit().putInt(KEY_STAB_DEPTH, value).apply()

    var magazineSlot: Int
        get() = prefs.getInt(KEY_MAGAZINE_SLOT, 0)
        set(value) = prefs.edit().putInt(KEY_MAGAZINE_SLOT, value).apply()

    var screenMode: ScreenMode
        get() {
            val id = prefs.getString(KEY_SCREEN_MODE, ScreenMode.INPUT.name)
            return try {
                ScreenMode.valueOf(id ?: ScreenMode.INPUT.name)
            } catch (_: Exception) {
                ScreenMode.INPUT
            }
        }
        set(value) = prefs.edit().putString(KEY_SCREEN_MODE, value.name).apply()

    fun getHistory(): List<FiringHistoryEntry> {
        val raw = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val v = CannonOriginVariant.entries.find { it.id == obj.optString("cannonVariant") }
                        ?: variant
                    val mode = FireMode.entries.find { it.id == obj.optString("fireMode") }
                        ?: FireMode.NUKE
                    add(
                        FiringHistoryEntry(
                            id = obj.optLong("id", System.currentTimeMillis() - i),
                            cannonVariant = v,
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
        val existing = getHistory()
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
        private const val KEY_FIRE_MODE = "fire_mode"
        private const val KEY_NUKE_SIZE = "nuke_size"
        private const val KEY_STAB_DEPTH = "stab_depth"
        private const val KEY_MAGAZINE_SLOT = "magazine_slot"
        private const val KEY_SCREEN_MODE = "screen_mode"
        private const val KEY_HISTORY = "firing_history"
    }
}

class MK6Memory(context: Context) : ModePreferencesManager(context, CannonOriginVariant.OSC_MK6)
class MSMemory(context: Context) : ModePreferencesManager(context, CannonOriginVariant.OSC_MS)
class SFMemory(context: Context) : ModePreferencesManager(context, CannonOriginVariant.OSC_SFC)
