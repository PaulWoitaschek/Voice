# ChapterReader [![CI Status](https://circleci.com/gh/PaulWoitaschek/ChapterReader.svg?&style=shield&circle-token=459db1375fb03763704edb88afc36136c091c33a)](https://circleci.com/gh/PaulWoitaschek/ChapterReader)
This library is the chapter reader for [Voice](https://github.com/PaulWoitaschek/Voice/).

# Usage
Create a chapter reader and let it parse a file.

```kotlin
val chapterReader = ChapterReaderFactory.create()
val result: List<Chapter> = chapterReader.read(file)
```

# Install

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}
dependencies {
    implementation "com.github.PaulWoitaschek:ChapterReader:X.Y.Z"
}
```
