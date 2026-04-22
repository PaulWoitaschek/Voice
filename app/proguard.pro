# JEBML uses reflection
-keepclassmembers class * extends org.ebml.Element {
    <init>(...);
}
