package de.ph1b.audiobook.dialog;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.ph1b.audiobook.R;
import de.ph1b.audiobook.model.Book;
import de.ph1b.audiobook.model.DataBaseHelper;
import de.ph1b.audiobook.uitools.CoverDownloader;
import de.ph1b.audiobook.uitools.CoverReplacement;
import de.ph1b.audiobook.uitools.DraggableBoxImageView;
import de.ph1b.audiobook.uitools.ImageHelper;
import de.ph1b.audiobook.utils.L;

public class EditBookDialogFragment extends DialogFragment implements View.OnClickListener {
    public static final String TAG = EditBookDialogFragment.class.getSimpleName();
    private static final String BOOK_COVER = "BOOK_COVER";
    private static final int REPLACEMENT_DIMEN = 500;
    private static final String COVER_POSITION = "COVER_POSITION";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private CoverDownloader coverDownloader;
    private DraggableBoxImageView coverImageView;
    private ProgressBar coverReplacement;
    private ImageButton previousCover;
    private ImageButton nextCover;
    private EditText nameEditText;
    private AddCoverAsync addCoverAsync;
    private int coverPosition = 0;
    private ArrayList<Bitmap> covers;
    private int googleCount = 0;
    @Nullable
    private OnEditBookFinished listener;

    public static EditBookDialogFragment newInstance(@NonNull Book book, @NonNull Context c) {
        EditBookDialogFragment editBookDialogFragment = new EditBookDialogFragment();
        Bundle bundle = new Bundle();

        ArrayList<Bitmap> covers = new ArrayList<>();
        CoverReplacement replacement = new CoverReplacement(book.getName(), c);
        covers.add(ImageHelper.drawableToBitmap(replacement,
                EditBookDialogFragment.REPLACEMENT_DIMEN,
                EditBookDialogFragment.REPLACEMENT_DIMEN));

        File coverFile = book.getCoverFile();
        if (coverFile.exists() && coverFile.canRead()) {
            Bitmap defaultCover = BitmapFactory.decodeFile(coverFile.getAbsolutePath());
            if (defaultCover != null) {
                covers.add(defaultCover);
            }
        }

        bundle.putParcelableArrayList(EditBookDialogFragment.BOOK_COVER, covers);
        bundle.putLong(Book.TAG, book.getId());

        editBookDialogFragment.setArguments(bundle);
        return editBookDialogFragment;
    }

