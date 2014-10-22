package de.ph1b.audiobook.fragment;


import android.app.Activity;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
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
import de.ph1b.audiobook.activity.BookChoose;
import de.ph1b.audiobook.activity.FilesChoose;
import de.ph1b.audiobook.activity.Settings;
import de.ph1b.audiobook.adapter.FileAdapter;
import de.ph1b.audiobook.dialog.EditBook;
import de.ph1b.audiobook.interfaces.OnBackPressedListener;
import de.ph1b.audiobook.utils.BookDetail;
import de.ph1b.audiobook.utils.CommonTasks;
import de.ph1b.audiobook.utils.DataBaseHelper;
import de.ph1b.audiobook.utils.MediaDetail;
import de.ph1b.audiobook.utils.NaturalOrderComparator;

public class FilesChooseFragment extends Fragment implements EditBook.OnEditBookFinished {

    private static final String TAG = "de.ph1b.audiobook.fragment.FilesChooseFragment";

    private final LinkedList<String> link = new LinkedList<String>();
    private final ArrayList<String> dirs = getStorageDirectories();
    private final ArrayList<File> fileList = new ArrayList<File>();
    private ArrayList<File> mediaFiles = new ArrayList<File>();


    private ListView fileListView;
    private Spinner dirSpinner;
    private FileAdapter adapter;

    private ActionMode actionMode;
    private ActionMode.Callback mActionModeCallback;

    private final ArrayList<String> audioTypes = genAudioTypes();

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


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
        ActionBar actionBar = ((ActionBarActivity) getActivity()).getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        PreferenceManager.setDefaultValues(getActivity(), R.xml.preferences, false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_files_choose, container, false);

        dirSpinner = (Spinner) v.findViewById(R.id.dirSpinner);
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
    private ArrayList<String> getStorageDirectories() {
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
        rv.add("/storage/extSdCard");
        rv.add(Environment.getExternalStorageDirectory().getAbsolutePath());
        if (BuildConfig.DEBUG) {
            rv.add("/storage/sdcard0/Audiobooks");
            rv.add("abc");
            rv.add("/system");
        }
        rv.add("/storage/emulated/0");
        rv.add("/storage/sdcard1");

        ArrayList<String> paths = new ArrayList<String>();
        for (String s : rv) {
            File f = new File(s);
            if (!paths.contains(s) && f.exists() && f.isDirectory() && f.canRead() && f.listFiles().length > 0)
                paths.add(s);
        }
        return paths;
    }

    private final OnBackPressedListener onBackPressedListener = new OnBackPressedListener() {
        @Override
        public synchronized void backPressed() {
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

        ((FilesChoose) getActivity()).setOnBackPressedListener(onBackPressedListener);
    }

    @Override
    public void onPause() {
        super.onPause();

        ((FilesChoose) getActivity()).setOnBackPressedListener(null);
    }


    public void checkStateChanged(final ArrayList<File> files) {
        if (BuildConfig.DEBUG) Log.d(TAG, "checkStateChanged!");
        if (files.size() > 0 && mActionModeCallback == null) {
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

                            if (BuildConfig.DEBUG) {
                                Log.d(TAG, "launching dialog with fiels");
                                for (File f : files) {
                                    Log.d(TAG, f.getName());
                                }
                            }
                            launchEditDialog(files);
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
        } else if (files.size() == 0) {
            if (actionMode != null) {
                actionMode.finish();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void launchEditDialog(ArrayList<File> files) {
        new AsyncTask<ArrayList<File>, Void, Void>() {
            private final ArrayList<File> imageFiles = new ArrayList<File>();
            private final ArrayList<Bitmap> bitmaps = new ArrayList<Bitmap>();
            private String bookTitle;


            @Override
            protected Void doInBackground(ArrayList<File>... params) {
                ArrayList<File> files = params[0];
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "doInBackground with");
                    for (File f : files)
                        Log.d(TAG, f.getName());
                }
                Collections.sort(files, new NaturalOrderComparator());

                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "have sorted with");
                    for (File f : files)
                        Log.d(TAG, f.getName());
                }

                //title
                File firstFile = files.get(0);
                bookTitle = firstFile.getName();
                if (firstFile.isFile())
                    bookTitle = bookTitle.substring(0, bookTitle.lastIndexOf("."));

                files = addFilesRecursive(files);

                mediaFiles = new ArrayList<File>();
                for (File f : files) {
                    if (isAudio(f)) {
                        mediaFiles.add(f);
                    } else if (isImage(f)) {
                        imageFiles.add(f);
                    }
                }

                //checking media files for covers
                int attempt = 0;
                for (File media : mediaFiles) {
                    if (isCancelled())
                        return null;
                    if (attempt++ > 4 || bitmaps.size() > 0)
                        break;
                    MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                    mmr.setDataSource(media.getAbsolutePath());
                    byte[] data = mmr.getEmbeddedPicture();
                    if (data != null) {
                        try {
                            Bitmap cover = BitmapFactory.decodeByteArray(data, 0, data.length);
                            if (cover != null)
                                bitmaps.add(cover);
                        } catch (Exception e) {
                            if (BuildConfig.DEBUG) Log.d(TAG, e.toString());
                        }
                    }
                }

                //checking imageFiles for cover
                for (File image : imageFiles) {
                    if (isCancelled())
                        return null;
                    Activity activity = getActivity();
                    if (activity != null) {
                        Bitmap cover = CommonTasks.genBitmapFromFile(image.getAbsolutePath(), activity.getApplicationContext());
                        if (cover != null)
                            bitmaps.add(cover);
                    }
                }
                return null;
            }


            @Override
            protected void onPostExecute(Void aVoid) {
                if (mediaFiles.size() == 0) {

                    CharSequence text = getString(R.string.book_no_media);
                    Toast toast = Toast.makeText(getActivity(), text, Toast.LENGTH_SHORT);
                    toast.show();
                } else {
                    //fragments onEditBookFinished sets visibilities of spinner and views
                    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
                    boolean fastAdd = sharedPref.getBoolean(getString(R.string.pref_fast_add), false);
                    if (!fastAdd) {
                        EditBook editBook = new EditBook();
                        Bundle bundle = new Bundle();

                        bundle.putParcelableArrayList(EditBook.BOOK_COVER, bitmaps);
                        bundle.putString(EditBook.DIALOG_TITLE, getString(R.string.book_add));
                        bundle.putString(EditBook.BOOK_NAME, bookTitle);

                        editBook.setArguments(bundle);
                        editBook.setTargetFragment(FilesChooseFragment.this, 0);
                        editBook.show(getFragmentManager(), TAG);
                    } else {
                        Bitmap cover = null;
                        if (bitmaps.size() > 0) {
                            cover = bitmaps.get(0);
                        }
                        onEditBookFinished(bookTitle, cover, true);
                    }
                }
            }
        }.execute(files);
    }


