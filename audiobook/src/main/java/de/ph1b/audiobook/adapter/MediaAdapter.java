package de.ph1b.audiobook.adapter;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;

import de.ph1b.audiobook.R;
import de.ph1b.audiobook.utils.BookDetail;
import de.ph1b.audiobook.utils.CommonTasks;
import de.ph1b.audiobook.utils.DataBaseHelper;


public class MediaAdapter extends BaseAdapter {

    private final ArrayList<BookDetail> data;
    private final Context c;
    private final DataBaseHelper db;


    public MediaAdapter(ArrayList<BookDetail> data, Context c) {
        this.data = data;
        this.c = c;
        db = DataBaseHelper.getInstance(c);
    }

    public int getCount() {
        return data != null ? data.size() : 0;
    }

    public Object getItem(int position) {
        return data.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewHolder viewHolder;
        if (convertView == null) {
            LayoutInflater vi = (LayoutInflater) c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = vi.inflate(R.layout.media_chooser_listview_layout, parent, false);

            viewHolder = new ViewHolder();
            viewHolder.iconImageView = (ImageView) convertView.findViewById(R.id.icon);
            viewHolder.textView = (TextView) convertView.findViewById(R.id.name);
            viewHolder.progressBar = (ProgressBar) convertView.findViewById(R.id.current_progress);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        final BookDetail b = data.get(position);

        //setting text
        String name = b.getName();
        viewHolder.textView.setText(name);

        String thumbPath = b.getThumb();
        if (thumbPath.equals("") || new File(thumbPath).isDirectory() || !(new File(thumbPath).exists())) {
            int px = CommonTasks.convertDpToPx(c.getResources().getDimension(R.dimen.thumb_size), c.getResources());
            viewHolder.iconImageView.setImageBitmap(CommonTasks.genCapital(b.getName(), px, c.getResources()));
        } else {
            viewHolder.iconImageView.setImageURI(Uri.parse(thumbPath));
        }

        //setting bar
        viewHolder.progressBar.setMax(1000);
        viewHolder.progressBar.setProgress(db.getGlobalProgress(b.getId()));

        return convertView;
    }


    static class ViewHolder {
        ImageView iconImageView;
        TextView textView;
        ProgressBar progressBar;
    }
}