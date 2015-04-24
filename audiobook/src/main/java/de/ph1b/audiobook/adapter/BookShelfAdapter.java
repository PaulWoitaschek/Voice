package de.ph1b.audiobook.adapter;

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
import de.ph1b.audiobook.utils.BaseApplication;

public class BookShelfAdapter extends RecyclerView.Adapter<BookShelfAdapter.ViewHolder> implements BaseApplication.OnBooksChangedListener {

    @NonNull
    private final ArrayList<Book> books;
    private final BaseApplication baseApplication;
    private final OnItemClickListener onItemClickListener;
    private int currentPlayingIndex = -1;

    public BookShelfAdapter(@NonNull ArrayList<Book> books, BaseApplication baseApplication,
                            OnItemClickListener onItemClickListener) {
        this.books = books;
        this.baseApplication = baseApplication;
        Book currentBook = baseApplication.getCurrentBook();
        if (currentBook != null) {
            this.currentPlayingIndex = books.indexOf(baseApplication.getCurrentBook());
        }
        this.onItemClickListener = onItemClickListener;

        setHasStableIds(true);
    }

    public void registerListener() {
        baseApplication.addOnBooksChangedListener(this);
    }

    public void unregisterListener() {
        baseApplication.removeOnBooksChangedListener(this);
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
        ViewGroup v = (ViewGroup) LayoutInflater.from(parent.getContext()).inflate(
                R.layout.fragment_book_shelf_row_layout, parent, false);
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
        Drawable coverReplacement = new CoverReplacement(b.getName().substring(0, 1), baseApplication);
        if (!b.isUseCoverReplacement() && coverFile.exists() && coverFile.canRead()) {
            Picasso.with(baseApplication).load(coverFile).placeholder(coverReplacement).into(viewHolder.coverView);
        } else {
            Picasso.with(baseApplication).cancelRequest(viewHolder.coverView);
            viewHolder.coverView.setImageDrawable(coverReplacement);
        }

        if (currentPlayingIndex == position) {
            viewHolder.currentPlayingIndicator.setVisibility(View.VISIBLE);
        } else {
            viewHolder.currentPlayingIndicator.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return books.size();
    }

    @Override
    public void onBookDeleted(int position) {

    }

    @Override
    public void onPlayStateChanged(BaseApplication.PlayState state) {

    }

    @Override
    public void onPositionChanged(boolean fileChanged) {

    }

    @Override
    public void onSleepStateChanged(boolean active) {

    }

    @Override
    public void onCurrentBookChanged(Book book) {
        int oldIndex = currentPlayingIndex;
        currentPlayingIndex = books.indexOf(book);
        notifyItemChanged(oldIndex);
        notifyItemChanged(currentPlayingIndex);
    }

    @Override
    public void onBookAdded(int position) {

    }

    @Override
    public void onScannerStateChanged(boolean active) {

    }

    @Override
    public void onCoverChanged(int position) {

    }

    public interface OnItemClickListener {
        void onCoverClicked(final int position, ImageView cover);

        void onMenuClicked(final int position);

    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView coverView;
        final TextView titleView;
        final ImageButton editBook;
        final ImageView currentPlayingIndicator;

        public ViewHolder(final ViewGroup itemView, final OnItemClickListener onItemClickListener) {
            super(itemView);
            coverView = (ImageView) itemView.findViewById(R.id.cover);
            titleView = (TextView) itemView.findViewById(R.id.title);
            editBook = (ImageButton) itemView.findViewById(R.id.editBook);
            currentPlayingIndicator = (ImageView) itemView.findViewById(R.id.currentPlayingIndicator);

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