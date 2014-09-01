package de.ph1b.audiobook.fragment;


import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.apache.http.conn.util.InetAddressUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

import de.ph1b.audiobook.BuildConfig;
import de.ph1b.audiobook.R;
import de.ph1b.audiobook.activity.BookChoose;
import de.ph1b.audiobook.activity.FilesAdd;
import de.ph1b.audiobook.activity.FilesChoose;
import de.ph1b.audiobook.activity.Settings;
import de.ph1b.audiobook.utils.BookDetail;
import de.ph1b.audiobook.utils.CommonTasks;
import de.ph1b.audiobook.utils.DataBaseHelper;
import de.ph1b.audiobook.utils.MediaDetail;
import de.ph1b.audiobook.utils.NaturalOrderComparator;


public class FilesAddFragment extends Fragment {

    private EditText fieldName;

    private DataBaseHelper db;

    private static final String TAG = "de.ph1b.audiobook.fragment.FilesAdd";

    private String bookName;
    private ImageView coverView;
    private ProgressBar coverLoadingBar;
    private int coverPosition = 0;
    private int pageCounter = 0;
    private ArrayList<File> dirAddList = new ArrayList<File>();

    private ProgressDialog progressDialog;

    private final ArrayList<Bitmap> bitmapList = new ArrayList<Bitmap>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_files_add, container, false);

        Bundle extras = ((FilesAdd) getActivity()).getExtras();

        ArrayList<String> fileFolderPaths = extras.getStringArrayList(FilesChoose.FILES_AS_STRING);
        for (String s : fileFolderPaths) {
            if (BuildConfig.DEBUG) Log.d(TAG, s);
            dirAddList.add(new File(s));
        }
        dirAddList = addFilesRecursive(dirAddList);

        coverView = (ImageView) v.findViewById(R.id.cover);
        coverLoadingBar = (ProgressBar) v.findViewById(R.id.cover_loading);
        fieldName = (EditText) v.findViewById(R.id.book_name);

        String defaultName = extras.getString(FilesChoose.BOOK_PROPERTIES_DEFAULT_NAME);
        fieldName.setText(defaultName);

        if (fieldName != null) {
            String name = fieldName.getText().toString();
            if (name.length() > 0) {
                int size = CommonTasks.getDisplayMinSize(getActivity());
                Bitmap cover = CommonTasks.genCapital(name, size, getResources());
                bitmapList.add(cover);
            }
        }

        // getting covers from local
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected void onPreExecute() {
                setCoverLoading(true);
            }

            @Override
            protected Boolean doInBackground(Void... voids) {

                //checks for covers in audio, stops after first score
                for (File f : dirAddList) {
                    String path = f.getAbsolutePath();
                    if (FilesChoose.isAudio(path)) {
                        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                        mmr.setDataSource(path);
                        byte[] data = mmr.getEmbeddedPicture();
                        if (data != null) {
                            try {
                                Bitmap cover = BitmapFactory.decodeByteArray(data, 0, data.length);
                                if (cover != null) {
                                    bitmapList.add(cover);
                                    break;
                                }
                            } catch (Exception e) {
                                if (BuildConfig.DEBUG) Log.d(TAG, e.toString());
                            }
                        }
                    }
                }

                //adds all covers found in picture files
                for (File f : dirAddList) {
                    String path = f.getAbsolutePath();
                    if (FilesChoose.isImage(path)) {
                        Bitmap cover = BitmapFactory.decodeFile(path);
                        if (cover != null)
                            bitmapList.add(cover);
                    }
                }
                return (bitmapList.size() > 1);
            }

            @Override
            protected void onPostExecute(Boolean result) {
                /*
                If a more than the default canvas cover was found, use that cover to show now.
                if that is not the case and it is possible to get covers from the internet,
                them immediately use that.
                If that is also not the case, display the canvas cover.
                 */
                if (result) {
                    coverPosition = 1;
                    coverView.setImageBitmap(bitmapList.get(1));
                    setCoverLoading(false);
                } else if (isOnline() && pageCounter < 64 && fieldName.getText().toString().length() > 0) {
                    genBitmapFromInternet(fieldName.getText().toString());
                } else if (bitmapList.size() > 0) {
                    coverView.setImageBitmap(bitmapList.get(0));
                    coverPosition = 0;
                    setCoverLoading(false);
                }
            }
        }.execute();

        ImageButton nextCover = (ImageButton) v.findViewById(R.id.next_cover);
        nextCover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int size = bitmapList.size();
                if (BuildConfig.DEBUG)
                    Log.d(TAG, "pressed next cover, current position and size is: " + coverPosition + ", " + size);
                if (size > 0 && coverPosition + 1 < size) {
                    setCoverLoading(false);
                    coverPosition++;
                    coverView.setImageBitmap(bitmapList.get(coverPosition));
                } else {
                    genBitmapFromInternet(fieldName.getText().toString());
                }
            }
        });
        ImageButton previousCover = (ImageButton) v.findViewById(R.id.previous_cover);
        previousCover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int size = bitmapList.size();
                if (BuildConfig.DEBUG)
                    Log.d(TAG, "pressed previous cover, current position and size is: " + coverPosition + ", " + size);
                if (size > 0 && coverPosition > 0) {
                    setCoverLoading(false);
                    coverPosition--;
                    coverView.setImageBitmap(bitmapList.get(coverPosition));
                }
            }
        });

        Button done = (Button) v.findViewById(R.id.book_add);
        done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bookName = fieldName.getText().toString();
                if (!bookName.equals("")) {
                    new AddBookAsync().execute();
                } else {
                    CharSequence text = getString(R.string.book_add_empty_title);
                    Toast toast = Toast.makeText(getActivity(), text, Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 0);
                    toast.show();
                }
            }
        });
        return v;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (BuildConfig.DEBUG)
            Log.d(TAG, "started onCreate task!");
        super.onCreate(savedInstanceState);

        db = DataBaseHelper.getInstance(getActivity());

        PreferenceManager.setDefaultValues(getActivity(), R.xml.preferences, false);

        ActionBar actionBar = ((ActionBarActivity) getActivity()).getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);


    }

    private void setCoverLoading(boolean loading) {
        if (loading) {
            coverView.setVisibility(View.GONE);
            coverLoadingBar.setVisibility(View.VISIBLE);
        } else {
            coverView.setVisibility(View.VISIBLE);
            coverLoadingBar.setVisibility(View.GONE);
        }
    }

    private String getIPAddress() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface i : interfaces) {
                List<InetAddress> internetAdresses = Collections.list(i.getInetAddresses());
                for (InetAddress a : internetAdresses) {
                    if (!a.isLoopbackAddress()) {
                        String address = a.getHostAddress().toUpperCase();
                        if (InetAddressUtils.isIPv4Address(address))
                            return address;
                    }
                }
            }
        } catch (Exception e) {
            if (BuildConfig.DEBUG) Log.d(TAG, e.getMessage());
        }
        return "";
    }

    private void genBitmapFromInternet(String search) {
        if (isOnline() && pageCounter < 64) {
            new AsyncTask<String, Void, Bitmap>() {

                @Override
                protected void onPreExecute() {
                    setCoverLoading(true);
                }

                @Override
                protected Bitmap doInBackground(String... params) {

                    String searchText = params[0] + " audiobook cover";
                    try {
                        URL searchUrl = new URL(
                                "https://ajax.googleapis.com/ajax/services/search/images?v=1.0&imgsz=large|xlarge&rsz=1&q=" + URLEncoder.encode(searchText, "UTF-8") + "&start=" + pageCounter++ + "&userip=" + getIPAddress());

                        if (BuildConfig.DEBUG)
                            Log.d(TAG, searchUrl.toString());
                        URLConnection connection = searchUrl.openConnection();

                        String line;
                        StringBuilder builder = new StringBuilder();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(
                                connection.getInputStream()));
                        while ((line = reader.readLine()) != null) {
                            builder.append(line);
                        }
                        JSONObject obj = new JSONObject(builder.toString());
                        JSONObject responseData = obj.getJSONObject("responseData");
                        JSONArray results = responseData.getJSONArray("results");

                        String imageUrl = results.getJSONObject(0).getString("url");

                        if (imageUrl != null) {
                            if (BuildConfig.DEBUG)
                                Log.d(TAG, imageUrl);

                            URL url = new URL(imageUrl);
                            return BitmapFactory.decodeStream(url.openConnection().getInputStream());
                        }
                    } catch (Exception e) {
                        if (BuildConfig.DEBUG)
                            Log.d(TAG, e.toString());
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(Bitmap result) {
                    if (result != null) {
                        bitmapList.add(result);
                        coverPosition = bitmapList.indexOf(result);
                        coverView.setImageBitmap(result);
                        setCoverLoading(false);
                    }
                }
            }.execute(search);
        }
    }


    /*
    * returns if the device is online.
    * If setting is set to ignore mobile connection,
    * it will only return true,
    * if there is a wifi connection
    */
    private boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
            boolean mobileConnectionAllowed = sharedPref.getBoolean(getString(R.string.pref_cover_on_internet), false);

            NetworkInfo info = cm.getActiveNetworkInfo();
            if (info != null && info.isConnected()){
                /*
                if type is NOT mobile return !(false && ?) == true
                if type IS mobile and mobileConnection is allowed return !(true && !true) == !(true && false)
                == !(false) = true
                if type IS mobile and mobileConnection is NOT allowed return !(true && !false) ==
                !(true && true) == !(true) == false
                 */
                return !(info.getType() == ConnectivityManager.TYPE_MOBILE && !mobileConnectionAllowed);
            }
        }
        return false;
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.action_book_add, menu);
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


    private class AddBookAsync extends AsyncTask<Void, Integer, Void> {

        @Override
        protected void onPreExecute() {
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setCancelable(false);
            progressDialog.setTitle(getString(R.string.book_add_progress_title));
            progressDialog.setMessage(getString(R.string.book_add_progress_message));
            progressDialog.setMax(dirAddList.size() + 1);
            progressDialog.show();
        }

        @Override
        protected Void doInBackground(Void... params) {

            LinkedHashMap<Integer, MediaDetail> media = new LinkedHashMap<Integer, MediaDetail>();
            for (File f : dirAddList) {
                if (FilesChoose.isAudio(f.getName())) {
                    MediaDetail m = new MediaDetail();
                    String fileName = f.getName();
                    if (fileName.indexOf(".") > 0)
                        fileName = fileName.substring(0, fileName.lastIndexOf("."));
                    m.setName(fileName);
                    m.setPath(f.getAbsolutePath());

                    MediaMetadataRetriever metaRetriever = new MediaMetadataRetriever();
                    metaRetriever.setDataSource(f.getAbsolutePath());
                    int duration = Integer.parseInt(metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
                    m.setDuration(duration);
                    int id = db.addMedia(m);
                    media.put(id, m);
                }
                // put progress here to increase for checked covers
                publishProgress(progressDialog.getProgress() + 1);
            }

            if (media.size() > 0) {
                BookDetail b = new BookDetail();
                String[] res;
                if (BuildConfig.DEBUG)
                    Log.d(TAG, "bitmap list size is: " + bitmapList.size() + "cover position is:" + coverPosition);
                if (bitmapList.size() != 0 && coverPosition > bitmapList.size()) {
                    if (BuildConfig.DEBUG)
                        Log.d(TAG, "saving bitmap with index 0!");
                    res = saveImages(bitmapList.get(0));
                } else {
                    if (BuildConfig.DEBUG)
                        Log.d(TAG, "saving bitmap with index 0!");
                    res = saveImages(bitmapList.get(coverPosition));
                }
                b.setName(bookName);
                b.setCover(res[0]);
                b.setThumb(res[1]);
                b.setMediaIDs(media);
                db.addBook(b);
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            progressDialog.setProgress(progress[0]);
        }

        @Override
        protected void onPostExecute(Void result) {
            progressDialog.cancel();
            startActivity(new Intent(getActivity(), BookChoose.class));
        }
    }


    private String[] saveImages(Bitmap cover) {
        String thumbPath = "";
        String coverPath = "";
        String packageName = getActivity().getPackageName();
        String fileName = String.valueOf(System.currentTimeMillis()) + ".png";
        int pixelCut = 10;

        // if cover is too big, scale it down
        int displayPx = CommonTasks.getDisplayMinSize(getActivity());
        if (cover.getWidth() > displayPx || cover.getHeight() > displayPx) {
            cover = Bitmap.createScaledBitmap(cover, displayPx, displayPx, false);
        }

        cover = Bitmap.createBitmap(cover, pixelCut, pixelCut, cover.getWidth() - 2 * pixelCut, cover.getHeight() - 2 * pixelCut); //crop n px from each side for poor images
        int thumbPx = CommonTasks.convertDpToPx(getResources().getDimension(R.dimen.thumb_size), getResources());
        Bitmap thumb = Bitmap.createScaledBitmap(cover, thumbPx, thumbPx, false);
        File thumbDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Android/data/" + packageName + "/thumbs");
        File imageDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Android/data/" + packageName + "/images");
        File thumbFile = new File(thumbDir, fileName);
        File imageFile = new File(imageDir, fileName);
        //noinspection ResultOfMethodCallIgnored
        thumbDir.mkdirs();
        //noinspection ResultOfMethodCallIgnored
        imageDir.mkdirs();

        try {
            FileOutputStream coverOut = new FileOutputStream(imageFile);
            FileOutputStream thumbOut = new FileOutputStream(thumbFile);
            cover.compress(Bitmap.CompressFormat.PNG, 90, coverOut);
            thumb.compress(Bitmap.CompressFormat.PNG, 90, thumbOut);
            coverOut.flush();
            thumbOut.flush();
            coverOut.close();
            thumbOut.close();
            coverPath = imageFile.getAbsolutePath();
            thumbPath = thumbFile.getAbsolutePath();
            if (BuildConfig.DEBUG)
                Log.d(TAG, "Saving image: " + coverPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new String[]{coverPath, thumbPath};
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
                    Collections.sort(tempReturn, new NaturalOrderComparator<File>());
                    returnList.addAll(tempReturn);
                }
            }
        }
        return returnList;
    }
}