    private boolean isAudio(File file) {
        for (String s : audioTypes)
            if (file.getName().toLowerCase().endsWith(s))
                return true;
        return false;
    }

    @Override
    public void onEditBookFinished(String bookName, Bitmap cover, Boolean success) {
        if (success) {
            //adds book and launches progress dialog
            new AddBookAsync(mediaFiles, bookName, cover).execute();
        }
    }


    private class AddBookAsync extends AsyncTask<Void, Integer, Void> {
        private ProgressDialog progressDialog;
        private final ArrayList<File> files;
        private final String defaultName;
        private final Bitmap cover;


        public AddBookAsync(ArrayList<File> files, String defaultName, Bitmap cover) {
            this.files = files;
            this.defaultName = defaultName;
            this.cover = cover;
        }

        @Override
        protected void onPreExecute() {
            if (BuildConfig.DEBUG) Log.d(TAG, "AddBookAsync, onPreEx" + System.currentTimeMillis());
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.setCancelable(false);
            progressDialog.setTitle(getString(R.string.book_add_progress_title));
            progressDialog.setMessage(getString(R.string.book_add_progress_message));
            progressDialog.setIndeterminate(true);
            progressDialog.show();
        }

        @Override
        protected Void doInBackground(Void... params) {
            if (BuildConfig.DEBUG)
                Log.d(TAG, "AddBookAsync, doInBack" + System.currentTimeMillis());

            DataBaseHelper db = DataBaseHelper.getInstance(getActivity());

            BookDetail b = new BookDetail();
            b.setName(defaultName);
            if (cover != null) {
                String[] imagePaths = CommonTasks.saveCovers(cover, getActivity());
                if (imagePaths != null) {
                    b.setCover(imagePaths[0]);
                    b.setThumb(imagePaths[1]);
                }
            }
            int bookId = db.addBook(b);


            ArrayList<MediaDetail> media = new ArrayList<MediaDetail>();
            for (File f : files) {
                MediaDetail m = new MediaDetail();
                String fileName = f.getName();
                if (fileName.indexOf(".") > 0)
                    fileName = fileName.substring(0, fileName.lastIndexOf("."));
                m.setName(fileName);
                String path = f.getAbsolutePath();
                m.setPath(path);
                MediaMetadataRetriever metaRetriever = new MediaMetadataRetriever();
                metaRetriever.setDataSource(f.getAbsolutePath());
                int duration = Integer.parseInt(metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
                m.setDuration(duration);
                m.setBookId(bookId);
                media.add(m);
            }


            db.addMedia(media);
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if (BuildConfig.DEBUG)
                Log.d(TAG, "AddBookAsync, onPostEx" + System.currentTimeMillis());
            progressDialog.cancel();
            Intent i = new Intent(getActivity(), BookChoose.class);
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);
        }
    }


    private Boolean isImage(File f) {
        String fileName = f.getName().toLowerCase();
        return fileName.endsWith(".jpg") || fileName.endsWith(".png");
    }

    private ArrayList<File> addFilesRecursive(ArrayList<File> dir) {
        ArrayList<File> returnList = new ArrayList<File>();
        for (File f : dir) {
            if (f.exists() && f.isFile())
                returnList.add(f);
            else if (f.exists() && f.isDirectory()) {
                File[] content = f.listFiles();
                if (content.length > 0) {
                    ArrayList<File> tempReturn = addFilesRecursive(new ArrayList<File>(Arrays.asList(content)));
                    Collections.sort(tempReturn, new NaturalOrderComparator());
                    returnList.addAll(tempReturn);
                }
            }
        }
        return returnList;
    }

    private synchronized void populateList() {
        String path = link.getLast();
        //finishing action mode on populating new folder
        if (actionMode != null)
            actionMode.finish();

        File f = new File(path);
        File[] files = f.listFiles(filterShowAudioAndFolder);
        fileList.clear();
        Collections.addAll(fileList, files);

        //fileList = new ArrayList<File>();
        Collections.sort(fileList, new NaturalOrderComparator());
        adapter = new FileAdapter(fileList, this);
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
        public boolean accept(File file) {
            return !file.isHidden() && (file.isDirectory() || isAudio(file));
        }
    };
}
