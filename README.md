# 💎 Renderium TV & Mobile Browser

Ek ultra-lightweight, lightning-fast aur world-class secure **Android TV & Mobile Web Browser** jo **Kotlin + Native Chromium + Rust Core** par tayyar kiya gaya hai. Isme full **Chrome Web Store Extensions support**, **Multi-Tab Browsing Engine**, **Apple Liquid Glass Frosted Glassmorphism UI**, high-performance **Rust Native Adblocker**, **Chrome-Style Glass Menu**, aur **World-Class Security Suite** shamil hain.

---

## 🚀 Key Features

1. **📑 Multi-Tab Browsing Engine**
   - Independent tabs system: Open multiple tabs, switch smoothly, close tabs, or open Private Incognito tabs.
   - Interactive Tabs Switcher dialog with live tab preview, active badge, and 1-tap close.
   - Real-time tab counter badge in top bar and mobile navigation bar.

2. **✨ Modern Apple Liquid Glass UI & Chrome Menu**
   - Frosted sapphire glass surfaces (`#A6141620`), thin reflective specular borders (`#33FFFFFF`), and electric cyan focus glow (`#00E5FF`).
   - 150ms spring/ease-out animations for fluid touch and TV D-Pad navigation without frame drops.
   - Sleek Chrome-style popup menu with quick navigation buttons, Desktop site toggle with clear ON/OFF status, and 1-tap search switcher.

3. **🔍 Dynamic Search Engine Switcher & Home Screen**
   - Instant switching between **Bing**, **Google**, and **DuckDuckGo**.
   - Dynamic Home screen logo & hint matching: Displays Google logo & hints when Google is active, and Microsoft Bing logo & hints when Bing is active.
   - Smart URL & search processing: automatic HTTPS upgrade, domain resolution, and query encoding.

4. **🛡️ World-Class Security Suite ("World ka sabse secure browser")**
   - **Google SafeBrowsing API**: Automated interception of phishing, malware, and harmful sites.
   - **Third-Party Cookie Isolation**: Strictly blocks cross-site tracking cookies.
   - **Local Filesystem Sandbox Lockdown**: File scheme access (`file:///`) disabled to prevent local data exfiltration.
   - **HTTPS Everywhere**: Upgrades insecure HTTP requests to encrypted HTTPS.
   - **Anti-Clickjacking Shield**: Blocks malicious unauthorized pop-up windows.

5. **🧩 Chrome Web Store Extensions Support**
   - Auto-detects Chrome Web Store extension pages and `.crx` downloads.
   - In-page Liquid Glass "Add to Renderium" prompt and English installation confirmation dialog.
   - Sandboxed extraction into app private storage with automatic content script injection.

6. **🦀 Rust Native Adblock Core (`renderium-adblock`)**
   - Pure Rust ultra-fast rule matching engine (`app/src/main/rust`).
   - EasyList, uBlock filters, and AdGuard syntax compatible.
   - Cosmetic element hiding and real-time blocked counter.

7. **📱 Dual Layouts: Aloha Mobile + TV Edge Arc**
   - **Mobile Mode**: Aloha-inspired layout with quick shortcut tiles, Microsoft Start news feed, and bottom navigation bar.
   - **TV Mode**: 10-foot Leanback layout with Virtual Mouse Pointer (D-Pad physics acceleration) and centered search stadium pill.

---

## 📂 Project Architecture

```
renderium/
├── app/
│   ├── src/main/java/org/helium/tv/browser/
│   │   ├── MainActivity.kt               # Central coordinator & multi-tab navigation
│   │   ├── tabs/TabManager.kt            # Independent tab lifecycle & state management
│   │   ├── ui/TvDialogs.kt               # Chrome-style Liquid Glass dialogs & menus
│   │   ├── ui/LiquidGlassUtils.kt        # 150ms spring physics & glass styling
│   │   ├── security/RenderiumSecurityManager.kt # World-class security & sandbox enforcement
│   │   ├── extensions/ExtensionManager.kt # Chrome Web Store CRX installer & injector
│   │   ├── adblock/                      # Rust JNI adblock filter bridges
│   │   ├── engine/                       # Hardened Chromium WebView & WebChrome clients
│   │   └── settings/                     # Wallpapers, layout & search engine preferences
│   ├── src/main/rust/                    # Native Rust adblock & rule matcher engine
│   └── src/main/res/                     # Liquid glass drawables, vector icons & layouts
└── .github/workflows/build.yml           # Automated multi-architecture APK builder
```

---

## 📦 APK Artifacts & Download

Automated builds produce ultra-compact APKs (< 4 MB raw, ~2.3 MB zipped):
- **`HeliumTV-arm64-v8a.apk`**: 64-bit Android phones, tablets, and modern 4K Android TVs.
- **`HeliumTV-armeabi-v7a.apk`**: 32-bit Android TVs and legacy devices.
