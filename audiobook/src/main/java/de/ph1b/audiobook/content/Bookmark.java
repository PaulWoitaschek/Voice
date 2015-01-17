package de.ph1b.audiobook.content;

import android.util.Log;

/**
 * @author <a href="mailto:woitaschek@posteo.de">Paul Woitaschek</a>
 * @link {http://www.paul-woitaschek.de}
 * @see <a href="http://www.paul-woitaschek.de">http://www.paul-woitaschek.de</a>
 */
public class Bookmark {

    private String title;
    private long id;
    private long mediaId;
    private long bookId;
    private int position;

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }
        if (o instanceof Bookmark) {
            Bookmark that = (Bookmark) o;
            Log.d("bm", "comparing:");
            Log.d("bm", mediaId + "/" + bookId + "/" + position);
            Log.d("bm", that.getMediaId() + "/" + that.getBookId() + "/" + that.getPosition());
            return (that.getMediaId() == mediaId && that.getBookId() == bookId
                    && that.getPosition() == position);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (int) mediaId + (int) bookId + position;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public long getMediaId() {
        return mediaId;
    }

    public void setMediaId(long mediaId) {
        this.mediaId = mediaId;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getBookId() {
        return bookId;
    }

    public void setBookId(long bookId) {
        this.bookId = bookId;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }
}
