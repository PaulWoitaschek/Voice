package de.paul_woitaschek.audiobook.activity;


import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.afollestad.materialdialogs.MaterialDialog;
import com.melnykov.fab.FloatingActionButton;

import java.util.ArrayList;

import de.paul_woitaschek.audiobook.R;
import de.paul_woitaschek.audiobook.adapter.FolderOverviewAdapter;
import de.paul_woitaschek.audiobook.service.BookAddingService;
import de.paul_woitaschek.audiobook.uitools.DividerItemDecoration;
import de.paul_woitaschek.audiobook.utils.PrefsManager;

public class FolderOverviewActivity extends BaseActivity {

    private final ArrayList<String> folders = new ArrayList<>();
    private PrefsManager prefs;
    private FolderOverviewAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_folder_overview);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(getString(R.string.audiobook_folders_title));

        prefs = new PrefsManager(this);

        //init views
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler);
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);

        // preparing list
        final LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addItemDecoration(new DividerItemDecoration(this));
        adapter = new FolderOverviewAdapter(folders, new FolderOverviewAdapter.OnFolderMoreClickedListener() {
            @Override
            public void onFolderMoreClicked(final int position) {
                new MaterialDialog.Builder(FolderOverviewActivity.this)
                        .title(R.string.delete_folder)
                        .content(getString(R.string.delete_folder_content) + "\n" + adapter.getItem(position))
                        .positiveText(R.string.remove)
                        .negativeText(R.string.dialog_cancel)
                        .callback(new MaterialDialog.ButtonCallback() {
                            @Override
                            public void onPositive(MaterialDialog dialog) {
                                adapter.removeItem(position);
                                prefs.setAudiobookFolders(folders);
                                startService(BookAddingService.getRescanIntent(FolderOverviewActivity.this, true));
                            }
                        })
                        .show();
            }
        });
        recyclerView.setAdapter(adapter);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(FolderOverviewActivity.this, FolderChooserActivity.class));
            }
        });
        fab.attachToRecyclerView(recyclerView);
    }

    @Override
    protected void onResume() {
        super.onResume();

        folders.clear();
        folders.addAll(prefs.getAudiobookFolders());
        adapter.notifyDataSetChanged();
    }
}
