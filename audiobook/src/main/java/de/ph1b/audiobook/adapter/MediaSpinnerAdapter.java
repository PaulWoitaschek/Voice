package de.ph1b.audiobook.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import de.ph1b.audiobook.R;
import de.ph1b.audiobook.content.Book;
import de.ph1b.audiobook.content.Media;


public class MediaSpinnerAdapter extends BaseAdapter {

    private final Context c;
    private final ArrayList<Media> data;

    public MediaSpinnerAdapter(Context c, Book book) {
        super();
        this.c = c;
        this.data = book.getContainingMedia();
    }


    @Override
    public int getCount() {
        return data != null ? data.size() : 0;
    }

    @Override
    public Media getItem(int position) {
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return getCustomView(position, convertView, parent);
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return getCustomView(position, convertView, parent);
    }


    private View getCustomView(int position, View convertView, ViewGroup parent) {
        final ViewHolder viewHolder;
        if (convertView == null) {
            LayoutInflater vi = (LayoutInflater) c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = vi.inflate(R.layout.spinner_view, parent, false);

            viewHolder = new ViewHolder();
            viewHolder.textView = (TextView) convertView.findViewById(R.id.spinnerText);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        viewHolder.textView.setText(data.get(position).getName());
        return convertView;
    }

    private static class ViewHolder {
        TextView textView;
    }
}
