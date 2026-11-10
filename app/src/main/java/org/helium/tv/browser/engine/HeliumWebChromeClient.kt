package org.helium.tv.browser.engine

import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView

class HeliumWebChromeClient(
    private val onProgressChanged: (progress: Int) -> Unit,
    private val onTitleChanged: (title: String) -> Unit,
    private val onCustomViewToggled: (view: View?, callback: CustomViewCallback?) -> Unit
) : WebChromeClient() {

    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        onProgressChanged(newProgress)
    }

    override fun onReceivedTitle(view: WebView?, title: String?) {
        super.onReceivedTitle(view, title)
        if (!title.isNullOrEmpty()) {
            onTitleChanged(title)
        }
    }

    override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
        super.onShowCustomView(view, callback)
        onCustomViewToggled(view, callback)
    }

    override fun onHideCustomView() {
        super.onHideCustomView()
        onCustomViewToggled(null, null)
    }
}
