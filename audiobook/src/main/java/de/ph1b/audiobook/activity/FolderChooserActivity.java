package de.ph1b.audiobook.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import de.ph1b.audiobook.R;
import de.ph1b.audiobook.adapter.FolderChooserAdapter;
import de.ph1b.audiobook.dialog.HideFolderDialog;
import de.ph1b.audiobook.model.NaturalOrderComparator;
import de.ph1b.audiobook.utils.FileRecognition;
import de.ph1b.audiobook.utils.L;

/**
 * Activity for choosing an audiobook folder. If there are multiple SD-Cards, the Activity unifies
 * them to a fake-folder structure. We must make sure that this is not choosable. When there are no
 * multiple sd-cards, we will directly show the content of the 1 SD Card.
 */
public class FolderChooserActivity extends BaseActivity implements View.OnClickListener {

    public static final String ACTIVITY_FOR_RESULT_REQUEST_CODE = "requestCode";
    public static final String CHOSEN_FILE = "chosenFile";
    public static final int ACTIVITY_FOR_RESULT_CODE_COLLECTION = 1;
    public static final int ACTIVITY_FOR_RESULT_CODE_SINGLE_BOOK = 2;

    private static final String CURRENT_FOLDER_NAME = "currentFolderName";
    private static final String TAG = FolderChooserActivity.class.getSimpleName();
    private final List<File> currentFolderContent = new ArrayList<>();
    private boolean multiSd = true;
    private List<File> rootDirs;
    private File currentFolder = null;
    private File chosenFile = null;
    private TextView currentFolderName;
    private Button chooseButton;
    private FolderChooserAdapter adapter;
    private ImageButton upButton;
    private int mode;

