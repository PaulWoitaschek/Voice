package de.ph1b.audiobook.dialog;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.ph1b.audiobook.R;
import de.ph1b.audiobook.model.Book;
import de.ph1b.audiobook.persistence.DataBaseHelper;
import de.ph1b.audiobook.uitools.CoverDownloader;
import de.ph1b.audiobook.uitools.CoverReplacement;
import de.ph1b.audiobook.uitools.DraggableBoxImageView;
import de.ph1b.audiobook.uitools.ImageHelper;
import de.ph1b.audiobook.utils.App;
import de.ph1b.audiobook.utils.L;

/**
 * Simple dialog to edit the cover of a book.
 */
public class EditCoverDialogFragment extends DialogFragment {
    public static final String TAG = EditCoverDialogFragment.class.getSimpleName();
    private static final String SI_COVER_POSITION = "siCoverPosition";
    private static final String SI_COVER_URLS = "siCoverUrls";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final List<String> imageURLS = new ArrayList<>(20);
    @Bind(R.id.edit_book) DraggableBoxImageView coverImageView;
    @Bind(R.id.cover_replacement) ProgressBar loadingProgressBar;
    @Bind(R.id.previous_cover) ImageButton previousCover;
    @Bind(R.id.next_cover) ImageButton nextCover;
    @Inject DataBaseHelper db;
    private CoverDownloader coverDownloader;
    private AddCoverAsync addCoverAsync;
    /**
     * The position in the {@link #imageURLS} or -1 if it is the {@link #coverReplacement}.
     */
    private int coverPosition = -1;
    private int googleCount = 0;
    @Nullable
    private OnEditBookFinished listener;
    private Book book;
    private CoverReplacement coverReplacement;
    private Picasso picasso;
    private boolean isOnline;

