package de.ph1b.audiobook.activity;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;

import net.i2p.android.ext.floatingactionbutton.FloatingActionButton;
import net.i2p.android.ext.floatingactionbutton.FloatingActionsMenu;

import java.util.ArrayList;

import de.ph1b.audiobook.R;
import de.ph1b.audiobook.adapter.FolderOverviewAdapter;
import de.ph1b.audiobook.uitools.DividerItemDecoration;
import de.ph1b.audiobook.utils.BaseApplication;
import de.ph1b.audiobook.utils.L;
import de.ph1b.audiobook.utils.PrefsManager;

public class FolderOverviewActivity extends BaseActivity {

    /**
     * This is an extra for the launching intent to indicate, that this activity was called from the
     * {@link de.ph1b.audiobook.fragment.BookShelfFragment}. If so, after choosing a file we will
     * finish the activity immediately.
     */
    public static final String ONESHOT_FROM_BOOKSHELF = "oneShotFromBookShelf";
    private static final String TAG = FolderOverviewActivity.class.getSimpleName();
    private static final String BACKGROUND_OVERLAY_VISIBLE = "backgroundOverlayVisibility";
    private final ArrayList<String> bookCollections = new ArrayList<>();
    private final ArrayList<String> singleBooks = new ArrayList<>();
    private PrefsManager prefs;
    private FolderOverviewAdapter adapter;
    private FloatingActionsMenu fam;
    private BaseApplication baseApplication;
    private FloatingActionButton buttonRepresentingTheFam;
    private View backgroundOverlay;

    /**
     * @return the point representing the center of the floating action menus button. Note, that the
     * fam is only a container, so we have to calculate the point relatively.
     */
    private Point getFamCenter() {
        int x = fam.getLeft() + ((buttonRepresentingTheFam.getLeft() + buttonRepresentingTheFam.getRight()) / 2);
        int y = fam.getTop() + ((buttonRepresentingTheFam.getTop() + buttonRepresentingTheFam.getBottom()) / 2);
        return new Point(x, y);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_folder_overview);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(getString(R.string.audiobook_folders_title));

        prefs = new PrefsManager(this);
        baseApplication = (BaseApplication) getApplication();

