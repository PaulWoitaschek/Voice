package de.paulwoitaschek.flowpref.android

import de.paulwoitaschek.flowpref.Pref
import de.paulwoitaschek.flowpref.android.internal.adapter.BooleanAdapter
import de.paulwoitaschek.flowpref.android.internal.adapter.EnumAdapter
import de.paulwoitaschek.flowpref.android.internal.adapter.IntAdapter
import de.paulwoitaschek.flowpref.android.internal.adapter.LongAdapter
import de.paulwoitaschek.flowpref.android.internal.adapter.StringAdapter
import de.paulwoitaschek.flowpref.android.internal.adapter.StringSetAdapter

@Suppress("unused")
fun AndroidPreferences.boolean(
  key: String,
  default: Boolean,
): Pref<Boolean> {
  return create(key, default, BooleanAdapter)
}

fun <E : Enum<E>> AndroidPreferences.enum(
  key: String,
  default: E,
  clazz: Class<E>,
): Pref<E> {
  return create(key, default, EnumAdapter(clazz))
}

@Suppress("unused")
inline fun <reified E : Enum<E>> AndroidPreferences.enum(
  key: String,
  default: E,
): Pref<E> {
  return enum(key, default, E::class.java)
}

@Suppress("unused")
fun AndroidPreferences.int(
  key: String,
  default: Int,
): Pref<Int> {
  return create(key, default, IntAdapter)
}

@Suppress("unused")
fun AndroidPreferences.long(
  key: String,
  default: Long,
): Pref<Long> {
  return create(key, default, LongAdapter)
}

fun AndroidPreferences.string(
  key: String,
  default: String,
): Pref<String> {
  return create(key, default, StringAdapter)
}

@Suppress("unused")
fun AndroidPreferences.stringSet(
  key: String,
  default: Set<String>,
): Pref<Set<String>> {
  return create(key, default, StringSetAdapter)
}
