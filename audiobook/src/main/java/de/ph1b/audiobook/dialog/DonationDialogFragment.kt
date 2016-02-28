package de.ph1b.audiobook.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.support.v4.app.DialogFragment

import com.afollestad.materialdialogs.MaterialDialog

import de.ph1b.audiobook.R


class DonationDialogFragment : DialogFragment() {

    private lateinit var callback: OnDonationClickedListener

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val donationListCallback = MaterialDialog.ListCallback { materialDialog, view, i, charSequence ->
            val item: String
            when (i) {
                0 -> item = "1donation"
                1 -> item = "2donation"
                2 -> item = "3donation"
                3 -> item = "5donation"
                4 -> item = "10donation"
                5 -> item = "20donation"
                else -> throw AssertionError("There are only 4 items")
            }
            callback.onDonationClicked(item)
        }

        return MaterialDialog.Builder(context)
                .title(R.string.pref_support_donation)
                .items(R.array.pref_support_money)
                .itemsCallback(donationListCallback)
                .build()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        callback = context as OnDonationClickedListener
    }

    interface OnDonationClickedListener {
        fun onDonationClicked(item: String)
    }

    companion object {
        val TAG = DonationDialogFragment::class.java.simpleName
    }
}
