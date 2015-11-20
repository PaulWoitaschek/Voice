-dontobfuscate
-keep class org.vinuxproject.sonic.Sonic
-dontwarn com.squareup.okhttp.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn sun.misc.Unsafe

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