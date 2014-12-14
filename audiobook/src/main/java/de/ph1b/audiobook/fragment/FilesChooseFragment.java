package de.ph1b.audiobook.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Intent;
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
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
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
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.File;
import java.io.FileFilter;
import java.lang.ref.WeakReference;
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
import de.ph1b.audiobook.content.BookDetail;
import de.ph1b.audiobook.content.DataBaseHelper;
import de.ph1b.audiobook.content.MediaDetail;
import de.ph1b.audiobook.dialog.EditBook;
import de.ph1b.audiobook.dialog.FileAddingErrorDialog;
import de.ph1b.audiobook.interfaces.OnBackPressedListener;
import de.ph1b.audiobook.utils.ImageHelper;
import de.ph1b.audiobook.utils.NaturalOrderComparator;

public class FilesChooseFragment extends Fragment implements EditBook.OnEditBookFinished, FileAddingErrorDialog.ConfirmationListener {
    private static final String TAG = "de.ph1b.audiobook.fragment.FilesChooseFragment";
    private static final Pattern DIR_SEPARATOR = Pattern.compile("/");
    private final LinkedList<String> link = new LinkedList<>();
    private final ArrayList<String> dirs = getStorageDirectories();
    private final ArrayList<String> audioTypes = genAudioTypes();
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
    private final FileFilter filterShowAudioAndFolder = new FileFilter() {
        @Override
        public boolean accept(File file) {
            return !file.isHidden() && (file.isDirectory() || isAudio(file));
        }
    };
    private ArrayList<File> mediaFiles = new ArrayList<>();
    private RecyclerView recyclerView;
    private Spinner dirSpinner;
    private ProgressBar progressBar;
    private FileAdapter adapter;
    private ActionMode actionMode;
    private ActionMode.Callback mActionModeCallback;

