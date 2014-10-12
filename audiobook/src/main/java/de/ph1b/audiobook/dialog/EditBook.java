package de.ph1b.audiobook.dialog;


import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;

import de.ph1b.audiobook.BuildConfig;
import de.ph1b.audiobook.R;
import de.ph1b.audiobook.utils.CommonTasks;

public class EditBook extends DialogFragment implements View.OnClickListener {

    public static final String BOOK_NAME = "BOOK_NAME";
    public static final String BOOK_COVER = "BOOK_COVER";
    public static final String DIALOG_TITLE = "DIALOG_TITLE";

    private ImageView cover;
    private ProgressBar coverReplacement;
    private ImageButton previousCover;
    private ImageButton nextCover;
    private EditText nameEditText;

    private int coverPosition;
    private ArrayList<Bitmap> covers;
    int googleCount = 0;

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.book_name_remove:
                nameEditText.setText("");
                break;
            case R.id.previous_cover:
                coverPosition--;
                cover.setImageBitmap(covers.get(coverPosition));
                cover.setVisibility(View.VISIBLE);
                coverReplacement.setVisibility(View.GONE);
                nextCover.setVisibility(View.VISIBLE);
                if (coverPosition == 0)
                    previousCover.setVisibility(View.INVISIBLE);
                break;
            case R.id.next_cover:
                if (coverPosition < covers.size() - 1) {
                    coverPosition++;
                    cover.setImageBitmap(covers.get(coverPosition));
                    previousCover.setVisibility(View.VISIBLE);
                } else {
                    new AddCoverAsync(nameEditText.getText().toString(), googleCount++).execute();
                }
                break;
            default:
                break;
        }
    }


    public interface OnEditBookFinished {
        public void onEditBookFinished(String bookName, Bitmap cover, Boolean success);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        Bundle b = getArguments();
        String defaultName = b.getString(BOOK_NAME);
        String dialogTitle = b.getString(DIALOG_TITLE);
        covers = b.getParcelableArrayList(BOOK_COVER);

        //init view
        //passing null is fine because of fragment
        LayoutInflater inflater = getActivity().getLayoutInflater();
        @SuppressLint("InflateParams") View v = inflater.inflate(R.layout.dialog_book_edit, null);
        builder.setView(v);

        //init items
        ViewGroup coverLayout = (ViewGroup) v.findViewById(R.id.cover_layout);
        nameEditText = (EditText) v.findViewById(R.id.book_name);
        ImageButton removeBookName = (ImageButton) v.findViewById(R.id.book_name_remove);
        cover = (ImageView) v.findViewById(R.id.cover);
        coverReplacement = (ProgressBar) v.findViewById(R.id.cover_replacement);
        previousCover = (ImageButton) v.findViewById(R.id.previous_cover);
        nextCover = (ImageButton) v.findViewById(R.id.next_cover);
        final TextView emptyTitleText = (TextView) v.findViewById(R.id.empty_title);

        //init listeners
        removeBookName.setOnClickListener(this);
        nextCover.setOnClickListener(this);
        previousCover.setOnClickListener(this);

        builder.setTitle(R.string.action_jump_to);
        builder.setNegativeButton(R.string.dialog_cancel, null);

        //init values
        nameEditText.setText(defaultName);

        previousCover.setVisibility(View.INVISIBLE);
        boolean online = CommonTasks.isOnline(getActivity());
        int coverSize = covers.size();
        if (BuildConfig.DEBUG) Log.d("ebk", String.valueOf(coverSize));

        //sets up cover according to online state and amount of covers available
        if (coverSize == 0) {
            if (online) {
                if (BuildConfig.DEBUG) Log.d("ebk", "p1");
                new AddCoverAsync(defaultName, googleCount++).execute();
            } else {
                coverLayout.setVisibility(View.GONE);
            }
        }
        if (coverSize > 0) {
            cover.setImageBitmap(covers.get(0));
            coverPosition = 0;
            if (!online && coverSize == 1)
                nextCover.setVisibility(View.INVISIBLE);
        }


        builder.setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                ((OnEditBookFinished) getTargetFragment()).onEditBookFinished(null, null, false);
            }
        });
        builder.setPositiveButton(R.string.dialog_confirm, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String bookName = nameEditText.getText().toString();
                Bitmap cover = null;
                if (covers.size() > 0)
                    cover = covers.get(coverPosition);
                ((OnEditBookFinished) getTargetFragment()).onEditBookFinished(bookName, cover, true);
            }
        });
        builder.setTitle(dialogTitle);

        final AlertDialog editBook = builder.create();
        nameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                if (charSequence.toString().length() == 0) {
                    emptyTitleText.setVisibility(View.VISIBLE);
                    editBook.getButton(Dialog.BUTTON_POSITIVE).setEnabled(false);
                } else {
                    emptyTitleText.setVisibility(View.INVISIBLE);
                    editBook.getButton(Dialog.BUTTON_POSITIVE).setEnabled(true);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
                googleCount = 0;
            }
        });
        return editBook;
    }

    private class AddCoverAsync extends AsyncTask<Void, Void, Bitmap> {
        String searchString;
        int pageCounter;

        public AddCoverAsync(String searchString, int pageCounter) {
            this.searchString = searchString;
            this.pageCounter = pageCounter;
        }

        @Override
        protected void onPreExecute() {
            coverReplacement.setVisibility(View.VISIBLE);
            cover.setVisibility(View.GONE);
        }

        @Override
        protected Bitmap doInBackground(Void... voids) {
            return CommonTasks.genBitmapFromInternet(searchString, pageCounter);
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            coverReplacement.setVisibility(View.GONE);
            cover.setVisibility(View.VISIBLE);
            if (bitmap != null) {
                covers.add(bitmap);
                coverPosition = covers.indexOf(bitmap);
                cover.setImageBitmap(bitmap);
                previousCover.setVisibility(View.VISIBLE);
            } else {
                new AddCoverAsync(searchString, googleCount++).execute();
            }
        }
    }
}