    public static EditCoverDialogFragment newInstance(@NonNull Book book) {
        EditCoverDialogFragment editCoverDialogFragment = new EditCoverDialogFragment();

        Bundle bundle = new Bundle();
        bundle.putLong(Book.TAG, book.id());
        editCoverDialogFragment.setArguments(bundle);

        return editCoverDialogFragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        @SuppressLint("InflateParams") View customView = inflater.inflate(R.layout.dialog_cover_edit, null);
        ButterKnife.bind(this, customView);
        App.getComponent().inject(this);

        // init values
        final long bookId = getArguments().getLong(Book.TAG);
        book = db.getBook(bookId);
        assert book != null;
        coverReplacement = new CoverReplacement(book.name(), getActivity());
        isOnline = ImageHelper.isOnline(getActivity());
        if (savedInstanceState == null) {
            coverPosition = -1;
        } else {
            imageURLS.clear();
            //noinspection ConstantConditions
            imageURLS.addAll(savedInstanceState.getStringArrayList(SI_COVER_URLS));
            coverPosition = savedInstanceState.getInt(SI_COVER_POSITION);
        }
        loadCoverPosition();
        setNextPreviousEnabledDisabled();

        MaterialDialog.SingleButtonCallback positiveCallback = new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {
                L.d(TAG, "edit book positive clicked. CoverPosition=" + coverPosition);
                if (addCoverAsync != null && !addCoverAsync.isCancelled()) {
                    addCoverAsync.cancel(true);
                }

                final Rect r = coverImageView.getSelectedRect();
                boolean useCoverReplacement;
                if (coverPosition > -1 && !r.isEmpty()) {
                    Bitmap cover = ImageHelper.picassoGetBlocking(getActivity(), imageURLS.get(coverPosition));
                    if (cover != null) {
                        cover = Bitmap.createBitmap(cover, r.left, r.top, r.width(), r.height());
                        ImageHelper.saveCover(cover, getActivity(), book.coverFile());

                        picasso.invalidate(book.coverFile());
                        useCoverReplacement = false;
                    } else {
                        useCoverReplacement = true;
                    }
                } else {
                    useCoverReplacement = true;
                }

                //noinspection SynchronizeOnNonFinalField
                synchronized (db) {
                    Book dbBook = db.getBook(book.id());
                    if (dbBook != null) {
                        dbBook = Book.builder(dbBook)
                                .useCoverReplacement(useCoverReplacement)
                                .build();
                        db.updateBook(dbBook);
                    }
                }

                if (listener != null) {
                    listener.onEditBookFinished();
                }
            }
        };
        MaterialDialog.SingleButtonCallback negativeCallback = new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {
                if (addCoverAsync != null && !addCoverAsync.isCancelled()) {
                    addCoverAsync.cancel(true);
                }
            }
        };

        return new MaterialDialog.Builder(getActivity())
                .customView(customView, true)
                .title(R.string.edit_book_cover)
                .positiveText(R.string.dialog_confirm)
                .negativeText(R.string.dialog_cancel)
                .onPositive(positiveCallback)
                .onNegative(negativeCallback)
                .build();
    }

    public void setOnEditBookFinished(@Nullable OnEditBookFinished listener) {
        this.listener = listener;
    }

    /**
     * Loads the current cover and sets progress replacement visibility accordingly.
     */
    private void loadCoverPosition() {
        if (coverPosition == -1) {
            loadingProgressBar.setVisibility(View.GONE);
            coverImageView.setVisibility(View.VISIBLE);
            coverImageView.setImageDrawable(coverReplacement);
        } else {
            loadingProgressBar.setVisibility(View.VISIBLE);
            coverImageView.setVisibility(View.GONE);
            picasso.load(imageURLS.get(coverPosition)).into(coverImageView, new Callback() {
                @Override
                public void onSuccess() {
                    loadingProgressBar.setVisibility(View.GONE);
                    coverImageView.setVisibility(View.VISIBLE);
                }

                @Override
                public void onError() {

                }
            });
        }
    }

    @OnClick({R.id.previous_cover, R.id.next_cover})
    void nextPreviousCoverClicked(View view) {
        switch (view.getId()) {
            case R.id.previous_cover:
                if (addCoverAsync != null && !addCoverAsync.isCancelled()) {
                    addCoverAsync.cancel(true);
                }
                coverPosition--;
                loadCoverPosition();
                setNextPreviousEnabledDisabled();
                break;
            case R.id.next_cover:
                if (coverPosition < imageURLS.size() - 1) {
                    coverPosition++;
                    loadCoverPosition();
                } else {
                    genCoverFromInternet(book.name());
                }
                setNextPreviousEnabledDisabled();
                break;
            default:
                break;
        }
    }

    /**
     * Initiates a search on a cover from the internet and shows it if successful
     *
     * @param searchString the name to search the cover by
     */
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
        picasso = Picasso.with(getActivity());
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(SI_COVER_POSITION, coverPosition);
        outState.putStringArrayList(SI_COVER_URLS, new ArrayList<>(imageURLS));
    }

    /**
     * Sets the next and previous buttons (that navigate within covers) visible / invisible,
     * accordingly to the current position.
     */
    private void setNextPreviousEnabledDisabled() {
        if (coverPosition > -1) {
            previousCover.setVisibility(View.VISIBLE);
        } else {
            previousCover.setVisibility(View.INVISIBLE);
        }

        if (isOnline || (coverPosition + 1 < imageURLS.size())) {
            nextCover.setVisibility(View.VISIBLE);
        } else {
            nextCover.setVisibility(View.INVISIBLE);
        }
    }


    public interface OnEditBookFinished {
        void onEditBookFinished();
    }

    private class AddCoverAsync extends AsyncTask<Void, Void, String> {
        private final String searchString;

        public AddCoverAsync(String searchString) {
            this.searchString = searchString;
        }

        @Override
        protected String doInBackground(Void... voids) {
            return coverDownloader.fetchCover(searchString, googleCount);
        }

        @Override
        protected void onPreExecute() {
            nextCover.setVisibility(View.INVISIBLE);
            if (!imageURLS.isEmpty()) {
                previousCover.setVisibility(View.VISIBLE);
            }
            loadingProgressBar.setVisibility(View.VISIBLE);
            coverImageView.setVisibility(View.GONE);
        }

        @Override
        protected void onPostExecute(String bitmapUrl) {
            if (isAdded()) {
                if (bitmapUrl != null) {
                    imageURLS.add(bitmapUrl);
                    coverPosition = imageURLS.size() - 1;
                }
                loadCoverPosition();
                setNextPreviousEnabledDisabled();
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