    private ArrayList<String> genAudioTypes() {
        ArrayList<String> audioTypes = new ArrayList<>();
        audioTypes.add(".3gp");
        audioTypes.add(".mp4");
        audioTypes.add(".m4a");
        audioTypes.add(".m4b");
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
        audioTypes.add(".aac");
        audioTypes.add(".flac");
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
        recyclerView = (RecyclerView) v.findViewById(R.id.recyclerView);
        progressBar = (ProgressBar) v.findViewById(R.id.progress);
        if (dirs.size() > 1) {
            dirSpinner.setVisibility(View.VISIBLE);
            ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(getActivity(), R.layout.sd_spinner_layout, dirs);
            spinnerAdapter.setDropDownViewResource(R.layout.sd_spinner_layout);
            dirSpinner.setAdapter(spinnerAdapter);
            dirSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
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

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private ArrayList<String> getStorageDirectories() {
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
        ArrayList<String> paths = new ArrayList<>();
        for (String s : rv) {
            File f = new File(s);
            if (!paths.contains(s) && f.exists() && f.isDirectory() && f.canRead() && f.listFiles().length > 0)
                paths.add(s);
        }
        return paths;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
// Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.action_only_settings, menu);
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

    @Override
    public void onButtonClicked(boolean keep, ArrayList<MediaDetail> intactFiles, int bookId) {
        Activity a = getActivity();
        if (a != null) {
            DataBaseHelper db = DataBaseHelper.getInstance(a);
            if (keep) {
                db.addMedia(intactFiles);
                Intent i = new Intent(a, BookChoose.class);
                i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
            } else {
                db.deleteBook(db.getBook(bookId));
            }
        }
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
            new AddBookAsync(mediaFiles, bookName, cover).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    private Boolean isImage(File f) {
        String fileName = f.getName().toLowerCase();
        return fileName.endsWith(".jpg") || fileName.endsWith(".png");
    }

    /**
     * Adds files recursively. First takes all files and adds them sorted to the return list. Then
     * sorts the folders, and then adds their content sorted to the return list.
     *
     * @param dir The dirs and files to be added
     * @return All the files containing in a natural sorted order.
     */
    private ArrayList<File> addFilesRecursive(ArrayList<File> dir) {
        ArrayList<File> returnList = new ArrayList<>();
        ArrayList<File> fileList = new ArrayList<>();
        ArrayList<File> dirList = new ArrayList<>();
        for (File f : dir) {
            if (f.exists() && f.isFile())
                fileList.add(f);
            else if (f.exists() && f.isDirectory()) {
                dirList.add(f);
            }
        }
        Collections.sort(fileList, new NaturalOrderComparator());
        returnList.addAll(fileList);
        Collections.sort(dirList, new NaturalOrderComparator());
        for (File f : dirList) {
            ArrayList<File> content = new ArrayList<>(Arrays.asList(f.listFiles()));
            if (content.size() > 0) {
                ArrayList<File> tempReturn = addFilesRecursive(content);
                returnList.addAll(tempReturn);
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
        ArrayList<File> fileList = new ArrayList<>(Arrays.asList(f.listFiles(filterShowAudioAndFolder)));
        Collections.sort(fileList, new NaturalOrderComparator());
        FileAdapter.ItemInteraction itemInteraction = new FileAdapter.ItemInteraction() {
            @Override
            public void onCheckStateChanged() {
                if (adapter.getCheckedItems().size() > 0 && mActionModeCallback == null) {
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
                                    new LaunchEditDialog(adapter.getCheckedItems(), dirSpinner, recyclerView, progressBar).execute();
                                    mode.finish();
                                    return true;
                                default:
                                    return false;
                            }
                        }

                        @Override
                        public void onDestroyActionMode(ActionMode mode) {
                            adapter.clearCheckBoxes();
                            mActionModeCallback = null;
                        }
                    };
                    actionMode = ((ActionBarActivity) getActivity()).startSupportActionMode(mActionModeCallback);
                } else if (adapter.getCheckedItems().size() == 0) {
                    if (actionMode != null) {
                        actionMode.finish();
                    }
                }
            }

            @Override
            public void onItemClicked(int position) {
                File f = adapter.getItem(position);
                if (f.isDirectory()) {
                    link.add(f.getAbsolutePath());
                    populateList();
                }
            }
        };
        adapter = new FileAdapter(fileList, itemInteraction);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
    }

    private class LaunchEditDialog extends AsyncTask<Void, Void, Void> {
        private final ArrayList<File> imageFiles = new ArrayList<>();
        private final ArrayList<Bitmap> bitmaps = new ArrayList<>();
        private final WeakReference<Spinner> spinnerWeakReference;
        private final WeakReference<RecyclerView> recyclerViewWeakReference;
        private final WeakReference<ProgressBar> progressBarWeakReference;
        private final int oldSpinnerVisibility;
        private String bookTitle;
        private ArrayList<File> files;

        public LaunchEditDialog(ArrayList<File> files, Spinner spinner, RecyclerView recyclerView, ProgressBar progressBar) {
            this.files = files;
            spinnerWeakReference = new WeakReference<>(spinner);
            recyclerViewWeakReference = new WeakReference<>(recyclerView);
            progressBarWeakReference = new WeakReference<>(progressBar);
            oldSpinnerVisibility = spinner.getVisibility();
        }

        @Override
        protected void onPreExecute() {
            Spinner spinner = spinnerWeakReference.get();
            RecyclerView recyclerView = recyclerViewWeakReference.get();
            ProgressBar progressBar = progressBarWeakReference.get();
            spinner.setVisibility(View.GONE);
            recyclerView.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected Void doInBackground(Void... params) {
            //title
            File firstFile = files.get(0);
            bookTitle = firstFile.getName();
            if (firstFile.isFile())
                bookTitle = bookTitle.substring(0, bookTitle.lastIndexOf("."));
            files = addFilesRecursive(files);
            mediaFiles = new ArrayList<>();
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
                try {
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
                } catch (IllegalArgumentException e) {
                    if (BuildConfig.DEBUG)
                        Log.d(TAG, "IllegalArgumentException at finding covers at: " + media.getAbsolutePath());
                } catch (RuntimeException e) {
                    if (BuildConfig.DEBUG)
                        Log.d(TAG, "RuntimeException at finding covers at: " + media.getAbsolutePath());
                }
            }
            //checking imageFiles for cover
            for (File image : imageFiles) {
                if (isCancelled())
                    return null;
                Activity activity = getActivity();
                if (activity != null) {
                    Bitmap cover = ImageHelper.genBitmapFromFile(image.getAbsolutePath(), activity, ImageHelper.TYPE_COVER);
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
                EditBook editBook = new EditBook();
                Bundle bundle = new Bundle();
                bundle.putParcelableArrayList(EditBook.BOOK_COVER, bitmaps);
                bundle.putString(EditBook.DIALOG_TITLE, getString(R.string.book_add));
                bundle.putString(EditBook.BOOK_NAME, bookTitle);
                editBook.setArguments(bundle);
                editBook.setTargetFragment(FilesChooseFragment.this, 0);
                editBook.show(getFragmentManager(), TAG);
            }
            Spinner spinner = spinnerWeakReference.get();
            RecyclerView recyclerView = recyclerViewWeakReference.get();
            ProgressBar progressBar = progressBarWeakReference.get();
            spinner.setVisibility(oldSpinnerVisibility);
            recyclerView.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
        }
    }

    private class AddBookAsync extends AsyncTask<Void, Integer, Boolean> {
        private final ArrayList<File> files;
        private final String defaultName;
        private final Bitmap cover;
        private final ArrayList<String> errorFiles = new ArrayList<>();
        private final ArrayList<MediaDetail> media = new ArrayList<>();
        private ProgressDialog progressDialog;
        private int bookId;

        public AddBookAsync(ArrayList<File> files, String defaultName, Bitmap cover) {
            this.files = files;
            this.defaultName = defaultName;
            this.cover = cover;
        }

        @Override
        protected void onPreExecute() {
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.setCancelable(false);
            progressDialog.setTitle(getString(R.string.book_add_progress_title));
            progressDialog.setMessage(getString(R.string.book_add_progress_message));
            progressDialog.setIndeterminate(true);
            progressDialog.show();
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            DataBaseHelper db = DataBaseHelper.getInstance(getActivity());
            BookDetail b = new BookDetail();
            b.setName(defaultName);
            if (cover != null) {
                String coverPath = ImageHelper.saveCover(cover, getActivity());
                if (coverPath != null) {
                    b.setCover(coverPath);
                }
            }

            MediaMetadataRetriever metaRetriever = new MediaMetadataRetriever();
            bookId = db.addBook(b);
            for (File f : files) {
                MediaDetail m = new MediaDetail();
                String fileName = f.getName();
                if (fileName.indexOf(".") > 0)
                    fileName = fileName.substring(0, fileName.lastIndexOf("."));
                m.setName(fileName);
                String path = f.getAbsolutePath();
                m.setPath(path);

                try {
                    long start = System.currentTimeMillis();
                    metaRetriever.setDataSource(f.getAbsolutePath());
                    int duration = Integer.parseInt(metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
                    m.setDuration(duration);
                    m.setBookId(bookId);
                    media.add(m);
                } catch (IllegalArgumentException e) {
                    if (BuildConfig.DEBUG)
                        Log.d(TAG, "IllegalArgumentException at getting duration of: " + f.getAbsolutePath());
                    errorFiles.add(f.getName());
                } catch (RuntimeException e) {
                    if (BuildConfig.DEBUG)
                        Log.d(TAG, "RuntimeException at getting duration of: " + f.getAbsolutePath());
                    errorFiles.add(f.getName());
                }
            }

            metaRetriever.release();

            if (errorFiles.size() == 0) {
                db.addMedia(media);
                return true;
            } else {
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            Activity a = getActivity();
            if (a != null) {
                progressDialog.cancel();
                if (result) {
                    Intent i = new Intent(a, BookChoose.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(i);
                } else {
                    if (media.size() == 0) {
                        CharSequence text = getString(R.string.error_in_file_all_defect);
                        Toast toast = Toast.makeText(a, text, Toast.LENGTH_SHORT);
                        toast.show();
                    } else {
                        FileAddingErrorDialog dialog = new FileAddingErrorDialog();
                        Bundle args = new Bundle();
                        args.putStringArrayList(FileAddingErrorDialog.ARG_ERROR_FILES, errorFiles);
                        args.putParcelableArrayList(FileAddingErrorDialog.ARG_INTACT_FILES, media);
                        args.putInt(FileAddingErrorDialog.ARG_BOOK_ID, bookId);
                        dialog.setArguments(args);
                        dialog.setTargetFragment(FilesChooseFragment.this, 42);
                        dialog.show(getFragmentManager(), FileAddingErrorDialog.TAG);
                    }
                }
            }
        }
    }
}