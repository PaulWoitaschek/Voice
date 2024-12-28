package voice.pref.internal.adapter

import android.content.SharedPreferences
import androidx.core.content.edit
import voice.pref.internal.InternalPrefAdapter

internal class EnumAdapter<E : Enum<E>>(private val clazz: Class<E>) : InternalPrefAdapter<E> {

  override fun get(
    key: String,
    prefs: SharedPreferences,
  ): E {
    val stringValue = prefs.getString(key, "")!!
    return java.lang.Enum.valueOf(clazz, stringValue)
  }

  override fun set(
    key: String,
    prefs: SharedPreferences,
    value: E,
    commit: Boolean,
  ) {
    prefs.edit(commit = commit) { putString(key, value.name) }
  }
}
