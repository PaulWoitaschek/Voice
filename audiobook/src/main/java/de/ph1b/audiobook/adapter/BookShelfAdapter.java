package de.ph1b.audiobook.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.util.SortedList;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.util.SortedListAdapterCallback;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.afollestad.materialdialogs.internal.MDTintHelper;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import de.ph1b.audiobook.R;
import de.ph1b.audiobook.fragment.BookShelfFragment;
import de.ph1b.audiobook.model.Book;
import de.ph1b.audiobook.model.NaturalOrderComparator;
import de.ph1b.audiobook.persistence.PrefsManager;
import de.ph1b.audiobook.uitools.CoverReplacement;
import de.ph1b.audiobook.utils.App;

/**
 * Adapter for a recycler-view book shelf that keeps the items in a sorted list.
 */
public class BookShelfAdapter extends RecyclerView.Adapter<BookShelfAdapter.BaseViewHolder> {

    private final BookShelfFragment.DisplayMode displayMode;
    @NonNull
    private final Context c;
    private final OnItemClickListener onItemClickListener;
    private final SortedList<Book> sortedList = new SortedList<>(Book.class, new SortedListAdapterCallback<Book>(this) {

        @Override
        public int compare(Book o1, Book o2) {
            return NaturalOrderComparator.naturalCompare(o1.getName(), o2.getName());
        }

        @Override
        public boolean areContentsTheSame(Book oldItem, Book newItem) {
            return oldItem.getGlobalPosition() == newItem.getGlobalPosition()
                    && oldItem.getName().equals(newItem.getName())
                    && oldItem.isUseCoverReplacement() == newItem.isUseCoverReplacement();
        }

        @Override
        public boolean areItemsTheSame(Book item1, Book item2) {
            return item1.getId() == item2.getId();
        }
    });
    @Inject PrefsManager prefs;

    /**
     * @param c                   the context
     * @param displayMode         the display mode
     * @param onItemClickListener the listener that will be called when a book has been selected
     */
    public BookShelfAdapter(@NonNull Context c, BookShelfFragment.DisplayMode displayMode, OnItemClickListener onItemClickListener) {
        this.c = c;
        this.onItemClickListener = onItemClickListener;
        this.displayMode = displayMode;
        App.getComponent().inject(this);
        setHasStableIds(true);
    }

    private static String formatTime(int ms) {
        String h = String.format("%02d", (TimeUnit.MILLISECONDS.toHours(ms)));
        String m = String.format("%02d", (TimeUnit.MILLISECONDS.toMinutes(ms) % 60));
        return h + ":" + m;
    }

    /**
     * Adds a book or updates it if it already exists.
     *
     * @param book The new book
     */
    public void updateOrAddBook(@NonNull Book book) {
        int index = -1;
        for (int i = 0; i < sortedList.size(); i++) {
            if (sortedList.get(i).getId() == book.getId()) {
                index = i;
                break;
            }
        }

        if (index == -1) {
            sortedList.add(book); // add it if it doesnt exist
        } else {
            sortedList.updateItemAt(index, book); // update it if it exists
        }
    }

    /**
     * Adds a new set of books and removes the ones that do not exist any longer
     *
     * @param books The new set of books
     */
    public void newDataSet(@NonNull List<Book> books) {
        sortedList.beginBatchedUpdates();
        try {
            // remove old books
            List<Book> booksToDelete = new ArrayList<>(sortedList.size());
            for (int i = 0; i < sortedList.size(); i++) {
                Book existing = sortedList.get(i);
                boolean deleteBook = true;
                for (Book b : books) {
                    if (existing.getId() == b.getId()) {
                        deleteBook = false;
                        break;
                    }
                }
                if (deleteBook) {
                    booksToDelete.add(existing);
                }
            }
            for (Book b : booksToDelete) {
                sortedList.remove(b);
            }

            // add new books
            for (Book b : books) {
                updateOrAddBook(b);
            }

        } finally {
            sortedList.endBatchedUpdates();
        }
    }

    @Override
    public long getItemId(int position) {
        return sortedList.get(position).getId();
    }

    /**
     * Gets the item at a requested position
     *
     * @param position the adapter position
     * @return the book at the position
     */
    @NonNull
    public Book getItem(int position) {
        return sortedList.get(position);
    }

