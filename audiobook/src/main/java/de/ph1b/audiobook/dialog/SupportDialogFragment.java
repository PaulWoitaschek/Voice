package de.ph1b.audiobook.dialog;


import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;

import com.afollestad.materialdialogs.MaterialDialog;

import de.ph1b.audiobook.R;


public class SupportDialogFragment extends DialogFragment {

    public static final String TAG = SupportDialogFragment.class.getSimpleName();


    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {


        final MaterialDialog.ListCallback onSupportListItemClicked =
                new MaterialDialog.ListCallback() {
                    @Override
                    public void onSelection(MaterialDialog materialDialog, View view, int i,
                                            CharSequence charSequence) {
                        switch (i) {
                            case 0: //dev and support
                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse
                                        ("https://github.com/Ph1b/MaterialAudiobookPlayer/" +
                                                "issues")));
                                break;
                            case 1: //translations
                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse
                                        ("https://www.transifex.com/projects/p/" +
                                                "material-audiobook-player/")));
                                break;
                            case 2:
                                new DonationDialogFragment().show(getFragmentManager(),
                                        DonationDialogFragment.TAG);
                                break;
                            default:
                                throw new AssertionError("There are just 3 items");
                        }
                    }
                };


        return new MaterialDialog.Builder(getActivity())
                .title(R.string.pref_support_title)
                .items(R.array.pref_support_values)
                .itemsCallback(onSupportListItemClicked)
                .build();
    }
}
