# We're open source, no need to hide anything
-dontobfuscate

# Revisit this when datastore > 1.1.6 is releaed
# Repro: ./gradlew :app:installGithubRelease (should crash on startup)
# https://issuetracker.google.com/issues/413078297
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
}

# JEBML uses reflection
-keepclassmembers class * extends org.ebml.Element {
    <init>(...);
}
