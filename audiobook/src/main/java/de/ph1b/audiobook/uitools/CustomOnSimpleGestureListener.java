package de.ph1b.audiobook.uitools;

import android.support.v7.widget.RecyclerView;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

public class CustomOnSimpleGestureListener extends GestureDetector.SimpleOnGestureListener {
    private final RecyclerView recyclerView;
    private final OnItemLongClickListener itemListener;

    public CustomOnSimpleGestureListener(RecyclerView recyclerView, OnItemLongClickListener itemListener) {
        this.recyclerView = recyclerView;
        this.itemListener = itemListener;
    }

    @Override
    public void onLongPress(MotionEvent e) {
        View child = recyclerView.findChildViewUnder(e.getX(), e.getY());
        if (child != null) {
            int position = recyclerView.getChildAdapterPosition(child);
            itemListener.onItemLongClicked(position, child);
        }
        super.onLongPress(e);
    }

    public interface OnItemLongClickListener {
        public void onItemLongClicked(int position, View view);
    }

}