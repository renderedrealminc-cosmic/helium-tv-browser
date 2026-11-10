package org.helium.tv.browser

import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.helium.tv.browser.adblock.FilterListManager
import org.helium.tv.browser.adblock.RustAdBlocker
import org.helium.tv.browser.ai.GeminiTvAssistant
import org.helium.tv.browser.engine.HeliumWebChromeClient
import org.helium.tv.browser.engine.HeliumWebViewClient
import org.helium.tv.browser.engine.TvNativeCore
import org.helium.tv.browser.input.TvActionListener
import org.helium.tv.browser.input.TvKeyHandler
import org.helium.tv.browser.input.VirtualMouseOverlay
import org.helium.tv.browser.pip.FloatingPipManager
import org.helium.tv.browser.search.OpenSearchManager
import org.helium.tv.browser.ui.TvDialogs
import org.helium.tv.browser.utils.UserAgentMode
import org.helium.tv.browser.utils.UserAgentUtils

class MainActivity : AppCompatActivity(), TvActionListener {

    companion object {
        private const val TAG = "HeliumMainActivity"
        const val HOME_URL = "https://www.bing.com"
        const val GOOGLE_URL = "https://www.google.com"
        const val YOUTUBE_URL = "https://www.youtube.com"
    }

    private lateinit var webView: WebView
    private lateinit var virtualMouseOverlay: VirtualMouseOverlay
    private lateinit var tvKeyHandler: TvKeyHandler
    private lateinit var pipManager: FloatingPipManager
    private lateinit var filterManager: FilterListManager
    private lateinit var openSearchManager: OpenSearchManager

    // Top Chromium Omnibox Bar Views
    private lateinit var topBar: View
    private lateinit var btnBack: ImageButton
    private lateinit var btnForward: ImageButton
    private lateinit var btnReload: ImageButton
    private lateinit var btnHome: ImageButton
    private lateinit var etOmnibox: EditText
    private lateinit var btnShieldLayout: View
    private lateinit var tvShieldCount: TextView
    private lateinit var btnMenu: ImageButton
    private lateinit var progressBar: ProgressBar

    // Home Screen Views
    private lateinit var homeView: View
    private lateinit var etCenterSearch: EditText
    private lateinit var btnShortcutBing: ImageButton
    private lateinit var btnShortcutGoogle: ImageButton
    private lateinit var btnShortcutYoutube: ImageButton
    private lateinit var btnShortcutGemini: ImageButton

    // Error & Retry Screen Views
    private lateinit var errorContainer: View
    private lateinit var tvErrorMessage: TextView
    private lateinit var btnRetry: Button
    private lateinit var btnErrorHome: Button

    // Gemini AI Panel Views
    private lateinit var geminiPanel: View
    private lateinit var btnCloseGemini: Button
    private lateinit var etGeminiPrompt: EditText
    private lateinit var btnQuickSummarize: Button
    private lateinit var btnQuickHindi: Button
    private lateinit var tvGeminiResponse: TextView

    private var currentUaMode = UserAgentMode.TV
    private var currentPageTitle = ""
    private var currentPageUrl = ""
    private var lastFailedUrl = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        openSearchManager = OpenSearchManager(this)
        initViews()
        initGeminiPanel()
        initWebView()
        initTvControls()

        filterManager = FilterListManager(this)
        lifecycleScope.launch {
            try {
                filterManager.initializeFilters()
            } catch (t: Throwable) {
                Log.w(TAG, "Error initializing adblock filters", t)
            }
        }

