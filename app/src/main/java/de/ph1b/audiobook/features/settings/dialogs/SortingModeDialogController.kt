package de.ph1b.audiobook.features.settings.dialogs

import android.app.Dialog
import android.os.Bundle
import com.afollestad.materialdialogs.MaterialDialog
import de.ph1b.audiobook.R
import de.ph1b.audiobook.features.bookOverview.list.BookComparator
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.injection.PrefKeys
import de.ph1b.audiobook.misc.DialogController
import de.ph1b.audiobook.persistence.pref.Pref
import javax.inject.Inject
import javax.inject.Named

class SortingModeDialogController : DialogController() {

    @field:[Inject Named(PrefKeys.SORTING_MODE)]
    lateinit var sortingPref: Pref<BookComparator>

    override fun onCreateDialog(savedViewState: Bundle?): Dialog {
        App.component.inject(this)

        val currentMode = sortingPref.value
        val sortingModes = BookComparator.values()
        val names = sortingModes.map { activity!!.getString(it.nameId) }

        return MaterialDialog.Builder(activity!!)
                .items(*names.toTypedArray())
                .itemsCallbackSingleChoice(sortingModes.indexOf(currentMode)) { _, _, i, _ ->
                    val newMode = sortingModes[i]
                    sortingPref.value = newMode
                    true
                }
                .positiveText(R.string.dialog_confirm)
                .negativeText(R.string.dialog_cancel)
                .title(R.string.pref_sort_title)
                .build()
    }
}
