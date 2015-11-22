package de.ph1b.audiobook.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.text.InputType;

import com.afollestad.materialdialogs.MaterialDialog;

import de.ph1b.audiobook.R;
import de.ph1b.audiobook.model.Book;

/**
 * Simple dialog for changing the name of a book
 *
 * @author Paul Woitaschek
 */
public class EditBookTitleDialogFragment extends DialogFragment {

    public static final String TAG = EditBookTitleDialogFragment.class.getSimpleName();
    private static final String NI_PRESET_NAME = "niPresetName";
    private static final String NI_BOOK_ID = "niBookId";

    public static <T extends Fragment & OnTextChanged> EditBookTitleDialogFragment newInstance(T target, @NonNull Book book) {

        Bundle args = new Bundle();
        args.putString(NI_PRESET_NAME, book.getName());
        args.putLong(NI_BOOK_ID, book.getId());

        EditBookTitleDialogFragment dialog = new EditBookTitleDialogFragment();
        dialog.setTargetFragment(target, 42);
        dialog.setArguments(args);
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final String presetName = getArguments().getString(NI_PRESET_NAME);
        long bookId = getArguments().getLong(NI_BOOK_ID);

        return new MaterialDialog.Builder(getActivity())
                .title(R.string.edit_book_title)
                .inputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_AUTO_CORRECT)
                .input(getString(R.string.bookmark_edit_hint), presetName, false, (materialDialog, charSequence) -> {
                    String newText = charSequence.toString();
                    if (!newText.equals(presetName)) {
                        OnTextChanged callback = (OnTextChanged) getTargetFragment();
                        callback.onTitleChanged(newText, bookId);
                    }
                })
                .positiveText(R.string.dialog_confirm)
                .build();
    }

    public interface OnTextChanged {
        void onTitleChanged(@NonNull String newTitle, long bookId);
    }
}
