package org.helium.tv.browser.settings

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Configuration
import org.helium.tv.browser.R

enum class WallpaperTheme(val title: String, val drawableRes: Int) {
    ALOHA_BLUE("🌊 Sky Blue Aloha", R.drawable.bg_aloha_wallpaper),
    EDGE_ARC_PASTEL("🫧 Iridescent Liquid Glass", R.drawable.bg_liquid_wallpaper),
    OLED_BLACK("🖤 OLED Pitch Black", R.drawable.bg_oled_wallpaper),
    CYBERPUNK_DARK("🔮 Cyberpunk Neon Dark", R.drawable.bg_cyberpunk_wallpaper)
}

enum class HomeLayoutMode(val title: String) {
    AUTO("🤖 Auto (Aloha on Mobile, Edge Arc on TV)"),
    MOBILE_ALOHA("📲 Aloha Mobile Layout"),
    TV_EDGE_ARC("📺 Edge / Arc TV Layout"),
    DIRECT_WEB("🌐 Direct Search Engine (Bing / Google)")
}

class PreferencesManager(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "renderium_browser_prefs"
        private const val KEY_THEME = "wallpaper_theme"
        private const val KEY_LAYOUT = "home_layout_mode"
        private const val KEY_SEARCH_ENGINE = "default_search_engine"
        private const val KEY_SHOW_NEWS = "show_news_feed"
        private const val KEY_LANGUAGE = "app_language"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var theme: WallpaperTheme
        get() {
            val name = prefs.getString(KEY_THEME, null)
            return if (name != null) {
                try {
                    WallpaperTheme.valueOf(name)
                } catch (e: Exception) {
                    getDefaultTheme()
                }
            } else {
                getDefaultTheme()
            }
        }
        set(value) {
            prefs.edit().putString(KEY_THEME, value.name).apply()
        }

    var layoutMode: HomeLayoutMode
        get() {
            val name = prefs.getString(KEY_LAYOUT, HomeLayoutMode.AUTO.name)
            return try {
                HomeLayoutMode.valueOf(name ?: HomeLayoutMode.AUTO.name)
            } catch (e: Exception) {
                HomeLayoutMode.AUTO
            }
        }
        set(value) {
            prefs.edit().putString(KEY_LAYOUT, value.name).apply()
        }

    var searchEngine: String
        get() = prefs.getString(KEY_SEARCH_ENGINE, "Bing") ?: "Bing"
        set(value) = prefs.edit().putString(KEY_SEARCH_ENGINE, value).apply()

    var showNewsFeed: Boolean
        get() = prefs.getBoolean(KEY_SHOW_NEWS, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_NEWS, value).apply()

    var language: String
        get() = prefs.getString(KEY_LANGUAGE, "en") ?: "en"
        set(value) = prefs.edit().putString(KEY_LANGUAGE, value).apply()

    fun isTvDevice(): Boolean {
        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? android.app.UiModeManager
        if (uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION) return true
        val hasLeanback = context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
        val hasTouch = context.packageManager.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN)
        return hasLeanback || !hasTouch
    }

    fun isAlohaMobileActive(): Boolean {
        return when (layoutMode) {
            HomeLayoutMode.MOBILE_ALOHA -> true
            HomeLayoutMode.TV_EDGE_ARC -> false
            HomeLayoutMode.DIRECT_WEB -> false
            HomeLayoutMode.AUTO -> !isTvDevice()
        }
    }

    fun isEdgeArcActive(): Boolean {
        return when (layoutMode) {
            HomeLayoutMode.TV_EDGE_ARC -> true
            HomeLayoutMode.MOBILE_ALOHA -> false
            HomeLayoutMode.DIRECT_WEB -> false
            HomeLayoutMode.AUTO -> isTvDevice()
        }
    }

    private fun getDefaultTheme(): WallpaperTheme {
        return if (isTvDevice()) WallpaperTheme.EDGE_ARC_PASTEL else WallpaperTheme.ALOHA_BLUE
    }
}
