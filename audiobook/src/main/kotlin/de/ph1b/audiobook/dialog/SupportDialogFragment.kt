package de.ph1b.audiobook.dialog


import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.DialogFragment

import com.afollestad.materialdialogs.MaterialDialog

import de.ph1b.audiobook.R


class SupportDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val onSupportListItemClicked = MaterialDialog.ListCallback { materialDialog, view, i, charSequence ->
            when (i) {
                0 //dev and support
                -> startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_URL)))
                1 //translations
                -> startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(TRANSLATION_URL)))
                2 -> DonationDialogFragment().show(fragmentManager,
                        DonationDialogFragment.TAG)
                else -> throw AssertionError("There are just 3 items")
            }
        }

        return MaterialDialog.Builder(activity)
                .title(R.string.pref_support_title)
                .items(R.array.pref_support_values)
                .itemsCallback(onSupportListItemClicked)
                .build()
    }

    private val GITHUB_URL = "https://github.com/Ph1b/MaterialAudiobookPlayer"
    private val TRANSLATION_URL = "https://www.transifex.com/projects/p/material-audiobook-player"

    companion object {
        @JvmField val TAG = SupportDialogFragment::class.java.simpleName
    }
}
