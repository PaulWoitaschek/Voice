package de.ph1b.audiobook.features.chapterReader


sealed class Header {

  abstract val id: String
  abstract val size: Int

  class FrameHeader(override val id: String, override val size: Int) : Header()
  class TagHeader(override val id: String, override val size: Int, val version: Char) : Header()
}