    @Override
    public BaseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (displayMode) {
            case GRID:
                return new GridViewHolder(parent);
            case LIST:
                return new ListViewHolder(parent);
            default:
                throw new IllegalStateException("Illegal viewType=" + viewType);
        }
    }

    /**
     * Calls {@link #notifyItemChanged(int)} for a specified id
     *
     * @param id the id of the item
     */
    public void notifyItemAtIdChanged(long id) {
        for (int i = 0; i < sortedList.size(); i++) {
            if (sortedList.get(i).getId() == id) {
                notifyItemChanged(i);
                break;
            }
        }
    }

    @Override
    public void onBindViewHolder(final BaseViewHolder holder, int position) {
        holder.bind(sortedList.get(position));
    }

    @Override
    public int getItemCount() {
        return sortedList.size();
    }

    public interface OnItemClickListener {
        /**
         * This method will be invoked when a item has been clicked
         *
         * @param position adapter position of the item
         */
        void onItemClicked(final int position);

        /**
         * This method will be invoked when the menu of an item has been clicked
         *
         * @param position The adapter position
         * @param view     The view that was clicked
         */
        void onMenuClicked(final int position, final View view);
    }

    public class ListViewHolder extends BaseViewHolder {

        @Bind(R.id.progressBar) ProgressBar progressBar;
        @Bind(R.id.leftTime) TextView leftTime;
        @Bind(R.id.rightTime) TextView rightTime;

        /**
         * Constructor for a list viewholder
         *
         * @param parent The parent view
         */
        public ListViewHolder(ViewGroup parent) {
            super(LayoutInflater.from(parent.getContext()).inflate(
                    R.layout.fragment_book_shelf_list_layout, parent, false));
            ButterKnife.bind(this, itemView);
            MDTintHelper.setTint(progressBar, ContextCompat.getColor(parent.getContext(), R.color.accent));
        }

        @Override
        public void bind(Book book) {
            super.bind(book);

            int globalPosition = book.getGlobalPosition();
            int globalDuration = book.getGlobalDuration();
            int progress = Math.round(100f * (float) globalPosition / (float) globalDuration);

            leftTime.setText(formatTime(globalPosition));
            progressBar.setProgress(progress);
            rightTime.setText(formatTime(globalDuration));
        }
    }

    public class GridViewHolder extends BaseViewHolder {

        /**
         * Constructor for a new grid viewholder
         *
         * @param parent The parent
         */
        public GridViewHolder(ViewGroup parent) {
            super(LayoutInflater.from(parent.getContext()).inflate(
                    R.layout.fragment_book_shelf_grid_layout, parent, false));
        }
    }

    public abstract class BaseViewHolder extends RecyclerView.ViewHolder {
        @Bind(R.id.coverView) public ImageView coverView;
        @Bind(R.id.currentPlayingIndicator) ImageView currentPlayingIndicator;
        @Bind(R.id.title) TextView titleView;
        @Bind(R.id.editBook) View editBook;

        /**
         * Constructor of a viewholder.
         *
         * @param itemView The view to bind to
         */
        public BaseViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        /**
         * Binds the ViewHolder to a book
         *
         * @param book The book to bind to
         */
        @CallSuper
        public void bind(Book book) {

            //setting text
            String name = book.getName();
            titleView.setText(name);

            // (Cover)
            final File coverFile = book.getCoverFile();
            final Drawable coverReplacement = new CoverReplacement(book.getName(), c);

            if (!book.isUseCoverReplacement() && coverFile.exists() && coverFile.canRead()) {
                Picasso.with(c).load(coverFile).placeholder(coverReplacement).into(coverView);
            } else {
                Picasso.with(c).cancelRequest(coverView);
                // we have to set the replacement in onPreDraw, else the transition will fail.
                coverView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        coverView.getViewTreeObserver().removeOnPreDrawListener(this);
                        coverView.setImageDrawable(coverReplacement);
                        return true;
                    }
                });
            }

            if (book.getId() == prefs.getCurrentBookId()) {
                currentPlayingIndicator.setVisibility(View.VISIBLE);
            } else {
                currentPlayingIndicator.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onItemClickListener.onItemClicked(getAdapterPosition());
                }
            });
            editBook.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onItemClickListener.onMenuClicked(getAdapterPosition(), v);
                }
            });

            ViewCompat.setTransitionName(coverView, book.getCoverTransitionName());
        }
    }
}