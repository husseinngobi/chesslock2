# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep chess library classes
-keep class com.github.bhlangonijr.chesslib.** { *; }
-dontwarn com.github.bhlangonijr.chesslib.**

# Keep our main classes
-keep class com.ngobi.chesslock.** { *; }

# Keep Android system classes we use
-keep class android.content.** { *; }
-keep class android.view.** { *; }
-keep class android.widget.** { *; }

# Keep lifecycle components
-keep class androidx.lifecycle.** { *; }

# Keep preferences
-keep class androidx.preference.** { *; }

# Keep biometric authentication
-keep class androidx.biometric.** { *; }

# Preserve line numbers for debugging
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile