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
import java.util.List;

import de.ph1b.audiobook.R;
import de.ph1b.audiobook.activity.FolderChooserActivity;

public class FolderChooserAdapter extends BaseAdapter {

    private final Context c;
    private final List<File> data;
    private final int mode;

    public FolderChooserAdapter(@NonNull Context c, @NonNull List<File> data, int mode) {
        this.c = c;
        this.data = data;

        if (mode != FolderChooserActivity.ACTIVITY_FOR_RESULT_CODE_SINGLE_BOOK &&
                mode != FolderChooserActivity.ACTIVITY_FOR_RESULT_CODE_COLLECTION) {
            throw new IllegalArgumentException("Invalid mode=" + mode);
        }
        this.mode = mode;
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    @NonNull
    public File getItem(final int position) {
        return data.get(position);
    }

    @Override
    public long getItemId(final int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, final ViewGroup parent) {
        final ViewHolder viewHolder;
        if (convertView == null) {
            LayoutInflater li = LayoutInflater.from(c);
            convertView = li.inflate(R.layout.activity_folder_chooser_adapter_row_layout, parent,
                    false);

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

        // if its not a collection its also fine to pick a file
        if (mode == FolderChooserActivity.ACTIVITY_FOR_RESULT_CODE_COLLECTION) {
            viewHolder.textView.setEnabled(isDirectory);
        }

        //noinspection deprecation
        Drawable icon = c.getResources().getDrawable(isDirectory ?
                R.drawable.ic_folder_white_48dp :
                R.drawable.ic_audiotrack_white_48dp);
        viewHolder.imageView.setImageDrawable(icon);

        return convertView;
    }

    private static class ViewHolder {
        TextView textView;
        ImageView imageView;
    }
}
