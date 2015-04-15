package de.ph1b.audiobook.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;

import de.ph1b.audiobook.R;
import de.ph1b.audiobook.uitools.ThemeUtil;

public class FolderAdapter extends BaseAdapter {

    private final Context c;
    private final ArrayList<File> data;

    public FolderAdapter(@NonNull Context c, @NonNull ArrayList<File> data) {
        this.c = c;
        this.data = data;
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    @NonNull
    public File getItem(int position) {
        return data.get(position);
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
            convertView = vi.inflate(R.layout.list_single_text_with_icon, parent, false);

            viewHolder = new ViewHolder();
            viewHolder.textView = (TextView) convertView.findViewById(R.id.singleline_text1);
            viewHolder.imageView = (ImageView) convertView.findViewById(R.id.singleline_image1);

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
        Drawable icon;
        if (isDirectory) {
            //noinspection deprecation
            icon = c.getResources().getDrawable(ThemeUtil.getResourceId(c, R.attr.folder_choose_folder));
        } else {
            //noinspection deprecation
            icon = c.getResources().getDrawable(ThemeUtil.getResourceId(c, R.attr.folder_choose_track));
        }
        viewHolder.imageView.setImageDrawable(icon);

        return convertView;
    }

    private static class ViewHolder {
        TextView textView;
        ImageView imageView;
    }
}