        //init views
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler);
        fam = (FloatingActionsMenu) findViewById(R.id.fam);
        buttonRepresentingTheFam = (FloatingActionButton) findViewById(R.id.fab_expand_menu_button);
        backgroundOverlay = findViewById(R.id.overlay);
        if (savedInstanceState != null) { // restoring overlay
            if (savedInstanceState.getBoolean(BACKGROUND_OVERLAY_VISIBLE)) {
                backgroundOverlay.setVisibility(View.VISIBLE);
            } else {
                backgroundOverlay.setVisibility(View.GONE);
            }
        }

        fam.setOnFloatingActionsMenuUpdateListener(new FloatingActionsMenu.OnFloatingActionsMenuUpdateListener() {
            @Override
            public void onMenuExpanded() {
                if (Build.VERSION.SDK_INT >= 21) {
                    Point famCenter = getFamCenter();
                    int cx = famCenter.x;
                    int cy = famCenter.y;

                    // get the final radius for the clipping circle
                    int finalRadius = Math.max(backgroundOverlay.getWidth(), backgroundOverlay.getHeight());

                    // create the animator for this view (the start radius is zero)
                    Animator anim = ViewAnimationUtils.createCircularReveal(backgroundOverlay, cx, cy, 0,
                            finalRadius);

                    // make the view visible and start the animation
                    backgroundOverlay.setVisibility(View.VISIBLE);
                    anim.start();
                } else {
                    backgroundOverlay.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onMenuCollapsed() {
                if (Build.VERSION.SDK_INT >= 21) {
                    // get the center for the clipping circle
                    Point famCenter = getFamCenter();
                    int cx = famCenter.x;
                    int cy = famCenter.y;

                    // get the initial radius for the clipping circle
                    int initialRadius = Math.max(backgroundOverlay.getHeight(), backgroundOverlay.getWidth());

                    // create the animation (the final radius is zero)
                    Animator anim = ViewAnimationUtils.createCircularReveal(backgroundOverlay, cx, cy,
                            initialRadius, 0);

                    // make the view invisible when the animation is done
                    anim.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            backgroundOverlay.setVisibility(View.INVISIBLE);
                        }
                    });

                    // start the animation
                    anim.start();
                } else {
                    backgroundOverlay.setVisibility(View.INVISIBLE);
                }
            }
        });

        // preparing list
        final LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addItemDecoration(new DividerItemDecoration(this));
        adapter = new FolderOverviewAdapter(this, bookCollections, singleBooks,
                new FolderOverviewAdapter.OnFolderMoreClickedListener() {
                    @Override
                    public void onFolderMoreClicked(final int position) {
                        new MaterialDialog.Builder(FolderOverviewActivity.this)
                                .title(R.string.delete_folder)
                                .content(getString(R.string.delete_folder_content) + "\n"
                                        + adapter.getItem(position))
                                .positiveText(R.string.remove)
                                .negativeText(R.string.dialog_cancel)
                                .callback(new MaterialDialog.ButtonCallback() {
                                    @Override
                                    public void onPositive(MaterialDialog dialog) {
                                        adapter.removeItem(position);
                                        prefs.setCollectionFolders(bookCollections);
                                        prefs.setSingleBookFolders(singleBooks);
                                        baseApplication.scanForFiles(true);
                                    }
                                })
                                .show();
                    }
                });
        recyclerView.setAdapter(adapter);

        findViewById(R.id.add_single).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fam.collapse();
                Intent intent = new Intent(FolderOverviewActivity.this,
                        FolderChooserActivity.class);
                int requestCode = FolderChooserActivity.ACTIVITY_FOR_RESULT_CODE_SINGLE_BOOK;
                intent.putExtra(FolderChooserActivity.ACTIVITY_FOR_RESULT_REQUEST_CODE,
                        requestCode);
                startActivityForResult(intent, requestCode);
            }
        });
        findViewById(R.id.add_library).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fam.collapse();
                Intent intent = new Intent(FolderOverviewActivity.this,
                        FolderChooserActivity.class);
                int requestCode = FolderChooserActivity.ACTIVITY_FOR_RESULT_CODE_COLLECTION;
                intent.putExtra(FolderChooserActivity.ACTIVITY_FOR_RESULT_REQUEST_CODE,
                        requestCode);
                startActivityForResult(intent, requestCode);
            }
        });
    }

    private boolean canAddNewFolder(@NonNull final String newFile) {
        PrefsManager prefs = new PrefsManager(this);
        ArrayList<String> folders = new ArrayList<>();
        folders.addAll(prefs.getCollectionFolders());
        folders.addAll(prefs.getSingleBookFolders());

        boolean filesAreSubsets = true;
        boolean firstAddedFolder = folders.size() == 0;
        boolean sameFolder = false;
        for (String s : folders) {
            if (s.equals(newFile)) {
                sameFolder = true;
            }
            String[] oldParts = s.split("/");
            String[] newParts = newFile.split("/");
            for (int i = 0; i < Math.min(oldParts.length, newParts.length); i++) {
                if (!oldParts[i].equals(newParts[i])) {
                    filesAreSubsets = false;
                }
            }
            if (!sameFolder && filesAreSubsets) {
                Toast.makeText(this, getString(R.string.adding_failed_subfolder) + "\n" + s + "\n" +
                        newFile, Toast.LENGTH_LONG).show();
            }
            if (filesAreSubsets) {
                break;
            }
        }

        return firstAddedFolder || (!sameFolder && !filesAreSubsets);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case FolderChooserActivity.ACTIVITY_FOR_RESULT_CODE_COLLECTION:
                    String chosenCollection = data.getStringExtra(
                            FolderChooserActivity.CHOSEN_FILE);
                    if (canAddNewFolder(chosenCollection)) {
                        bookCollections.add(chosenCollection);
                        prefs.setCollectionFolders(bookCollections);
                        String action = getIntent().getAction();
                        if (action != null && action.equals(ONESHOT_FROM_BOOKSHELF)) {
                            finish();
                        }
                    }
                    L.v(TAG, "chosenCollection=" + chosenCollection);
                    break;
                case FolderChooserActivity.ACTIVITY_FOR_RESULT_CODE_SINGLE_BOOK:
                    String chosenSingleBook = data.getStringExtra(
                            FolderChooserActivity.CHOSEN_FILE);
                    if (canAddNewFolder(chosenSingleBook)) {
                        singleBooks.add(chosenSingleBook);
                        prefs.setSingleBookFolders(singleBooks);
                        String action = getIntent().getAction();
                        if (action != null && action.equals(ONESHOT_FROM_BOOKSHELF)) {
                            finish();
                        }
                    }
                    L.v(TAG, "chosenSingleBook=" + chosenSingleBook);
                    break;
            }
            baseApplication.scanForFiles(true);
        }
    }

    @Override
    public void onBackPressed() {
        if (fam.isExpanded()) {
            fam.collapse();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        bookCollections.clear();
        bookCollections.addAll(prefs.getCollectionFolders());
        singleBooks.clear();
        singleBooks.addAll(prefs.getSingleBookFolders());
        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(BACKGROUND_OVERLAY_VISIBLE, backgroundOverlay.getVisibility() == View.VISIBLE);

        super.onSaveInstanceState(outState);
    }
}