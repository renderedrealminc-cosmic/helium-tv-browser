package org.helium.tv.browser.ui

data class SpeedDialItem(
    val title: String,
    val url: String,
    val category: String,
    val iconResId: Int = 0
)

object DefaultBookmarks {
    val ITEMS = listOf(
        SpeedDialItem("YouTube TV", "https://tv.youtube.com", "Video"),
        SpeedDialItem("YouTube Desktop", "https://www.youtube.com", "Video"),
        SpeedDialItem("Twitch TV", "https://www.twitch.tv", "Live Streaming"),
        SpeedDialItem("Reddit", "https://www.reddit.com", "Social"),
        SpeedDialItem("GitHub", "https://github.com", "Dev"),
        SpeedDialItem("Wikipedia", "https://www.wikipedia.org", "Knowledge"),
        SpeedDialItem("Firefox Add-ons", "https://addons.mozilla.org/firefox/extensions/", "Store")
    )
}
