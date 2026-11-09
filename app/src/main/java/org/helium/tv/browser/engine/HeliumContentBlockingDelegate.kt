package org.helium.tv.browser.engine

import android.net.Uri
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.helium.tv.browser.adblock.RustAdBlocker
import org.mozilla.geckoview.ContentBlocking
import org.mozilla.geckoview.GeckoSession

class HeliumContentBlockingDelegate(
    private val coroutineScope: CoroutineScope,
    private val onBlockedEvent: ((count: Int, url: String) -> Unit)? = null
) : ContentBlocking.Delegate {

    companion object {
        private const val TAG = "HeliumContentBlocking"
    }

    override fun onContentBlocked(session: GeckoSession, event: ContentBlocking.BlockEvent) {
        val uri = event.uri
        Log.d(TAG, "Content blocked by GeckoView: $uri")

        val currentCount = RustAdBlocker.getBlockedCount()
        onBlockedEvent?.invoke(currentCount, uri)
    }

    fun injectCosmeticFilters(session: GeckoSession, url: String) {
        coroutineScope.launch(Dispatchers.Default) {
            try {
                val uri = Uri.parse(url)
                val host = uri.host ?: return@launch
                val script = RustAdBlocker.getCosmeticScriptForDomain(host)
                if (script.isNotEmpty()) {
                    Log.d(TAG, "Cosmetic filters ready for host: $host")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed injecting cosmetic filters for $url", e)
            }
        }
    }
}
