-dontobfuscate
-optimizationpasses 5
-dontwarn javax.annotation.**
-dontwarn sun.misc.Unsafe
-dontnote
-dontwarn okio.**

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
-dontwarn okhttp3.internal.platform.*

# chapterreader
-dontwarn org.slf4j.**

# dagger
-dontwarn com.google.errorprone.annotations.*

# Moshi: https://github.com/square/moshi/issues/738
# Retain generated JsonAdapters if annotated type is retained.
-keep class **JsonAdapter {
    <init>(...);
    <fields>;
}
