# BypNet ProGuard Rules

# Keep VPN service
-keep class com.bypnet.app.tunnel.BypNetVpnService { *; }

# Keep config data classes for Gson
-keep class com.bypnet.app.config.** { *; }
-keep class com.bypnet.app.data.entity.** { *; }

# JSch
-keep class com.jcraft.jsch.** { *; }
-dontwarn com.jcraft.jsch.**

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
