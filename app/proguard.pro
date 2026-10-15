# JEBML uses reflection
-keepclassmembers class * extends org.ebml.Element {
    <init>(...);
}

# Glance finds the widgets to update by the class name of their GlanceAppWidget. If R8 merges
# NowPlayingWidget and ShelfWidget into one class, updating one turns the other into it.
-keep class * extends androidx.glance.appwidget.GlanceAppWidget
