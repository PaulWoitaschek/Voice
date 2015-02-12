package de.ph1b.audiobook.activity;

import android.os.Bundle;

import de.ph1b.audiobook.R;
import de.ph1b.audiobook.interfaces.OnBackPressedListener;


public class FilesChooseActivity extends BaseActivity {


    private OnBackPressedListener onBackPressedListener;

    @Override
    public void onBackPressed() {
        if (onBackPressedListener != null)
            onBackPressedListener.backPressed();
        else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_files_choose);
    }

    public void setOnBackPressedListener(OnBackPressedListener onBackPressedListener) {
        this.onBackPressedListener = onBackPressedListener;
    }
}
