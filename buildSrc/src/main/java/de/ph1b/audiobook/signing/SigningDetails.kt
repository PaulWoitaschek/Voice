package de.ph1b.audiobook.signing

import java.io.File

data class SigningDetails(
  val storeFile: File,
  val storePassword: String,
  val keyAlias: String,
  val keyPassword: String
)
