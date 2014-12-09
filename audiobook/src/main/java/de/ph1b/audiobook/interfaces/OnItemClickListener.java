package de.ph1b.audiobook.interfaces;


import android.view.View;

public interface OnItemClickListener {
    public void onCoverClicked(int position);

    public void onPopupMenuClicked(View view, int position);
}
