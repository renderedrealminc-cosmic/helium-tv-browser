package org.helium.tv.browser.ui

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import org.helium.tv.browser.extensions.ExtensionManager
import org.helium.tv.browser.search.OpenSearchManager
import org.helium.tv.browser.search.SearchEngine
import org.helium.tv.browser.settings.HomeLayoutMode
import org.helium.tv.browser.settings.PreferencesManager
import org.helium.tv.browser.settings.WallpaperTheme

object TvDialogs {
    private const val TAG = "TvDialogs"

    /**
     * Modern Apple Liquid Glass Confirmation Popup for installing a Chrome extension.
     */
    fun showAddExtensionDialog(
        context: Context,
        extensionName: String,
        extensionId: String,
        onConfirm: () -> Unit,
        onCancel: (() -> Unit)? = null
    ) {
        try {
            val message = "Do you want to add \"$extensionName\" to Renderium?\n\n" +
                    "It can:\n" +
                    "• Read and change all your data on the websites you visit\n" +
                    "• Modify web page content and block requests"

            val dialog = AlertDialog.Builder(context)
                .setTitle("🧩 Add Extension to Renderium?")
                .setMessage(message)
                .setPositiveButton("Add Extension") { d, _ ->
                    d.dismiss()
                    onConfirm()
                }
                .setNegativeButton("Cancel") { d, _ ->
                    d.dismiss()
                    onCancel?.invoke()
                }
                .create()

            dialog.window?.setBackgroundDrawableResource(org.helium.tv.browser.R.drawable.bg_liquid_glass_dialog)
            dialog.show()
        } catch (t: Throwable) {
            Log.e(TAG, "Failed showing add extension dialog", t)
        }
    }

    /**
     * Extension Manager dialog showing installed extensions with ON/OFF switch & uninstall.
     */
    fun showExtensionsManagerDialog(
        context: Context,
        extensionManager: ExtensionManager,
        onOpenWebStore: () -> Unit,
        onRefresh: () -> Unit
    ) {
        try {
            val installed = extensionManager.getInstalledExtensions()
            val items = mutableListOf<String>()

            if (installed.isEmpty()) {
                items.add("No extensions installed yet")
            } else {
                for (ext in installed) {
                    val status = if (ext.enabled) "✅ Active" else "⏸️ Disabled"
                    items.add("${ext.name} ($status)")
                }
            }
            items.add("🌐 Open Chrome Web Store")

            AlertDialog.Builder(context)
                .setTitle("🧩 Extensions Manager (${installed.size})")
                .setItems(items.toTypedArray()) { dialog, which ->
                    dialog.dismiss()
                    if (installed.isEmpty()) {
                        if (which == 1) onOpenWebStore()
                    } else {
                        if (which < installed.size) {
                            val selected = installed[which]
                            showExtensionOptions(context, selected.id, selected.name, selected.enabled, extensionManager) {
                                onRefresh()
                            }
                        } else {
                            onOpenWebStore()
                        }
                    }
                }
                .setNegativeButton("Close", null)
                .show()
        } catch (t: Throwable) {
            Log.e(TAG, "Failed showing extension manager dialog", t)
        }
    }

    private fun showExtensionOptions(
        context: Context,
        extId: String,
        extName: String,
        currentlyEnabled: Boolean,
        extensionManager: ExtensionManager,
        onChanged: () -> Unit
    ) {
        val toggleAction = if (currentlyEnabled) "⏸️ Disable Extension" else "▶️ Enable Extension"
        val options = arrayOf(toggleAction, "🗑️ Remove / Uninstall")

        AlertDialog.Builder(context)
            .setTitle(extName)
            .setItems(options) { dialog, which ->
                dialog.dismiss()
                when (which) {
                    0 -> {
                        extensionManager.setExtensionEnabled(extId, !currentlyEnabled)
                        Toast.makeText(context, "$extName: ${if (!currentlyEnabled) "Enabled" else "Disabled"}", Toast.LENGTH_SHORT).show()
                        onChanged()
                    }
                    1 -> {
                        extensionManager.uninstallExtension(extId)
                        Toast.makeText(context, "Uninstalled $extName", Toast.LENGTH_SHORT).show()
                        onChanged()
                    }
                }
            }
            .setNegativeButton("Back", null)
            .show()
    }

