# DroidCoder2 ProGuard Rules

# Keep JNI native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.deividsrk.droidcoder.**$$serializer { *; }
-keepclassmembers class com.deividsrk.droidcoder.** {
    *** Companion;
}
-keepclasseswithmembers class com.deividsrk.droidcoder.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep JGit
-keep class org.eclipse.jgit.** { *; }
-dontwarn org.eclipse.jgit.**

# Keep OkHttp
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# General Android
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Keep data models
-keep class com.deividsrk.droidcoder.agent.** { *; }
-keep class com.deividsrk.droidcoder.tool.** { *; }
