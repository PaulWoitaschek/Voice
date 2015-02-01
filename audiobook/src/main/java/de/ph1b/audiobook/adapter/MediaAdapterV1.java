package de.ph1b.audiobook.adapter;


import android.content.ComponentCallbacks;
import android.content.Context;
import android.content.res.Configuration;

import java.util.ArrayList;

import de.ph1b.audiobook.content.Book;
import de.ph1b.audiobook.interfaces.OnItemClickListener;

class MediaAdapterV1 extends MediaAdapter implements ComponentCallbacks {

    public MediaAdapterV1(ArrayList<Book> data, Context c, OnItemClickListener onItemClickListener, OnCoverChangedListener onCoverChangedListener) {
        super(data, c, onItemClickListener, onCoverChangedListener);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {

    }

    @Override
    public void onLowMemory() {
        imageCache.evictAll();
    }
}
