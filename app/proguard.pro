# Revisit this when datastore > 1.1.6 is releaed
# Repro: ./gradlew :app:installGithubProprietaryRelease (should crash on startup)
# https://issuetracker.google.com/issues/413078297
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
}
