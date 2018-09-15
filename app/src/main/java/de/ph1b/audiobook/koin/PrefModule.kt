package de.ph1b.audiobook.koin

import android.preference.PreferenceManager
import com.f2prateek.rx.preferences2.Preference
import com.f2prateek.rx.preferences2.RxSharedPreferences
import de.ph1b.audiobook.injection.PrefKeys
import de.ph1b.audiobook.persistence.pref.PersistentPref
import de.ph1b.audiobook.persistence.pref.Pref
import de.ph1b.audiobook.uitools.ThemeUtil
import org.koin.dsl.module.module
import java.util.UUID

val PrefModule = module {
  val rxPrefs by lazy { get<RxSharedPreferences>() }
  single {
    val prefs = PreferenceManager.getDefaultSharedPreferences(get())
    RxSharedPreferences.create(prefs)
  }
  single<Pref<UUID>>(PrefKeys.CURRENT_BOOK) {
    val pref =
      get<RxSharedPreferences>().getObject(
        PrefKeys.CURRENT_BOOK,
        UUID.randomUUID(),
        object : Preference.Converter<UUID> {
          override fun deserialize(serialized: String): UUID = UUID.fromString(serialized)
          override fun serialize(value: UUID): String = value.toString()
        })
    PersistentPref(pref)
  }
  single<Pref<Set<String>>>(PrefKeys.COLLECTION_BOOK_FOLDERS) {
    val pref = rxPrefs.getStringSet(PrefKeys.COLLECTION_BOOK_FOLDERS, emptySet())
    PersistentPref(pref)
  }
  single<Pref<Set<String>>>(PrefKeys.SINGLE_BOOK_FOLDERS) {
    val pref = rxPrefs.getStringSet(PrefKeys.SINGLE_BOOK_FOLDERS, emptySet())
    PersistentPref(pref)
  }
  single<Pref<Boolean>>(PrefKeys.SHAKE_TO_RESET) {
    val pref = rxPrefs.getBoolean(PrefKeys.SHAKE_TO_RESET, false)
    PersistentPref(pref)
  }
  single<Pref<Int>>(PrefKeys.SLEEP_TIME) {
    val pref = rxPrefs.getInteger(PrefKeys.SLEEP_TIME, 20)
    PersistentPref(pref)
  }
  single<Pref<Boolean>>(PrefKeys.RESUME_ON_REPLUG) {
    val pref = rxPrefs.getBoolean(PrefKeys.RESUME_ON_REPLUG, true)
    PersistentPref(pref)
  }
  single<Pref<Int>>(PrefKeys.AUTO_REWIND_AMOUNT) {
    val pref = rxPrefs.getInteger(PrefKeys.AUTO_REWIND_AMOUNT, 2)
    PersistentPref(pref)
  }
  single<Pref<Int>>(PrefKeys.SEEK_TIME) {
    val pref = rxPrefs.getInteger(PrefKeys.SEEK_TIME, 20)
    PersistentPref(pref)
  }
  single<Pref<Boolean>>(PrefKeys.RESUME_AFTER_CALL) {
    val pref = rxPrefs.getBoolean(PrefKeys.RESUME_AFTER_CALL, false)
    PersistentPref(pref)
  }
  single<Pref<ThemeUtil.Theme>>(PrefKeys.THEME) {
    val pref =
      rxPrefs.getEnum(PrefKeys.THEME, ThemeUtil.Theme.DAY_NIGHT, ThemeUtil.Theme::class.java)
    PersistentPref(pref)
  }
  single<Pref<Boolean>>(PrefKeys.CRASH_REPORT_ENABLED) {
    val pref = rxPrefs.getBoolean(PrefKeys.CRASH_REPORT_ENABLED, false)
    PersistentPref(pref)
  }
}
