# Proguard Rules for Helium TV Browser

# Disable obfuscation for 100% stability across all Android devices
-dontobfuscate

# Keep Chromium WebView & WebKit classes
-keep class android.webkit.** { *; }
-keep interface android.webkit.** { *; }
-keep class androidx.webkit.** { *; }
-keep interface androidx.webkit.** { *; }

# Keep Helium Browser package & data models
-keep class org.helium.tv.browser.** { *; }
-keepclassmembers class org.helium.tv.browser.** { *; }

# Keep Native JNI methods for Rust AdBlocker and C++ Native Core
-keepclasseswithmembernames class * {
    native <methods>;
}

-keep class org.helium.tv.browser.adblock.RustAdBlocker {
    *;
}

-keep class org.helium.tv.browser.engine.TvNativeCore {
    *;
}

# Keep Kotlin Coroutines and reflection
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
-dontwarn kotlin.**
-dontwarn kotlinx.**
