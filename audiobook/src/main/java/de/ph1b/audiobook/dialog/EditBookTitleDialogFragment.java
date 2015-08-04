package de.ph1b.audiobook.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.InputType;

import com.afollestad.materialdialogs.MaterialDialog;

import de.ph1b.audiobook.R;

/**
 * Simple dialog for changing the name of a book
 *
 * @author Paul Woitaschek
 */
public class EditBookTitleDialogFragment extends BaseDialogFragment {

    public static final String TAG = EditBookTitleDialogFragment.class.getSimpleName();
    private static final String NI_PRESET_NAME = "niPresetName";
    @Nullable
    private OnTextChanged listener;

    public static EditBookTitleDialogFragment newInstance(@NonNull String presetName) {
        Bundle args = new Bundle();
        args.putString(NI_PRESET_NAME, presetName);

        EditBookTitleDialogFragment dialog = new EditBookTitleDialogFragment();
        dialog.setArguments(args);
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final String presetName = getArguments().getString(NI_PRESET_NAME);

        return new MaterialDialog.Builder(getActivity())
                .title(R.string.edit_book_title)
                .inputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_AUTO_CORRECT)
                .input(getString(R.string.bookmark_edit_hint), presetName, false, new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(MaterialDialog materialDialog, CharSequence charSequence) {
                        String newText = charSequence.toString();
                        if (!newText.equals(presetName) && listener != null) {
                            listener.onTitleChanged(newText);
                        }
                    }
                })
                .positiveText(R.string.dialog_confirm)
                .build();
    }

    public void setOnTextChangedListener(@Nullable OnTextChanged listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public interface OnTextChanged {
        void onTitleChanged(@NonNull String newTitle);
    }
}
