package de.ph1b.audiobook.dialog;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.preference.DialogPreference;
import android.util.AttributeSet;


public class VariablePlaybackSpeedPreferenceDialog extends DialogPreference {


    public VariablePlaybackSpeedPreferenceDialog(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                try {
                    Intent playStoreIntent = new Intent(Intent.ACTION_VIEW,
                            Uri.parse("market://details?id=com.falconware.prestissimo"));
                    VariablePlaybackSpeedPreferenceDialog.this.getContext().startActivity(playStoreIntent);
                } catch (ActivityNotFoundException e) {
                    e.printStackTrace();
                }
                break;
            default:
                super.onClick(dialog, which);
                break;
        }
    }
}
