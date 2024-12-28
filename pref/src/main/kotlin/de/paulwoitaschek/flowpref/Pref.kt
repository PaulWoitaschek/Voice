package de.paulwoitaschek.flowpref

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlin.properties.ReadWriteProperty

abstract class Pref<T> : ReadWriteProperty<Any, T> {

  @Suppress("LeakingThis")
  var value: T by this

  abstract fun setAndCommit(value: T)

  abstract val flow: Flow<T>

  abstract fun delete(commit: Boolean = false)
}

@Suppress("unused")
val <T : Any> Pref<T?>.flowNotNull: Flow<T>
  get() = flow.filterNotNull()
