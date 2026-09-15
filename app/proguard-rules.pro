# PDFBox
-keep class com.tom_roush.** { *; }
-keep class org.apache.** { *; }
-dontwarn org.apache.**
-dontwarn com.tom_roush.**

# Gson
-keep class com.google.gson.** { *; }
-keepattributes *Annotation*
-keepattributes Signature

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule { <init>(...); }
-keep class com.bumptech.glide.** { *; }

# BouncyCastle (PDFBox dependency)
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**