    public void setOnEditBookFinished(@Nullable OnEditBookFinished listener) {
        this.listener = listener;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.previous_cover:
                if (addCoverAsync != null && !addCoverAsync.isCancelled()) {
                    addCoverAsync.cancel(true);
                }
                if (coverPosition > 0) {
                    coverPosition--;
                }
                coverImageView.setImageBitmap(covers.get(coverPosition));
                coverImageView.setVisibility(View.VISIBLE);
                coverReplacement.setVisibility(View.GONE);
                nextCover.setVisibility(View.VISIBLE);
                if (coverPosition == 0) {
                    previousCover.setVisibility(View.INVISIBLE);
                }
                break;
            case R.id.next_cover:
                if (coverPosition < covers.size() - 1) {
                    coverPosition++;
                    coverImageView.setImageBitmap(covers.get(coverPosition));
                    previousCover.setVisibility(View.VISIBLE);
                    if (!ImageHelper.isOnline(getActivity()) &&
                            coverPosition == covers.size() - 1) {
                        nextCover.setVisibility(View.INVISIBLE);
                    }
                } else {
                    genCoverFromInternet(nameEditText.getText().toString());
                }
                break;
            default:
                break;
        }
    }

    private void genCoverFromInternet(String searchString) {
        //cancels task if running
        if (addCoverAsync != null) {
            if (!addCoverAsync.isCancelled()) {
                addCoverAsync.cancel(true);
            }
        }
        addCoverAsync = new AddCoverAsync(searchString);
        googleCount++;
        addCoverAsync.execute();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        coverDownloader = new CoverDownloader(getActivity());
        if (savedInstanceState == null) {
            covers = getArguments().getParcelableArrayList(BOOK_COVER);

            // defaulting only to capital cover when its the only one.
            assert covers != null;
            if (covers.size() == 1) {
                coverPosition = 0;
            } else {
                coverPosition = 1;
            }
        } else {
            covers = savedInstanceState.getParcelableArrayList(BOOK_COVER);
            coverPosition = savedInstanceState.getInt(COVER_POSITION);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelableArrayList(BOOK_COVER, covers);
        outState.putInt(COVER_POSITION, coverPosition);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        final DataBaseHelper db = DataBaseHelper.getInstance(getActivity());
        final long bookId = getArguments().getLong(Book.TAG);
        final Book book = db.getBook(bookId);
        assert book != null;

        //init view
        //passing null is fine because of fragment
        LayoutInflater inflater = getActivity().getLayoutInflater();
        @SuppressLint("InflateParams") View customView = inflater.inflate(R.layout.dialog_book_edit,
                null);

        //init items
        nameEditText = (EditText) customView.findViewById(R.id.book_name);
        coverImageView = (DraggableBoxImageView) customView.findViewById(R.id.edit_book);
        coverReplacement = (ProgressBar) customView.findViewById(R.id.cover_replacement);
        previousCover = (ImageButton) customView.findViewById(R.id.previous_cover);
        nextCover = (ImageButton) customView.findViewById(R.id.next_cover);
        final TextView emptyTitleText = (TextView) customView.findViewById(R.id.empty_title);

        //init listeners
        nextCover.setOnClickListener(this);
        previousCover.setOnClickListener(this);

        //init values
        nameEditText.setText(book.getName());

        boolean online = ImageHelper.isOnline(getActivity());

        coverImageView.setImageBitmap(covers.get(coverPosition));
        if (!online && (coverPosition == (covers.size() - 1))) {
            nextCover.setVisibility(View.INVISIBLE);
        }
        if (coverPosition == 0) {
            previousCover.setVisibility(View.INVISIBLE);
        }

        MaterialDialog.ButtonCallback buttonCallback = new MaterialDialog.ButtonCallback() {
            @Override
            public void onPositive(MaterialDialog dialog) {
                L.d(TAG, "edit book positive clicked. CoverPosition=" + coverPosition);
                if (addCoverAsync != null && !addCoverAsync.isCancelled()) {
                    addCoverAsync.cancel(true);
                }

                String bookName = nameEditText.getText().toString();
                Rect r = coverImageView.getSelectedRect();
                boolean useCoverReplacement;
                if (coverPosition > 0 && r.width() > 0 && r.height() > 0) {
                    Bitmap cover = covers.get(coverPosition);
                    cover = Bitmap.createBitmap(cover, r.left, r.top, r.width(), r.height());
                    ImageHelper.saveCover(cover, getActivity(), book.getCoverFile());
                    Picasso.with(getActivity()).invalidate(book.getCoverFile());
                    useCoverReplacement = false;
                } else {
                    useCoverReplacement = true;
                }

                Picasso.with(getActivity()).invalidate(book.getCoverFile());

                synchronized (db) {
                    Book dbBook = db.getBook(book.getId());
                    if (dbBook != null) {
                        dbBook.setUseCoverReplacement(useCoverReplacement);
                        dbBook.setName(bookName);
                        db.updateBook(dbBook);
                    }
                }

                if (listener != null) {
                    listener.onEditBookFinished();
                }
            }

            @Override
            public void onNegative(MaterialDialog dialog) {
                if (addCoverAsync != null && !addCoverAsync.isCancelled()) {
                    addCoverAsync.cancel(true);
                }
            }
        };

        final MaterialDialog editBook = new MaterialDialog.Builder(getActivity())
                .customView(customView, true)
                .title(R.string.edit_book_title)
                .positiveText(R.string.dialog_confirm)
                .negativeText(R.string.dialog_cancel)
                .callback(buttonCallback)
                .build();

        nameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                String newName = charSequence.toString();
                int textLength = newName.length();
                if (textLength == 0) {
                    emptyTitleText.setVisibility(View.VISIBLE);
                    editBook.getActionButton(DialogAction.POSITIVE).setEnabled(false);
                } else {
                    emptyTitleText.setVisibility(View.INVISIBLE);
                    editBook.getActionButton(DialogAction.POSITIVE).setEnabled(true);
                    Bitmap newLetterCover = ImageHelper.drawableToBitmap(new CoverReplacement(
                            newName, getActivity()), REPLACEMENT_DIMEN, REPLACEMENT_DIMEN);

                    covers.set(0, newLetterCover);
                    L.d(TAG, "onTextChanged, setting new cover with newName=" + newName);
                    if (coverPosition == 0) {
                        L.d(TAG, "textLength > 0 and position==0, so setting new image");
                        coverImageView.setImageBitmap(newLetterCover);
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
                googleCount = 0;
            }
        });

        // if we are online and at the first (always replacement) cover, immediately load a cover
        if (coverPosition == 0 && ImageHelper.isOnline(getActivity())) {
            nextCover.performClick();
        }

        return editBook;
    }

    public interface OnEditBookFinished {
        void onEditBookFinished();
    }

    private class AddCoverAsync extends AsyncTask<Void, Void, Bitmap> {
        private final String searchString;
        private final WeakReference<ProgressBar> progressBarWeakReference;
        private final WeakReference<ImageView> imageViewWeakReference;
        private final WeakReference<ImageButton> previousCoverWeakReference;
        private final WeakReference<ImageButton> nextCoverWeakReference;

        public AddCoverAsync(String searchString) {
            this.searchString = searchString;
            progressBarWeakReference = new WeakReference<>(coverReplacement);
            imageViewWeakReference = new WeakReference<ImageView>(coverImageView);
            previousCoverWeakReference = new WeakReference<>(previousCover);
            nextCoverWeakReference = new WeakReference<>(nextCover);
        }

        @Override
        protected Bitmap doInBackground(Void... voids) {
            return coverDownloader.getCover(searchString, googleCount);
        }

        @Override
        protected void onPreExecute() {
            nextCover.setVisibility(View.INVISIBLE);
            if (covers.size() > 0) {
                previousCover.setVisibility(View.VISIBLE);
            }
            coverReplacement.setVisibility(View.VISIBLE);
            coverImageView.setVisibility(View.GONE);
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            ProgressBar coverReplacement = progressBarWeakReference.get();
            ImageView cover = imageViewWeakReference.get();
            ImageButton previousCover = previousCoverWeakReference.get();
            ImageButton nextCover = nextCoverWeakReference.get();

            if (coverReplacement != null && cover != null && previousCover != null && nextCover != null) {
                coverReplacement.setVisibility(View.GONE);
                cover.setVisibility(View.VISIBLE);
                nextCover.setVisibility(View.VISIBLE);
                if (bitmap != null) {
                    covers.add(bitmap);
                    coverPosition = covers.indexOf(bitmap);
                    cover.setImageBitmap(bitmap);
                    if (covers.size() > 1) {
                        previousCover.setVisibility(View.VISIBLE);
                    }
                } else {
                    //if we found no bitmap, set old one
                    cover.setImageBitmap(covers.get(coverPosition));
                    if (coverPosition == 0) {
                        previousCover.setVisibility(View.INVISIBLE);
                    }
                }
            }
        }

        @Override
        protected void onCancelled() {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    coverDownloader.cancel();
                }
            });
        }
    }
}
