package de.ph1b.audiobook.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import de.ph1b.audiobook.R;
import de.ph1b.audiobook.content.Book;
import de.ph1b.audiobook.content.Bookmark;
import de.ph1b.audiobook.content.Media;

/**
 * @author <a href="mailto:woitaschek@posteo.de">Paul Woitaschek</a>
 * @link {http://www.paul-woitaschek.de}
 * @see <a href="http://www.paul-woitaschek.de">http://www.paul-woitaschek.de</a>
 */
public class BookmarkAdapter extends RecyclerView.Adapter<BookmarkAdapter.ViewHolder> {

    private final ArrayList<Bookmark> bookmarks;
    private final OnOptionsMenuClickedListener listener;
    private final ArrayList<Media> allMedia;

    public BookmarkAdapter(ArrayList<Bookmark> bookmarks, OnOptionsMenuClickedListener listener,
                           Book book) {
        this.bookmarks = bookmarks;
        this.listener = listener;
        this.allMedia = book.getContainingMedia();
    }

    /**
     * @param bookmark The bookmark to be added
     * @return The position at which the bookmark was added.
     */
    public int addItem(Bookmark bookmark) {
        bookmarks.add(bookmark);
        Collections.sort(bookmarks);
        int index = bookmarks.indexOf(bookmark);
        notifyItemInserted(index);
        return index;
    }

    public void removeItem(int position) {
        bookmarks.remove(position);
        notifyItemRemoved(position);
    }

    public Bookmark getItem(int position) {
        return bookmarks.get(position);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate
                (R.layout.bookmark_adapter_row_layout, parent, false);
        return new ViewHolder(v, listener);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Bookmark bookmark = bookmarks.get(position);
        holder.title.setText(bookmark.getTitle());

        int size = allMedia.size();
        holder.summary.setText("(" + (bookmark.getPosition() + 1) + "/" + size + ") ");
        holder.time.setText(formatTime(bookmark.getTime()) + " / " + formatTime(allMedia.get(bookmark.getPosition()).getDuration()));
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    private String formatTime(int ms) {
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

    @Override
    public int getItemCount() {
        return bookmarks.size();
    }

    public interface OnOptionsMenuClickedListener {
        public void onOptionsMenuClicked(int position, View v);

        public void onBookmarkClicked(int position);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

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
                    listener.onBookmarkClicked(getPosition());
                }
            });

            imageButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onOptionsMenuClicked(getPosition(), imageButton);
                }
            });
        }
    }
}