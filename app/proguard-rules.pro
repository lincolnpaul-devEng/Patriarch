# ---- Aggressive R8 hardening (release) ----
-allowaccessmodification
-repackageclasses ''
-overloadaggressively
-optimizationpasses 5
-renamesourcefileattribute ''
# Drop source/line metadata so stack traces and decompiled output carry no file names.
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,Exceptions

# Strip all logging from release binaries (no leaked flow/state in logcat).
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
    public static int wtf(...);
}
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    public static void checkNotNull(...);
    public static void checkNotNullParameter(...);
    public static void checkExpressionValueIsNotNull(...);
    public static void checkNotNullExpressionValue(...);
    public static void checkParameterIsNotNull(...);
    public static void checkReturnedValueIsNotNull(...);
    public static void checkFieldIsNotNull(...);
}

# ---- Required keeps (reflection / serialization) ----
-keep class com.campus.network.CampusApplication { *; }
-keep class com.campus.network.MainActivity { *; }
-keep class * extends android.content.BroadcastReceiver
-keep class * extends com.google.firebase.messaging.FirebaseMessagingService

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Kotlin coroutines / serialization
-keepclassmembernames class kotlinx.** { volatile <fields>; }
-dontwarn kotlinx.coroutines.debug.**

# Stream Chat, Firebase, Play services ship consumer rules; silence optional deps.
-dontwarn io.getstream.**
-dontwarn com.google.android.gms.**
-dontwarn org.slf4j.**
-dontwarn javax.annotation.**

# JNI: native entry points must keep names.
-keepclasseswithmembernames class * { native <methods>; }
