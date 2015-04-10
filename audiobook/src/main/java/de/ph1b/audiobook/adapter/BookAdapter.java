package de.ph1b.audiobook.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
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

import de.ph1b.audiobook.R;
import de.ph1b.audiobook.model.Book;
import de.ph1b.audiobook.uitools.CoverReplacement;

public class BookAdapter extends RecyclerView.Adapter<BookAdapter.ViewHolder> {

    @NonNull
    private final ArrayList<Book> books;
    private final Context c;
    private final OnItemClickListener onItemClickListener;

    public BookAdapter(@NonNull ArrayList<Book> books, Context c, OnItemClickListener onItemClickListener) {
        this.books = books;
        this.c = c;
        this.onItemClickListener = onItemClickListener;

        setHasStableIds(true);
    }

    @Override
    public long getItemId(int position) {
        return books.get(position).getId();
    }

    @NonNull
    public Book getItem(int position) {
        return books.get(position);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int i) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_book_shelf_row_layout, parent, false);
        return new ViewHolder(v, onItemClickListener);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {
        Book b = books.get(position);

        //setting text
        String name = b.getName();
        viewHolder.titleView.setText(name);
        viewHolder.titleView.setActivated(true);

        // (Cover)
        File coverFile = b.getCoverFile();
        Drawable coverReplacement = new CoverReplacement(b.getName().substring(0, 1), c);
        if (!b.isUseCoverReplacement() && coverFile.exists() && coverFile.canRead()) {
            Picasso.with(c).load(coverFile).placeholder(coverReplacement).into(viewHolder.coverView);
        } else {
            Picasso.with(c).cancelRequest(viewHolder.coverView);
            viewHolder.coverView.setImageDrawable(coverReplacement);
        }
    }

    @Override
    public int getItemCount() {
        return books.size();
    }

    public interface OnItemClickListener {
        void onCoverClicked(final int position, final ImageView imageView);

        void onMenuClicked(final int position);

    }

    public class ViewHolder extends RecyclerView.ViewHolder {
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
                    onItemClickListener.onCoverClicked(getAdapterPosition(), coverView);
                }
            });
            editBook.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onItemClickListener.onMenuClicked(getAdapterPosition());
                }
            });
        }
    }
}