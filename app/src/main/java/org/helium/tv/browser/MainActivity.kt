package org.helium.tv.browser

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.helium.tv.browser.adblock.FilterListManager
import org.helium.tv.browser.adblock.RustAdBlocker
import org.helium.tv.browser.ai.GeminiTvAssistant
import org.helium.tv.browser.engine.GeckoEngineManager
import org.helium.tv.browser.engine.HeliumContentBlockingDelegate
import org.helium.tv.browser.engine.HeliumNavigationDelegate
import org.helium.tv.browser.input.TvActionListener
import org.helium.tv.browser.input.TvKeyHandler
import org.helium.tv.browser.input.VirtualMouseOverlay
import org.helium.tv.browser.pip.FloatingPipManager
import org.helium.tv.browser.search.OpenSearchManager
import org.helium.tv.browser.ui.TvDialogs
import org.helium.tv.browser.utils.UserAgentMode
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView

class MainActivity : AppCompatActivity(), TvActionListener {

    companion object {
        private const val TAG = "HeliumMainActivity"
        private const val HOME_URL = "https://www.bing.com"
        private const val GOOGLE_URL = "https://www.google.com"
        private const val YOUTUBE_URL = "https://www.youtube.com"
    }

    private lateinit var geckoView: GeckoView
    private lateinit var geckoSession: GeckoSession
    private lateinit var engineManager: GeckoEngineManager
    private lateinit var virtualMouseOverlay: VirtualMouseOverlay
    private lateinit var tvKeyHandler: TvKeyHandler
    private lateinit var pipManager: FloatingPipManager
    private lateinit var filterManager: FilterListManager
    private lateinit var openSearchManager: OpenSearchManager

    // Top Navigation & Address Bar Views
    private lateinit var browserTopBar: View
    private lateinit var ivAppLogo: ImageView
    private lateinit var btnHome: ImageButton
    private lateinit var etUrl: EditText
    private lateinit var btnReload: ImageButton
    private lateinit var btnShieldLayout: View
    private lateinit var tvShieldCount: TextView
    private lateinit var btnMenu: ImageButton
    private lateinit var progressBar: ProgressBar

    // Bottom Shortcut Dock Views
    private lateinit var bottomShortcutBar: View
    private lateinit var btnQuickBing: Button
    private lateinit var btnQuickGoogle: Button
    private lateinit var btnQuickYoutube: Button
    private lateinit var btnQuickGemini: Button
    private lateinit var btnQuickExtensions: Button

    // Gemini AI Panel
    private lateinit var geminiPanel: View
    private lateinit var btnCloseGemini: Button
    private lateinit var etGeminiPrompt: EditText
    private lateinit var btnQuickSummarize: Button
    private lateinit var btnQuickHindi: Button
    private lateinit var tvGeminiResponse: TextView

    private var currentUaMode = UserAgentMode.TV
    private var currentPageTitle = ""
    private var currentPageUrl = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        openSearchManager = OpenSearchManager(this)
        initViews()
        initGeminiPanel()
        initBrowserEngine()
        initTvControls()

        filterManager = FilterListManager(this)
        lifecycleScope.launch {
            try {
                filterManager.initializeFilters()
            } catch (t: Throwable) {
                Log.w(TAG, "Error initializing filters", t)
            }
        }

