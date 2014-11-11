package de.ph1b.audiobook.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import de.ph1b.audiobook.R;


public class CustomExpandableListAdapter extends BaseExpandableListAdapter {


    private Context c;


    private ArrayList<File> data = new ArrayList<File>();

    public CustomExpandableListAdapter(String baseFolder, Context c) {
        data = getFiles(baseFolder);
        this.c = c;
    }

    private ArrayList<File> getFiles(String path) {
        File rootFile = new File(path);
        File[] containing = rootFile.listFiles();
        return new ArrayList<File>(Arrays.asList(containing));
    }

    @Override
    public int getGroupCount() {
        return data.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        File group = data.get(groupPosition);
        return getFiles(group.getAbsolutePath()).size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return data.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        //todo
        return data.get(groupPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        //todo
        return 0;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {


        if (convertView == null) {
            LayoutInflater layoutInflater = (LayoutInflater) c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = layoutInflater.inflate(R.layout.expandable_list_view_folder, null);
        }

        TextView textView = (TextView) convertView.findViewById(R.id.textView);
        textView.setText(data.get(groupPosition).getName());

        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater layInflator = (LayoutInflater) this.c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = layInflator.inflate(R.layout.expandable_list_view_file, null);
        }

        TextView textView = (TextView) convertView.findViewById(R.id.textView);
        ArrayList<File> containing = getFiles(data.get(groupPosition).getAbsolutePath());
        textView.setText(containing.get(childPosition).getName());

        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return false;
    }
}
