package de.ph1b.audiobook.adapter;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import de.ph1b.audiobook.R;
import de.ph1b.audiobook.content.Bookmark;
import de.ph1b.audiobook.content.MediaDetail;

/**
 * @author <a href="mailto:woitaschek@posteo.de">Paul Woitaschek</a>
 * @link {http://www.paul-woitaschek.de}
 * @see <a href="http://www.paul-woitaschek.de">http://www.paul-woitaschek.de</a>
 */
public class BookmarkAdapter extends RecyclerView.Adapter<BookmarkAdapter.ViewHolder> {

    private final ArrayList<Bookmark> bookmarks;
    private final OnOptionsMenuClickedListener listener;
    private final ArrayList<MediaDetail> allMedia;

    public BookmarkAdapter(ArrayList<Bookmark> bookmarks, OnOptionsMenuClickedListener listener,
                           ArrayList<MediaDetail> allMedia) {
        this.bookmarks = bookmarks;
        this.listener = listener;
        this.allMedia = allMedia;
    }

    /**
     * Adds an item and scrolls to that one.
     *
     * @param bookmark The bookmark to be added
     */
    public int addItem(Bookmark bookmark) {
        Log.d("bma", "added bookmark has index: " + bookmark.getMediaId());
        int preferredIndex = 0;
        for (int i = 0; i < bookmarks.size(); i++) {
            Bookmark atIndex = bookmarks.get(i);
            if ((bookmark.getMediaId() > atIndex.getMediaId()) ||
                    ((atIndex.getMediaId() == bookmark.getMediaId()) &&
                            (atIndex.getPosition() < bookmark.getPosition()))) {
                Log.d("bma", "swapping:" + atIndex.getMediaId());
                preferredIndex = i + 1;
            }
        }

        bookmarks.add(preferredIndex, bookmark);
        notifyItemInserted(preferredIndex);
        return preferredIndex;
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
        Bookmark b = bookmarks.get(position);
        holder.title.setText(b.getTitle());

        int size = allMedia.size();
        int index = 1;
        for (int i = 0; i < allMedia.size(); i++) {
            if (allMedia.get(i).getId() == b.getMediaId()) {
                index = i;
            }
        }
        holder.summary.setText("(" + (index + 1) + "/" + size + ") " + formatTime(b.getPosition()) + " /"
                + formatTime(allMedia.get(index).getDuration()));
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

        public ViewHolder(View itemView, final OnOptionsMenuClickedListener listener) {
            super(itemView);
            imageButton = (ImageButton) itemView.findViewById(R.id.edit);
            title = (TextView) itemView.findViewById(R.id.text1);
            summary = (TextView) itemView.findViewById(R.id.text2);

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