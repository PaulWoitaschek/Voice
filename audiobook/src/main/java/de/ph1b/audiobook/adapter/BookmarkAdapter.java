package de.ph1b.audiobook.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.ph1b.audiobook.R;
import de.ph1b.audiobook.model.Book;
import de.ph1b.audiobook.model.Bookmark;
import de.ph1b.audiobook.model.Chapter;
import de.ph1b.audiobook.utils.App;

/**
 * Adapter for displaying a list of bookmarks.
 *
 * @author Paul Woitaschek
 */
public class BookmarkAdapter extends RecyclerView.Adapter<BookmarkAdapter.ViewHolder> {

    @NonNull
    private final Book book;
    @NonNull
    private final OnOptionsMenuClickedListener listener;
    @Inject Context c;


    public BookmarkAdapter(@NonNull Book book, @NonNull OnOptionsMenuClickedListener listener) {
        App.getComponent().inject(this);
        this.book = book;
        this.listener = listener;
    }

    @NonNull
    private static String formatTime(int ms) {
        String h = String.valueOf(TimeUnit.MILLISECONDS.toHours(ms));
        String m = String.format("%02d", (TimeUnit.MILLISECONDS.toMinutes(ms) % 60));
        String s = String.format("%02d", (TimeUnit.MILLISECONDS.toSeconds(ms) % 60));
        String returnString = "";
        if (!h.equals("0")) {
            returnString += h + ":";
        }
        returnString += m + ":" + s;
        return returnString;
    }

    public void removeItem(Bookmark bookmark) {
        int index = book.bookmarks().indexOf(bookmark);
        book.bookmarks().remove(bookmark);
        notifyItemRemoved(index);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate
                (R.layout.dialog_bookmark_row_layout, parent, false);
        return new ViewHolder(v, listener);
    }

    public void bookmarkUpdated(Bookmark oldBookmark, Bookmark newBookmark) {
        List<Bookmark> bookmarks = book.bookmarks();
        int oldIndex = bookmarks.indexOf(oldBookmark);
        bookmarks.set(oldIndex, newBookmark);
        notifyItemChanged(oldIndex);
        Collections.sort(bookmarks);
        notifyItemMoved(oldIndex, bookmarks.indexOf(newBookmark));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Bookmark bookmark = book.bookmarks().get(position);
        holder.title.setText(bookmark.title());

        int size = book.chapters().size();
        Chapter currentChapter = null;
        for (Chapter c : book.chapters()) {
            if (c.file().equals(bookmark.mediaFile())) {
                currentChapter = c;
            }
        }
        if (currentChapter == null) {
            throw new IllegalArgumentException("Current chapter not found with bookmark=" + bookmark);
        }
        int index = book.chapters().indexOf(currentChapter);


        holder.summary.setText(c.getString(R.string.format_bookmarks_n_of, index + 1, size));
        holder.time.setText(c.getString(R.string.format_bookmarks_time, formatTime(bookmark.time()),
                formatTime(currentChapter.duration())));
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return book.bookmarks().size();
    }

    public interface OnOptionsMenuClickedListener {
        void onOptionsMenuClicked(Bookmark bookmark, View v);

        void onBookmarkClicked(Bookmark bookmark);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        @Bind(R.id.edit) ImageButton imageButton;
        @Bind(R.id.text1) TextView title;
        @Bind(R.id.text2) TextView summary;
        @Bind(R.id.text3) TextView time;

        public ViewHolder(View itemView, final OnOptionsMenuClickedListener listener) {
            super(itemView);
            ButterKnife.bind(this, itemView);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onBookmarkClicked(book.bookmarks().get(getAdapterPosition()));
                }
            });
        }

        @OnClick(R.id.edit)
        void optionsMenuClicked() {
            listener.onOptionsMenuClicked(book.bookmarks().get(getAdapterPosition()), imageButton);
        }
    }
}