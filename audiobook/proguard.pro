-dontobfuscate
-optimizationpasses 5
-dontwarn javax.annotation.**
-dontwarn sun.misc.Unsafe
-dontnote

# Retrolambda
-dontwarn java.lang.invoke.*

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

# picasso
-dontwarn com.squareup.picasso.OkHttpDownloader

# mp4parser
-keep class * implements com.coremedia.iso.boxes.Box { *; }
-dontwarn okio.**
-dontwarn com.googlecode.mp4parser.**
