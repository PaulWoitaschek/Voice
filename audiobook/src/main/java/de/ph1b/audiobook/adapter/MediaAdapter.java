package de.ph1b.audiobook.adapter;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.ph1b.audiobook.R;
import de.ph1b.audiobook.model.Book;
import de.ph1b.audiobook.model.DataBaseHelper;
import de.ph1b.audiobook.utils.L;

public class MediaAdapter extends RecyclerView.Adapter<MediaAdapter.ViewHolder> {

    private static final String TAG = MediaAdapter.class.getSimpleName();
    private final ArrayList<Book> books;
    private final DataBaseHelper db;
    private final Context c;
    private final OnItemClickListener onItemClickListener;
    private final ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();

    public MediaAdapter(ArrayList<Book> books, Activity a, OnItemClickListener onItemClickListener) {
        this.books = books;
        this.c = a;
        this.onItemClickListener = onItemClickListener;
        db = DataBaseHelper.getInstance(a);
    }

    public void updateItem(final Book book) {
        for (int position = 0; position < books.size(); position++) {
            if (books.get(position).getId() == book.getId()) {
                books.set(position, book);
                notifyItemChanged(position);
                singleThreadExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        db.updateBook(book);
                    }
                });
            }
        }
    }

    public Book getItem(int position) {
        return books.get(position);
    }

    public ArrayList<Book> getBooks() {
        return books;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int i) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.media_chooser_recycler_grid_item, parent, false);
        return new ViewHolder(v, onItemClickListener);
    }

    /**
     * Swaps elements in the detailList and saves them to the database.
     *
     * @param from The first book to swap
     * @param to   The second book to swap
     */
    public void swapItems(int from, int to) {
        L.v(TAG, "swap items from to" + String.valueOf(from) + "/" + String.valueOf(to));
        final int finalFrom = from;
        if (from != to) {
            if (from > to) {
                while (from > to) {
                    swapItemsInData(from, --from);
                }
            } else {
                while (from < to) {
                    swapItemsInData(from, ++from);
                }
            }
        }
        notifyItemMoved(finalFrom, to);
    }

    private void swapItemsInData(int from, int to) {
        L.d(TAG, "swapInData:" + from + "/" + to);
        final Book oldBook = books.get(from);
        final Book newBook = books.get(to);
        long oldSortId = oldBook.getSortId();
        long newSortId = newBook.getSortId();
        oldBook.setSortId(newSortId);
        newBook.setSortId(oldSortId);
        books.set(from, newBook);
        books.set(to, oldBook);
        singleThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                db.updateBook(newBook);
                db.updateBook(oldBook);
            }
        });
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {
        Book b = books.get(position);

        //setting text
        String name = b.getName();
        viewHolder.titleView.setText(name);
        viewHolder.titleView.setActivated(true);
        if (b.getCover() != null) {
            Picasso.with(c).load(new File(b.getCover())).into(viewHolder.coverView);
        }
    }

    @Override
    public int getItemCount() {
        return books.size();
    }

    public interface OnItemClickListener {
        public void onCoverClicked(int position);

        public void onMenuClicked(int position);

    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView coverView;
        final TextView titleView;
        final ImageButton editBook;

        public ViewHolder(View itemView, final OnItemClickListener onItemClickListener) {
            super(itemView);
            coverView = (ImageView) itemView.findViewById(R.id.cover);
            titleView = (TextView) itemView.findViewById(R.id.title);
            editBook = (ImageButton) itemView.findViewById(R.id.editBook);

            coverView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onItemClickListener.onCoverClicked(getPosition());
                }
            });
            editBook.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onItemClickListener.onMenuClicked(getPosition());
                }
            });
        }
    }
}