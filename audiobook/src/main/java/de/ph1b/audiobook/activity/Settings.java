package de.ph1b.audiobook.activity;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

import de.ph1b.audiobook.R;


public class Settings extends ActionBarActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_preferences);
    }
}
