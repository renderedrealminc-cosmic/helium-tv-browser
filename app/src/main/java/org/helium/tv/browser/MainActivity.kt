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
import org.helium.tv.browser.ai.GeminiTvAssistant
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
    private lateinit var btnGeminiAi: Button
    private lateinit var speedDialRecyclerView: RecyclerView
    private lateinit var speedDialContainer: View

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
        toolbarContainer = findViewById(R.id.toolbarContainer)
        urlEditText = findViewById(R.id.urlEditText)
        progressBar = findViewById(R.id.progressBar)
        btnMouseToggle = findViewById(R.id.btnMouseToggle)
        btnExtensions = findViewById(R.id.btnExtensions)
        btnAdblockStats = findViewById(R.id.btnAdblockStats)
        btnUserAgent = findViewById(R.id.btnUserAgent)
        btnOpacity = findViewById(R.id.btnOpacity)
        btnGeminiAi = findViewById(R.id.btnGeminiAi)
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
        btnGeminiAi.setOnClickListener { toggleGeminiPanel() }
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
                    currentPageUrl = url
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
                btnAdblockStats.text = "🛡️ $count"
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
        // Microsoft Bing as default search engine
        val url = when {
            rawInput.startsWith("http://") || rawInput.startsWith("https://") -> rawInput
            rawInput.contains(".") && !rawInput.contains(" ") -> "https://$rawInput"
            else -> "https://www.bing.com/search?q=${android.net.Uri.encode(rawInput)}"
        }
        urlEditText.setText(url)
        currentPageUrl = url
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
        btnMouseToggle.text = if (active) "🎯 Pointer: ON" else "🎯 Focus Mode"
        Toast.makeText(
            this,
            if (active) "Neon Pointer Activated" else "D-Pad Focus Mode Activated",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onOpenExtensionManager() {
        TvDialogs.showExtensionManagerDialog(this, engineManager.extensionManager, lifecycleScope)
    }

    override fun onToggleAdBlocker() {
        val count = RustAdBlocker.getBlockedCount()
        Toast.makeText(this, "Rust AdBlocker: $count ads & trackers blocked", Toast.LENGTH_SHORT).show()
    }

    override fun onTogglePipMode() {
        pipManager.enterSystemPip()
    }

    override fun onOpenMenu() {
        toolbarContainer.visibility = if (toolbarContainer.visibility == View.VISIBLE) View.GONE else View.VISIBLE
    }

    override fun onNavigateBack(): Boolean {
        if (geminiPanel.visibility == View.VISIBLE) {
            geminiPanel.visibility = View.GONE
            return true
        }
        if (toolbarContainer.visibility == View.VISIBLE) {
            toolbarContainer.visibility = View.GONE
            return true
        }
        if (speedDialContainer.visibility == View.GONE) {
            geckoSession.goBack()
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
            toolbarContainer.visibility = View.GONE
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
