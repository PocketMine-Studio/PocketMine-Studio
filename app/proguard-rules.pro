# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\tools\adt-bundle-windows-x86_64-20131030\sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
-keepclassmembers class fqcn.of.javascript.interface.for.webview {
   public *;
}

# Gson
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}

# JSoup
-keep class org.jsoup.** { *; }

# MPAndroidChart
-keep class com.github.mikephil.charting.** { *; }

# Sora Editor
-keep class io.github.rosemoe.sora.** { *; }
-keep class org.eclipse.tm4e.** { *; }

# General Android
-keep class net.eqozqq.pocketminestudio.models.** { *; }

-dontwarn kotlin.**
-dontwarn org.jspecify.**
-dontwarn java.lang.invoke.**
-dontwarn org.apache.commons.**