    fun showSettingsDialog(
        context: Context,
        prefsManager: PreferencesManager,
        onSettingsChanged: () -> Unit
    ) {
        try {
            val items = arrayOf(
                "🎨 Wallpaper: ${prefsManager.theme.title}",
                "📱 Home Layout: ${prefsManager.layoutMode.title}",
                "🔍 Search Engine: ${prefsManager.searchEngine}",
                "📰 Microsoft Start News Feed: ${if (prefsManager.showNewsFeed) "ON" else "OFF"}",
                "🌐 App Language: ${if (prefsManager.language == "hi") "हिन्दी (Hindi)" else "English"}"
            )

            AlertDialog.Builder(context)
                .setTitle("⚙️ Settings & Customization")
                .setItems(items) { dialog, which ->
                    dialog.dismiss()
                    when (which) {
                        0 -> showWallpaperPicker(context, prefsManager, onSettingsChanged)
                        1 -> showLayoutPicker(context, prefsManager, onSettingsChanged)
                        2 -> showSearchEnginePicker(context, prefsManager, onSettingsChanged)
                        3 -> {
                            prefsManager.showNewsFeed = !prefsManager.showNewsFeed
                            onSettingsChanged()
                            Toast.makeText(
                                context,
                                "News Feed: ${if (prefsManager.showNewsFeed) "ON" else "OFF"}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        4 -> {
                            prefsManager.language = if (prefsManager.language == "hi") "en" else "hi"
                            onSettingsChanged()
                            Toast.makeText(
                                context,
                                "Language set to: ${if (prefsManager.language == "hi") "हिन्दी" else "English"}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
                .setNegativeButton("Close", null)
                .show()
        } catch (t: Throwable) {
            Log.e(TAG, "Failed showing settings dialog", t)
        }
    }

    private fun showWallpaperPicker(
        context: Context,
        prefsManager: PreferencesManager,
        onSettingsChanged: () -> Unit
    ) {
        val themes = WallpaperTheme.values()
        val themeNames = themes.map { it.title }.toTypedArray()
        val currentIndex = themes.indexOf(prefsManager.theme).coerceAtLeast(0)

        AlertDialog.Builder(context)
            .setTitle("🎨 Choose Wallpaper Theme")
            .setSingleChoiceItems(themeNames, currentIndex) { dialog, which ->
                prefsManager.theme = themes[which]
                onSettingsChanged()
                Toast.makeText(context, "Theme: ${themes[which].title}", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showLayoutPicker(
        context: Context,
        prefsManager: PreferencesManager,
        onSettingsChanged: () -> Unit
    ) {
        val layouts = HomeLayoutMode.values()
        val layoutNames = layouts.map { it.title }.toTypedArray()
        val currentIndex = layouts.indexOf(prefsManager.layoutMode).coerceAtLeast(0)

        AlertDialog.Builder(context)
            .setTitle("📱 Choose Home Screen Layout")
            .setSingleChoiceItems(layoutNames, currentIndex) { dialog, which ->
                prefsManager.layoutMode = layouts[which]
                onSettingsChanged()
                Toast.makeText(context, "Layout: ${layouts[which].title}", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showSearchEnginePicker(
        context: Context,
        prefsManager: PreferencesManager,
        onSettingsChanged: () -> Unit
    ) {
        val engines = arrayOf("Bing", "Google", "DuckDuckGo")
        val currentIndex = engines.indexOf(prefsManager.searchEngine).coerceAtLeast(0)

        AlertDialog.Builder(context)
            .setTitle("🔍 Default Search Engine")
            .setSingleChoiceItems(engines, currentIndex) { dialog, which ->
                prefsManager.searchEngine = engines[which]
                onSettingsChanged()
                Toast.makeText(context, "Search: ${engines[which]}", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    fun showMenuDialog(
        context: Context,
        onGeminiClick: () -> Unit,
        onPointerToggle: () -> Unit,
        onUserAgentClick: () -> Unit,
        onSearchEngineClick: () -> Unit,
        onPipClick: () -> Unit,
        onReloadClick: () -> Unit,
        onClearDataClick: () -> Unit,
        onSettingsClick: () -> Unit,
        onExtensionsClick: () -> Unit
    ) {
        try {
            val options = arrayOf(
                "⚙️ Settings & Customization",
                "🧩 Extensions Manager",
                "✨ Gemini AI Assistant",
                "🎯 Toggle Mouse Pointer",
                "🖥️ Switch User-Agent (TV / Desktop / Mobile)",
                "🔍 Search Engine (Bing / Google)",
                "🪟 Picture-in-Picture Mode",
                "🔄 Reload Page",
                "🗑️ Clear Cache & Cookies"
            )

            AlertDialog.Builder(context)
                .setTitle("⚙️ Options")
                .setItems(options) { dialog, which ->
                    try {
                        when (which) {
                            0 -> onSettingsClick()
                            1 -> onExtensionsClick()
                            2 -> onGeminiClick()
                            3 -> onPointerToggle()
                            4 -> onUserAgentClick()
                            5 -> onSearchEngineClick()
                            6 -> onPipClick()
                            7 -> onReloadClick()
                            8 -> onClearDataClick()
                        }
                    } catch (t: Throwable) {
                        Log.e(TAG, "Error in menu action", t)
                    }
                    dialog.dismiss()
                }
                .setNegativeButton("Close", null)
                .show()
        } catch (t: Throwable) {
            Log.e(TAG, "Failed showing menu dialog", t)
            Toast.makeText(context, "Options Menu", Toast.LENGTH_SHORT).show()
        }
    }

    fun showSearchEngineDialog(
        context: Context,
        searchManager: OpenSearchManager,
        onEngineSelected: (SearchEngine) -> Unit
    ) {
        try {
            val engines = searchManager.getAvailableEngines()
            val names = engines.map { "${it.shortName} (${it.description})" }.toTypedArray()

            AlertDialog.Builder(context)
                .setTitle("🔍 Select Search Engine")
                .setItems(names) { dialog, which ->
                    val selected = engines[which]
                    searchManager.activeEngine = selected
                    onEngineSelected(selected)
                    Toast.makeText(context, "Search Engine: ${selected.shortName}", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel", null)
                .show()
        } catch (t: Throwable) {
            Log.e(TAG, "Failed showing search engine dialog", t)
        }
    }

    fun showTabsDialog(
        context: Context,
        tabCount: Int,
        onNewTab: () -> Unit
    ) {
        try {
            val items = arrayOf(
                "Current Tab (Active)",
                "➕ Open New Blank Tab"
            )

            AlertDialog.Builder(context)
                .setTitle("📑 Tabs ($tabCount open)")
                .setItems(items) { dialog, which ->
                    if (which == 1) {
                        onNewTab()
                    }
                    dialog.dismiss()
                }
                .setNegativeButton("Close", null)
                .show()
        } catch (t: Throwable) {
            Log.e(TAG, "Failed showing tabs dialog", t)
        }
    }
}
