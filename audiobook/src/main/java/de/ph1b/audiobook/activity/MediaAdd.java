package de.ph1b.audiobook.activity;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;

import de.ph1b.audiobook.BuildConfig;
import de.ph1b.audiobook.R;
import de.ph1b.audiobook.adapter.FileAdapter;
import de.ph1b.audiobook.helper.CommonTasks;
import de.ph1b.audiobook.helper.NaturalOrderComparator;


public class MediaAdd extends ActionBarActivity implements CompoundButton.OnCheckedChangeListener {

    private static final String TAG = "MediaAdd";
    public static final String BOOK_PROPERTIES_DEFAULT_NAME = "defaultName";

    private ArrayList<File> fileList;
    private final LinkedList<String> link = new LinkedList<String>();
    public static final int AUDIO = 1;
    public static final int IMAGE = 2;
    public final static String FILES_AS_STRING = "filesAsString";
    private ListView fileListView;
    private Spinner dirSpinner;
    private ProgressBar progressBar;
    private FileAdapter adapter;

    private ActionMode actionMode;
    private ActionMode.Callback mActionModeCallback;

    private static ArrayList<File> endList;
    private ArrayList<File> dirAddList;


    private static ArrayList<String> audioTypes;

    private final ArrayList<String> dirs = new ArrayList<String>();


    public static final FileFilter filterShowAudioAndFolder = new FileFilter() {
        @Override
        public boolean accept(File pathname) {
            return !pathname.isHidden() && (pathname.isDirectory() || isAudio(pathname.getName()));
        }
    };


    private static boolean isAudio(String name) {
        for (String s : audioTypes)
            if (name.endsWith(s))
                return true;
        return false;
    }

    public static boolean isImage(String s) {
        return s.endsWith(".jpg") || s.endsWith(".png");
    }

    private ArrayList<String> genAudioTypes() {
        ArrayList<String> audioTypes = new ArrayList<String>();
        audioTypes.add(".3gp");
        audioTypes.add(".mp4");
        audioTypes.add(".m4a");
        audioTypes.add(".mp3");
        audioTypes.add(".mid");
        audioTypes.add(".xmf");
        audioTypes.add(".mxmf");
        audioTypes.add(".rtttl");
        audioTypes.add(".rtx");
        audioTypes.add(".ota");
        audioTypes.add(".imy");
        audioTypes.add(".ogg");
        audioTypes.add(".wav");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
            audioTypes.add(".aac");
            audioTypes.add(".flac");
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
            audioTypes.add(".mkv");

        return audioTypes;
    }

    /*
    adds spinner paths. returns true
    if there is more than one file
    for the spinner to add
     */
    private void addPathToSpinner() {
        ArrayList<String> paths = new ArrayList<String>();
        paths.add("/storage/extSdCard");
        paths.add(Environment.getExternalStorageDirectory().getAbsolutePath());
        if (BuildConfig.DEBUG)
            paths.add("/storage/sdcard0/Audiobooks");
        paths.add("/storage/emulated/0");
        for (String s : paths)
            if (!dirs.contains(s) && new File(s).isDirectory())
                dirs.add(s);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        audioTypes = genAudioTypes();

        setContentView(R.layout.activity_file_chooser);

        PreferenceManager.setDefaultValues(this, R.xml.preference_screen, false);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        dirSpinner = (Spinner) findViewById(R.id.dirSpinner);
        progressBar = (ProgressBar) findViewById(R.id.progress);
        fileListView = (ListView) findViewById(R.id.fileListView);

        addPathToSpinner();

        if (dirs.size() > 1) {
            dirSpinner.setVisibility(View.VISIBLE);
            ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(this, R.layout.sd_spinner_layout, dirs);
            spinnerAdapter.setDropDownViewResource(R.layout.sd_spinner_layout);
            dirSpinner.setAdapter(spinnerAdapter);
            dirSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (BuildConfig.DEBUG)
                        Log.d(TAG, "onItemSelected for chooser was called!");
                    link.clear();
                    link.add(dirs.get(position));
                    populateList(dirs.get(position));
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
        } else {
            dirSpinner.setVisibility(View.GONE);
        }

        if (dirs.size() > 0) {
            link.add(dirs.get(0)); //first element of file hierarchy
            populateList(dirs.get(0)); //Setting path to external storage directory to list it
        }
    }

