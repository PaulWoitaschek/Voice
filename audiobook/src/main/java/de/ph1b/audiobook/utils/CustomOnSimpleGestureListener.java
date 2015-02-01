package de.ph1b.audiobook.utils;

import android.support.v7.widget.RecyclerView;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import de.ph1b.audiobook.interfaces.OnItemLongClickListener;

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
            int position = recyclerView.getChildPosition(child);
            itemListener.onItemLongClicked(position, child);
        }
        L.d("rtd", "onLongPress");
        super.onLongPress(e);
    }
}