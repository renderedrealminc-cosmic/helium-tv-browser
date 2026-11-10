package org.helium.tv.browser.ui

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import org.helium.tv.browser.R
import org.helium.tv.browser.extensions.ExtensionManager
import org.helium.tv.browser.search.OpenSearchManager
import org.helium.tv.browser.search.SearchEngine
import org.helium.tv.browser.settings.HomeLayoutMode
import org.helium.tv.browser.settings.PreferencesManager
import org.helium.tv.browser.settings.WallpaperTheme
import org.helium.tv.browser.tabs.TabManager

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

            dialog.window?.setBackgroundDrawableResource(R.drawable.bg_liquid_glass_dialog)
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

            val dialog = AlertDialog.Builder(context)
                .setTitle("🧩 Extensions Manager (${installed.size})")
                .setItems(items.toTypedArray()) { d, which ->
                    d.dismiss()
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
                .create()

            dialog.window?.setBackgroundDrawableResource(R.drawable.bg_liquid_glass_dialog)
            dialog.show()
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

        val dialog = AlertDialog.Builder(context)
            .setTitle(extName)
            .setItems(options) { d, which ->
                d.dismiss()
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
            .create()

        dialog.window?.setBackgroundDrawableResource(R.drawable.bg_liquid_glass_dialog)
        dialog.show()
    }

    /**
     * Sleek Chrome-Style Liquid Glass Menu Popup
     */
    fun showChromeMenuDialog(
        context: Context,
        isDesktopMode: Boolean,
        currentSearchEngine: String,
        tabCount: Int,
        onBack: () -> Unit,
        onForward: () -> Unit,
        onReload: () -> Unit,
        onHome: () -> Unit,
        onPip: () -> Unit,
        onNewTab: () -> Unit,
        onNewIncognitoTab: () -> Unit,
        onTabsOverview: () -> Unit,
        onToggleDesktopMode: (Boolean) -> Unit,
        onSearchEngineClick: () -> Unit,
        onExtensionsClick: () -> Unit,
        onGeminiClick: () -> Unit,
        onPointerToggle: () -> Unit,
        onSettingsClick: () -> Unit,
        onClearDataClick: () -> Unit
    ) {
        try {
            val view = LayoutInflater.from(context).inflate(R.layout.dialog_chrome_menu, null)
            val dialog = AlertDialog.Builder(context)
                .setView(view)
                .create()

            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            var desktopActive = isDesktopMode

            // Header close
            view.findViewById<ImageButton>(R.id.btnMenuClose)?.setOnClickListener {
                dialog.dismiss()
            }

            // Quick actions
            view.findViewById<ImageButton>(R.id.btnQuickBack)?.setOnClickListener {
                dialog.dismiss()
                onBack()
            }
            view.findViewById<ImageButton>(R.id.btnQuickForward)?.setOnClickListener {
                dialog.dismiss()
                onForward()
            }
            view.findViewById<ImageButton>(R.id.btnQuickReload)?.setOnClickListener {
                dialog.dismiss()
                onReload()
            }
            view.findViewById<ImageButton>(R.id.btnQuickHome)?.setOnClickListener {
                dialog.dismiss()
                onHome()
            }
            view.findViewById<ImageButton>(R.id.btnQuickPip)?.setOnClickListener {
                dialog.dismiss()
                onPip()
            }

            // New Tab
            view.findViewById<View>(R.id.rowNewTab)?.setOnClickListener {
                dialog.dismiss()
                onNewTab()
            }

            // New Incognito Tab
            view.findViewById<View>(R.id.rowIncognitoTab)?.setOnClickListener {
                dialog.dismiss()
                onNewIncognitoTab()
            }

            // Tabs Overview
            view.findViewById<TextView>(R.id.tvMenuTabCountBadge)?.text = "$tabCount tab${if (tabCount > 1) "s" else ""}"
            view.findViewById<View>(R.id.rowTabsOverview)?.setOnClickListener {
                dialog.dismiss()
                onTabsOverview()
            }

            // Desktop Site Toggle with clear visual state
            val tvDesktopBadge = view.findViewById<TextView>(R.id.tvDesktopStatusBadge)
            val tvDesktopSub = view.findViewById<TextView>(R.id.tvDesktopSub)

            fun updateDesktopBadge(active: Boolean) {
                if (active) {
                    tvDesktopBadge?.text = "✓ ON"
                    tvDesktopBadge?.setTextColor(Color.parseColor("#00E5FF"))
                    tvDesktopSub?.text = "Desktop mode active"
                    tvDesktopSub?.setTextColor(Color.parseColor("#38BDF8"))
                } else {
                    tvDesktopBadge?.text = "OFF"
                    tvDesktopBadge?.setTextColor(Color.parseColor("#94A3B8"))
                    tvDesktopSub?.text = "Request desktop version"
                    tvDesktopSub?.setTextColor(Color.parseColor("#94A3B8"))
                }
            }
            updateDesktopBadge(desktopActive)

            view.findViewById<View>(R.id.rowDesktopSite)?.setOnClickListener {
                desktopActive = !desktopActive
                updateDesktopBadge(desktopActive)
                onToggleDesktopMode(desktopActive)
                dialog.dismiss()
            }

            // Search Engine Changer
            val tvEngineBadge = view.findViewById<TextView>(R.id.tvMenuSearchEngineBadge)
            val ivEngineIcon = view.findViewById<ImageView>(R.id.ivMenuSearchEngineIcon)
            tvEngineBadge?.text = currentSearchEngine
            if (currentSearchEngine.equals("Bing", ignoreCase = true)) {
                ivEngineIcon?.setImageResource(R.drawable.ic_bing)
            } else if (currentSearchEngine.equals("Google", ignoreCase = true)) {
                ivEngineIcon?.setImageResource(R.drawable.ic_google_g)
            } else {
                ivEngineIcon?.setImageResource(R.drawable.ic_search)
            }

            view.findViewById<View>(R.id.rowSearchEngine)?.setOnClickListener {
                dialog.dismiss()
                onSearchEngineClick()
            }

            // Extensions Manager
            view.findViewById<View>(R.id.rowExtensions)?.setOnClickListener {
                dialog.dismiss()
                onExtensionsClick()
            }

            // Gemini AI
            view.findViewById<View>(R.id.rowGeminiAi)?.setOnClickListener {
                dialog.dismiss()
                onGeminiClick()
            }

            // Mouse Pointer
            view.findViewById<View>(R.id.rowMousePointer)?.setOnClickListener {
                dialog.dismiss()
                onPointerToggle()
            }

            // Settings & Themes
            view.findViewById<View>(R.id.rowSettings)?.setOnClickListener {
                dialog.dismiss()
                onSettingsClick()
            }

            // Clear Data
            view.findViewById<View>(R.id.rowClearData)?.setOnClickListener {
                dialog.dismiss()
                onClearDataClick()
            }

            dialog.show()
        } catch (t: Throwable) {
            Log.e(TAG, "Failed showing chrome menu dialog", t)
        }
    }

    /**
     * Interactive Multi-Tab Switcher Dialog
     */
    fun showTabsDialog(
        context: Context,
        tabManager: TabManager,
        onSelectTab: (String) -> Unit,
        onCloseTab: (String) -> Unit,
        onNewTab: () -> Unit,
        onCloseAllTabs: () -> Unit
    ) {
        try {
            val view = LayoutInflater.from(context).inflate(R.layout.dialog_tabs_switcher, null)
            val dialog = AlertDialog.Builder(context)
                .setView(view)
                .create()

            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            val tabsContainer = view.findViewById<LinearLayout>(R.id.tabsContainer)
            val tvTabsSubtitle = view.findViewById<TextView>(R.id.tvTabsSubtitle)
            val btnDialogNewTab = view.findViewById<Button>(R.id.btnDialogNewTab)
            val btnCloseAllTabs = view.findViewById<Button>(R.id.btnCloseAllTabs)
            val btnDialogCloseTabs = view.findViewById<ImageButton>(R.id.btnDialogCloseTabs)

            btnDialogCloseTabs?.setOnClickListener { dialog.dismiss() }

            btnDialogNewTab?.setOnClickListener {
                dialog.dismiss()
                onNewTab()
            }

            btnCloseAllTabs?.setOnClickListener {
                dialog.dismiss()
                onCloseAllTabs()
            }

            fun populateTabs() {
                tabsContainer?.removeAllViews()
                val tabs = tabManager.tabs
                val count = tabs.size
                tvTabsSubtitle?.text = "$count open tab${if (count != 1) "s" else ""}"

                val inflater = LayoutInflater.from(context)
                for (tab in tabs) {
                    val card = inflater.inflate(R.layout.item_tab_card, tabsContainer, false)
                    val tvTitle = card.findViewById<TextView>(R.id.tvTabTitle)
                    val tvUrl = card.findViewById<TextView>(R.id.tvTabUrl)
                    val ivIcon = card.findViewById<ImageView>(R.id.ivTabIcon)
                    val tvActive = card.findViewById<TextView>(R.id.tvActiveBadge)
                    val btnClose = card.findViewById<ImageButton>(R.id.btnTabClose)

                    tvTitle.text = if (tab.title.isNotBlank()) tab.title else "New Tab"
                    tvUrl.text = if (tab.url.isNotBlank()) tab.url else "Home"

                    if (tab.isIncognito) {
                        ivIcon.setImageResource(R.drawable.ic_incognito)
                    } else {
                        ivIcon.setImageResource(R.drawable.ic_tabs)
                    }

                    val isActive = tab.id == tabManager.activeTabId
                    tvActive.visibility = if (isActive) View.VISIBLE else View.GONE

                    card.setOnClickListener {
                        dialog.dismiss()
                        onSelectTab(tab.id)
                    }

                    btnClose.setOnClickListener {
                        onCloseTab(tab.id)
                        if (tabManager.count == 0) {
                            dialog.dismiss()
                        } else {
                            populateTabs()
                        }
                    }

                    tabsContainer?.addView(card)
                }
            }

            populateTabs()
            dialog.show()
        } catch (t: Throwable) {
            Log.e(TAG, "Failed showing tabs switcher dialog", t)
        }
    }

    fun showSearchEngineDialog(
        context: Context,
        currentEngine: String,
        onEngineSelected: (String) -> Unit
    ) {
        try {
            val engines = arrayOf("Bing", "Google", "DuckDuckGo")
            val currentIndex = engines.indexOfFirst { it.equals(currentEngine, ignoreCase = true) }.coerceAtLeast(0)

            val dialog = AlertDialog.Builder(context)
                .setTitle("🔍 Select Default Search Engine")
                .setSingleChoiceItems(engines, currentIndex) { d, which ->
                    d.dismiss()
                    val selected = engines[which]
                    onEngineSelected(selected)
                    Toast.makeText(context, "Search Engine: $selected", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .create()

            dialog.window?.setBackgroundDrawableResource(R.drawable.bg_liquid_glass_dialog)
            dialog.show()
        } catch (t: Throwable) {
            Log.e(TAG, "Failed showing search engine dialog", t)
        }
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

            val dialog = AlertDialog.Builder(context)
                .setTitle("⚙️ Settings & Customization")
                .setItems(items) { d, which ->
                    d.dismiss()
                    when (which) {
                        0 -> showWallpaperPicker(context, prefsManager, onSettingsChanged)
                        1 -> showLayoutPicker(context, prefsManager, onSettingsChanged)
                        2 -> showSearchEngineDialog(context, prefsManager.searchEngine) { newEngine ->
                            prefsManager.searchEngine = newEngine
                            onSettingsChanged()
                        }
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
                .create()

            dialog.window?.setBackgroundDrawableResource(R.drawable.bg_liquid_glass_dialog)
            dialog.show()
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

        val dialog = AlertDialog.Builder(context)
            .setTitle("🎨 Choose Wallpaper Theme")
            .setSingleChoiceItems(themeNames, currentIndex) { d, which ->
                prefsManager.theme = themes[which]
                onSettingsChanged()
                Toast.makeText(context, "Theme: ${themes[which].title}", Toast.LENGTH_SHORT).show()
                d.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.window?.setBackgroundDrawableResource(R.drawable.bg_liquid_glass_dialog)
        dialog.show()
    }

    private fun showLayoutPicker(
        context: Context,
        prefsManager: PreferencesManager,
        onSettingsChanged: () -> Unit
    ) {
        val layouts = HomeLayoutMode.values()
        val layoutNames = layouts.map { it.title }.toTypedArray()
        val currentIndex = layouts.indexOf(prefsManager.layoutMode).coerceAtLeast(0)

        val dialog = AlertDialog.Builder(context)
            .setTitle("📱 Choose Home Screen Layout")
            .setSingleChoiceItems(layoutNames, currentIndex) { d, which ->
                prefsManager.layoutMode = layouts[which]
                onSettingsChanged()
                Toast.makeText(context, "Layout: ${layouts[which].title}", Toast.LENGTH_SHORT).show()
                d.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.window?.setBackgroundDrawableResource(R.drawable.bg_liquid_glass_dialog)
        dialog.show()
    }
}
