package org.helium.tv.browser.ui

import org.helium.tv.browser.R

data class SpeedDialItem(
    val title: String,
    val url: String,
    val iconResId: Int? = null,
    val letter: String? = null,
    val displayUrl: String = url,
    val isPinned: Boolean = true
)

object DefaultBookmarks {
    val ITEMS = listOf(
        SpeedDialItem(
            title = "YouTube",
            url = "https://www.youtube.com",
            iconResId = R.drawable.ic_youtube,
            displayUrl = "https://w...",
            isPinned = true
        ),
        SpeedDialItem(
            title = "Gemini",
            url = "https://gemini.google.com",
            iconResId = R.drawable.ic_gemini_sparkle,
            displayUrl = "https://ge...",
            isPinned = true
        ),
        SpeedDialItem(
            title = "Search",
            url = "https://www.google.com",
            letter = "S",
            displayUrl = "https://w...",
            isPinned = true
        ),
        SpeedDialItem(
            title = "Tools",
            url = "https://zs.org",
            letter = "Z",
            displayUrl = "https://zs...",
            isPinned = true
        ),
        SpeedDialItem(
            title = "App Downloads",
            url = "https://addons.mozilla.org/firefox/extensions/",
            letter = "📦",
            displayUrl = "App d...",
            isPinned = false
        )
    )
}
