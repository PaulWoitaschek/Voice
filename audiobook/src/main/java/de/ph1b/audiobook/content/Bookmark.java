package de.ph1b.audiobook.content;

import android.support.annotation.NonNull;

/**
 * @author <a href="mailto:woitaschek@posteo.de">Paul Woitaschek</a>
 * @link {http://www.paul-woitaschek.de}
 * @see <a href="http://www.paul-woitaschek.de">http://www.paul-woitaschek.de</a>
 */
public class Bookmark implements Comparable<Bookmark> {

    private final int time;
    private final long bookId;
    private final int position;
    private String title;
    private long id;

    public Bookmark(long bookId, int position, int time) {
        this.bookId = bookId;
        this.position = position;
        this.time = time;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }
        if (o instanceof Bookmark) {
            Bookmark that = (Bookmark) o;
            return (that.time == time && that.bookId == bookId
                    && that.position == position);
        }
        return false;
    }


    @Override
    public int hashCode() {
        return time + (int) bookId + position;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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

    public int getPosition() {
        return position;
    }

    public int getTime() {
        return time;
    }


    /**
     * @param another the Bookmark to be compared
     * @return -1 if position in book is smaller, 0 if its the same position in the book or +1 if its
     * a later position in the book
     */

    @Override
    public int compareTo(@NonNull Bookmark another) {
        if (bookId > another.bookId) {
            return 1;
        } else if (bookId < another.bookId) {
            return -1;
        }

        if (position > another.position) {
            return 1;
        } else if (position < another.position) {
            return -1;
        }

        if (time > another.time) {
            return 1;
        } else if (time < another.time) {
            return -1;
        }

        return 0;
    }
}
