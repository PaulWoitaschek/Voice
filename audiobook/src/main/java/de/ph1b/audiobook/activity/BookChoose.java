package de.ph1b.audiobook.activity;


import android.os.Bundle;

import de.ph1b.audiobook.R;

public class BookChoose extends BaseActivity {

    private static final String TAG = "de.ph1b.audiobook.activity.BookChoose";
    public static final String SHARED_PREFS_CURRENT = TAG + ".SHARED_PREFS_CURRENT";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_choose);
    }
}
