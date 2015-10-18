package de.ph1b.audiobook.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import de.ph1b.audiobook.R;
import de.ph1b.audiobook.activity.FolderChooserActivity;


/**
 * Adapter for displaying files and folders.
 *
 * @author Paul Woitaschek
 */
public class FolderChooserAdapter extends BaseAdapter {

    private final Context c;
    private final List<File> data;
    private final FolderChooserActivity.OperationMode mode;

    /**
     * Constructor that initializes the class with the necessary values
     *
     * @param c    The context
     * @param data The files to show
     * @param mode The operation mode which defines the interaction.
     */
    public FolderChooserAdapter(@NonNull Context c, @NonNull List<File> data, FolderChooserActivity.OperationMode mode) {
        this.c = c;
        this.data = data;

        if (mode != FolderChooserActivity.OperationMode.SINGLE_BOOK &&
                mode != FolderChooserActivity.OperationMode.COLLECTION_BOOK) {
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

            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        File selectedFile = data.get(position);
        boolean isDirectory = selectedFile.isDirectory();

        viewHolder.textView.setText(selectedFile.getName());

        // if its not a collection its also fine to pick a file
        if (mode == FolderChooserActivity.OperationMode.COLLECTION_BOOK) {
            viewHolder.textView.setEnabled(isDirectory);
        }

        Drawable icon = ContextCompat.getDrawable(c, isDirectory ?
                R.drawable.ic_folder :
                R.drawable.ic_audiotrack_white_48dp);
        viewHolder.imageView.setImageDrawable(icon);
        viewHolder.imageView.setContentDescription(c.getString(isDirectory ?
                R.string.content_is_folder : R.string.content_is_file));

        return convertView;
    }

    static class ViewHolder {
        @Bind(R.id.singleline_text1) TextView textView;
        @Bind(R.id.singleline_image1) ImageView imageView;

        public ViewHolder(View parent) {
            ButterKnife.bind(this, parent);
        }
    }
}
