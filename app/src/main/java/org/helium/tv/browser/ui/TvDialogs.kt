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

object TvDialogs {

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