        // On launch: immediately load Microsoft Bing as requested by the user
        loadUrl(HOME_URL)
    }

    private fun initViews() {
        geckoView = findViewById(R.id.geckoView)
        virtualMouseOverlay = findViewById(R.id.virtualMouseOverlay)
        pipManager = FloatingPipManager(this)

        // Top Navigation Bar
        browserTopBar = findViewById(R.id.browserTopBar)
        ivAppLogo = findViewById(R.id.ivAppLogo)
        btnHome = findViewById(R.id.btnHome)
        etUrl = findViewById(R.id.etUrl)
        btnReload = findViewById(R.id.btnReload)
        btnShieldLayout = findViewById(R.id.btnShieldLayout)
        tvShieldCount = findViewById(R.id.tvShieldCount)
        btnMenu = findViewById(R.id.btnMenu)
        progressBar = findViewById(R.id.progressBar)

        // Bottom Shortcut Dock
        bottomShortcutBar = findViewById(R.id.bottomShortcutBar)
        btnQuickBing = findViewById(R.id.btnQuickBing)
        btnQuickGoogle = findViewById(R.id.btnQuickGoogle)
        btnQuickYoutube = findViewById(R.id.btnQuickYoutube)
        btnQuickGemini = findViewById(R.id.btnQuickGemini)
        btnQuickExtensions = findViewById(R.id.btnQuickExtensions)

        // Button Click Listeners with Crash-Safe wrappers
        btnHome.setOnClickListener {
            safelyRun { loadUrl(HOME_URL) }
        }

        btnReload.setOnClickListener {
            safelyRun { geckoSession.reload() }
        }

        btnShieldLayout.setOnClickListener {
            safelyRun { onToggleAdBlocker() }
        }

        btnMenu.setOnClickListener {
            safelyRun { showTvMenu() }
        }

        // Bottom Shortcut Listeners
        btnQuickBing.setOnClickListener {
            safelyRun { loadUrl(HOME_URL) }
        }

        btnQuickGoogle.setOnClickListener {
            safelyRun { loadUrl(GOOGLE_URL) }
        }

        btnQuickYoutube.setOnClickListener {
            safelyRun { loadUrl(YOUTUBE_URL) }
        }

        btnQuickGemini.setOnClickListener {
            safelyRun { toggleGeminiPanel() }
        }

        btnQuickExtensions.setOnClickListener {
            safelyRun { onOpenExtensionManager() }
        }

        // URL / Search input listener
        etUrl.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                val input = etUrl.text.toString().trim()
                if (input.isNotEmpty()) {
                    loadUrl(input)
                }
                true
            } else {
                false
            }
        }
    }

    private fun initGeminiPanel() {
        geminiPanel = findViewById(R.id.geminiPanel)
        btnCloseGemini = findViewById(R.id.btnCloseGemini)
        etGeminiPrompt = findViewById(R.id.etGeminiPrompt)
        btnQuickSummarize = findViewById(R.id.btnQuickSummarize)
        btnQuickHindi = findViewById(R.id.btnQuickHindi)
        tvGeminiResponse = findViewById(R.id.tvGeminiResponse)

        btnCloseGemini.setOnClickListener {
            geminiPanel.visibility = View.GONE
        }

        etGeminiPrompt.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND || actionId == EditorInfo.IME_ACTION_DONE) {
                val query = etGeminiPrompt.text.toString().trim()
                if (query.isNotEmpty()) {
                    runGeminiQuery(query, "Page Title: $currentPageTitle | URL: $currentPageUrl")
                }
                true
            } else {
                false
            }
        }

        btnQuickSummarize.setOnClickListener {
            safelyRun {
                runGeminiQuery(
                    "Is web page ke mukhya points ko aasan 3-4 bullet points me summarize karo.",
                    "Title: $currentPageTitle | URL: $currentPageUrl"
                )
            }
        }

        btnQuickHindi.setOnClickListener {
            safelyRun {
                runGeminiQuery(
                    "Is website ke baare me simple Hindi me explain karo ki ye page kis baare me hai.",
                    "Title: $currentPageTitle | URL: $currentPageUrl"
                )
            }
        }
    }

    private fun toggleGeminiPanel() {
        if (geminiPanel.visibility == View.VISIBLE) {
            geminiPanel.visibility = View.GONE
        } else {
            geminiPanel.visibility = View.VISIBLE
            etGeminiPrompt.requestFocus()
        }
    }

    private fun runGeminiQuery(prompt: String, context: String) {
        tvGeminiResponse.text = "⚡ Gemini soch raha hai..."
        lifecycleScope.launch {
            try {
                val reply = GeminiTvAssistant.queryGemini(prompt, context)
                tvGeminiResponse.text = reply
            } catch (t: Throwable) {
                tvGeminiResponse.text = "Error: Could not reach Gemini AI."
            }
        }
    }

    private fun initBrowserEngine() {
        try {
            engineManager = GeckoEngineManager(this, lifecycleScope)
            geckoSession = engineManager.createSession()

            val navDelegate = HeliumNavigationDelegate(
                context = this,
                onPageStarted = { url ->
                    runOnUiThread {
                        if (url.isNotEmpty() && url != "about:blank") {
                            currentPageUrl = url
                            etUrl.setText(url)
                            progressBar.visibility = View.VISIBLE
                        }
                    }
                },
                onPageFinished = {
                    runOnUiThread {
                        progressBar.visibility = View.GONE
                    }
                },
                onTitleChanged = { title ->
                    runOnUiThread {
                        currentPageTitle = title
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
                    if (count > 0) {
                        tvShieldCount.visibility = View.VISIBLE
                        tvShieldCount.text = "$count"
                    }
                }
            }

            geckoSession.navigationDelegate = navDelegate
            geckoSession.progressDelegate = navDelegate
            geckoSession.contentBlockingDelegate = cbDelegate

            geckoSession.open(engineManager.runtime)
            geckoView.setSession(geckoSession)

            virtualMouseOverlay.targetView = geckoView
        } catch (t: Throwable) {
            Log.e(TAG, "Fatal initializing GeckoView engine", t)
        }
    }

    private fun initTvControls() {
        tvKeyHandler = TvKeyHandler(virtualMouseOverlay, this)
    }

    fun loadUrl(rawInput: String) {
        if (rawInput.isBlank()) return

        val url = when {
            rawInput.startsWith("http://") || rawInput.startsWith("https://") -> rawInput
            rawInput.contains(".") && !rawInput.contains(" ") -> "https://$rawInput"
            else -> openSearchManager.buildSearchUrl(rawInput)
        }

        currentPageUrl = url
        etUrl.setText(url)
        progressBar.visibility = View.VISIBLE

        safelyRun {
            geckoSession.loadUri(url)
        }
    }

    private fun showTvMenu() {
        TvDialogs.showMenuDialog(
            context = this,
            onGeminiClick = { toggleGeminiPanel() },
            onExtensionsClick = { onOpenExtensionManager() },
            onPointerToggle = { onToggleMousePointer() },
            onUserAgentClick = { cycleUserAgent() },
            onSearchEngineClick = {
                TvDialogs.showSearchEngineDialog(this, openSearchManager) { selected ->
                    etUrl.hint = "Search ${selected.shortName} or type URL"
                }
            },
            onPipClick = { onTogglePipMode() },
            onReloadClick = { geckoSession.reload() },
            onClearDataClick = {
                engineManager.clearBrowsingData()
                Toast.makeText(this, "Browsing cache & cookies cleared!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun cycleUserAgent() {
        currentUaMode = when (currentUaMode) {
            UserAgentMode.TV -> UserAgentMode.DESKTOP
            UserAgentMode.DESKTOP -> UserAgentMode.MOBILE
            UserAgentMode.MOBILE -> UserAgentMode.TV
        }
        safelyRun {
            engineManager.setUserAgentMode(geckoSession, currentUaMode)
            geckoSession.reload()
            Toast.makeText(this, "Switched to ${currentUaMode.name} User-Agent", Toast.LENGTH_SHORT).show()
        }
    }

    private inline fun safelyRun(block: () -> Unit) {
        try {
            block()
        } catch (t: Throwable) {
            Log.w(TAG, "Safe execution caught exception", t)
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        try {
            if (event.action == KeyEvent.ACTION_DOWN) {
                if (tvKeyHandler.onKeyDown(event.keyCode, event)) return true
            } else if (event.action == KeyEvent.ACTION_UP) {
                if (tvKeyHandler.onKeyUp(event.keyCode, event)) return true
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Key dispatch exception", t)
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onToggleMousePointer() {
        val active = virtualMouseOverlay.toggleMousePointer()
        Toast.makeText(
            this,
            if (active) "🎯 Pointer: ON" else "🎮 Pointer: OFF (Touch / D-Pad)",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onOpenExtensionManager() {
        TvDialogs.showExtensionManagerDialog(this, engineManager.extensionManager, lifecycleScope)
    }

    override fun onToggleAdBlocker() {
        val count = RustAdBlocker.getBlockedCount()
        Toast.makeText(this, "🛡️ Rust AdBlocker: $count ads blocked", Toast.LENGTH_SHORT).show()
    }

    override fun onTogglePipMode() {
        pipManager.enterSystemPip()
    }

    override fun onOpenMenu() {
        showTvMenu()
    }

    override fun onNavigateBack(): Boolean {
        if (geminiPanel.visibility == View.VISIBLE) {
            geminiPanel.visibility = View.GONE
            return true
        }
        // Try to navigate back in web session
        safelyRun {
            geckoSession.goBack()
        }
        return true
    }

    override fun onMediaKey(keyCode: Int): Boolean {
        return true
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        pipManager.onPictureInPictureModeChanged(isInPictureInPictureMode)
        if (isInPictureInPictureMode) {
            browserTopBar.visibility = View.GONE
            bottomShortcutBar.visibility = View.GONE
            virtualMouseOverlay.visibility = View.GONE
            geminiPanel.visibility = View.GONE
        } else {
            browserTopBar.visibility = View.VISIBLE
            bottomShortcutBar.visibility = View.VISIBLE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        safelyRun { geckoSession.close() }
    }
}
