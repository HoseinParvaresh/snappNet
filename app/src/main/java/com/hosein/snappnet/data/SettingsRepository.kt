package com.hosein.snappnet.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/** Look of the floating card. Colours are 24-bit RGB; opacity is a percentage. */
data class PopupStyle(
    val bgColor: Int = 0x111418,
    val bgAlpha: Int = 95,
    val netColor: Int = 0x2ECC71,
    val textColor: Int = 0xB0B7C0,
    val netSize: Int = 28,
    val textSize: Int = 12,
    val corner: Int = 16,
    val padding: Int = 12,
    val widthDp: Int = 0, // 0 = automatic
)

data class AppSettings(
    val detectionEnabled: Boolean = true,
    val overlayEnabled: Boolean = true,
    val displayInToman: Boolean = true,
    val showTitle: Boolean = true,
    val showGross: Boolean = true,
    val showDeductions: Boolean = true,
    val startMinimized: Boolean = false,
    val lowThresholdToman: Int = 0, // 0 = off
    val overlayX: Int = -1, // -1 = default position
    val overlayY: Int = -1,
    val debugMode: Boolean = false,
    val style: PopupStyle = PopupStyle(),
)

/** In-memory state shared by the service and the UI (same process). */
object AppState {
    data class Diag(
        val events: Int = 0,
        val scans: Int = 0,
        val snappPackage: String = "",
        val otherPackage: String = "",
        val texts: List<String> = emptyList(),
        val extractedFare: Long? = null,
        val windows: String = "",
    )

    val diag = MutableStateFlow(Diag())
    val serviceRunning = MutableStateFlow(false)
}

object Keys {
    val DETECTION = booleanPreferencesKey("detection_enabled")
    val OVERLAY = booleanPreferencesKey("overlay_enabled")
    val TOMAN = booleanPreferencesKey("display_in_toman")
    val TITLE = booleanPreferencesKey("show_title")
    val GROSS = booleanPreferencesKey("show_gross")
    val DEDUCTIONS = booleanPreferencesKey("show_deductions")
    val START_MIN = booleanPreferencesKey("start_minimized")
    val LOW = intPreferencesKey("low_threshold_toman")
    val X = intPreferencesKey("overlay_x")
    val Y = intPreferencesKey("overlay_y")
    val DEBUG = booleanPreferencesKey("debug_mode")

    val BG = intPreferencesKey("style_bg")
    val BG_ALPHA = intPreferencesKey("style_bg_alpha")
    val NET_COLOR = intPreferencesKey("style_net_color")
    val TEXT_COLOR = intPreferencesKey("style_text_color")
    val NET_SIZE = intPreferencesKey("style_net_size")
    val TEXT_SIZE = intPreferencesKey("style_text_size")
    val CORNER = intPreferencesKey("style_corner")
    val PADDING = intPreferencesKey("style_padding")
    val WIDTH = intPreferencesKey("style_width")

    val STYLE_KEYS: List<Preferences.Key<*>> =
        listOf(BG, BG_ALPHA, NET_COLOR, TEXT_COLOR, NET_SIZE, TEXT_SIZE, CORNER, PADDING, WIDTH, TITLE, GROSS, DEDUCTIONS, LOW)
}

private val Context.dataStore by preferencesDataStore(name = "snapp_net_settings")

class SettingsRepository(context: Context) {
    private val store = context.applicationContext.dataStore

    val settings: Flow<AppSettings> = store.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { p ->
            val d = AppSettings()
            val ds = PopupStyle()
            AppSettings(
                detectionEnabled = p[Keys.DETECTION] ?: d.detectionEnabled,
                overlayEnabled = p[Keys.OVERLAY] ?: d.overlayEnabled,
                displayInToman = p[Keys.TOMAN] ?: d.displayInToman,
                showTitle = p[Keys.TITLE] ?: d.showTitle,
                showGross = p[Keys.GROSS] ?: d.showGross,
                showDeductions = p[Keys.DEDUCTIONS] ?: d.showDeductions,
                startMinimized = p[Keys.START_MIN] ?: d.startMinimized,
                lowThresholdToman = (p[Keys.LOW] ?: d.lowThresholdToman).coerceIn(0, 10_000_000),
                overlayX = p[Keys.X] ?: d.overlayX,
                overlayY = p[Keys.Y] ?: d.overlayY,
                debugMode = p[Keys.DEBUG] ?: d.debugMode,
                style = PopupStyle(
                    bgColor = (p[Keys.BG] ?: ds.bgColor) and 0xFFFFFF,
                    bgAlpha = (p[Keys.BG_ALPHA] ?: ds.bgAlpha).coerceIn(20, 100),
                    netColor = (p[Keys.NET_COLOR] ?: ds.netColor) and 0xFFFFFF,
                    textColor = (p[Keys.TEXT_COLOR] ?: ds.textColor) and 0xFFFFFF,
                    netSize = (p[Keys.NET_SIZE] ?: ds.netSize).coerceIn(14, 56),
                    textSize = (p[Keys.TEXT_SIZE] ?: ds.textSize).coerceIn(8, 24),
                    corner = (p[Keys.CORNER] ?: ds.corner).coerceIn(0, 40),
                    padding = (p[Keys.PADDING] ?: ds.padding).coerceIn(4, 32),
                    widthDp = (p[Keys.WIDTH] ?: ds.widthDp).coerceIn(0, 420),
                ),
            )
        }

    suspend fun <T> set(key: Preferences.Key<T>, value: T) {
        store.edit { it[key] = value }
    }

    suspend fun setOverlayPosition(x: Int, y: Int) {
        store.edit { it[Keys.X] = x; it[Keys.Y] = y }
    }

    /** Restores the popup look and its content options (keeps position and permissions-related switches). */
    suspend fun resetPopupStyle() {
        store.edit { p -> Keys.STYLE_KEYS.forEach { p.remove(it) } }
    }

    suspend fun resetAll() {
        store.edit { it.clear() }
    }
}
