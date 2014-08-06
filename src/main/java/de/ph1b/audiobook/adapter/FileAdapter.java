package de.ph1b.audiobook.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;

import de.ph1b.audiobook.BuildConfig;
import de.ph1b.audiobook.R;
import de.ph1b.audiobook.activity.MediaAdd;

public class FileAdapter extends BaseAdapter {

    private final ArrayList<File> data;
    private final Context c;
    private static boolean[] checked;
    private static final String TAG = "FileAdapter";

    public FileAdapter(ArrayList<File> data, Context c) {
        this.data = data;
        this.c = c;
        checked = new boolean[getCount()];
    }

    @Override
    public int getCount() {
        return data != null ? data.size() : 0;
    }

    @Override
    public Object getItem(int position) {
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final ViewHolder viewHolder;

        if (convertView == null) {
            LayoutInflater vi = (LayoutInflater) c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = vi.inflate(R.layout.file_chooser_listview_layout, parent, false);

            viewHolder = new ViewHolder();
            viewHolder.symbolView = (TextView) convertView.findViewById(R.id.file_symbol);
            viewHolder.nameView = (TextView) convertView.findViewById(R.id.file_name);
            viewHolder.checkBox = (CheckBox) convertView.findViewById(R.id.file_check);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        //setting checkbox tag to identify correct box
        viewHolder.checkBox.setTag(position);

        viewHolder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                int pos = (Integer) buttonView.getTag();
                checked[pos] = isChecked;
                viewHolder.checkBox.setChecked(checked[pos]);

                ArrayList<File> dirAddList = new ArrayList<File>();
                for (int i = 0; i < checked.length; i++) {
                    if (checked[i]) {
                        dirAddList.add(data.get(i));
                        if (BuildConfig.DEBUG)
                            Log.d(TAG, "data is checked: " + data.get(i).getAbsolutePath());
                    }
                }
                ((MediaAdd) c).checkStateChanged(dirAddList);
            }
        });

        //setting correct value
        viewHolder.checkBox.setChecked(checked[position]);

        File f = data.get(position);
        String name = f.getName();

        String capital = name.substring(0, 1).toUpperCase();

        viewHolder.symbolView.setText(capital);
        if (f.isFile()) {
            viewHolder.symbolView.setBackgroundColor(c.getResources().getColor(R.color.file_chooser_audio));
        } else {
            viewHolder.symbolView.setBackgroundColor(c.getResources().getColor(R.color.file_chooser_folder));
        }
        viewHolder.nameView.setText(name);

        return convertView;
    }

    public void clearCheckBoxes() {
        if (BuildConfig.DEBUG) Log.d(TAG, "clearCheckBoxes() was called!");
        //for (int i = 0; i < getCount(); i++)
        //  checked[i] = false;
        checked = new boolean[getCount()];
        notifyDataSetChanged();
    }

    static class ViewHolder {
        TextView symbolView;
        TextView nameView;
        CheckBox checkBox;
    }
}