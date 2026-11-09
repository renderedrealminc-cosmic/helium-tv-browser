package org.helium.tv.browser.utils

enum class UserAgentMode {
    TV,
    DESKTOP,
    MOBILE
}

object UserAgentUtils {
    // Android TV User-Agent
    const val UA_TV = "Mozilla/5.0 (Linux; Android 12; Android TV Build/STT2.220908.001; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/120.0.6099.144 Mobile Safari/537.36 LargeScreen HeliumTV/1.0"

    // Modern High-Performance Desktop User-Agent (Forces full desktop websites with 4K/1080p layouts)
    const val UA_DESKTOP = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"

    // Standard Mobile User-Agent
    const val UA_MOBILE = "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.6261.90 Mobile Safari/537.36"

    fun getUserAgent(mode: UserAgentMode): String {
        return when (mode) {
            UserAgentMode.TV -> UA_TV
            UserAgentMode.DESKTOP -> UA_DESKTOP
            UserAgentMode.MOBILE -> UA_MOBILE
        }
    }
}
