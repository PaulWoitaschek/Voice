package de.ph1b.audiobook.activity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import de.ph1b.audiobook.BuildConfig;
import de.ph1b.audiobook.R;
import de.ph1b.audiobook.adapter.FolderAdapter;
import de.ph1b.audiobook.service.BookAddingService;
import de.ph1b.audiobook.utils.NaturalOrderComparator;
import de.ph1b.audiobook.utils.PrefsManager;

public class FolderChooserActivity extends ActionBarActivity {

    public static final String CHOSEN_FOLDER = "chosenFolder";
    private static final String CURRENT_FOLDER_NAME = "currentFolderName";
    private PrefsManager prefs;
    private ArrayList<File> rootDirs;
    private File chosenFolder = null;
    private ArrayList<File> currentFolderContent;
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
        final String rawSecondaryStoragesStr = System.getenv("SECONDARY_STORAGE");
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
        // Add all secondary storages
        if (!TextUtils.isEmpty(rawSecondaryStoragesStr)) {
            // All Secondary SD-CARDs splited into array
            final String[] rawSecondaryStorages = rawSecondaryStoragesStr.split(File.pathSeparator);
            Collections.addAll(rv, rawSecondaryStorages);
        }
        rv.add("/storage/extSdCard");
        rv.add(Environment.getExternalStorageDirectory().getAbsolutePath());
        if (BuildConfig.DEBUG) {
            rv.add("/storage/sdcard0/Audiobooks");
            rv.add("/mnt/shared");
        }
        rv.add("/storage/emulated/0");
        rv.add("/storage/sdcard1");
        ArrayList<File> paths = new ArrayList<>();
        for (String s : rv) {
            File f = new File(s);
            if (f.exists() && f.isDirectory() && !paths.contains(f) && f.canRead() && f.listFiles().length > 0)
                paths.add(f);
        }
        Collections.sort(paths, new NaturalOrderComparator());
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


    /**
     * @return true if we are in the first level of the folder hierarchy.
     */
    private boolean weAreOnTop() {
        if (chosenFolder != null) {
            File parent = chosenFolder.getParentFile();
            ArrayList<File> parentContaining = getFilesFromFolder(parent);
            for (File f : parentContaining) {
                if (rootDirs.contains(f)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_folder_chooser);
        rootDirs = getStorageDirectories();
        prefs = new PrefsManager(this);
        currentFolderContent = new ArrayList<>(rootDirs);
        adapter = new FolderAdapter(this, currentFolderContent);

        ListView listView = (ListView) findViewById(R.id.listView);
        upButton = (ImageButton) findViewById(R.id.up);
        currentFolderName = (TextView) findViewById(R.id.chosenFolder);
        chooseButton = (Button) findViewById(R.id.choose);
        Button abortButton = (Button) findViewById(R.id.abort);
        if (savedInstanceState != null) {
            String savedFolderPath = savedInstanceState.getString(CURRENT_FOLDER_NAME);
            if (savedFolderPath != null) {
                changeFolder(new File(savedFolderPath));
            }
        } else {
            File prefsAudiobookFolder = new File(prefs.latestAudiobookFolder());
            if (prefsAudiobookFolder.exists() && prefsAudiobookFolder.isDirectory() && prefsAudiobookFolder.canRead()) {
                changeFolder(prefsAudiobookFolder);
            }
        }
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                changeFolder(adapter.getItem(position));
            }
        });
        upButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (chosenFolder != null) {
                    if (weAreOnTop()) {
                        chosenFolder = null;
                        currentFolderName.setText("");
                        currentFolderContent.clear();
                        currentFolderContent.addAll(rootDirs);
                        adapter.notifyDataSetChanged();
                    } else {
                        File parent = chosenFolder.getParentFile();
                        chosenFolder = parent;
                        currentFolderName.setText(chosenFolder.getName());
                        currentFolderContent.clear();
                        ArrayList<File> parentContaining = getFilesFromFolder(parent);
                        currentFolderContent.addAll(parentContaining);
                        adapter.notifyDataSetChanged();
                    }
                }
                setButtonEnabledDisabled();
            }
        });
        chooseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String path = chosenFolder.getAbsolutePath();
                prefs.setLatestChosenFolder(path);
                Intent returnIntent = new Intent();
                returnIntent.putExtra(CHOSEN_FOLDER, path);
                setResult(RESULT_OK, returnIntent);
                finish();
            }
        });
        abortButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        setButtonEnabledDisabled();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (chosenFolder != null) {
            outState.putString(CURRENT_FOLDER_NAME, chosenFolder.getAbsolutePath());
        }
    }

    /**
     * Sets the choose button enabled or disabled, depending on where we are in the hierarchy
     */
    private void setButtonEnabledDisabled() {
        chooseButton.setEnabled(chosenFolder != null);
        upButton.setEnabled(weAreOnTop());
    }

    /**
     * Gets the containing files of a folder (restricted to music and folders) in a naturally sorted
     * order.
     * @param file The file to look for containing files
     * @return The containing files
     */
    private ArrayList<File> getFilesFromFolder(File file) {
        File[] containing = file.listFiles(BookAddingService.folderAndMusicFilter);
        ArrayList<File> asList = new ArrayList<>(Arrays.asList(containing));
        Collections.sort(asList, new NaturalOrderComparator());
        return asList;
    }
}