package org.helium.tv.browser.ui

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import org.helium.tv.browser.search.OpenSearchManager
import org.helium.tv.browser.search.SearchEngine

object TvDialogs {
    private const val TAG = "TvDialogs"

    fun showMenuDialog(
        context: Context,
        onGeminiClick: () -> Unit,
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
                            1 -> onPointerToggle()
                            2 -> onUserAgentClick()
                            3 -> onSearchEngineClick()
                            4 -> onPipClick()
                            5 -> onReloadClick()
                            6 -> onClearDataClick()
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
