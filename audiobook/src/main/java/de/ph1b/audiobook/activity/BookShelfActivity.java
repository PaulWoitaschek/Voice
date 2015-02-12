package de.ph1b.audiobook.activity;


import android.os.Bundle;
import android.preference.PreferenceManager;

import de.ph1b.audiobook.R;

public class BookShelfActivity extends BaseActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_choose);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
    }
}
