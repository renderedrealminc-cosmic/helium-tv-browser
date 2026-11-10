# Proguard Rules for Helium TV Browser

# Disable obfuscation to guarantee 100% stability for GeckoView JNI and Rust JNI bindings
-dontobfuscate

# Keep GeckoView internal and external bindings
-keep class org.mozilla.geckoview.** { *; }
-keep interface org.mozilla.geckoview.** { *; }
-keep class org.mozilla.gecko.** { *; }
-keep interface org.mozilla.gecko.** { *; }
-dontwarn org.mozilla.geckoview.**
-dontwarn org.mozilla.gecko.**

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
