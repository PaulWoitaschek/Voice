package de.ph1b.audiobook.adapter;

import android.support.v7.widget.RecyclerView;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;

import de.ph1b.audiobook.R;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.ViewHolder> {

    public interface ItemInteraction {
        public void onCheckStateChanged();

        public void onItemClicked(int position);
    }

    public interface CheckStateChanged {
        public void onCheckStateChanged(int position, boolean isChecked);
    }

    private final ArrayList<File> data;
    private final SparseBooleanArray checked;
    private final ItemInteraction itemInteraction;
    private final CheckStateChanged checkStateChanged;

    public FileAdapter(ArrayList<File> data, final ItemInteraction itemInteraction) {
        this.data = data;
        this.itemInteraction = itemInteraction;
        this.checked = new SparseBooleanArray();
        checkStateChanged = new CheckStateChanged() {
            @Override
            public void onCheckStateChanged(int position, boolean isChecked) {
                checked.put(position, isChecked);
                itemInteraction.onCheckStateChanged();
            }
        };
    }

    public ArrayList<File> getCheckedItems() {
        ArrayList<File> checkedItems = new ArrayList<File>();
        for (int i = 0; i < getItemCount(); i++) {
            if (checked.get(i)) {
                checkedItems.add(data.get(i));
            }
        }
        return checkedItems;
    }


    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, final int i) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.files_chooser_listview, viewGroup, false);
        return new ViewHolder(v, itemInteraction, checkStateChanged);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int i) {
        boolean fileIsChecked = checked.get(i);
        viewHolder.checkBox.setChecked(fileIsChecked);

        File f = data.get(i);
        String name = f.getName();

        if (f.isFile())
            viewHolder.icon.setImageResource(R.drawable.ic_album_grey600_48dp);
        else
            viewHolder.icon.setImageResource(R.drawable.ic_folder_grey600_48dp);

        viewHolder.nameView.setText(name);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return data != null ? data.size() : 0;
    }

    public File getItem(int position) {
        return data.get(position);
    }

    public void clearCheckBoxes() {
        for (int i = 0; i < getItemCount(); i++) {
            if (checked.get(i)) {
                checked.delete(i);
                notifyItemChanged(i);
            }
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView nameView;
        final CheckBox checkBox;

        public ViewHolder(View itemView, final ItemInteraction itemInteraction, final CheckStateChanged checkStateChanged) {
            super(itemView);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    itemInteraction.onItemClicked(getPosition());
                }
            });
            icon = (ImageView) itemView.findViewById(R.id.icon);
            nameView = (TextView) itemView.findViewById(R.id.file_name);
            checkBox = (CheckBox) itemView.findViewById(R.id.file_check);
            checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    checkStateChanged.onCheckStateChanged(getPosition(), isChecked);
                }
            });
        }
    }
}