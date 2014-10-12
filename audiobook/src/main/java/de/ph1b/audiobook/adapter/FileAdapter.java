package de.ph1b.audiobook.adapter;

import android.app.Fragment;
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
import de.ph1b.audiobook.fragment.FilesChooseFragment;

public class FileAdapter extends BaseAdapter {

    private final ArrayList<File> data;
    private final Fragment fragment;
    private final ArrayList<File> checked = new ArrayList<File>();
    private static final String TAG = "FileAdapter";

    public FileAdapter(ArrayList<File> data, Fragment fragment) {
        this.data = data;
        this.fragment = fragment;
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
            LayoutInflater vi = (LayoutInflater) fragment.getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = vi.inflate(R.layout.adapter_files_choose, parent, false);

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
                if (isChecked)
                    checked.add(data.get(pos));
                else
                    checked.remove(data.get(pos));

                viewHolder.checkBox.setChecked(isChecked);

                //sends only checked files
                ArrayList<File> checkedFiles = new ArrayList<File>();
                for (File f : checked)
                    checkedFiles.add(f);
                ((FilesChooseFragment) fragment).checkStateChanged(checkedFiles);
            }
        });

        //setting correct value

        File fileOfPosition = data.get(position);
        Boolean fileIsChecked = checked.contains(fileOfPosition);
        viewHolder.checkBox.setChecked(fileIsChecked);

        File f = data.get(position);
        String name = f.getName();

        String capital = name.substring(0, 1).toUpperCase();

        viewHolder.symbolView.setText(capital);
        if (f.isFile())
            viewHolder.symbolView.setBackgroundColor(fragment.getActivity().getResources().getColor(R.color.file_chooser_audio));
        else
            viewHolder.symbolView.setBackgroundColor(fragment.getActivity().getResources().getColor(R.color.file_chooser_folder));

        viewHolder.nameView.setText(name);

        return convertView;
    }

    public void clearCheckBoxes() {
        if (BuildConfig.DEBUG)
            Log.d(TAG, "clearCheckBoxes() was called!");
        checked.clear();
        notifyDataSetChanged();
    }

    static class ViewHolder {
        TextView symbolView;
        TextView nameView;
        CheckBox checkBox;
    }
}