package de.ph1b.audiobook.activity;

import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import de.ph1b.audiobook.R;
import de.ph1b.audiobook.adapter.FolderAdapter;
import de.ph1b.audiobook.model.NaturalStringComparator;
import de.ph1b.audiobook.service.BookAddingService;
import de.ph1b.audiobook.uitools.ThemeUtil;
import de.ph1b.audiobook.utils.L;
import de.ph1b.audiobook.utils.PrefsManager;

/**
 * Activity for choosing an audiobook folder. If there are multiple SD-Cards, the Activity unifies
 * them to a fake-folder structure. We must make sure that this is not choosable. When there are no
 * multiple sd-cards, we will directly show the content of the 1 SD Card.
 */
public class FolderChooserActivity extends BaseActivity implements View.OnClickListener {

    private static final String CURRENT_FOLDER_NAME = "currentFolderName";
    private static final String TAG = FolderChooserActivity.class.getSimpleName();
    private final ArrayList<File> currentFolderContent = new ArrayList<>();
    private boolean multiSd = true;
    private ArrayList<File> rootDirs;
    private File chosenFolder = null;
    private TextView currentFolderName;
    private Button chooseButton;
    private FolderAdapter adapter;
    private ImageButton upButton;

    private ArrayList<File> getStorageDirectories() {
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
        ArrayList<File> paths = new ArrayList<>();
        for (String s : rv) {
            File f = new File(s);
            if (f.exists() && f.isDirectory() && !paths.contains(f) && f.canRead() && f.listFiles() != null && f.listFiles().length > 0) {
                paths.add(f);
            }
        }
        Collections.sort(paths, new NaturalStringComparator());
        return paths;
    }

    private void changeFolder(File newFolder) {
        chosenFolder = newFolder;
        currentFolderContent.clear();
        currentFolderContent.addAll(getFilesFromFolder(chosenFolder));
        currentFolderName.setText(chosenFolder.getName());
        adapter.notifyDataSetChanged();
        setButtonEnabledDisabled();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // init fields
        setContentView(R.layout.activity_folder_chooser);

        // toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //get views
        ListView listView = (ListView) findViewById(R.id.listView);
        upButton = (ImageButton) findViewById(R.id.up);
        currentFolderName = (TextView) findViewById(R.id.chosenFolder);
        chooseButton = (Button) findViewById(R.id.choose);
        Button abortButton = (Button) findViewById(R.id.abort);

        //set listener
        upButton.setOnClickListener(this);
        chooseButton.setOnClickListener(this);
        abortButton.setOnClickListener(this);

        //setup
        adapter = new FolderAdapter(this, currentFolderContent);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                File selectedFile = adapter.getItem(position);
                if (selectedFile.isDirectory() && selectedFile.canRead()) {
                    changeFolder(adapter.getItem(position));
                }
            }
        });

        rootDirs = getStorageDirectories();
        if (rootDirs.size() == 1) {
            changeFolder(rootDirs.get(0));
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
                    changeFolder(f);
                }
            }
        }

        setButtonEnabledDisabled();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (chosenFolder != null) {
            outState.putString(CURRENT_FOLDER_NAME, chosenFolder.getAbsolutePath());
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
            return chosenFolder != null;
        } else {
            for (File f : rootDirs) {
                if (f.equals(chosenFolder)) {
                    return false; //to go up we must not already be in top level
                }
            }
            return true;
        }
    }

    private void up() {
        L.d(TAG, "up called. chosenFolder=" + chosenFolder);

        boolean chosenFolderIsInRoot = false;
        for (File f : rootDirs) {
            if (f.equals(chosenFolder)) {
                L.d(TAG, "chosen folder is in root");
                chosenFolderIsInRoot = true;
            }
        }
        if (multiSd && chosenFolderIsInRoot) {
            chosenFolder = null;
            currentFolderName.setText("");
            currentFolderContent.clear();
            currentFolderContent.addAll(rootDirs);
            adapter.notifyDataSetChanged();
        } else {
            chosenFolder = chosenFolder.getParentFile();
            currentFolderName.setText(chosenFolder.getName());
            ArrayList<File> parentContaining = getFilesFromFolder(chosenFolder);
            currentFolderContent.clear();
            currentFolderContent.addAll(parentContaining);
            adapter.notifyDataSetChanged();
        }
        setButtonEnabledDisabled();
    }

    /**
     * Gets the containing files of a folder (restricted to music and folders) in a naturally sorted
     * order.
     *
     * @param file The file to look for containing files
     * @return The containing files
     */
    private ArrayList<File> getFilesFromFolder(File file) {
        ArrayList<File> asList = new ArrayList<>();
        File[] containing = file.listFiles(BookAddingService.folderAndMusicFilter);
        if (containing != null) {
            asList = new ArrayList<>(Arrays.asList(containing));
            Collections.sort(asList, new NaturalStringComparator());
        }
        return asList;
    }

    /**
     * Sets the choose button enabled or disabled, depending on where we are in the hierarchy
     */
    private void setButtonEnabledDisabled() {
        boolean upEnabled = canGoBack();
        boolean chooseEnabled = !multiSd || upEnabled;

        chooseButton.setEnabled(chooseEnabled);
        upButton.setEnabled(upEnabled);
        @SuppressWarnings("deprecation") Drawable upIcon = upEnabled ? getResources().getDrawable(ThemeUtil.getResourceId(this, R.attr.folder_choose_up)) : null;
        upButton.setImageDrawable(upIcon);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.up:
                up();
                break;
            case R.id.choose:
                String newFolder = chosenFolder.getAbsolutePath();
                PrefsManager prefs = new PrefsManager(this);
                ArrayList<String> folders = prefs.getAudiobookFolders();
                boolean filesAreSubsets = true;
                boolean firstAddedFolder = folders.size() == 0;
                boolean sameFolder = false;
                for (String s : folders) {
                    if (s.equals(newFolder)) {
                        sameFolder = true;
                    }
                    String[] oldParts = s.split("/");
                    String[] newParts = newFolder.split("/");
                    for (int i = 0; i < Math.min(oldParts.length, newParts.length); i++) {
                        if (!oldParts[i].equals(newParts[i])) {
                            filesAreSubsets = false;
                        }
                    }
                    if (!sameFolder && filesAreSubsets) {
                        Toast.makeText(this, getString(R.string.adding_failed_subfolder) + "\n" + s + "\n" + newFolder, Toast.LENGTH_LONG).show();
                    }
                    if (filesAreSubsets) {
                        break;
                    }
                }

                if (firstAddedFolder || (!sameFolder && !filesAreSubsets)) {
                    folders.add(newFolder);
                    prefs.setAudiobookFolders(folders);
                    startService(BookAddingService.getRescanIntent(this, true));
                }

                finish();
                break;
            case R.id.abort:
                finish();
                break;
        }
    }
}