package de.ph1b.audiobook.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import de.ph1b.audiobook.R;
import de.ph1b.audiobook.utils.L;

public class FolderOverviewAdapter extends RecyclerView.Adapter<FolderOverviewAdapter.ViewHolder> {

    private static final String TAG = FolderOverviewAdapter.class.getSimpleName();
    private final ArrayList<String> folders;
    private OnFolderMoreClickedListener listener = null;

    public FolderOverviewAdapter(ArrayList<String> folders) {
        this.folders = folders;
        L.d(TAG, "created adapter with folders.size()=" + folders.size());
        for (String s : folders) {
            L.d(TAG, s);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate
                (R.layout.dialog_folder_row_layout, parent, false);
        return new ViewHolder(v, listener);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        String folder = folders.get(position);
        holder.textView.setText(folder);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return folders.size();
    }

    public void removeItem(int position) {
        folders.remove(position);
        notifyItemRemoved(position);
    }

    public void addItem(String folder) {
        folders.add(folder);
        notifyItemInserted(folders.indexOf(folder));
    }

    public String getItem(int position) {
        return folders.get(position);
    }

    public void setOnFolderMoreClickedListener(OnFolderMoreClickedListener listener) {
        this.listener = listener;
    }

    public interface OnFolderMoreClickedListener {
        public void onFolderMoreClicked(int position);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        final ImageView imageView;
        final TextView textView;

        public ViewHolder(View itemView, final OnFolderMoreClickedListener listener) {
            super(itemView);
            imageView = (ImageView) itemView.findViewById(R.id.more);
            textView = (TextView) itemView.findViewById(R.id.containing);

            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onFolderMoreClicked(getAdapterPosition());
                }
            });
        }
    }
}
