package de.ph1b.audiobook.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewCompat;
import android.support.v7.util.SortedList;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.util.SortedListAdapterCallback;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import de.ph1b.audiobook.R;
import de.ph1b.audiobook.model.Book;
import de.ph1b.audiobook.model.NaturalOrderComparator;
import de.ph1b.audiobook.persistence.PrefsManager;
import de.ph1b.audiobook.uitools.CoverReplacement;

public class BookShelfAdapter extends RecyclerView.Adapter<BookShelfAdapter.ViewHolder> {

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
            return oldItem.getName().equals(newItem.getName()) &&
                    oldItem.isUseCoverReplacement() == newItem.isUseCoverReplacement();
        }

        @Override
        public boolean areItemsTheSame(Book item1, Book item2) {
            return item1.getId() == item2.getId();
        }
    });

    public BookShelfAdapter(@NonNull Context c, OnItemClickListener onItemClickListener) {
        this.c = c;
        this.onItemClickListener = onItemClickListener;
        this.prefs = PrefsManager.getInstance(c);

        setHasStableIds(true);
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
    public ViewHolder onCreateViewHolder(ViewGroup parent, int i) {
        ViewGroup v = (ViewGroup) LayoutInflater.from(parent.getContext()).inflate(
                R.layout.activity_book_shelf_row_layout, parent, false);
        return new ViewHolder(v);
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
    public void onBindViewHolder(final ViewHolder viewHolder, int position) {
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

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final ImageView coverView;
        private final TextView titleView;
        private final View editBook;
        private final ImageView currentPlayingIndicator;
        private final View view;

        public ViewHolder(final ViewGroup itemView) {
            super(itemView);
            this.view = itemView;
            coverView = (ImageView) itemView.findViewById(R.id.coverView);
            titleView = (TextView) itemView.findViewById(R.id.title);
            editBook = itemView.findViewById(R.id.editBook);
            currentPlayingIndicator = (ImageView) itemView.findViewById(R.id.currentPlayingIndicator);
        }

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

            view.setOnClickListener(new View.OnClickListener() {
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