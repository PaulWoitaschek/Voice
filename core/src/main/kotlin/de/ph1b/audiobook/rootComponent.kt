package de.ph1b.audiobook

lateinit var rootComponent: Any

inline fun <reified T> rootComponentAs(): T = rootComponent as T
