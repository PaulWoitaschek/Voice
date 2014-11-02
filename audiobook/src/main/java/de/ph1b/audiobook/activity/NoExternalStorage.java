package de.ph1b.audiobook.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;

import de.ph1b.audiobook.R;


public class NoExternalStorage extends BaseActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_no_external_storage);
    }

    @Override
    public void onBackPressed() {
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()))
            super.onBackPressed();
        else {
            Intent i = new Intent(Intent.ACTION_MAIN);
            i.addCategory(Intent.CATEGORY_HOME);
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);
        }
    }
}
