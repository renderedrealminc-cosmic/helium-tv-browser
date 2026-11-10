package org.helium.tv.browser.ui

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.helium.tv.browser.R
import org.helium.tv.browser.engine.ExtensionItem
import org.helium.tv.browser.engine.ExtensionManager
import org.helium.tv.browser.search.OpenSearchManager
import org.helium.tv.browser.search.SearchEngine

object TvDialogs {

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
        val options = arrayOf(
            "✨ Gemini AI Assistant",
            "🧩 Extensions & Add-ons (.xpi)",
            "🎯 Toggle Neon Mouse Pointer",
            "🖥️ Switch User-Agent (TV / Desktop / Mobile)",
            "🔍 Search Engine (OpenSearch XML)",
            "🪟 Picture-in-Picture Mode",
            "🔄 Reload Page",
            "🗑️ Clear Cache & Cookies"
        )

        AlertDialog.Builder(context)
            .setTitle("⚙️ Helium TV - Options")
            .setItems(options) { dialog, which ->
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
                dialog.dismiss()
            }
            .setNegativeButton("Close", null)
            .show()
    }

    fun showSearchEngineDialog(
        context: Context,
        searchManager: OpenSearchManager,
        onEngineSelected: (SearchEngine) -> Unit
    ) {
        val engines = searchManager.getAvailableEngines()
        val names = engines.map { "${it.shortName} (${it.description})" }.toTypedArray()

        AlertDialog.Builder(context)
            .setTitle("🔍 Select OpenSearch Engine")
            .setItems(names) { dialog, which ->
                val selected = engines[which]
                searchManager.activeEngine = selected
                onEngineSelected(selected)
                Toast.makeText(context, "Search Engine: ${selected.shortName}", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    fun showTabsDialog(
        context: Context,
        tabCount: Int,
        onNewTab: () -> Unit
    ) {
        val items = arrayOf(
            "Active Tab 1 (Current Session)",
            "➕ Open New Blank Tab"
        )

        AlertDialog.Builder(context)
            .setTitle("📑 Tabs Manager ($tabCount open)")
            .setItems(items) { dialog, which ->
                if (which == 1) {
                    onNewTab()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Close", null)
            .show()
    }

    fun showExtensionManagerDialog(
        context: Context,
        extensionManager: ExtensionManager,
        scope: CoroutineScope
    ) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("🧩 Helium TV - Extension Manager")

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 24, 32, 24)
        }

        val tvStatus = TextView(context).apply {
            text = "Loading installed extensions..."
            textSize = 16f
        }
        container.addView(tvStatus)

        val installedListContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
        container.addView(installedListContainer)

        val recommendedHeader = TextView(context).apply {
            text = "\n🌟 Recommended TV Extensions (1-Click Install):"
            textSize = 18f
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
                    tvStatus.text = "No extensions installed yet. Click below to install popular TV extensions:"
                } else {
                    tvStatus.text = "Installed Extensions (${list.size}):"
                    for (ext in list) {
                        val row = LinearLayout(context).apply {
                            orientation = LinearLayout.HORIZONTAL
                            setPadding(0, 8, 0, 8)
                        }
                        val title = TextView(context).apply {
                            text = "• ${ext.name} (v${ext.version})"
                            textSize = 16f
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
    }
}
