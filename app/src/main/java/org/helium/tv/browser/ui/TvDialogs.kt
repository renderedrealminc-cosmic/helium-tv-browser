package org.helium.tv.browser.ui

import android.content.Context
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.helium.tv.browser.engine.ExtensionItem
import org.helium.tv.browser.engine.ExtensionManager
import org.helium.tv.browser.search.OpenSearchManager
import org.helium.tv.browser.search.SearchEngine

object TvDialogs {
    private const val TAG = "TvDialogs"

    fun showMenuDialog(
        context: Context,
        onGeminiClick: () -> Unit,
        onExtensionsClick: () -> Unit,
        onPointerToggle: () -> Unit,
        onUserAgentClick: () -> Unit,
        onSearchEngineClick: () -> Unit,
        onPipClick: () -> Unit,
        onReloadClick: () -> Unit,
        onClearDataClick: () -> Unit
    ) {
        try {
            val options = arrayOf(
                "✨ Gemini AI Assistant",
                "🧩 Extensions & Add-ons (.xpi)",
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
                            0 -> onGeminiClick()
                            1 -> onExtensionsClick()
                            2 -> onPointerToggle()
                            3 -> onUserAgentClick()
                            4 -> onSearchEngineClick()
                            5 -> onPipClick()
                            6 -> onReloadClick()
                            7 -> onClearDataClick()
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

    fun showExtensionManagerDialog(
        context: Context,
        extensionManager: ExtensionManager,
        scope: CoroutineScope
    ) {
        try {
            val builder = AlertDialog.Builder(context)
            builder.setTitle("🧩 Extension Manager")

            val container = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(32, 24, 32, 24)
            }

            val tvStatus = TextView(context).apply {
                text = "Loading installed extensions..."
                textSize = 15f
            }
            container.addView(tvStatus)

            val installedListContainer = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
            }
            container.addView(installedListContainer)

            val recommendedHeader = TextView(context).apply {
                text = "\n🌟 Recommended Extensions:"
                textSize = 16f
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
            container.addView(recommendedHeader)

            for (rec in ExtensionManager.RECOMMENDED_EXTENSIONS) {
                val btnInstall = Button(context).apply {
                    text = "Install: ${rec.first} (${rec.second})"
                    isFocusable = true
                    setOnClickListener {
                        isEnabled = false
                        text = "Downloading ${rec.first}..."
                        scope.launch {
                            val success = extensionManager.downloadAndInstallXpi(rec.third)
                            withContext(Dispatchers.Main) {
                                if (success) {
                                    Toast.makeText(context, "✅ Installed ${rec.first} successfully!", Toast.LENGTH_SHORT).show()
                                    text = "Installed: ${rec.first}"
                                } else {
                                    Toast.makeText(context, "❌ Failed installing ${rec.first}", Toast.LENGTH_SHORT).show()
                                    isEnabled = true
                                    text = "Install: ${rec.first}"
                                }
                            }
                        }
                    }
                }
                container.addView(btnInstall)
            }

            builder.setView(container)
            builder.setPositiveButton("Close") { dialog, _ -> dialog.dismiss() }
            val dialog = builder.create()
            dialog.show()

            // Fetch installed extensions asynchronously
            scope.launch {
                val list = extensionManager.refreshInstalledExtensions()
                withContext(Dispatchers.Main) {
                    installedListContainer.removeAllViews()
                    if (list.isEmpty()) {
                        tvStatus.text = "No extensions installed yet."
                    } else {
                        tvStatus.text = "Installed Extensions (${list.size}):"
                        for (ext in list) {
                            val row = LinearLayout(context).apply {
                                orientation = LinearLayout.HORIZONTAL
                                setPadding(0, 8, 0, 8)
                            }
                            val title = TextView(context).apply {
                                text = "• ${ext.name} (v${ext.version})"
                                textSize = 14f
                                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                            }
                            val btnRemove = Button(context).apply {
                                text = "Uninstall"
                                isFocusable = true
                                setOnClickListener {
                                    ext.rawExtension?.let { raw ->
                                        scope.launch {
                                            extensionManager.uninstallExtension(raw)
                                            withContext(Dispatchers.Main) {
                                                installedListContainer.removeView(row)
                                                Toast.makeText(context, "Uninstalled ${ext.name}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                }
                            }
                            row.addView(title)
                            row.addView(btnRemove)
                            installedListContainer.addView(row)
                        }
                    }
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Failed showing extension manager dialog", t)
        }
    }
}
