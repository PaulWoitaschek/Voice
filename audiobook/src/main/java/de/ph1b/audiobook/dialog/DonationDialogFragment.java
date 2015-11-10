package de.ph1b.audiobook.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;

import com.afollestad.materialdialogs.MaterialDialog;

import de.ph1b.audiobook.R;


public class DonationDialogFragment extends DialogFragment {
    public static final String TAG = DonationDialogFragment.class.getSimpleName();

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final MaterialDialog.ListCallback donationListCallback = (materialDialog, view, i, charSequence) -> {
            String item;
            switch (i) {
                case 0:
                    item = "1donation";
                    break;
                case 1:
                    item = "2donation";
                    break;
                case 2:
                    item = "3donation";
                    break;
                case 3:
                    item = "5donation";
                    break;
                case 4:
                    item = "10donation";
                    break;
                case 5:
                    item = "20donation";
                    break;
                default:
                    throw new AssertionError("There are only 4 items");
            }
            ((OnDonationClickedListener) getActivity()).onDonationClicked(item);
        };

        return new MaterialDialog.Builder(getActivity())
                .title(R.string.pref_support_donation)
                .items(R.array.pref_support_money)
                .itemsCallback(donationListCallback)
                .build();
    }

    public interface OnDonationClickedListener {
        void onDonationClicked(String item);
    }
}
