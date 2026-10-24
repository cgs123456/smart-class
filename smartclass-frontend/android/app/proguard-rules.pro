# Capacitor
-keep class com.getcapacitor.** { *; }
-keep class com.smartclass.app.** { *; }
-keepclassmembers class * { @com.getcapacitor.Plugin *; }
-keep class io.ionic.starter.** { *; }

# Vue / Axios
-dontwarn org.apache.commons.logging.**
-dontwarn android.net.**
