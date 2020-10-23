# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# The following prevents aboutLibraries from being blank due to minifyEnabled.
-keepclasseswithmembers class **.R$* {
    public static final int define_*;
}

# The following keeps information to make debugging through Crashlytics easier (or possible at all, really).
-keepattributes SourceFile,LineNumberTable

-keepattributes Signature

# The following are necessary to send information to Firebase.
-keepclassmembers class com.google.android.gms.maps.model.CircleOptions {
    *;
}
-keepclassmembers class com.google.android.gms.maps.model.PolygonOptions {
    *;
}
-keepclassmembers class com.google.android.gms.maps.model.LatLng {
    *;
}
# Disable logcat messages.
-assumenosideeffects class android.util.Log {
    *;
}