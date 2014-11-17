package de.ph1b.audiobook.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import de.ph1b.audiobook.BuildConfig;
import de.ph1b.audiobook.utils.NaturalOrderComparator;


public class FileAdderAdapter extends BaseAdapter {

    public interface Callback {
        public void onFolderChanged(String folder);
    }

    private static final String TAG = "FileAdderAdapter";

    private final ArrayList<File> root;
    private ArrayList<File> data;
    private File target;
    private final Context c;
    private final Callback callback;

    public void openFolder(int position) {
        File folder = data.get(position);
        navigateTo(folder);
    }

    public void navigateUp() {
        if (target != null && !root.contains(target.getParentFile()))
            navigateTo(target.getParentFile());
    }

    private void navigateTo(File target) {
        this.target = target;
        callback.onFolderChanged(target.getAbsolutePath());
        ArrayList<File> content = new ArrayList<File>(Arrays.asList(target.listFiles()));
        Collections.sort(content, new NaturalOrderComparator());

        boolean tooFarUp = false;
        for (File f : root) {
            if (content.contains(f)) {
                if (BuildConfig.DEBUG) Log.d(TAG, "we are navigating too far up.");
                tooFarUp = true;
                break;
            }
        }
        data.clear();
        if (tooFarUp) {
            data = cloneList(root);
        } else {
            for (File f : content) {
                if (f.isDirectory()) {
                    data.add(f);
                }
            }
        }
        notifyDataSetChanged();
    }

    public FileAdderAdapter(ArrayList<File> root, Context c, Callback callback) {
        this.root = root;
        this.data = cloneList(root);
        this.c = c;
        this.callback = callback;
    }

    private ArrayList<File> cloneList(ArrayList<File> from) {
        ArrayList<File> to = new ArrayList<File>();
        for (File f : from) {
            to.add(f);
        }
        return to;
    }

    @Override
    public int getCount() {
        return data.size();
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
    public View getView(int position, View v, ViewGroup parent) {
        ViewHolder holder;
        if (v == null) {
            LayoutInflater inflater = (LayoutInflater) c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = inflater.inflate(android.R.layout.simple_list_item_1, null);
            holder = new ViewHolder();
            holder.textView = (TextView) v.findViewById(android.R.id.text1);
            v.setTag(holder);
        } else {
            holder = (ViewHolder) v.getTag();
        }

        String fileName = data.get(position).getName();
        holder.textView.setText(fileName);

        return v;
    }

    static class ViewHolder {
        TextView textView;
    }
}
