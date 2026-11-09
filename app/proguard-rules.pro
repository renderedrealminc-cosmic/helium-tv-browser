# Proguard Rules for Helium TV Browser

# Keep GeckoView internal and external bindings
-keep class org.mozilla.geckoview.** { *; }
-keep interface org.mozilla.geckoview.** { *; }
-dontwarn org.mozilla.geckoview.**

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
