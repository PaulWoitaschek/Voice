package de.ph1b.audiobook.common


interface Logger {
  fun i(message: String)
  fun e(message: String)
  fun e(throwable: Throwable)
}
