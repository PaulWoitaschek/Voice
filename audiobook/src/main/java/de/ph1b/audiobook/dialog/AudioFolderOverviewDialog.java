package de.ph1b.audiobook.dialog;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import com.afollestad.materialdialogs.AlertDialogWrapper;
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

        LayoutInflater inflater = getActivity().getLayoutInflater();

        prefs = new PrefsManager(getActivity());
        folders = prefs.getAudiobookFolders();
        AlertDialogWrapper.Builder builder = new AlertDialogWrapper.Builder(getActivity());

        builder.setTitle(R.string.audiobook_folders_title);

        //passing null is fine because of fragment
        @SuppressLint("InflateParams") View customView = inflater.inflate(R.layout.dialog_folder_overview, null);
        builder.setView(customView);

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

        builder.setNegativeButton(R.string.dialog_cancel, null);
        builder.setPositiveButton(R.string.dialog_confirm, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                prefs.setAudiobookFolders(folders);
                getActivity().startService(BookAddingService.getUpdateIntent(getActivity()));
            }
        });

        return builder.create();
    }
}