    private static List<File> getStorageDirectories() {
        Pattern DIR_SEPARATOR = Pattern.compile("/");
        // Final set of paths
        final Set<String> rv = new HashSet<>();
        // Primary physical SD-CARD (not emulated)
        final String rawExternalStorage = System.getenv("EXTERNAL_STORAGE");
        // All Secondary SD-CARDs (all exclude primary) separated by ":"
        final String rawSecondaryStorageStr = System.getenv("SECONDARY_STORAGE");
        // Primary emulated SD-CARD
        final String rawEmulatedStorageTarget = System.getenv("EMULATED_STORAGE_TARGET");
        if (TextUtils.isEmpty(rawEmulatedStorageTarget)) {
            // Device has physical external storage; use plain paths.
            if (TextUtils.isEmpty(rawExternalStorage)) {
                // EXTERNAL_STORAGE undefined; falling back to default.
                rv.add("/storage/sdcard0");
            } else {
                rv.add(rawExternalStorage);
            }
        } else {
            // Device has emulated storage; external storage paths should have
            // userId burned into them.
            final String rawUserId;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
                rawUserId = "";
            } else {
                final String path = Environment.getExternalStorageDirectory().getAbsolutePath();
                final String[] folders = DIR_SEPARATOR.split(path);
                final String lastFolder = folders[folders.length - 1];
                boolean isDigit = false;
                try {
                    //noinspection ResultOfMethodCallIgnored
                    Integer.valueOf(lastFolder);
                    isDigit = true;
                } catch (NumberFormatException ignored) {
                }
                rawUserId = isDigit ? lastFolder : "";
            }
            // /storage/emulated/0[1,2,...]
            if (TextUtils.isEmpty(rawUserId)) {
                rv.add(rawEmulatedStorageTarget);
            } else {
                rv.add(rawEmulatedStorageTarget + File.separator + rawUserId);
            }
        }
        // Add all secondary storage
        if (!TextUtils.isEmpty(rawSecondaryStorageStr)) {
            // All Secondary SD-CARDs splitted into array
            final String[] rawSecondaryStorage = rawSecondaryStorageStr.split(File.pathSeparator);
            Collections.addAll(rv, rawSecondaryStorage);
        }
        rv.add("/storage/extSdCard");
        rv.add(Environment.getExternalStorageDirectory().getAbsolutePath());
        rv.add("/storage/emulated/0");
        rv.add("/storage/sdcard1");
        List<File> paths = new ArrayList<>();
        for (String s : rv) {
            File f = new File(s);
            if (f.exists() && f.isDirectory() && !paths.contains(f) && f.canRead() && f.listFiles() != null && f.listFiles().length > 0) {
                paths.add(f);
            }
        }
        Collections.sort(paths, NaturalOrderComparator.INSTANCE);
        return paths;
    }

    /**
     * Gets the containing files of a folder (restricted to music and folders) in a naturally sorted
     * order.
     *
     * @param file The file to look for containing files
     * @return The containing files
     */
    private static List<File> getFilesFromFolder(File file) {
        List<File> asList = new ArrayList<>();
        File[] containing = file.listFiles(FileRecognition.folderAndMusicFilter);
        if (containing != null) {
            asList = new ArrayList<>(Arrays.asList(containing));
            Collections.sort(asList, NaturalOrderComparator.INSTANCE);
        }
        return asList;
    }

    private void changeFolder(File newFolder) {
        currentFolder = newFolder;
        currentFolderContent.clear();
        currentFolderContent.addAll(getFilesFromFolder(currentFolder));
        adapter.notifyDataSetChanged();
        setButtonEnabledDisabled();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int requestCode = getIntent().getIntExtra(ACTIVITY_FOR_RESULT_REQUEST_CODE, -1);
        if (requestCode != ACTIVITY_FOR_RESULT_CODE_COLLECTION && requestCode != ACTIVITY_FOR_RESULT_CODE_SINGLE_BOOK) {
            throw new IllegalArgumentException("Illegal requestCode=" + requestCode);
        } else {
            mode = requestCode;
        }

        // init fields
        setContentView(R.layout.activity_folder_chooser);

        // toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //get views
        ListView listView = (ListView) findViewById(R.id.listView);
        upButton = (ImageButton) findViewById(R.id.twoline_image1);
        currentFolderName = (TextView) findViewById(R.id.twoline_text2);
        ((TextView) findViewById(R.id.twoline_text1)).setText(R.string.chosen_folder_description);
        chooseButton = (Button) findViewById(R.id.choose);
        Button abortButton = (Button) findViewById(R.id.abort);

        //set listener
        upButton.setOnClickListener(this);
        chooseButton.setOnClickListener(this);
        abortButton.setOnClickListener(this);

        //setup
        adapter = new FolderChooserAdapter(this, currentFolderContent, mode);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                File selectedFile = adapter.getItem(position);
                if (selectedFile.isDirectory() && selectedFile.canRead()) {
                    chosenFile = selectedFile;
                    currentFolderName.setText(chosenFile.getName());
                    changeFolder(adapter.getItem(position));
                } else if (mode == ACTIVITY_FOR_RESULT_CODE_SINGLE_BOOK && selectedFile.isFile()) {
                    chosenFile = selectedFile;
                    currentFolderName.setText(chosenFile.getName());
                }
            }
        });

        rootDirs = getStorageDirectories();
        if (rootDirs.size() == 1) {
            chosenFile = rootDirs.get(0);
            currentFolderName.setText(chosenFile.getName());
            changeFolder(chosenFile);
            multiSd = false;
        } else {
            currentFolderContent.addAll(rootDirs);
        }

        //handle runtime
        if (savedInstanceState != null) {
            String savedFolderPath = savedInstanceState.getString(CURRENT_FOLDER_NAME);
            if (savedFolderPath != null) {
                File f = new File(savedFolderPath);
                if (f.exists() && f.canRead()) {
                    chosenFile = f;
                    currentFolderName.setText(chosenFile.getName());
                    changeFolder(f);
                }
            }
        }

        setButtonEnabledDisabled();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (currentFolder != null) {
            outState.putString(CURRENT_FOLDER_NAME, currentFolder.getAbsolutePath());
        }
    }

    @Override
    public void onBackPressed() {
        if (canGoBack()) {
            up();
        } else {
            super.onBackPressed();
        }
    }

    private boolean canGoBack() {
        if (multiSd) {
            return currentFolder != null;
        } else {
            for (File f : rootDirs) {
                if (f.equals(currentFolder)) {
                    return false; //to go up we must not already be in top level
                }
            }
            return true;
        }
    }

    private void up() {
        L.d(TAG, "up called. currentFolder=" + currentFolder);

        boolean chosenFolderIsInRoot = false;
        for (File f : rootDirs) {
            if (f.equals(currentFolder)) {
                L.d(TAG, "chosen folder is in root");
                chosenFolderIsInRoot = true;
            }
        }
        if (multiSd && chosenFolderIsInRoot) {
            currentFolder = null;
            currentFolderName.setText("");
            currentFolderContent.clear();
            currentFolderContent.addAll(rootDirs);
            adapter.notifyDataSetChanged();
        } else {
            currentFolder = currentFolder.getParentFile();
            chosenFile = currentFolder;
            currentFolderName.setText(currentFolder.getName());
            List<File> parentContaining = getFilesFromFolder(currentFolder);
            currentFolderContent.clear();
            currentFolderContent.addAll(parentContaining);
            adapter.notifyDataSetChanged();
        }
        setButtonEnabledDisabled();
    }

    /**
     * Sets the choose button enabled or disabled, depending on where we are in the hierarchy
     */
    private void setButtonEnabledDisabled() {
        boolean upEnabled = canGoBack();
        boolean chooseEnabled = !multiSd || upEnabled;

        chooseButton.setEnabled(chooseEnabled);
        upButton.setEnabled(upEnabled);
        @SuppressWarnings("deprecation") Drawable upIcon = upEnabled ? getResources().getDrawable(R.drawable.ic_arrow_up_white_48dp) : null;
        upButton.setImageDrawable(upIcon);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.twoline_image1:
                up();
                break;
            case R.id.choose:
                if (chosenFile.isDirectory() && !HideFolderDialog.getNoMediaFileByFolder(chosenFile).exists()) {
                    HideFolderDialog hideFolderDialog = HideFolderDialog.newInstance(chosenFile);
                    hideFolderDialog.show(getSupportFragmentManager(), HideFolderDialog.TAG);
                    hideFolderDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            finishActivityWithSuccess(chosenFile);
                        }
                    });
                } else {
                    finishActivityWithSuccess(chosenFile);
                }
                break;
            case R.id.abort:
                finish();
                break;
            default:
                break;
        }
    }

    private void finishActivityWithSuccess(@NonNull File chosenFile) {
        Intent data = new Intent();
        data.putExtra(CHOSEN_FILE, chosenFile.getAbsolutePath());
        setResult(RESULT_OK, data);
        finish();
    }
}