package de.ph1b.audiobook.fragment;


import android.app.Fragment;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;

import de.ph1b.audiobook.BuildConfig;
import de.ph1b.audiobook.R;
import de.ph1b.audiobook.activity.MediaAdd;
import de.ph1b.audiobook.activity.MediaView;
import de.ph1b.audiobook.adapter.FileAdapter;
import de.ph1b.audiobook.interfaces.OnBackPressedListener;
import de.ph1b.audiobook.utils.NaturalOrderComparator;

public class FilesChoose extends Fragment implements CompoundButton.OnCheckedChangeListener {

    public static final String TAG = "de.ph1b.audiobook.fragment.ChooseFilesFragment";

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

        ((MediaAdd) getActivity()).setOnBackPressedListener(new OnBackPressedListener() {
            @Override
            public void backPressed() {
                if (BuildConfig.DEBUG)
                    Log.d(TAG, "backPressed called with link size: " + link.size());
                if (link.getLast().equals(link.getFirst())) {
                    startActivity(new Intent(getActivity(), MediaView.class));
                }
                link.removeLast();
                String now = link.getLast();
                if (link.size() > 0)
                    link.removeLast();
                populateList(now);
            }
        });

        return v;
    }

    public boolean allowBackPress() {
        return link.size() > 0;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.action_media_add, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                startActivity(new Intent(getActivity(), MediaView.class));
                return true;
            case R.id.action_settings:
                getFragmentManager().beginTransaction()
                        .replace(android.R.id.content, new Preferences())
                        .addToBackStack(Preferences.TAG)
                        .commit();
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
                            for (File f : FilesChoose.this.dirAddList)
                                if (BuildConfig.DEBUG)
                                    Log.d(TAG, "Adding: " + f.getAbsolutePath());
                            addMediaBundleAsync(FilesChoose.this.dirAddList);
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

                ArrayList<File> files = MediaAdd.dirsToFiles(MediaAdd.filterShowAudioAndFolder, dirAddList, MediaAdd.AUDIO);
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
                //if adding worked start next fragment,  otherwise stay here and make toast
                if (result) {
                    Bundle bundle = new Bundle();
                    bundle.putString(MediaAdd.BOOK_PROPERTIES_DEFAULT_NAME, defaultName);
                    bundle.putStringArrayList(MediaAdd.FILES_AS_STRING, dirAddAsString);
                    FilesAdd fragment = new FilesAdd();
                    fragment.setArguments(bundle);

                    getFragmentManager().beginTransaction()
                            .replace(android.R.id.content, fragment)
                            .addToBackStack(FilesAdd.TAG)
                            .commit();
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
        }.execute(dirAddList);
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
        File[] files = f.listFiles(MediaAdd.filterShowAudioAndFolder);
        fileList = new ArrayList<File>(Arrays.asList(files));

        //fileList = new ArrayList<File>();
        Collections.sort(fileList, new NaturalOrderComparator<File>());
        adapter = new FileAdapter(fileList, getActivity(), this);
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
}
