package de.ph1b.audiobook.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.afollestad.materialdialogs.MaterialDialog;

import java.io.File;
import java.io.IOException;

import de.ph1b.audiobook.R;

/**
 * A dialog giving the option to hide the selected book from other players.
 */
public class HideFolderDialog extends BaseDialogFragment {

    public static final String TAG = HideFolderDialog.class.getSimpleName();
    private static final String PATH_TO_HIDE = "pathToHide";
    @Nullable
    private DialogInterface.OnDismissListener listener;

    /**
     * Returns a file that called .nomedia that prevents music players from recognizing the book as
     * music.
     *
     * @param folder The folder
     * @return The file that provides the hiding
     */
    public static File getNoMediaFileByFolder(@NonNull File folder) {
        return new File(folder, ".nomedia");
    }

    public static HideFolderDialog newInstance(@NonNull File pathToHide) {
        Bundle args = new Bundle();
        args.putString(PATH_TO_HIDE, pathToHide.getAbsolutePath());

        HideFolderDialog hideFolderDialog = new HideFolderDialog();
        hideFolderDialog.setArguments(args);
        return hideFolderDialog;
    }

    public void setOnDismissListener(DialogInterface.OnDismissListener listener) {
        this.listener = listener;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);

        if (listener != null) {
            listener.onDismiss(dialog);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String pathToHide = getArguments().getString(PATH_TO_HIDE);
        assert pathToHide != null;
        final File hideFile = getNoMediaFileByFolder(new File(pathToHide));
        return new MaterialDialog.Builder(getActivity())
                .title(R.string.hide_folder_title)
                .content(R.string.hide_folder_content)
                .positiveText(R.string.hide_confirm)
                .negativeText(R.string.dialog_cancel)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        try {
                            //noinspection ResultOfMethodCallIgnored
                            hideFile.createNewFile();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                })
                .build();
    }
}
