package de.ph1b.audiobook.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;

import de.ph1b.audiobook.R;
import de.ph1b.audiobook.utils.ThemeUtil;

public class FolderAdapter extends BaseAdapter {

    private final Context c;
    private final ArrayList<File> data;

    public FolderAdapter(Context c, ArrayList<File> data) {
        super();
        this.c = c;
        this.data = data;
    }

    @Override
    public int getCount() {
        return data != null ? data.size() : 0;
    }

    @Override
    public File getItem(int position) {
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

    @SuppressWarnings("deprecation")
    private View getCustomView(int position, View convertView, ViewGroup parent) {
        final ViewHolder viewHolder;
        if (convertView == null) {
            LayoutInflater vi = (LayoutInflater) c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = vi.inflate(R.layout.folder_adapter_row_layout, parent, false);

            viewHolder = new ViewHolder();
            viewHolder.textView = (TextView) convertView.findViewById(R.id.text1);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        File selectedFile = data.get(position);
        boolean isDirectory = selectedFile.isDirectory();

        viewHolder.textView.setText(selectedFile.getName());

        if (isDirectory) {
            viewHolder.textView.setTextColor(c.getResources().getColor(ThemeUtil.getResourceId(c, R.attr.text_primary)));
        } else {
            viewHolder.textView.setTextColor(c.getResources().getColor(ThemeUtil.getResourceId(c, R.attr.text_secondary)));
        }
        Drawable icon = isDirectory ? c.getResources().getDrawable(ThemeUtil.getResourceId(c, R.attr.folder_choose_folder)) :
                c.getResources().getDrawable(ThemeUtil.getResourceId(c, R.attr.folder_choose_track));
        viewHolder.textView.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);

        return convertView;
    }

    private static class ViewHolder {
        TextView textView;
    }
}
