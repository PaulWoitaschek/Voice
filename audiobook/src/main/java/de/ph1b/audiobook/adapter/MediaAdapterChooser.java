package de.ph1b.audiobook.adapter;

import android.content.Context;
import android.os.Build;

import java.util.ArrayList;

import de.ph1b.audiobook.content.BookDetail;
import de.ph1b.audiobook.interfaces.OnItemClickListener;


/**
 * Class returning the correct MediaAdapter implementation. This is done because
 * android.content.ComponentCallbacks2 is only available for API>=14.
 */
public class MediaAdapterChooser {

    public static MediaAdapter getAdapter(ArrayList<BookDetail> data, Context c, OnItemClickListener onItemClickListener, MediaAdapter.OnCoverChangedListener onCoverChangedListener) {
        if (Build.VERSION.SDK_INT >= 14) {
            return new MediaAdapterV14(data, c, onItemClickListener, onCoverChangedListener);
        } else {
            return new MediaAdapterV1(data, c, onItemClickListener, onCoverChangedListener);
        }
    }
}