# 🎈 Helium TV Browser (Rust + Kotlin)

Ek tagada aur powerful **Android TV Web Browser** jo **Kotlin + Rust + Mozilla GeckoView** par tayyar kiya gaya hai. Isme full **WebExtensions support (.xpi / Firefox Addons)**, high-performance **Rust Native Adblocker**, **Hybrid Virtual Mouse Pointer**, aur Helium browser ke signature **Floating Picture-in-Picture & Translucency** features shamil hain.

---

## 🚀 Key Features

1. **🧩 Full WebExtensions Support (GeckoView)**
   - Mozilla GeckoView web engine ke zariye real desktop/mobile browser extensions support.
   - 1-Click Install for TV-optimized extensions:
     - **uBlock Origin**: Ultimate ad and tracker blocker.
     - **SponsorBlock**: YouTube videos me sponsorships, intros aur outros skip karta hai.
     - **Dark Reader**: Big TV screens par eye strain kam karne ke liye dark mode.
     - **Violentmonkey / Tampermonkey**: Userscript support.
   - Kisi bhi `.xpi` file ya direct Firefox Add-on store URL se extensions install karne ki sahulat.

2. **🦀 Rust Native Adblock & Anti-Tracker Core (`helium-adblock`)**
   - Pure Rust me likha gaya ultra-fast rule matching engine (`app/src/main/rust`).
   - EasyList, uBlock filters, aur AdGuard syntax compatible.
   - Cosmetic element hiding (blank spaces aur ad containers ko CSS ke zariye hide karta hai).
   - JNI bindings ke zariye real-time request classification aur blocking stats.

3. **🖱️ Hybrid TV Remote & Virtual Mouse Pointer**
   - **Smooth Acceleration Physics**: TV Remote ke D-Pad (Up, Down, Left, Right) se pixel-precise arrow cursor move hota hai.
   - **Click & Drag**: `DPAD_CENTER` (OK button) press karne par touch/click events dispatch hote hain.
   - **Fast Scrolling**: TV remote ke `CHANNEL UP / DOWN` buttons se web pages smooth scroll hote hain.
   - **Dual Mode**: Remote ke Green button se Virtual Mouse aur Native D-Pad Focus mode ke darmiyan instant switch.

4. **🪟 Helium Signature Features (Floating PiP & Opacity)**
   - **System Picture-in-Picture**: Video chalte waqt TV remote ke Blue button se instant PiP mode.
   - **Translucency / Window Opacity**: Browser ki transparency (100%, 80%, 60%, 40%) adjust karke peeche chalne wali TV screen ya video ko dekhna.

5. **📺 TV Speed Dial & User-Agent Switcher**
   - 10-foot Leanback UI: YouTube TV, Twitch, Reddit, Wikipedia, aur custom bookmarks ke cards.
   - **User-Agent Profiles**: TV (Leanback), Desktop (1080p/4K Windows Chrome layout), aur Mobile modes ke darmiyan 1-tap switch.

---

## 🎮 TV Remote Controls & Shortcuts

| TV Remote Button | Functionality |
| :--- | :--- |
| **D-Pad (Arrows)** | Virtual Mouse Pointer ko smoothly move karta hai (acceleration ke sath) |
| **D-Pad Center (OK)** | Web elements, links, aur video controls par click karta hai |
| **🔴 RED Button** | Extension Manager modal open karta hai (Add-on toggle / install) |
| **🟢 GREEN Button** | Virtual Mouse Pointer ON / OFF (Focus Mode) toggle karta hai |
| **🟡 YELLOW Button** | Rust Adblock stats (Blocked ads counter) display karta hai |
| **🔵 BLUE Button** | Floating Picture-in-Picture (PiP) mode trigger karta hai |
| **📺 MENU Button** | TV Quick Bar (Address bar, UA switcher, Opacity slider) show/hide |
| **🔼 / 🔽 CH+/-** | Web page ko fast up/down scroll karta hai |
| **🔙 BACK Button** | Web history me pichhe jata hai ya Menu close karta hai |

---

## 📂 Project Architecture

```
helium-tv-browser/
├── app/
│   ├── build.gradle.kts           # GeckoView, Leanback, NDK dependencies
│   ├── CMakeLists.txt             # Rust cdylib NDK compilation bridge
│   └── src/main/
│       ├── AndroidManifest.xml    # Leanback Launcher, PiP, hardware acceleration
│       ├── rust/                  # 🦀 Pure Rust Adblock Engine
│       │   ├── Cargo.toml         # Rust crate configuration
│       │   └── src/
│       │       ├── lib.rs         # JNI exports for Android Kotlin
│       │       ├── matcher.rs     # EasyList/ABP wildcard matching engine
│       │       └── cosmetic.rs    # CSS element-hiding generator
│       ├── java/org/helium/tv/browser/
│       │   ├── MainActivity.kt    # Main TV Coordinator
│       │   ├── HeliumTvApplication.kt
│       │   ├── adblock/           # JNI bridge & filter list updater
│       │   ├── engine/            # GeckoView runtime & WebExtension controller
│       │   ├── input/             # Virtual Mouse cursor & D-Pad event handler
│       │   ├── pip/               # Picture-in-Picture & window translucency
│       │   ├── ui/                # TV Speed Dial, Dialogs, Leanback adapters
│       │   └── utils/             # TV/Desktop User-Agent profiles
│       └── res/                   # TV layouts, styles, and Leanback banners
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

---

## 🛠️ How to Build

### Option 1: Android Studio (PC / Laptop)
1. Project folder ko Android Studio me open karein.
2. NDK (version 25+) aur Rust toolchain (`cargo`, `rustup target add aarch64-linux-android`) install honi chahiye.
3. **Build -> Build Bundle(s) / APK(s) -> Build APK(s)** run karein.
4. Output APK `app/build/outputs/apk/release/app-release.apk` ko apne Android TV par Sideload (e.g. via Send Files to TV) karein.

### Option 2: Termux / CLI Build
Termux me compile karne ke liye zaroori tools (`openjdk-17`, `rust`, `clang`, Android SDK command-line tools) ki zaroorat hoti hai.
Build command:
```bash
./gradlew assembleRelease
```
