-dontobfuscate
-dontwarn com.squareup.okhttp.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn sun.misc.Unsafe
-dontnote

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

# LibVLC
-keep class org.videolan.libvlc.** { *; }