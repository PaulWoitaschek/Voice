package de.paulwoitaschek.flowpref.android

interface PrefAdapter<T> {

  fun toString(value: T): String
  fun fromString(string: String): T
}
