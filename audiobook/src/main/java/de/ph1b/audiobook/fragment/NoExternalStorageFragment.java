package de.ph1b.audiobook.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import de.ph1b.audiobook.R;


public class NoExternalStorageFragment extends Fragment {


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ActionBar actionBar = ((ActionBarActivity) getActivity()).getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setTitle(getString(R.string.no_external_storage_action_bar_title));
        return inflater.inflate(R.layout.fragment_no_external_storage, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            getActivity().onBackPressed();
        }
    }
}