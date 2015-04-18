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
import de.ph1b.audiobook.activity.FolderChooserActivity;
import de.ph1b.audiobook.uitools.ThemeUtil;

public class FolderChooserAdapter extends BaseAdapter {

    private final Context c;
    private final ArrayList<File> data;
    private final int mode;

    public FolderChooserAdapter(final @NonNull Context c, final @NonNull ArrayList<File> data,
                                final int mode) {
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
            LayoutInflater vi = (LayoutInflater) c.getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
            convertView = vi.inflate(R.layout.activity_folder_chooser_adapter_row_layout, parent,
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

        Drawable icon;
        if (isDirectory) {
            //noinspection deprecation
            icon = c.getResources().getDrawable(
                    ThemeUtil.getResourceId(c, R.attr.folder_choose_folder));
        } else {
            //noinspection deprecation
            icon = c.getResources().getDrawable(
                    ThemeUtil.getResourceId(c, R.attr.folder_choose_track));
        }
        viewHolder.imageView.setImageDrawable(icon);

        return convertView;
    }

    private static class ViewHolder {
        TextView textView;
        ImageView imageView;
    }
}
