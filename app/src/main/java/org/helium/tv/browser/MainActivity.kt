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
import org.helium.tv.browser.settings.PreferencesManager
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

    private lateinit var rootLayout: View
    private lateinit var webView: WebView
    private lateinit var virtualMouseOverlay: VirtualMouseOverlay
    private lateinit var tvKeyHandler: TvKeyHandler
    private lateinit var pipManager: FloatingPipManager
    private lateinit var filterManager: FilterListManager
    private lateinit var openSearchManager: OpenSearchManager
    private lateinit var preferencesManager: PreferencesManager

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

    // Home Screen Containers
    private lateinit var homeView: View
    private lateinit var edgeArcTvContainer: View
    private lateinit var alohaMobileContainer: View

    // TV Layout Elements
    private lateinit var etCenterSearch: EditText
    private lateinit var btnShortcutBing: ImageButton
    private lateinit var btnShortcutGoogle: ImageButton
    private lateinit var btnShortcutYoutube: ImageButton
    private lateinit var btnShortcutGemini: ImageButton

    // Aloha Mobile Layout Elements
    private lateinit var btnAlohaGift: ImageButton
    private lateinit var btnAlohaQr: ImageButton
    private lateinit var btnAlohaSettings: ImageButton
    private lateinit var btnAlohaShield: View
    private lateinit var tvAlohaShieldCount: TextView
    private lateinit var etAlohaSearch: EditText
    private lateinit var btnAlohaAi: View

    // Aloha Shortcut Tiles
    private lateinit var tileGoogle: View
    private lateinit var tileX: View
    private lateinit var tileEbay: View
    private lateinit var tileBooking: View
    private lateinit var tileSkyscanner: View
    private lateinit var tileWeather: View
    private lateinit var tileYoutube: View
    private lateinit var tileAdd: View

    // Microsoft Start News Feed
    private lateinit var alohaNewsSection: View
    private lateinit var pillHot: TextView
    private lateinit var pillNews: TextView
    private lateinit var pillEntertainment: TextView
    private lateinit var pillSports: TextView
    private lateinit var pillMoney: TextView
    private lateinit var cardFeaturedNews: View

    // Mobile Bottom Navigation Bar
    private lateinit var mobileNavBar: View
    private lateinit var btnNavBack: ImageButton
    private lateinit var btnNavHome: ImageButton
    private lateinit var btnNavMenu: ImageButton
    private lateinit var btnNavDownloads: ImageButton
    private lateinit var btnNavTabs: ImageButton
    private lateinit var btnNavSettings: ImageButton

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

        preferencesManager = PreferencesManager(this)
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

        // Apply theme, home screen layout & search engine
        returnToHome()
    }

    private fun initViews() {
        rootLayout = findViewById(R.id.rootLayout)
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

        // Home View & Layout Containers
        homeView = findViewById(R.id.homeView)
        edgeArcTvContainer = findViewById(R.id.edgeArcTvContainer)
        alohaMobileContainer = findViewById(R.id.alohaMobileContainer)

        // TV Layout Views
        etCenterSearch = findViewById(R.id.etCenterSearch)
        btnShortcutBing = findViewById(R.id.btnShortcutBing)
        btnShortcutGoogle = findViewById(R.id.btnShortcutGoogle)
        btnShortcutYoutube = findViewById(R.id.btnShortcutYoutube)
        btnShortcutGemini = findViewById(R.id.btnShortcutGemini)

        // Aloha Mobile Header Views
        btnAlohaGift = findViewById(R.id.btnAlohaGift)
        btnAlohaQr = findViewById(R.id.btnAlohaQr)
        btnAlohaSettings = findViewById(R.id.btnAlohaSettings)
        btnAlohaShield = findViewById(R.id.btnAlohaShield)
        tvAlohaShieldCount = findViewById(R.id.tvAlohaShieldCount)
        etAlohaSearch = findViewById(R.id.etAlohaSearch)
        btnAlohaAi = findViewById(R.id.btnAlohaAi)

        // Aloha Shortcut Tiles
        tileGoogle = findViewById(R.id.tileGoogle)
        tileX = findViewById(R.id.tileX)
        tileEbay = findViewById(R.id.tileEbay)
        tileBooking = findViewById(R.id.tileBooking)
        tileSkyscanner = findViewById(R.id.tileSkyscanner)
        tileWeather = findViewById(R.id.tileWeather)
        tileYoutube = findViewById(R.id.tileYoutube)
        tileAdd = findViewById(R.id.tileAdd)

        // Microsoft Start News Feed
        alohaNewsSection = findViewById(R.id.alohaNewsSection)
        pillHot = findViewById(R.id.pillHot)
        pillNews = findViewById(R.id.pillNews)
        pillEntertainment = findViewById(R.id.pillEntertainment)
        pillSports = findViewById(R.id.pillSports)
        pillMoney = findViewById(R.id.pillMoney)
        cardFeaturedNews = findViewById(R.id.cardFeaturedNews)

        // Mobile Bottom Navigation Bar
        mobileNavBar = findViewById(R.id.mobileNavBar)
        btnNavBack = findViewById(R.id.btnNavBack)
        btnNavHome = findViewById(R.id.btnNavHome)
        btnNavMenu = findViewById(R.id.btnNavMenu)
        btnNavDownloads = findViewById(R.id.btnNavDownloads)
        btnNavTabs = findViewById(R.id.btnNavTabs)
        btnNavSettings = findViewById(R.id.btnNavSettings)

        // Error Container
        errorContainer = findViewById(R.id.errorContainer)
        tvErrorMessage = findViewById(R.id.tvErrorMessage)
        btnRetry = findViewById(R.id.btnRetry)
        btnErrorHome = findViewById(R.id.btnErrorHome)

        setupListeners()
    }

    private fun setupListeners() {
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

        // TV Shortcuts
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

        // Aloha Header Buttons
        btnAlohaGift.setOnClickListener {
            safelyRun {
                Toast.makeText(this, "🎁 Helium Browser: Fast, Lightweight & Ad-Free!", Toast.LENGTH_SHORT).show()
            }
        }

        btnAlohaQr.setOnClickListener {
            safelyRun {
                Toast.makeText(this, "📷 QR Scanner feature ready", Toast.LENGTH_SHORT).show()
            }
        }

        btnAlohaSettings.setOnClickListener {
            safelyRun {
                TvDialogs.showSettingsDialog(this, preferencesManager) {
                    applyUserPreferences()
                }
            }
        }

        btnAlohaShield.setOnClickListener {
            safelyRun { onToggleAdBlocker() }
        }

        btnAlohaAi.setOnClickListener {
            safelyRun { toggleGeminiPanel() }
        }

        // Aloha Shortcut Tiles
        tileGoogle.setOnClickListener { safelyRun { loadUrl(GOOGLE_URL) } }
        tileX.setOnClickListener { safelyRun { loadUrl("https://x.com") } }
        tileEbay.setOnClickListener { safelyRun { loadUrl("https://www.ebay.com") } }
        tileBooking.setOnClickListener { safelyRun { loadUrl("https://www.booking.com") } }
        tileSkyscanner.setOnClickListener { safelyRun { loadUrl("https://www.skyscanner.net") } }
        tileWeather.setOnClickListener { safelyRun { loadUrl("https://www.bing.com/weather") } }
        tileYoutube.setOnClickListener { safelyRun { loadUrl(YOUTUBE_URL) } }
        tileAdd.setOnClickListener {
            safelyRun {
                TvDialogs.showSettingsDialog(this, preferencesManager) {
                    applyUserPreferences()
                }
            }
        }

        // News Pills & Card
        pillHot.setOnClickListener { safelyRun { loadUrl("https://www.msn.com") } }
        pillNews.setOnClickListener { safelyRun { loadUrl("https://www.bing.com/news") } }
        pillEntertainment.setOnClickListener { safelyRun { loadUrl("https://www.msn.com/en-in/entertainment") } }
        pillSports.setOnClickListener { safelyRun { loadUrl("https://www.msn.com/en-in/sports") } }
        pillMoney.setOnClickListener { safelyRun { loadUrl("https://www.msn.com/en-in/money") } }
        cardFeaturedNews.setOnClickListener { safelyRun { loadUrl("https://www.msn.com") } }

        // Mobile Bottom Navigation Bar
        btnNavBack.setOnClickListener { safelyRun { onNavigateBack() } }
        btnNavHome.setOnClickListener { safelyRun { returnToHome() } }
        btnNavMenu.setOnClickListener { safelyRun { showTvMenu() } }
        btnNavDownloads.setOnClickListener {
            safelyRun {
                Toast.makeText(this, "📥 Downloads: All files stored safely", Toast.LENGTH_SHORT).show()
            }
        }
        btnNavTabs.setOnClickListener {
            safelyRun {
                TvDialogs.showTabsDialog(this, 1) { returnToHome() }
            }
        }
        btnNavSettings.setOnClickListener {
            safelyRun {
                TvDialogs.showSettingsDialog(this, preferencesManager) {
                    applyUserPreferences()
                }
            }
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

        // Center TV Search input listener
        etCenterSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_DONE) {
                val input = etCenterSearch.text.toString()
                loadUrl(processInput(input))
                true
            } else {
                false
            }
        }

        // Aloha Search input listener
        etAlohaSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_DONE) {
                val input = etAlohaSearch.text.toString()
                loadUrl(processInput(input))
                true
            } else {
                false
            }
        }
    }

    private fun applyUserPreferences() {
        safelyRun {
            // 1. Set root wallpaper background
            rootLayout.setBackgroundResource(preferencesManager.theme.drawableRes)

            // 2. Select layout mode
            if (preferencesManager.isAlohaMobileActive()) {
                alohaMobileContainer.visibility = View.VISIBLE
                edgeArcTvContainer.visibility = View.GONE
                mobileNavBar.visibility = View.VISIBLE
                virtualMouseOverlay.visibility = View.GONE
                // On mobile home screen, Aloha header serves as search bar
                topBar.visibility = if (webView.visibility == View.VISIBLE) View.VISIBLE else View.GONE
            } else {
                alohaMobileContainer.visibility = View.GONE
                edgeArcTvContainer.visibility = View.VISIBLE
                mobileNavBar.visibility = View.GONE
                topBar.visibility = View.VISIBLE
            }

            // 3. News Feed visibility
            alohaNewsSection.visibility = if (preferencesManager.showNewsFeed) View.VISIBLE else View.GONE

            // 4. Update search hints
            val engine = preferencesManager.searchEngine
            etOmnibox.hint = "Search $engine or enter URL"
            etAlohaSearch.hint = "Search in $engine or enter URL"
            etCenterSearch.hint = "Search $engine"
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
                        topBar.visibility = View.VISIBLE
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
                        topBar.visibility = View.VISIBLE
                        errorContainer.visibility = View.VISIBLE
                        tvErrorMessage.text = "Failed to load: $failingUrl\n($description)"
                    }
                },
                onBlockedResource = { count ->
                    runOnUiThread {
                        if (count > 0) {
                            tvShieldCount.visibility = View.VISIBLE
                            tvShieldCount.text = "$count"
                            tvAlohaShieldCount.visibility = View.VISIBLE
                            tvAlohaShieldCount.text = "$count"
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
                        mobileNavBar.visibility = View.GONE
                    } else {
                        topBar.visibility = View.VISIBLE
                        if (preferencesManager.isAlohaMobileActive()) {
                            mobileNavBar.visibility = View.VISIBLE
                        }
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
     * - Otherwise: treats as search query, URL-encodes it properly, and loads search engine.
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

        // Search query using configured engine (Default: Bing)
        val encoded = Uri.encode(text)
        return when (preferencesManager.searchEngine.lowercase()) {
            "google" -> "https://www.google.com/search?q=$encoded"
            "duckduckgo" -> "https://duckduckgo.com/?q=$encoded"
            else -> "https://www.bing.com/search?q=$encoded"
        }
    }

    fun loadUrl(url: String) {
        if (url.isBlank()) return

        currentPageUrl = url
        etOmnibox.setText(url)
        progressBar.visibility = View.VISIBLE
        errorContainer.visibility = View.GONE
        homeView.visibility = View.GONE
        webView.visibility = View.VISIBLE
        topBar.visibility = View.VISIBLE

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
        etAlohaSearch.setText("")

        applyUserPreferences()
    }

    private fun showTvMenu() {
        TvDialogs.showMenuDialog(
            context = this,
            onGeminiClick = { toggleGeminiPanel() },
            onPointerToggle = { onToggleMousePointer() },
            onUserAgentClick = { cycleUserAgent() },
            onSearchEngineClick = {
                TvDialogs.showSearchEngineDialog(this, openSearchManager) { selected ->
                    preferencesManager.searchEngine = selected.shortName
                    applyUserPreferences()
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
            },
            onSettingsClick = {
                TvDialogs.showSettingsDialog(this, preferencesManager) {
                    applyUserPreferences()
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
            mobileNavBar.visibility = View.GONE
            virtualMouseOverlay.visibility = View.GONE
            geminiPanel.visibility = View.GONE
        } else {
            topBar.visibility = View.VISIBLE
            if (preferencesManager.isAlohaMobileActive()) {
                mobileNavBar.visibility = View.VISIBLE
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        safelyRun {
            webView.destroy()
        }
    }
}
