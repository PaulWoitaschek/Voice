package de.ph1b.audiobook.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewCompat;
import android.support.v7.util.SortedList;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.util.SortedListAdapterCallback;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import de.ph1b.audiobook.R;
import de.ph1b.audiobook.activity.BookShelfActivity;
import de.ph1b.audiobook.model.Book;
import de.ph1b.audiobook.model.NaturalOrderComparator;
import de.ph1b.audiobook.persistence.PrefsManager;
import de.ph1b.audiobook.uitools.CoverReplacement;

public class BookShelfAdapter extends RecyclerView.Adapter<BookShelfAdapter.BaseViewHolder> {

    private final BookShelfActivity.DisplayMode displayMode;
    @NonNull
    private final Context c;
    private final PrefsManager prefs;
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

    public BookShelfAdapter(@NonNull Context c, BookShelfActivity.DisplayMode displayMode, OnItemClickListener onItemClickListener) {
        this.c = c;
        this.onItemClickListener = onItemClickListener;
        this.prefs = PrefsManager.getInstance(c);
        this.displayMode = displayMode;
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

    @NonNull
    public Book getItem(int position) {
        return sortedList.get(position);
    }

    @Override
    public int getItemViewType(int position) {
        return super.getItemViewType(position);
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

    public void notifyItemAtIdChanged(long id) {
        for (int i = 0; i < sortedList.size(); i++) {
            if (sortedList.get(i).getId() == id) {
                notifyItemChanged(i);
                break;
            }
        }
    }

    @Override
    public void onBindViewHolder(final BaseViewHolder viewHolder, int position) {
        viewHolder.bind(position);
    }

    @Override
    public int getItemCount() {
        return sortedList.size();
    }

    public interface OnItemClickListener {
        void onCoverClicked(final int position);

        void onMenuClicked(final int position, View view);
    }

    public class ListViewHolder extends BaseViewHolder {

        private final ProgressBar progressBar;
        private final TextView leftTime;
        private final TextView rightTime;

        public ListViewHolder(ViewGroup parent) {
            super(LayoutInflater.from(parent.getContext()).inflate(
                    R.layout.activity_book_shelf_list_layout, parent, false));
            progressBar = (ProgressBar) itemView.findViewById(R.id.progressBar);
            leftTime = (TextView) itemView.findViewById(R.id.leftTime);
            rightTime = (TextView) itemView.findViewById(R.id.rightTime);
        }

        @Override
        public void bind(int position) {
            super.bind(position);

            Book b = sortedList.get(position);
            int globalPosition = b.getGlobalPosition();
            int globalDuration = b.getGlobalDuration();
            int progress = Math.round(100f * (float) globalPosition / (float) globalDuration);

            leftTime.setText(formatTime(globalPosition));
            progressBar.setProgress(progress);
            rightTime.setText(formatTime(globalDuration));
        }
    }

    public class GridViewHolder extends BaseViewHolder {

        public GridViewHolder(ViewGroup parent) {
            super(LayoutInflater.from(parent.getContext()).inflate(
                    R.layout.activity_book_shelf_grid_layout, parent, false));
        }
    }

    public abstract class BaseViewHolder extends RecyclerView.ViewHolder {
        public final ImageView coverView;
        private final TextView titleView;
        private final View editBook;
        private final ImageView currentPlayingIndicator;

        public BaseViewHolder(View itemView) {
            super(itemView);
            coverView = (ImageView) itemView.findViewById(R.id.coverView);
            titleView = (TextView) itemView.findViewById(R.id.title);
            editBook = itemView.findViewById(R.id.editBook);
            currentPlayingIndicator = (ImageView) itemView.findViewById(R.id.currentPlayingIndicator);
        }

        @CallSuper
        public void bind(int position) {
            Book b = sortedList.get(position);

            //setting text
            String name = b.getName();
            titleView.setText(name);

            // (Cover)
            File coverFile = b.getCoverFile();
            Drawable coverReplacement = new CoverReplacement(b.getName(), c);

            if (!b.isUseCoverReplacement() && coverFile.exists() && coverFile.canRead()) {
                Picasso.with(c).load(coverFile).placeholder(coverReplacement).into(coverView);
            } else {
                Picasso.with(c).cancelRequest(coverView);
                coverView.setImageDrawable(coverReplacement);
            }

            if (b.getId() == prefs.getCurrentBookId()) {
                currentPlayingIndicator.setVisibility(View.VISIBLE);
            } else {
                currentPlayingIndicator.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onItemClickListener.onCoverClicked(getAdapterPosition());
                }
            });
            editBook.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onItemClickListener.onMenuClicked(getAdapterPosition(), v);
                }
            });

            ViewCompat.setTransitionName(coverView, b.getCoverTransitionName());
        }
    }
}