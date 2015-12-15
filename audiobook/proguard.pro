-dontobfuscate
-keep class org.vinuxproject.sonic.Sonic
-dontwarn com.squareup.okhttp.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn sun.misc.Unsafe
-dontnote

# butterknife
-keep class butterknife.** { *; }
-dontwarn butterknife.internal.**
-keep class **$$ViewBinder { *; }
-keepclasseswithmembernames class * {
    @butterknife.* <fields>;
}
-keepclasseswithmembernames class * {
    @butterknife.* <methods>;
}

# Retrolambda
-dontwarn java.lang.invoke.*

# retrofit
-dontwarn retrofit.**
-keep class retrofit.** { *; }
-keepattributes Signature
-keepattributes Exceptions

# rxJava
-keepclassmembers class rx.internal.util.unsafe.*ArrayQueue*Field* {
   long producerIndex;
   long consumerIndex;
}
-keepclassmembers class rx.internal.util.unsafe.BaseLinkedQueueProducerNodeRef {
    rx.internal.util.atomic.LinkedQueueNode producerNode;
}
-keepclassmembers class rx.internal.util.unsafe.BaseLinkedQueueConsumerNodeRef {
    rx.internal.util.atomic.LinkedQueueNode consumerNode;
}

# Kotlin exceptions. TODO: Remove this once Kotlin bug https://youtrack.jetbrains.com/issue/KT-10259 is fixed
-dontwarn de.ph1b.audiobook.playback.BookReaderService$onCreate$$inlined$apply$lambda$1
-dontwarn de.ph1b.audiobook.mediaplayer.MediaPlayerController$prepare$$inlined$withLock$lambda$1