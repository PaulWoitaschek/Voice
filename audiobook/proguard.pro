-dontobfuscate
-keep class org.vinuxproject.sonic.Sonic
-keep class de.ph1b.audiobook.uitools.FabBehavior {
    public <methods>;
}
-dontwarn com.squareup.okhttp.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn sun.misc.Unsafe