package de.ph1b.audiobook.activity;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.ph1b.audiobook.R;
import de.ph1b.audiobook.adapter.FolderOverviewAdapter;
import de.ph1b.audiobook.model.BookAdder;
import de.ph1b.audiobook.persistence.PrefsManager;
import de.ph1b.audiobook.uitools.DividerItemDecoration;
import de.ph1b.audiobook.utils.App;
import de.ph1b.audiobook.utils.L;

/**
 * Activity that lets the user add, edit or remove the set audiobook folders.
 *
 * @author Paul Woitaschek
 */
public class FolderOverviewActivity extends BaseActivity {


    private static final String TAG = FolderOverviewActivity.class.getSimpleName();
    private static final String BACKGROUND_OVERLAY_VISIBLE = "backgroundOverlayVisibility";
    private static final int PICKER_REQUEST_CODE = 42;
    private final List<String> bookCollections = new ArrayList<>(10);
    private final List<String> singleBooks = new ArrayList<>(10);
    @Bind(R.id.fam) FloatingActionsMenu fam;
    @Bind(R.id.add_library) FloatingActionButton libraryBookButton;
    @Bind(R.id.add_single) FloatingActionButton singleBookButton;
    @Bind(R.id.fab_expand_menu_button) FloatingActionButton buttonRepresentingTheFam;
    @Bind(R.id.overlay) View backgroundOverlay;
    private final FloatingActionsMenu.OnFloatingActionsMenuUpdateListener famMenuListener =
            new FloatingActionsMenu.OnFloatingActionsMenuUpdateListener() {

                private final Point famCenter = new Point();

                @Override
                public void onMenuExpanded() {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        getFamCenter(famCenter);

                        // get the final radius for the clipping circle
                        int finalRadius = Math.max(backgroundOverlay.getWidth(), backgroundOverlay.getHeight());

                        // create the animator for this view (the start radius is zero)
                        Animator anim = ViewAnimationUtils.createCircularReveal(backgroundOverlay,
                                famCenter.x, famCenter.y, 0, finalRadius);

                        // make the view visible and start the animation
                        backgroundOverlay.setVisibility(View.VISIBLE);
                        anim.start();
                    } else {
                        backgroundOverlay.setVisibility(View.VISIBLE);
                    }
                }

                @Override
                public void onMenuCollapsed() {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        // get the center for the clipping circle
                        getFamCenter(famCenter);

                        // get the initial radius for the clipping circle
                        int initialRadius = Math.max(backgroundOverlay.getHeight(), backgroundOverlay.getWidth());

                        // create the animation (the final radius is zero)
                        Animator anim = ViewAnimationUtils.createCircularReveal(backgroundOverlay,
                                famCenter.x, famCenter.y, initialRadius, 0);

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
            };
    @Bind(R.id.toolbar) Toolbar toolbar;
    @Bind(R.id.recycler) RecyclerView recyclerView;
    @Inject PrefsManager prefs;
    @Inject BookAdder bookAdder;
    private FolderOverviewAdapter adapter;

    /**
     * Calculates the point representing the center of the floating action menus button. Note, that
     * the fam is only a container, so we have to calculate the point relatively.
     */
    private void getFamCenter(Point point) {
        int x = fam.getLeft() + ((buttonRepresentingTheFam.getLeft() + buttonRepresentingTheFam.getRight()) / 2);
        int y = fam.getTop() + ((buttonRepresentingTheFam.getTop() + buttonRepresentingTheFam.getBottom()) / 2);
        point.set(x, y);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_folder_overview);
        ButterKnife.bind(this);
        App.getComponent().inject(this);

        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(getString(R.string.audiobook_folders_title));


        //init views
        if (savedInstanceState != null) { // restoring overlay
            if (savedInstanceState.getBoolean(BACKGROUND_OVERLAY_VISIBLE)) {
                backgroundOverlay.setVisibility(View.VISIBLE);
            } else {
                backgroundOverlay.setVisibility(View.INVISIBLE);
            }
        }

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
                                .onPositive(new MaterialDialog.SingleButtonCallback() {
                                    @Override
                                    public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {
                                        adapter.removeItem(position);
                                        prefs.setCollectionFolders(bookCollections);
                                        prefs.setSingleBookFolders(singleBooks);
                                        bookAdder.scanForFiles(true);
                                    }
                                })
                                .show();
                    }
                });
        recyclerView.setAdapter(adapter);

        fam.setOnFloatingActionsMenuUpdateListener(famMenuListener);

        singleBookButton.setTitle(getString(R.string.folder_add_single_book) + "\n" + getString(R.string.for_example)
                + " Harry Potter 4");
        libraryBookButton.setTitle(getString(R.string.folder_add_collection) + "\n" + getString(R.string.for_example)
                + " AudioBooks");
    }

    @OnClick({R.id.add_single, R.id.add_library})
    void clickedAdd(View view) {
        startFolderChooserActivity(view.getId() == R.id.add_single ? FolderChooserActivity.OperationMode.SINGLE_BOOK :
                FolderChooserActivity.OperationMode.COLLECTION_BOOK);
    }

    private void startFolderChooserActivity(FolderChooserActivity.OperationMode operationMode) {
        Intent intent = FolderChooserActivity.newInstanceIntent(this, operationMode);
        startActivityForResult(intent, PICKER_REQUEST_CODE);
    }

    /**
     * @param newFile the new folder file
     * @return true if the new folder is not added yet and is no sub- or parent folder of an existing
     * book folder
     */
    private boolean canAddNewFolder(@NonNull final String newFile) {
        List<String> folders = new ArrayList<>(bookCollections.size() + singleBooks.size());
        folders.addAll(bookCollections);
        folders.addAll(singleBooks);

        boolean filesAreSubsets = true;
        boolean firstAddedFolder = folders.isEmpty();
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

        // we don't want our listener be informed.
        fam.setOnFloatingActionsMenuUpdateListener(null);
        fam.collapseImmediately();
        fam.setOnFloatingActionsMenuUpdateListener(famMenuListener);

        backgroundOverlay.setVisibility(View.INVISIBLE);

        if (resultCode == RESULT_OK && requestCode == PICKER_REQUEST_CODE) {
            FolderChooserActivity.OperationMode mode = FolderChooserActivity.OperationMode.valueOf(data.getStringExtra(FolderChooserActivity.RESULT_OPERATION_MODE));
            switch (mode) {
                case COLLECTION_BOOK:
                    String chosenCollection = data.getStringExtra(
                            FolderChooserActivity.RESULT_CHOSEN_FILE);
                    if (canAddNewFolder(chosenCollection)) {
                        bookCollections.add(chosenCollection);
                        prefs.setCollectionFolders(bookCollections);
                    }
                    L.v(TAG, "chosenCollection=" + chosenCollection);
                    break;
                case SINGLE_BOOK:
                    String chosenSingleBook = data.getStringExtra(
                            FolderChooserActivity.RESULT_CHOSEN_FILE);
                    if (canAddNewFolder(chosenSingleBook)) {
                        singleBooks.add(chosenSingleBook);
                        prefs.setSingleBookFolders(singleBooks);
                    }
                    L.v(TAG, "chosenSingleBook=" + chosenSingleBook);
                    break;
                default:
                    break;
            }
            bookAdder.scanForFiles(true);
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