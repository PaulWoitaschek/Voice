package de.ph1b.audiobook.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import de.ph1b.audiobook.R;
import de.ph1b.audiobook.content.Bookmark;

/**
 * @author <a href="mailto:woitaschek@posteo.de">Paul Woitaschek</a>
 * @link {http://www.paul-woitaschek.de}
 * @see <a href="http://www.paul-woitaschek.de">http://www.paul-woitaschek.de</a>
 */
public class BookmarkAdapter extends BaseAdapter {

    private final Context c;
    private final ArrayList<Bookmark> bookmarks;

    public BookmarkAdapter(Context c, ArrayList<Bookmark> bookmarks) {
        this.c = c;
        this.bookmarks = bookmarks;
    }

    @Override
    public int getCount() {
        return bookmarks.size();
    }

    @Override
    public Bookmark getItem(int position) {
        return bookmarks.get(position);
    }

    public void addItem(Bookmark bookmark) {
        bookmarks.add(bookmark);
        notifyDataSetChanged();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewHolder viewHolder;
        if (convertView == null) {
            LayoutInflater vi = (LayoutInflater) c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = vi.inflate(R.layout.bookmark_adapter_row_layout, parent, false);

            viewHolder = new ViewHolder();
            viewHolder.title = (TextView) convertView.findViewById(R.id.text1);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        viewHolder.title.setText(bookmarks.get(position).getTitle());
        return convertView;
    }

    private static class ViewHolder {
        TextView title;
    }
}
