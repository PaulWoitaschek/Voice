package de.ph1b.audiobook.fragment;


import android.app.Fragment;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.regex.Pattern;

import de.ph1b.audiobook.BuildConfig;
import de.ph1b.audiobook.R;
import de.ph1b.audiobook.activity.FilesAdd;
import de.ph1b.audiobook.activity.FilesChoose;
import de.ph1b.audiobook.activity.Settings;
import de.ph1b.audiobook.adapter.FileAdapter;
import de.ph1b.audiobook.interfaces.OnBackPressedListener;
import de.ph1b.audiobook.utils.CommonTasks;
import de.ph1b.audiobook.utils.NaturalOrderComparator;

public class FilesChooseFragment extends Fragment implements CompoundButton.OnCheckedChangeListener {

    private static final String TAG = "de.ph1b.audiobook.fragment.FilesChooseFragment";

    private final LinkedList<String> link = new LinkedList<String>();
    private final ArrayList<String> dirs = new ArrayList<String>();
    private ArrayList<File> fileList;
    private ArrayList<File> dirAddList;

    private ListView fileListView;
    private Spinner dirSpinner;
    private ProgressBar progressBar;
    private FileAdapter adapter;

    private ActionMode actionMode;
    private ActionMode.Callback mActionModeCallback;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
        ActionBar actionBar = ((ActionBarActivity) getActivity()).getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        //TESTING IF THIS WORKS!!! (for backup leave traditional hardcoded way for now.)
        String[] storage = getStorageDirectories();
        Collections.addAll(dirs, storage);
        addPathToSpinner();

        PreferenceManager.setDefaultValues(getActivity(), R.xml.preferences, false);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_files_choose, container, false);

        dirSpinner = (Spinner) v.findViewById(R.id.dirSpinner);
        progressBar = (ProgressBar) v.findViewById(R.id.progress);
        fileListView = (ListView) v.findViewById(R.id.fileListView);

        if (dirs.size() > 1) {
            dirSpinner.setVisibility(View.VISIBLE);
            ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(getActivity(), R.layout.sd_spinner_layout, dirs);
            spinnerAdapter.setDropDownViewResource(R.layout.sd_spinner_layout);
            dirSpinner.setAdapter(spinnerAdapter);
            dirSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (BuildConfig.DEBUG)
                        Log.d(TAG, "onItemSelected for chooser was called!");
                    link.clear();
                    link.add(dirs.get(position));
                    populateList();
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
            populateList(); //Setting path to external storage directory to list it
        }

        return v;
    }

    private static final Pattern DIR_SEPARATOR = Pattern.compile("/");

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private String[] getStorageDirectories() {
        // Final set of paths
        final Set<String> rv = new HashSet<String>();
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
        return rv.toArray(new String[rv.size()]);
    }

    private final OnBackPressedListener onBackPressedListener = new OnBackPressedListener() {
        @Override
        public synchronized void backPressed() {
            if (BuildConfig.DEBUG)
                Log.d(TAG, "backPressed called with link size: " + link.size());
            if (link.size() < 2) {
                //setting onBackPressedListener to null and invoke new onBackPressed
                //to invoke super.onBackPressed();
                ((FilesChoose) getActivity()).setOnBackPressedListener(null);
                getActivity().onBackPressed();
            } else {
                link.removeLast();
                populateList();
            }
        }
    };

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.action_media_add, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(getActivity(), Settings.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (dirs.size() > 1)
            dirSpinner.setVisibility(View.VISIBLE);
        fileListView.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);

        ((FilesChoose) getActivity()).setOnBackPressedListener(onBackPressedListener);
    }

    @Override
    public void onPause() {
        super.onPause();

        ((FilesChoose) getActivity()).setOnBackPressedListener(null);
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
                            addMediaBundleAsync();
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

            actionMode = ((ActionBarActivity) getActivity()).startSupportActionMode(mActionModeCallback);
        } else if (dirAddList.size() == 0) {
            if (actionMode != null) {
                actionMode.finish();
            }
        }
    }

    private void addPathToSpinner() {
        ArrayList<String> paths = new ArrayList<String>();
        paths.add("/storage/extSdCard");
        paths.add(Environment.getExternalStorageDirectory().getAbsolutePath());
        if (BuildConfig.DEBUG)
            paths.add("/storage/sdcard0/Audiobooks");
        paths.add("/storage/emulated/0");
        paths.add("/storage/sdcard1");

        for (String s : paths)
            if (!dirs.contains(s) && new File(s).isDirectory())
                dirs.add(s);
    }

    private boolean hasAudio(ArrayList<File> files) {
        for (File f : files) {
            if (FilesChoose.isAudio(f.getName())) {
                return true;
            } else if (f.isDirectory()) {
                if (hasAudio(new ArrayList<File>(Arrays.asList(f.listFiles()))))
                    return true;
            }
        }
        return false;
    }

    private void addMediaBundleAsync() {

        new AsyncTask<Void, Void, Boolean>() {

            @Override
            protected void onPreExecute() {
                fileListView.setVisibility(View.GONE);
                dirSpinner.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            protected Boolean doInBackground(Void... params) {
                Collections.sort(dirAddList, new NaturalOrderComparator<File>());
                return hasAudio(dirAddList);
            }

            @Override
            protected void onPostExecute(Boolean result) {
                //if adding worked start next activity,  otherwise stay here and make toast
                if (result) {
                    ArrayList<String> dirAddAsString = new ArrayList<String>();
                    for (File f : dirAddList)
                        dirAddAsString.add(f.getAbsolutePath());
                    String defaultName = dirAddList.get(0).getName();

                    Intent i = new Intent(getActivity(), FilesAdd.class);
                    Bundle bundle = new Bundle();
                    bundle.putString(FilesChoose.BOOK_PROPERTIES_DEFAULT_NAME, defaultName);
                    bundle.putStringArrayList(FilesChoose.FILES_AS_STRING, dirAddAsString);
                    i.putExtras(bundle);
                    startActivity(i);
                } else {
                    if (dirs.size() > 1)
                        dirSpinner.setVisibility(View.VISIBLE);
                    fileListView.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.GONE);

                    CharSequence text = getString(R.string.book_no_media);
                    Toast toast = Toast.makeText(getActivity(), text, Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
        }.execute();
    }

    private synchronized void populateList() {
        String path = link.getLast();
        //finishing action mode on populating new folder
        if (actionMode != null) {
            actionMode.finish();
        }
        if (BuildConfig.DEBUG)
            Log.e(TAG, "Populate this folder: " + path);

        File f = new File(path);
        File[] files = f.listFiles(filterShowAudioAndFolder);
        fileList = new ArrayList<File>(Arrays.asList(files));

        //fileList = new ArrayList<File>();
        Collections.sort(fileList, new NaturalOrderComparator<File>());
        adapter = new FileAdapter(fileList, getActivity(), this);
        fileListView.setAdapter(adapter);
        fileListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (fileList.get(position).isDirectory()) {
                    link.add(fileList.get(position).getAbsolutePath());
                    populateList();
                }
            }
        });
    }

    private final FileFilter filterShowAudioAndFolder = new FileFilter() {
        @Override
        public boolean accept(File pathname) {
            return !pathname.isHidden() && (pathname.isDirectory() || FilesChoose.isAudio(pathname.getName()));
        }
    };
}
