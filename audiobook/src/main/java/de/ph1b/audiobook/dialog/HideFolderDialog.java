package de.ph1b.audiobook.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;

import com.afollestad.materialdialogs.MaterialDialog;

import java.io.File;
import java.io.IOException;

import de.ph1b.audiobook.R;
import timber.log.Timber;

/**
 * A dialog giving the option to hide the selected book from other players.
 */
public class HideFolderDialog extends DialogFragment {

    public static final String TAG = HideFolderDialog.class.getSimpleName();
    private static final String PATH_TO_HIDE = "pathToHide";
    private OnChosenListener callback;

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

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        callback = (OnChosenListener) context;
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
                .onPositive((materialDialog, dialogAction) -> {
                    try {
                        Timber.i("Create new File will be called.");
                        //noinspection ResultOfMethodCallIgnored
                        hideFile.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                })
                .onAny((materialDialog, dialogAction) -> callback.onChosen())
                .build();
    }

    public interface OnChosenListener {
        void onChosen();
    }
}
