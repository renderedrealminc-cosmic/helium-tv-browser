package org.helium.tv.browser

import android.content.res.Configuration
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import org.helium.tv.browser.adblock.FilterListManager
import org.helium.tv.browser.adblock.RustAdBlocker
import org.helium.tv.browser.engine.GeckoEngineManager
import org.helium.tv.browser.engine.HeliumContentBlockingDelegate
import org.helium.tv.browser.engine.HeliumNavigationDelegate
import org.helium.tv.browser.input.TvActionListener
import org.helium.tv.browser.input.TvKeyHandler
import org.helium.tv.browser.input.VirtualMouseOverlay
import org.helium.tv.browser.pip.FloatingPipManager
import org.helium.tv.browser.ui.DefaultBookmarks
import org.helium.tv.browser.ui.TvDialogs
import org.helium.tv.browser.ui.TvSpeedDialAdapter
import org.helium.tv.browser.utils.UserAgentMode
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView

class MainActivity : AppCompatActivity(), TvActionListener {

    private lateinit var geckoView: GeckoView
    private lateinit var geckoSession: GeckoSession
    private lateinit var engineManager: GeckoEngineManager
    private lateinit var virtualMouseOverlay: VirtualMouseOverlay
    private lateinit var tvKeyHandler: TvKeyHandler
    private lateinit var pipManager: FloatingPipManager
    private lateinit var filterManager: FilterListManager

    // UI elements
    private lateinit var toolbarContainer: View
    private lateinit var urlEditText: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var btnMouseToggle: Button
    private lateinit var btnExtensions: Button
    private lateinit var btnAdblockStats: Button
    private lateinit var btnUserAgent: Button
    private lateinit var btnOpacity: Button
    private lateinit var speedDialRecyclerView: RecyclerView
    private lateinit var speedDialContainer: View

    private var currentUaMode = UserAgentMode.TV

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        initBrowserEngine()
        initTvControls()
        initSpeedDial()

