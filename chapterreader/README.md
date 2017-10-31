This library is the chapter reader for [Voice](https://github.com/PaulWoitaschek/Voice/).

# Usage
Create a chapter reader and let it parse a file.

```kotlin
val chapterReader = ChapterReaderFactory.create(logger)
val result: Map<Int, String> = chapterReader.read(file)
```

The resulting Map has the start of each chapter as the key (in ms) and the chapter title as the value.

# Install

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}
dependencies {
    implementation "com.github.PaulWoitaschek:ChapterReader:X.Y.Z"
}
```
