package org.helium.tv.browser.ui

data class SpeedDialItem(
    val title: String,
    val url: String,
    val category: String,
    val emoji: String = "🌐"
)

object DefaultBookmarks {
    val ITEMS = listOf(
        SpeedDialItem("Microsoft Bing", "https://www.bing.com", "Search Engine", "🔍"),
        SpeedDialItem("YouTube TV", "https://tv.youtube.com", "4K Video", "📺"),
        SpeedDialItem("YouTube Web", "https://www.youtube.com", "Video & Music", "▶️"),
        SpeedDialItem("Twitch TV", "https://www.twitch.tv", "Live Streams", "🎮"),
        SpeedDialItem("Reddit", "https://www.reddit.com", "Community", "💬"),
        SpeedDialItem("Wikipedia", "https://www.wikipedia.org", "Knowledge", "📚"),
        SpeedDialItem("Firefox Add-ons", "https://addons.mozilla.org/firefox/extensions/", "Web Extensions", "🧩")
    )
}
