package de.paulwoitaschek.chapterreader.misc

interface Logger {
  fun i(message: String)
  fun e(throwable: Throwable)
  fun e(message: String)
  fun e(message: String, cause: Throwable?)
}
