package de.ph1b.audiobook.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.media.MediaMetadataRetriever;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
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
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.ph1b.audiobook.BuildConfig;
import de.ph1b.audiobook.R;
import de.ph1b.audiobook.helper.BookDetail;
import de.ph1b.audiobook.helper.CommonTasks;
import de.ph1b.audiobook.helper.DataBaseHelper;
import de.ph1b.audiobook.helper.MediaDetail;


public class BookAdd extends ActionBarActivity {

    private EditText fieldName;

    private final DataBaseHelper db = DataBaseHelper.getInstance(this);
    private LocalBroadcastManager bcm;

    private static final String TAG = "BookProperties";
    private ArrayList<String> fileFolderPaths;
    private String bookName;
    public static final String BOOK_ADDED = "bookAdded";
    private final int MANY_FILES_MIN_AMOUNT = 15;
    private ImageView coverView;
    private ProgressBar coverLoadingBar;
    private int coverPosition = 0;
    private int pageCounter = 0;

    private final ArrayList<Bitmap> bitmapList = new ArrayList<Bitmap>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_properties);
        new CommonTasks().checkExternalStorage(this);

        bcm = LocalBroadcastManager.getInstance(this);

        PreferenceManager.setDefaultValues(this, R.xml.preference_screen, false);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);


        fileFolderPaths = getIntent().getStringArrayListExtra(MediaAdd.FILES_AS_STRING);

        coverView = (ImageView) findViewById(R.id.cover);
        coverLoadingBar = (ProgressBar) findViewById(R.id.cover_loading);

        String defaultName = getIntent().getStringExtra(MediaAdd.BOOK_PROPERTIES_DEFAULT_NAME);
        fieldName = (EditText) findViewById(R.id.book_name);
        fieldName.setText(defaultName);

        if (isOnline()) {
            setCoverLoading(true);
            genBitmapFromInternet(fieldName.getText().toString());
        } else {
            setCoverLoading(false);
            genBitmapFromLocal();
            coverView.setImageBitmap(bitmapList.get(0));
        }

        ImageButton nextCover = (ImageButton) findViewById(R.id.next_cover);
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
                    if (isOnline() && pageCounter < 64) {
                        setCoverLoading(true);
                        genBitmapFromInternet(fieldName.getText().toString());
                    }
                }
            }
        });
        ImageButton previousCover = (ImageButton) findViewById(R.id.previous_cover);
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

        Button done = (Button) findViewById(R.id.book_add);
        done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bookName = fieldName.getText().toString();
                if (!bookName.equals("")) {
                    new AddBookAsync().execute();

                    if (fileFolderPaths.size() > MANY_FILES_MIN_AMOUNT) {
                        String text = getString(R.string.many_files);
                        Toast toast = Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG);
                        toast.show();
                    }
                    startActivity(new Intent(getApplicationContext(), MediaView.class));
                } else {
                    CharSequence text = getString(R.string.book_add_empty_title);
                    Toast toast = Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 0);
                    toast.show();
                }
            }
        });
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
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress().toUpperCase();
                        boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                        if (isIPv4)
                            return sAddr;
                    }
                }
            }
        } catch (Exception e) {
            if (BuildConfig.DEBUG)
                Log.d(TAG, e.getMessage());
        }
        return "";
    }

    private void genBitmapFromInternet(String search) {
        new AsyncTask<String, Void, Bitmap>() {
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
                        Bitmap coverBmp = BitmapFactory.decodeStream(url.openConnection().getInputStream());
                        bitmapList.add(coverBmp);
                        return coverBmp;
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
                    coverPosition = bitmapList.indexOf(result);
                    coverView.setImageBitmap(result);
                    setCoverLoading(false);
                }
            }
        }.execute(search);
    }


    private void genBitmapFromLocal() {
        // makes a file list out of the path names to add
        ArrayList<File> dirAddList = new ArrayList<File>();
        for (String s : fileFolderPaths) {
            dirAddList.add(new File(s));
        }

        // if an image file is found in any folder, stop there.
        ArrayList<File> imageList = MediaAdd.dirsToFiles(filterShowImagesAndFolder, dirAddList, MediaAdd.IMAGE);
        for (File f1 : imageList) {
            Bitmap cover = BitmapFactory.decodeFile(f1.getAbsolutePath());
            if (cover != null) {
                bitmapList.add(cover);
                break;
            }
        }

        // if no image file was found in any folder, search for audio files.
        ArrayList<File> musicList = MediaAdd.dirsToFiles(MediaAdd.filterShowAudioAndFolder, dirAddList, MediaAdd.AUDIO);
        for (File f1 : musicList) {
            String path = f1.getAbsolutePath();
            MediaMetadataRetriever mmr = new MediaMetadataRetriever();
            mmr.setDataSource(path);
            if (BuildConfig.DEBUG)
                Log.d(TAG, "getting embedded picture of " + path);
            byte[] data = mmr.getEmbeddedPicture();
            if (data != null) {
                try {
                    if (BuildConfig.DEBUG)
                        Log.d(TAG, "Data is not null!");
                    Bitmap cover = BitmapFactory.decodeByteArray(data, 0, data.length);
                    if (cover != null) {
                        bitmapList.add(cover);
                        break;
                    }
                } catch (Exception e) {
                    if (BuildConfig.DEBUG)
                        Log.d(TAG, e.toString());
                }
            }
        }

        // adding canvas with capital to bitmap - list
        if (fieldName != null) {
            String capital = fieldName.getText().toString().substring(0, 1);
            if (capital.length() > 0) {
                int width = 500;
                int height = 500;
                Bitmap cover = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
                Canvas c = new Canvas(cover);
                Paint textPaint = new Paint();
                textPaint.setTextSize(4 * width / 5);
                Resources r = getApplicationContext().getResources();
                textPaint.setColor(r.getColor(android.R.color.white));
                textPaint.setAntiAlias(true);
                textPaint.setTextAlign(Paint.Align.CENTER);
                Paint backgroundPaint = new Paint();
                backgroundPaint.setColor(r.getColor(R.color.file_chooser_audio));
                c.drawRect(0, 0, width, height, backgroundPaint);
                int y = (int) ((c.getHeight() / 2) - ((textPaint.descent() + textPaint.ascent()) / 2));
                c.drawText(fieldName.getText().toString().substring(0, 1).toUpperCase(), width / 2, y, textPaint);
                bitmapList.add(cover);
            }
        }
    }

    /*
    * returns if the device is online.
    * If setting is set to ignore mobile connection,
    * it will only return true,
    * if there is a wifi connection    *
    */
    private boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        boolean isWifi = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
                .isConnectedOrConnecting();
        boolean isMobile = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)
                .isConnectedOrConnecting();

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        boolean useMobileConnection = sharedPref.getBoolean(getString(R.string.pref_cover_on_internet), true);
        return isWifi || (useMobileConnection && isMobile);
    }


    private ArrayList<MediaDetail> filesToMedia(ArrayList<File> files) {
        ArrayList<MediaDetail> media = new ArrayList<MediaDetail>();
        for (File f : files) {
            MediaDetail m = new MediaDetail();
            String fileName = f.getName();
            if (fileName.indexOf(".") > 0)
                fileName = fileName.substring(0, fileName.lastIndexOf("."));
            m.setName(fileName);
            m.setPath(f.getAbsolutePath());
            media.add(m);
        }
        return media;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.action_book_add, menu);
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


    private class AddBookAsync extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {

            ArrayList<File> dirAddList = new ArrayList<File>();
            for (String s : fileFolderPaths) {
                dirAddList.add(new File(s));
            }

            ArrayList<MediaDetail> mediaList = filesToMedia(dirAddList);

            if (mediaList.size() != 0) {

                int[] mediaIDs = new int[mediaList.size()];

                for (int i = 0; i < mediaList.size(); i++) {
                    MediaDetail m = mediaList.get(i);

                    String path = m.getPath();
                    MediaMetadataRetriever metaRetriever = new MediaMetadataRetriever();
                    metaRetriever.setDataSource(path);
                    int duration = Integer.parseInt(metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
                    m.setDuration(duration);
                    int id = db.addMedia(m);
                    mediaIDs[i] = id;
                }

                BookDetail b = new BookDetail();
                b.setName(bookName);
                b.setMediaIds(mediaIDs);
                String[] res;
                if (coverPosition > bitmapList.size()) {
                    res = saveImages(bitmapList.get(0));
                } else {
                    res = saveImages(bitmapList.get(coverPosition));
                }
                b.setCover(res[0]);
                b.setThumb(res[1]);
                b.setMediaIds(mediaIDs);
                db.addBook(b);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            Intent i = new Intent(BOOK_ADDED);
            i.setAction(BOOK_ADDED);
            bcm.sendBroadcast(i);
        }
    }


    private String[] saveImages(Bitmap cover) {
        String thumbPath = "";
        String coverPath = "";
        String packageName = getPackageName();
        String fileName = String.valueOf(System.currentTimeMillis()) + ".png";
        int pixelCut = 10;

        int coverWidth = cover.getWidth();
        int coverHeight = cover.getHeight();

        // if cover is too big, scale it down
        if (coverWidth > 500 || coverHeight > 500) {
            cover = Bitmap.createScaledBitmap(cover, 500, 500, false);
        }

        cover = Bitmap.createBitmap(cover, pixelCut, pixelCut, cover.getWidth() - 2 * pixelCut, cover.getHeight() - 2 * pixelCut); //crop n px from each side for poor images
        Bitmap thumb = Bitmap.createScaledBitmap(cover, 200, 200, false);
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

    private final FileFilter filterShowImagesAndFolder = new FileFilter() {
        @Override
        public boolean accept(File pathname) {
            return !pathname.isHidden() && (pathname.isDirectory() || MediaAdd.isImage(pathname.getName()));
        }
    };

    @Override
    public void onResume() {
        super.onResume();

        //checking if external storage is available
        new CommonTasks().checkExternalStorage(this);
    }
}
