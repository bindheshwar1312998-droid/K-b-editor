-keepattributes *Annotation*
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <methods>;
}
-keep,allowobfuscation,allowshrinking class kotlinx.serialization.json.** { *; }
-keepnames class com.aistudio.kbeditor.r7v4qx.model.** { *; }

-keep class com.aistudio.kbeditor.r7v4qx.media.NativeMediaEngine { *; }
-keepclassmembers class com.aistudio.kbeditor.r7v4qx.media.NativeMediaEngine {
    native <methods>;
}
-keep class org.tensorflow.lite.** { *; }
