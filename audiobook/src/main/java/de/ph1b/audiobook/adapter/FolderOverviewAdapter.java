package de.ph1b.audiobook.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import de.ph1b.audiobook.R;

public class FolderOverviewAdapter extends RecyclerView.Adapter<FolderOverviewAdapter.ViewHolder> {

    @NonNull
    private final Context c;
    @NonNull
    private final List<String> bookCollections;
    @NonNull
    private final List<String> singleBooks;
    @NonNull
    private final OnFolderMoreClickedListener listener;

    public FolderOverviewAdapter(@NonNull final Context c,
                                 @NonNull final List<String> bookCollections,
                                 @NonNull final List<String> singleBooks,
                                 @NonNull final OnFolderMoreClickedListener listener) {
        this.c = c;
        this.bookCollections = bookCollections;
        this.singleBooks = singleBooks;
        this.listener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate
                (R.layout.activity_folder_overview_row_layout, parent, false);
        return new ViewHolder(v, listener);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        String file = getItem(position);
        holder.textView.setText(file);

        if (bookCollections.contains(file)) {
            holder.icon.setImageDrawable(ContextCompat.getDrawable(c, R.drawable.ic_folder_multiple_white_48dp));
            holder.icon.setContentDescription(c.getString(R.string.folder_add_collection));
        } else {
            holder.icon.setImageDrawable(ContextCompat.getDrawable(c, R.drawable.ic_folder_white_48dp));
            holder.icon.setContentDescription(c.getString(R.string.folder_add_single_book));
        }
    }

    @Override
    public long getItemId(final int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return bookCollections.size() + singleBooks.size();
    }

    public void removeItem(final int position) {
        if (bookCollections.size() > position) {
            bookCollections.remove(position);
        } else {
            singleBooks.remove(position - bookCollections.size());
        }
        notifyItemRemoved(position);
    }

    @NonNull
    public String getItem(final int position) {
        if (bookCollections.size() > position) {
            return bookCollections.get(position);
        } else {
            return singleBooks.get(position - bookCollections.size());
        }
    }

    public interface OnFolderMoreClickedListener {
        void onFolderMoreClicked(int position);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        @Bind(R.id.icon) ImageView icon;
        @Bind(R.id.containing) TextView textView;
        @Bind(R.id.remove) ImageButton imageButton;

        public ViewHolder(final View itemView, final OnFolderMoreClickedListener listener) {
            super(itemView);
            ButterKnife.bind(this, itemView);

            imageButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onFolderMoreClicked(getAdapterPosition());
                }
            });
        }
    }
}
