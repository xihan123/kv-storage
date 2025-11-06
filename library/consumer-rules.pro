# Keep KV Storage classes
-keep class website.xihan.kv.** { *; }

# Keep Koin
-keep class org.koin.** { *; }
-keepclassmembers class * {
    @org.koin.core.annotation.* <methods>;
}

# Keep ContentProvider
-keep public class * extends android.content.ContentProvider

# Keep BroadcastReceiver
-keep public class * extends android.content.BroadcastReceiver