    private void addMediaBundleAsync(ArrayList<File> dirAddList) {

        new AsyncTask<Object, Void, Boolean>() {

            private String defaultName;
            private ArrayList<String> dirAddAsString;

            @Override
            protected void onPreExecute() {
                fileListView.setVisibility(View.GONE);
                dirSpinner.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            protected Boolean doInBackground(Object... arrayLists) {
                @SuppressWarnings("unchecked")
                ArrayList<File> dirAddList = (ArrayList<File>) arrayLists[0];

                Collections.sort(dirAddList, new NaturalOrderComparator<File>());

                ArrayList<File> files = dirsToFiles(filterShowAudioAndFolder, dirAddList, AUDIO);
                if (files.size() != 0) {
                    defaultName = dirAddList.get(0).getName();
                    if (!dirAddList.get(0).isDirectory())
                        defaultName = defaultName.substring(0, defaultName.length() - 4);

                    dirAddAsString = new ArrayList<String>();
                    for (File f : files) {
                        dirAddAsString.add(f.getAbsolutePath());
                    }
                    return true;
                } else
                    return false;
            }

            @Override
            protected void onPostExecute(Boolean result) {
                //if adding worked start next activity,  otherwise stay here and make toast
                if (result) {
                    Intent intent = new Intent(getApplicationContext(), BookAdd.class);
                    intent.putExtra(BOOK_PROPERTIES_DEFAULT_NAME, defaultName);
                    intent.putStringArrayListExtra(FILES_AS_STRING, dirAddAsString);
                    startActivity(intent);
                } else {

                    if (dirs.size() > 1)
                        dirSpinner.setVisibility(View.VISIBLE);
                    fileListView.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.GONE);

                    CharSequence text = getString(R.string.book_no_media);
                    Toast toast = Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
        }.execute(dirAddList);
    }

    @Override
    public void onBackPressed() {
        if (link.getLast().equals(link.getFirst())) {
            Intent intent = new Intent(this, MediaView.class);
            startActivity(intent);
        }
        link.removeLast();
        String now = link.getLast();
        link.removeLast();
        populateList(now);
    }

    private void populateList(String path) {

        //finishing action mode on populating new folder
        if (actionMode != null) {
            actionMode.finish();
        }
        if (BuildConfig.DEBUG)
            Log.e(TAG, "Populate this folder: " + path);

        link.add(path);
        File f = new File(path);
        File[] files = f.listFiles(filterShowAudioAndFolder);
        fileList = new ArrayList<File>(Arrays.asList(files));

        //fileList = new ArrayList<File>();
        Collections.sort(fileList, new NaturalOrderComparator<File>());
        adapter = new FileAdapter(fileList, this);
        fileListView.setAdapter(adapter);

        fileListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (fileList.get(position).isDirectory()) {
                    populateList(fileList.get(position).getAbsolutePath());
                }
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.action_media_add, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(this, Preferences.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public static ArrayList<File> dirsToFiles(FileFilter filter, ArrayList<File> dir, int choice) {
        endList = new ArrayList<File>();
        for (File f : dir) {
            if (choice == AUDIO && isAudio(f.getName())) {
                endList.add(f);
            } else if (choice == IMAGE && isImage(f.getName())) {
                endList.add(f);
            }
            addDirRec(f, filter);
        }
        return endList;
    }

    private static void addDirRec(File file, FileFilter filter) {
        ArrayList<File> returnList = new ArrayList<File>();
        if (file.isDirectory()) {
            File[] tempList = file.listFiles(filter);

            if (tempList != null) {
                Collections.sort(Arrays.asList(tempList), new NaturalOrderComparator<File>());
                for (File f : tempList) {
                    if (f.isDirectory()) {
                        addDirRec(f, filter);
                    }
                }
                for (File f : tempList) {
                    if (!f.isDirectory())
                        returnList.add(f);
                }
            }
        }
        Collections.sort(returnList, new NaturalOrderComparator<File>());
        for (File f : returnList)
            if (BuildConfig.DEBUG)
                Log.d(TAG, f.getAbsolutePath());
        endList.addAll(returnList);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (dirs.size() > 1)
            dirSpinner.setVisibility(View.VISIBLE);
        fileListView.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);
        //checking if external storage is available
        new CommonTasks().checkExternalStorage(this);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (BuildConfig.DEBUG)
            Log.d(TAG, String.valueOf(isChecked));
        fileListView.getPositionForView(buttonView);
    }

    public void checkStateChanged(ArrayList<File> dirAddList) {
        this.dirAddList = dirAddList;
        if (dirAddList.size() > 0 && mActionModeCallback == null) {
            mActionModeCallback = new ActionMode.Callback() {

                @Override
                public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                    MenuInflater inflater = mode.getMenuInflater();
                    inflater.inflate(R.menu.action_mode_mediaadd, menu);
                    return true;
                }

                @Override
                public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                    return false;
                }

                @Override
                public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                    switch (item.getItemId()) {
                        case R.id.action_add_badge:
                            for (File f : MediaAdd.this.dirAddList)
                                if (BuildConfig.DEBUG) Log.d(TAG, "Adding: " + f.getAbsolutePath());
                            addMediaBundleAsync(MediaAdd.this.dirAddList);
                            mode.finish();
                            return true;
                        default:
                            return false;
                    }
                }

                @Override
                public void onDestroyActionMode(ActionMode mode) {
                    if (adapter != null)
                        adapter.clearCheckBoxes();
                    mActionModeCallback = null;
                }
            };

            actionMode = startSupportActionMode(mActionModeCallback);
        } else if (dirAddList.size() == 0) {
            if (actionMode != null) {
                actionMode.finish();
            }
        }
    }
}