        // Show Home View initially (Bing / Search Home)
        returnToHome()
    }

    private fun initViews() {
        webView = findViewById(R.id.webView)
        virtualMouseOverlay = findViewById(R.id.virtualMouseOverlay)
        pipManager = FloatingPipManager(this)

        // Top Omnibox Bar
        topBar = findViewById(R.id.topBar)
        btnBack = findViewById(R.id.btnBack)
        btnForward = findViewById(R.id.btnForward)
        btnReload = findViewById(R.id.btnReload)
        btnHome = findViewById(R.id.btnHome)
        etOmnibox = findViewById(R.id.etOmnibox)
        btnShieldLayout = findViewById(R.id.btnShieldLayout)
        tvShieldCount = findViewById(R.id.tvShieldCount)
        btnMenu = findViewById(R.id.btnMenu)
        progressBar = findViewById(R.id.progressBar)

        // Home View & Center Search
        homeView = findViewById(R.id.homeView)
        etCenterSearch = findViewById(R.id.etCenterSearch)
        btnShortcutBing = findViewById(R.id.btnShortcutBing)
        btnShortcutGoogle = findViewById(R.id.btnShortcutGoogle)
        btnShortcutYoutube = findViewById(R.id.btnShortcutYoutube)
        btnShortcutGemini = findViewById(R.id.btnShortcutGemini)

        // Error Container
        errorContainer = findViewById(R.id.errorContainer)
        tvErrorMessage = findViewById(R.id.tvErrorMessage)
        btnRetry = findViewById(R.id.btnRetry)
        btnErrorHome = findViewById(R.id.btnErrorHome)

        // Navigation button listeners
        btnBack.setOnClickListener {
            safelyRun {
                if (webView.visibility == View.VISIBLE && webView.canGoBack()) {
                    webView.goBack()
                } else {
                    returnToHome()
                }
            }
        }

        btnForward.setOnClickListener {
            safelyRun {
                if (webView.visibility == View.VISIBLE && webView.canGoForward()) {
                    webView.goForward()
                }
            }
        }

        btnReload.setOnClickListener {
            safelyRun {
                if (webView.visibility == View.VISIBLE) {
                    webView.reload()
                } else {
                    loadUrl(HOME_URL)
                }
            }
        }

        btnHome.setOnClickListener {
            safelyRun { returnToHome() }
        }

        btnShieldLayout.setOnClickListener {
            safelyRun { onToggleAdBlocker() }
        }

        btnMenu.setOnClickListener {
            safelyRun { showTvMenu() }
        }

        // Shortcuts
        btnShortcutBing.setOnClickListener {
            safelyRun { loadUrl(HOME_URL) }
        }

        btnShortcutGoogle.setOnClickListener {
            safelyRun { loadUrl(GOOGLE_URL) }
        }

        btnShortcutYoutube.setOnClickListener {
            safelyRun { loadUrl(YOUTUBE_URL) }
        }

        btnShortcutGemini.setOnClickListener {
            safelyRun { toggleGeminiPanel() }
        }

        // Error retry buttons
        btnRetry.setOnClickListener {
            safelyRun {
                if (lastFailedUrl.isNotEmpty()) {
                    loadUrl(lastFailedUrl)
                } else {
                    loadUrl(HOME_URL)
                }
            }
        }

        btnErrorHome.setOnClickListener {
            safelyRun { returnToHome() }
        }

        // Omnibox URL / Search input listener
        etOmnibox.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                val input = etOmnibox.text.toString()
                loadUrl(processInput(input))
                true
            } else {
                false
            }
        }

        // Center Search input listener
        etCenterSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_DONE) {
                val input = etCenterSearch.text.toString()
                loadUrl(processInput(input))
                true
            } else {
                false
            }
        }
    }

    private fun initWebView() {
        try {
            val settings = webView.settings
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            settings.setSupportMultipleWindows(false)
            settings.mediaPlaybackRequiresUserGesture = false
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            settings.userAgentString = UserAgentUtils.getUserAgent(currentUaMode)

            // Hardware acceleration
            webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)

            webView.webViewClient = HeliumWebViewClient(
                context = this,
                onPageStarted = { url ->
                    runOnUiThread {
                        currentPageUrl = url
                        etOmnibox.setText(url)
                        progressBar.visibility = View.VISIBLE
                        errorContainer.visibility = View.GONE
                        homeView.visibility = View.GONE
                        webView.visibility = View.VISIBLE
                    }
                },
                onPageFinished = { url ->
                    runOnUiThread {
                        currentPageUrl = url
                        etOmnibox.setText(url)
                        progressBar.visibility = View.GONE
                    }
                },
                onError = { failingUrl, description ->
                    runOnUiThread {
                        lastFailedUrl = failingUrl
                        progressBar.visibility = View.GONE
                        webView.visibility = View.GONE
                        homeView.visibility = View.GONE
                        errorContainer.visibility = View.VISIBLE
                        tvErrorMessage.text = "Failed to load: $failingUrl\n($description)"
                    }
                },
                onBlockedResource = { count ->
                    runOnUiThread {
                        if (count > 0) {
                            tvShieldCount.visibility = View.VISIBLE
                            tvShieldCount.text = "$count"
                        }
                    }
                }
            )

            webView.webChromeClient = HeliumWebChromeClient(
                onProgressChanged = { progress ->
                    runOnUiThread {
                        progressBar.progress = progress
                        if (progress >= 100) {
                            progressBar.visibility = View.GONE
                        }
                    }
                },
                onTitleChanged = { title ->
                    runOnUiThread {
                        currentPageTitle = title
                    }
                },
                onCustomViewToggled = { customView, _ ->
                    // Fullscreen video toggle
                    if (customView != null) {
                        topBar.visibility = View.GONE
                    } else {
                        topBar.visibility = View.VISIBLE
                    }
                }
            )

            virtualMouseOverlay.targetView = webView
        } catch (t: Throwable) {
            Log.e(TAG, "Fatal initializing Chromium WebView", t)
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

    private fun initTvControls() {
        tvKeyHandler = TvKeyHandler(virtualMouseOverlay, this)
    }

    /**
     * Bing Search & URL Logic specified by User:
     * - Trims extra spaces.
     * - If starts with http:// or https:// or looks like a valid domain: loads URL directly (adding https:// if missing).
     * - Otherwise: treats as search query, URL-encodes it properly, and loads: https://www.bing.com/search?q={query}.
     */
    fun processInput(rawInput: String): String {
        val text = rawInput.trim()
        if (text.isEmpty()) return HOME_URL

        // Check if starts with http:// or https://
        if (text.startsWith("http://", ignoreCase = true) || text.startsWith("https://", ignoreCase = true)) {
            return text
        }

        // Check if looks like a valid domain (e.g. example.com, youtube.com, bbc.co.uk)
        val domainPattern = Regex("^[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+(/.*)?$")
        if (domainPattern.matches(text) && !text.contains(" ")) {
            return "https://$text"
        }

        // Treat as search query: properly URL-encode text and load Bing
        val encoded = Uri.encode(text)
        return "https://www.bing.com/search?q=$encoded"
    }

    fun loadUrl(url: String) {
        if (url.isBlank()) return

        currentPageUrl = url
        etOmnibox.setText(url)
        progressBar.visibility = View.VISIBLE
        errorContainer.visibility = View.GONE
        homeView.visibility = View.GONE
        webView.visibility = View.VISIBLE

        safelyRun {
            webView.loadUrl(url)
        }
    }

    fun returnToHome() {
        homeView.visibility = View.VISIBLE
        webView.visibility = View.GONE
        errorContainer.visibility = View.GONE
        progressBar.visibility = View.GONE
        currentPageUrl = ""
        etOmnibox.setText("")
        etCenterSearch.setText("")
    }

    private fun showTvMenu() {
        TvDialogs.showMenuDialog(
            context = this,
            onGeminiClick = { toggleGeminiPanel() },
            onPointerToggle = { onToggleMousePointer() },
            onUserAgentClick = { cycleUserAgent() },
            onSearchEngineClick = {
                TvDialogs.showSearchEngineDialog(this, openSearchManager) { selected ->
                    etOmnibox.hint = "Search ${selected.shortName} or type URL"
                }
            },
            onPipClick = { onTogglePipMode() },
            onReloadClick = { webView.reload() },
            onClearDataClick = {
                safelyRun {
                    webView.clearCache(true)
                    webView.clearHistory()
                    android.webkit.CookieManager.getInstance().removeAllCookies(null)
                    Toast.makeText(this, "Cache & Cookies cleared!", Toast.LENGTH_SHORT).show()
                }
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
            webView.settings.userAgentString = UserAgentUtils.getUserAgent(currentUaMode)
            webView.reload()
            Toast.makeText(this, "User-Agent: ${currentUaMode.name}", Toast.LENGTH_SHORT).show()
        }
    }

    private inline fun safelyRun(block: () -> Unit) {
        try {
            block()
        } catch (t: Throwable) {
            Log.w(TAG, "Safely caught exception", t)
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        try {
            TvNativeCore.recordFrame()
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
        // Not used right now as per user instruction: "not use heavy features right now"
        Toast.makeText(this, "Core Browser Mode Active", Toast.LENGTH_SHORT).show()
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
        if (errorContainer.visibility == View.VISIBLE) {
            returnToHome()
            return true
        }
        if (webView.visibility == View.VISIBLE) {
            if (webView.canGoBack()) {
                webView.goBack()
                return true
            } else {
                returnToHome()
                return true
            }
        }
        return false
    }

    override fun onMediaKey(keyCode: Int): Boolean {
        return true
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        pipManager.onPictureInPictureModeChanged(isInPictureInPictureMode)
        if (isInPictureInPictureMode) {
            topBar.visibility = View.GONE
            virtualMouseOverlay.visibility = View.GONE
            geminiPanel.visibility = View.GONE
        } else {
            topBar.visibility = View.VISIBLE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        safelyRun {
            webView.destroy()
        }
    }
}
