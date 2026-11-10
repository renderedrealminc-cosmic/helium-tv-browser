package org.helium.tv.browser

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.speech.RecognizerIntent
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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
    private lateinit var openSearchManager: OpenSearchManager

    // Home Screen Views (Light Minimalist Material 3 Theme)
    private lateinit var homeScreenContainer: View
    private lateinit var btnHome: ImageButton
    private lateinit var ivHeaderLogo: ImageView
    private lateinit var btnShieldLayout: View
    private lateinit var tvShieldCount: TextView
    private lateinit var btnTabs: View
    private lateinit var tvTabCount: TextView
    private lateinit var btnMenu: ImageButton
    private lateinit var etHomeSearch: EditText
    private lateinit var btnVoiceSearch: ImageButton
    private lateinit var speedDialRecyclerView: RecyclerView

    // Web Browsing Top Bar (Active when web page is loaded)
    private lateinit var browserTopBar: View
    private lateinit var btnBrowserHome: ImageButton
    private lateinit var etBrowserUrl: EditText
    private lateinit var btnBrowserShieldLayout: View
    private lateinit var btnBrowserTabs: View
    private lateinit var tvBrowserTabCount: TextView
    private lateinit var btnBrowserMenu: ImageButton
    private lateinit var progressBar: ProgressBar

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
        initSpeedDial()

        filterManager = FilterListManager(this)
        lifecycleScope.launch {
            filterManager.initializeFilters()
        }
    }

    private fun initViews() {
        geckoView = findViewById(R.id.geckoView)
        virtualMouseOverlay = findViewById(R.id.virtualMouseOverlay)
        pipManager = FloatingPipManager(this)

        // Home screen container & elements
        homeScreenContainer = findViewById(R.id.homeScreenContainer)
        btnHome = findViewById(R.id.btnHome)
        ivHeaderLogo = findViewById(R.id.ivHeaderLogo)
        btnShieldLayout = findViewById(R.id.btnShieldLayout)
        tvShieldCount = findViewById(R.id.tvShieldCount)
        btnTabs = findViewById(R.id.btnTabs)
        tvTabCount = findViewById(R.id.tvTabCount)
        btnMenu = findViewById(R.id.btnMenu)
        etHomeSearch = findViewById(R.id.etHomeSearch)
        btnVoiceSearch = findViewById(R.id.btnVoiceSearch)
        speedDialRecyclerView = findViewById(R.id.speedDialRecyclerView)

        // Browser top bar elements
        browserTopBar = findViewById(R.id.browserTopBar)
        btnBrowserHome = findViewById(R.id.btnBrowserHome)
        etBrowserUrl = findViewById(R.id.etBrowserUrl)
        btnBrowserShieldLayout = findViewById(R.id.btnBrowserShieldLayout)
        btnBrowserTabs = findViewById(R.id.btnBrowserTabs)
        tvBrowserTabCount = findViewById(R.id.tvBrowserTabCount)
        btnBrowserMenu = findViewById(R.id.btnBrowserMenu)
        progressBar = findViewById(R.id.progressBar)

        // Home Top Bar listeners
        btnHome.setOnClickListener { returnToHome() }
        btnShieldLayout.setOnClickListener { onToggleAdBlocker() }
        btnTabs.setOnClickListener { showTabsDialog() }
        btnMenu.setOnClickListener { showTvMenu() }

        // Browser Top Bar listeners
        btnBrowserHome.setOnClickListener { returnToHome() }
        btnBrowserShieldLayout.setOnClickListener { onToggleAdBlocker() }
        btnBrowserTabs.setOnClickListener { showTabsDialog() }
        btnBrowserMenu.setOnClickListener { showTvMenu() }

        // Home search listener
        etHomeSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_DONE) {
                val input = etHomeSearch.text.toString().trim()
                if (input.isNotEmpty()) {
                    loadUrl(input)
                }
                true
            } else {
                false
            }
        }

        // Browser URL listener
        etBrowserUrl.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                val input = etBrowserUrl.text.toString().trim()
                if (input.isNotEmpty()) {
                    loadUrl(input)
                }
                true
            } else {
                false
            }
        }

        btnVoiceSearch.setOnClickListener { launchVoiceSearch() }
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
            runGeminiQuery(
                "Is web page ke mukhya points ko aasan 3-4 bullet points me summarize karo.",
                "Title: $currentPageTitle | URL: $currentPageUrl"
            )
        }

        btnQuickHindi.setOnClickListener {
            runGeminiQuery(
                "Is website ke baare me simple Hindi me explain karo ki ye page kis baare me hai.",
                "Title: $currentPageTitle | URL: $currentPageUrl"
            )
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
            val reply = GeminiTvAssistant.queryGemini(prompt, context)
            tvGeminiResponse.text = reply
        }
    }

    private fun initBrowserEngine() {
        engineManager = GeckoEngineManager(this, lifecycleScope)
        geckoSession = engineManager.createSession()

        val navDelegate = HeliumNavigationDelegate(
            context = this,
            onPageStarted = { url ->
                runOnUiThread {
                    // CRITICAL FIX: Only hide home screen if loading a REAL URL!
                    // geckoSession.open fires on "about:blank" on startup - never hide home screen on blank!
                    if (url.isNotEmpty() && url != "about:blank" && !url.startsWith("about:")) {
                        currentPageUrl = url
                        etBrowserUrl.setText(url)
                        progressBar.visibility = View.VISIBLE
                        homeScreenContainer.visibility = View.GONE
                        browserTopBar.visibility = View.VISIBLE
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
        if (rawInput.isBlank()) return

        val url = when {
            rawInput.startsWith("http://") || rawInput.startsWith("https://") -> rawInput
            rawInput.contains(".") && !rawInput.contains(" ") -> "https://$rawInput"
            else -> openSearchManager.buildSearchUrl(rawInput)
        }

        currentPageUrl = url
        etBrowserUrl.setText(url)
        homeScreenContainer.visibility = View.GONE
        browserTopBar.visibility = View.VISIBLE
        progressBar.visibility = View.VISIBLE

        geckoSession.loadUri(url)
    }

    private fun returnToHome() {
        homeScreenContainer.visibility = View.VISIBLE
        browserTopBar.visibility = View.GONE
        progressBar.visibility = View.GONE
        currentPageUrl = ""
        etHomeSearch.setText("")
        geckoSession.loadUri("about:blank")
        etHomeSearch.requestFocus()
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
                    etHomeSearch.hint = "Search ${selected.shortName} or type URL"
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

    private fun showTabsDialog() {
        TvDialogs.showTabsDialog(this, 1) {
            returnToHome()
        }
    }

    private fun launchVoiceSearch() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Search Google or say a website URL...")
            }
            startActivityForResult(intent, 1001)
        } catch (e: Exception) {
            Toast.makeText(this, "Voice search is not available on this device", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001 && resultCode == RESULT_OK && data != null) {
            val results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!results.isNullOrEmpty()) {
                val query = results[0]
                etHomeSearch.setText(query)
                loadUrl(query)
            }
        }
    }

    private fun cycleUserAgent() {
        currentUaMode = when (currentUaMode) {
            UserAgentMode.TV -> UserAgentMode.DESKTOP
            UserAgentMode.DESKTOP -> UserAgentMode.MOBILE
            UserAgentMode.MOBILE -> UserAgentMode.TV
        }
        engineManager.setUserAgentMode(geckoSession, currentUaMode)
        geckoSession.reload()
        Toast.makeText(this, "Switched to ${currentUaMode.name} User-Agent", Toast.LENGTH_SHORT).show()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            if (tvKeyHandler.onKeyDown(event.keyCode, event)) return true
        } else if (event.action == KeyEvent.ACTION_UP) {
            if (tvKeyHandler.onKeyUp(event.keyCode, event)) return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onToggleMousePointer() {
        val active = virtualMouseOverlay.toggleMousePointer()
        Toast.makeText(
            this,
            if (active) "🎯 Neon Pointer Activated" else "🎮 D-Pad Focus Mode Activated",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onOpenExtensionManager() {
        TvDialogs.showExtensionManagerDialog(this, engineManager.extensionManager, lifecycleScope)
    }

    override fun onToggleAdBlocker() {
        val count = RustAdBlocker.getBlockedCount()
        Toast.makeText(this, "🛡️ Rust AdBlocker: $count ads & trackers blocked", Toast.LENGTH_SHORT).show()
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
        if (homeScreenContainer.visibility == View.GONE) {
            returnToHome()
            return true
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
            browserTopBar.visibility = View.GONE
            virtualMouseOverlay.visibility = View.GONE
            geminiPanel.visibility = View.GONE
        } else {
            virtualMouseOverlay.visibility = View.VISIBLE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        geckoSession.close()
    }
}
