# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.

# Keep the app's classes
-keep class com.re9ant.peacekeeper.** { *; }

# Keep Android components
-keep public class * extends android.app.Activity
-keep public class * extends android.telecom.CallScreeningService

# Keep Parcelable implementations
-keepclassmembers class * implements android.os.Parcelable {
    static ** CREATOR;
}
