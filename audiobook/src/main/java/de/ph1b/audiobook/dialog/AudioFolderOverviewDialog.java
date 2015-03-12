package de.ph1b.audiobook.dialog;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.melnykov.fab.FloatingActionButton;

import java.util.ArrayList;

import de.ph1b.audiobook.R;
import de.ph1b.audiobook.activity.FolderChooserActivity;
import de.ph1b.audiobook.adapter.FolderOverviewAdapter;
import de.ph1b.audiobook.service.BookAddingService;
import de.ph1b.audiobook.utils.L;
import de.ph1b.audiobook.utils.PrefsManager;

public class AudioFolderOverviewDialog extends DialogFragment {

    private static final int REQUEST_NEW_FOLDER = 1;
    private static final String TAG = AudioFolderOverviewDialog.class.getSimpleName();
    private PrefsManager prefs;
    private FolderOverviewAdapter adapter;
    private ArrayList<String> folders;

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        L.d(TAG, "onActivityResult, requestCode=" + requestCode + ", resultCode=" + resultCode + ", data=" + data);
        if (requestCode == REQUEST_NEW_FOLDER && resultCode == Activity.RESULT_OK) {
            String newFolder = data.getStringExtra(FolderChooserActivity.CHOSEN_FOLDER);

            boolean shouldAddFolder = true;
            String theFolder = getString(R.string.audiobook_dialog_left);
            String mustNotBe = getString(R.string.audiobook_dialog_right);
            for (String s : folders) {
                if (s.contains(newFolder)) {
                    Toast.makeText(getActivity(), theFolder + "\n" + newFolder + "\n" + mustNotBe + "\n" + s, Toast.LENGTH_LONG).show();
                    shouldAddFolder = false;
                }
                if (newFolder.contains(s)) {
                    Toast.makeText(getActivity(), theFolder + "\n" + s + "\n" + mustNotBe + "\n" + newFolder, Toast.LENGTH_LONG).show();
                    shouldAddFolder = false;
                }
            }
            if (shouldAddFolder) {
                adapter.addItem(newFolder);
            }
        }
    }

    public Dialog onCreateDialog(Bundle savedInstanceState) {
        prefs = new PrefsManager(getActivity());
        folders = prefs.getAudiobookFolders();

        LayoutInflater inflater = getActivity().getLayoutInflater();
        @SuppressLint("InflateParams") View customView = inflater.inflate(R.layout.dialog_folder_overview, null);

        //init views
        RecyclerView recyclerView = (RecyclerView) customView.findViewById(R.id.recycler);
        FloatingActionButton fab = (FloatingActionButton) customView.findViewById(R.id.fab);

        // preparing list
        final LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new FolderOverviewAdapter(folders);
        recyclerView.setAdapter(adapter);
        adapter.setOnFolderMoreClickedListener(new FolderOverviewAdapter.OnFolderMoreClickedListener() {
            @Override
            public void onFolderMoreClicked(int position) {
                adapter.removeItem(position);
            }
        });

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(getActivity(), FolderChooserActivity.class), REQUEST_NEW_FOLDER);
            }
        });

        return new MaterialDialog.Builder(getActivity())
                .title(R.string.audiobook_folders_title)
                .positiveText(R.string.dialog_confirm)
                .negativeText(R.string.dialog_cancel)
                .customView(customView, true)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        prefs.setAudiobookFolders(folders);
                        getActivity().startService(BookAddingService.getUpdateIntent(getActivity()));
                    }
                })
                .build();
    }
}