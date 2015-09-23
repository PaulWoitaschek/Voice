package de.ph1b.audiobook.adapter;

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

import de.ph1b.audiobook.R;
import de.ph1b.audiobook.model.Book;
import de.ph1b.audiobook.model.Bookmark;
import de.ph1b.audiobook.model.Chapter;

/**
 * @author <a href="mailto:woitaschek@posteo.de">Paul Woitaschek</a>
 * @link {http://www.paul-woitaschek.de}
 * @see <a href="http://www.paul-woitaschek.de">http://www.paul-woitaschek.de</a>
 */
public class BookmarkAdapter extends RecyclerView.Adapter<BookmarkAdapter.ViewHolder> {

    @NonNull
    private final Book book;
    @NonNull
    private final OnOptionsMenuClickedListener listener;

    public BookmarkAdapter(@NonNull Book book, @NonNull OnOptionsMenuClickedListener listener) {
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
        int index = book.getBookmarks().indexOf(bookmark);
        book.getBookmarks().remove(bookmark);
        notifyItemRemoved(index);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate
                (R.layout.dialog_bookmark_row_layout, parent, false);
        return new ViewHolder(v, listener);
    }

    public void bookmarkUpdated(Bookmark oldBookmark, Bookmark newBookmark) {
        List<Bookmark> bookmarks = book.getBookmarks();
        int oldIndex = bookmarks.indexOf(oldBookmark);
        bookmarks.set(oldIndex, newBookmark);
        notifyItemChanged(oldIndex);
        Collections.sort(bookmarks);
        notifyItemMoved(oldIndex, bookmarks.indexOf(newBookmark));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Bookmark bookmark = book.getBookmarks().get(position);
        holder.title.setText(bookmark.getTitle());

        int size = book.getChapters().size();
        Chapter currentChapter = null;
        for (Chapter c : book.getChapters()) {
            if (c.getFile().equals(bookmark.getMediaFile())) {
                currentChapter = c;
            }
        }
        if (currentChapter == null) {
            throw new IllegalArgumentException("Current chapter not found with bookmark=" + bookmark);
        }
        int index = book.getChapters().indexOf(currentChapter);

        holder.summary.setText("(" + (index + 1) + "/" + size + ") ");
        holder.time.setText(formatTime(bookmark.getTime()) + " / " + formatTime(currentChapter.getDuration()));
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return book.getBookmarks().size();
    }

    public interface OnOptionsMenuClickedListener {
        void onOptionsMenuClicked(Bookmark bookmark, View v);

        void onBookmarkClicked(Bookmark bookmark);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        final ImageButton imageButton;
        final TextView title;
        final TextView summary;
        final TextView time;

        public ViewHolder(View itemView, final OnOptionsMenuClickedListener listener) {
            super(itemView);
            imageButton = (ImageButton) itemView.findViewById(R.id.edit);
            title = (TextView) itemView.findViewById(R.id.text1);
            summary = (TextView) itemView.findViewById(R.id.text2);
            time = (TextView) itemView.findViewById(R.id.text3);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onBookmarkClicked(book.getBookmarks().get(getAdapterPosition()));
                }
            });

            imageButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onOptionsMenuClicked(book.getBookmarks().get(getAdapterPosition()), imageButton);
                }
            });
        }
    }
}