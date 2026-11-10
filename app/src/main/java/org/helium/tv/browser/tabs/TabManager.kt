package org.helium.tv.browser.tabs

import android.webkit.WebView

data class RenderiumTab(
    val id: String,
    var title: String = "New Tab",
    var url: String = "",
    val isIncognito: Boolean = false,
    val webView: WebView
)

class TabManager {

    private val tabsList = mutableListOf<RenderiumTab>()
    var activeTabId: String? = null
        private set

    val tabs: List<RenderiumTab>
        get() = tabsList.toList()

    val count: Int
        get() = tabsList.size

    fun addTab(tab: RenderiumTab, makeActive: Boolean = true) {
        tabsList.add(tab)
        if (makeActive || activeTabId == null) {
            activeTabId = tab.id
        }
    }

    fun getActiveTab(): RenderiumTab? {
        return tabsList.firstOrNull { it.id == activeTabId } ?: tabsList.firstOrNull()
    }

    fun getTab(id: String): RenderiumTab? {
        return tabsList.firstOrNull { it.id == id }
    }

    fun selectTab(id: String): RenderiumTab? {
        val target = tabsList.firstOrNull { it.id == id }
        if (target != null) {
            activeTabId = target.id
        }
        return target
    }

    fun removeTab(id: String): RenderiumTab? {
        val index = tabsList.indexOfFirst { it.id == id }
        if (index == -1) return null

        val removed = tabsList.removeAt(index)
        if (activeTabId == id) {
            activeTabId = if (tabsList.isNotEmpty()) {
                val newIndex = index.coerceAtMost(tabsList.size - 1)
                tabsList[newIndex].id
            } else {
                null
            }
        }
        return removed
    }

    fun clearAll(): List<RenderiumTab> {
        val all = tabsList.toList()
        tabsList.clear()
        activeTabId = null
        return all
    }
}