        // Load filter lists in background
        filterManager = FilterListManager(this)
        lifecycleScope.launch {
            filterManager.initializeFilters()
        }
    }

    private fun initViews() {
        geckoView = findViewById(R.id.geckoView)
        virtualMouseOverlay = findViewById(R.id.virtualMouseOverlay)
        toolbarContainer = findViewById(R.id.toolbarContainer)
        urlEditText = findViewById(R.id.urlEditText)
        progressBar = findViewById(R.id.progressBar)
        btnMouseToggle = findViewById(R.id.btnMouseToggle)
        btnExtensions = findViewById(R.id.btnExtensions)
        btnAdblockStats = findViewById(R.id.btnAdblockStats)
        btnUserAgent = findViewById(R.id.btnUserAgent)
        btnOpacity = findViewById(R.id.btnOpacity)
        speedDialRecyclerView = findViewById(R.id.speedDialRecyclerView)
        speedDialContainer = findViewById(R.id.speedDialContainer)

        pipManager = FloatingPipManager(this)

        urlEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_DONE) {
                val input = urlEditText.text.toString().trim()
                loadUrl(input)
                true
            } else {
                false
            }
        }

        btnMouseToggle.setOnClickListener { onToggleMousePointer() }
        btnExtensions.setOnClickListener { onOpenExtensionManager() }
        btnAdblockStats.setOnClickListener { onToggleAdBlocker() }
        btnUserAgent.setOnClickListener { cycleUserAgent() }
        btnOpacity.setOnClickListener {
            val alpha = pipManager.cycleTranslucency(findViewById(R.id.rootLayout))
            btnOpacity.text = "Alpha: ${(alpha * 100).toInt()}%"
        }
    }

    private fun initBrowserEngine() {
        engineManager = GeckoEngineManager(this, lifecycleScope)
        geckoSession = engineManager.createSession()

        val navDelegate = HeliumNavigationDelegate(
            context = this,
            onPageStarted = { url ->
                runOnUiThread {
                    urlEditText.setText(url)
                    progressBar.visibility = View.VISIBLE
                    speedDialContainer.visibility = View.GONE
                }
            },
            onPageFinished = {
                runOnUiThread {
                    progressBar.visibility = View.GONE
                }
            },
            onTitleChanged = { title ->
                runOnUiThread {
                    if (!urlEditText.hasFocus()) {
                        // Title updated
                    }
                }
            },
            onProgressChanged = { progress ->
                runOnUiThread {
                    progressBar.progress = progress
                    if (progress >= 100) {
                        progressBar.visibility = View.GONE
                    }
                }
            }
        )

        val cbDelegate = HeliumContentBlockingDelegate(lifecycleScope) { count, _ ->
            runOnUiThread {
                btnAdblockStats.text = "🛡️ $count"
            }
        }

        geckoSession.navigationDelegate = navDelegate
        geckoSession.progressDelegate = navDelegate
        geckoSession.contentBlockingDelegate = cbDelegate

        geckoSession.open(engineManager.runtime)
        geckoView.setSession(geckoSession)

        // Bind virtual mouse to geckoView
        virtualMouseOverlay.targetView = geckoView
    }

    private fun initTvControls() {
        tvKeyHandler = TvKeyHandler(virtualMouseOverlay, this)
    }

    private fun initSpeedDial() {
        speedDialRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        speedDialRecyclerView.adapter = TvSpeedDialAdapter(DefaultBookmarks.ITEMS) { bookmark ->
            loadUrl(bookmark.url)
        }
    }

    private fun loadUrl(rawInput: String) {
        val url = when {
            rawInput.startsWith("http://") || rawInput.startsWith("https://") -> rawInput
            rawInput.contains(".") && !rawInput.contains(" ") -> "https://$rawInput"
            else -> "https://duckduckgo.com/?q=${android.net.Uri.encode(rawInput)}"
        }
        urlEditText.setText(url)
        geckoSession.loadUri(url)
        speedDialContainer.visibility = View.GONE
    }

    private fun cycleUserAgent() {
        currentUaMode = when (currentUaMode) {
            UserAgentMode.TV -> UserAgentMode.DESKTOP
            UserAgentMode.DESKTOP -> UserAgentMode.MOBILE
            UserAgentMode.MOBILE -> UserAgentMode.TV
        }
        engineManager.setUserAgentMode(geckoSession, currentUaMode)
        btnUserAgent.text = "UA: ${currentUaMode.name}"
        geckoSession.reload()
        Toast.makeText(this, "Switched to ${currentUaMode.name} User-Agent", Toast.LENGTH_SHORT).show()
    }

    // Key dispatching for TV Remote
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            if (tvKeyHandler.onKeyDown(event.keyCode, event)) return true
        } else if (event.action == KeyEvent.ACTION_UP) {
            if (tvKeyHandler.onKeyUp(event.keyCode, event)) return true
        }
        return super.dispatchKeyEvent(event)
    }

    // TvActionListener callbacks
    override fun onToggleMousePointer() {
        val active = virtualMouseOverlay.toggleMousePointer()
        btnMouseToggle.text = if (active) "🖱️ Mouse: ON" else "🎯 Focus Mode"
        Toast.makeText(
            this,
            if (active) "Virtual Mouse Pointer Activated" else "D-Pad Focus Mode Activated",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onOpenExtensionManager() {
        TvDialogs.showExtensionManagerDialog(this, engineManager.extensionManager, lifecycleScope)
    }

    override fun onToggleAdBlocker() {
        val count = RustAdBlocker.getBlockedCount()
        Toast.makeText(this, "Rust AdBlocker: $count ads/trackers blocked so far", Toast.LENGTH_SHORT).show()
    }

    override fun onTogglePipMode() {
        pipManager.enterSystemPip()
    }

    override fun onOpenMenu() {
        toolbarContainer.visibility = if (toolbarContainer.visibility == View.VISIBLE) View.GONE else View.VISIBLE
    }

    override fun onNavigateBack(): Boolean {
        if (toolbarContainer.visibility == View.VISIBLE) {
            toolbarContainer.visibility = View.GONE
            return true
        }
        if (speedDialContainer.visibility == View.GONE) {
            // Try going back in web history
            geckoSession.goBack()
            return true
        }
        return false
    }

    override fun onMediaKey(keyCode: Int): Boolean {
        // Can send javascript media control
        return true
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        pipManager.onPictureInPictureModeChanged(isInPictureInPictureMode)
        if (isInPictureInPictureMode) {
            toolbarContainer.visibility = View.GONE
            virtualMouseOverlay.visibility = View.GONE
        } else {
            virtualMouseOverlay.visibility = View.VISIBLE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        geckoSession.close()
    }
}
