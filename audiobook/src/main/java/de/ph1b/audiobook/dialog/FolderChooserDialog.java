package de.ph1b.audiobook.dialog;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
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


public class FolderChooserDialog extends DialogFragment {

    ArrayList<File> rootDirs;
    File chosenFolder = null;
    ArrayList<File> currentFolderContent;


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        @SuppressLint("InflateParams") View v = inflater.inflate(R.layout.dialog_folder_chooser, null);

        builder.setTitle("chose folder");
        builder.setPositiveButton("set", null);
        builder.setNegativeButton("cancel", null);

        ListView listView = (ListView) v.findViewById(R.id.listView);
        ImageButton upButton = (ImageButton) v.findViewById(R.id.up);
        final TextView currentFolderName = (TextView) v.findViewById(R.id.chosenFolder);

        rootDirs = getStorageDirectories();


        // clone list so root dirs won't get changed when we navigate
        currentFolderContent = new ArrayList<>(rootDirs);

        final FolderAdapter adapter = new FolderAdapter(getActivity(), currentFolderContent);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                chosenFolder = adapter.getItem(position);

                currentFolderContent.clear();
                currentFolderContent.addAll(getFilesFromFolder(chosenFolder));
                adapter.notifyDataSetChanged();
                currentFolderName.setText(chosenFolder.getName());
            }
        });

        upButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (chosenFolder != null) {
                    File parent = chosenFolder.getParentFile();


                    ArrayList<File> parentContaining = getFilesFromFolder(parent);
                    boolean weAreOnTop = false;
                    for (File f : parentContaining) {
                        if (rootDirs.contains(f)) {
                            weAreOnTop = true;
                        }
                    }
                    if (weAreOnTop) {
                        chosenFolder = null;
                        currentFolderContent.clear();
                        currentFolderContent.addAll(rootDirs);
                        adapter.notifyDataSetChanged();
                    } else {
                        chosenFolder = parent;
                        currentFolderContent.clear();
                        currentFolderContent.addAll(parentContaining);
                        adapter.notifyDataSetChanged();
                    }
                }
            }
        });


        builder.setView(v);
        AlertDialog dialog = builder.create();
        dialog.getWindow().getAttributes().height = WindowManager.LayoutParams.MATCH_PARENT;
        return dialog;
    }

    private ArrayList<File> getFilesFromFolder(File file) {
        FileFilter directoryFilter = new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }
        };

        File[] containing = file.listFiles(directoryFilter);
        return new ArrayList<>(Arrays.asList(containing));
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
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
        return paths;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog d = getDialog();


        /**

         //noinspection deprecation
         int displayHeight = dialog.getWindow()
         .getWindowManager().getDefaultDisplay()
         .getHeight();


         WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
         lp.copyFrom(dialog.getWindow().getAttributes());
         lp.height = (int) (displayHeight * 0.8f);**/

        //   dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
    }
}
