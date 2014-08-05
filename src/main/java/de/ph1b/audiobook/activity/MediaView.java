package de.ph1b.audiobook.activity;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;

import de.ph1b.audiobook.R;
import de.ph1b.audiobook.adapter.MediaAdapter;
import de.ph1b.audiobook.helper.BookDetail;
import de.ph1b.audiobook.helper.CommonTasks;
import de.ph1b.audiobook.helper.DataBaseHelper;
import de.ph1b.audiobook.interfaces.OnStateChangedListener;
import de.ph1b.audiobook.service.AudioPlayerService;
import de.ph1b.audiobook.service.PlayerStates;
import de.ph1b.audiobook.service.StateManager;

public class MediaView extends ActionBarActivity {

    public static final String PLAY_BOOK = "playBook";
    private ArrayList<BookDetail> details;
    private ArrayList<BookDetail> deleteList;
    private final DataBaseHelper db = DataBaseHelper.getInstance(this);
    private MediaAdapter adapt;

    private LocalBroadcastManager bcm;
    private static final String TAG = "de.ph1b.audiobook.MediaView";

    private static final String SHARED_PREFS = "sharedPreferences";
    private static final String SHARED_PREFS_CURRENT = "sharedPreferencesCurrent";

    private ImageView currentCover;
    private TextView currentText;
    private ImageButton currentPlaying;


    private LinearLayout current;

    //private static final String TAG = "MediaView";


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_chooser);
        new CommonTasks().checkExternalStorage(this);

        bcm = LocalBroadcastManager.getInstance(this);

        current = (LinearLayout) findViewById(R.id.current);
        currentCover = (ImageView) findViewById(R.id.current_cover);
        currentText = (TextView) findViewById(R.id.current_text);
        currentPlaying = (ImageButton) findViewById(R.id.current_playing);

        PreferenceManager.setDefaultValues(this, R.xml.preference_screen, false);

        details = db.getAllBooks();

        ListView mediaListView = (ListView) findViewById(R.id.listMediaView);
        deleteList = new ArrayList<BookDetail>();
        adapt = new MediaAdapter(details, this);
        mediaListView.setAdapter(adapt);

        mediaListView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
        mediaListView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
            @Override
            public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
                if (checked) {
                    deleteList.add(details.get(position));
                } else {
                    deleteList.remove(details.get(position));
                }
            }

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                MenuInflater inflater = mode.getMenuInflater();
                inflater.inflate(R.menu.action_mode_mediaview, menu);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_delete:
                        SharedPreferences settings = getSharedPreferences(SHARED_PREFS, 0);
                        int position = settings.getInt(SHARED_PREFS_CURRENT, -1);
                        for (BookDetail b : deleteList) {
                            //setting visibility of play list at bottom to gone if book is gone
                            if (b.getId() == position) {
                                CommonTasks.logD(TAG, "Deleted Book that is marked as current with ID: " + position);
                                current.setVisibility(View.GONE);
                            }
                            details.remove(b);
                        }

                        adapt.notifyDataSetChanged();

                        new DeleteBookAsync().execute();
                        mode.finish();
                        return true;
                    default:
                        return false;
                }
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
            }
        });

        mediaListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView a, View v, int position, long id) {
                BookDetail book = details.get(position);
                int bookId = book.getId();

                if (book.getMediaIds().length > 0) {
                    SharedPreferences settings = getSharedPreferences(SHARED_PREFS, 0);
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putInt(SHARED_PREFS_CURRENT, bookId);
                    editor.apply();

                    Intent intent = new Intent(getApplicationContext(), MediaPlay.class);
                    intent.putExtra(PLAY_BOOK, bookId);
                    startActivity(intent);
                }
            }

        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.action_media_view, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_add:
                startActivity(new Intent(this, MediaAdd.class));
                return true;
            case R.id.action_settings:
                startActivity(new Intent(this, Preferences.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    private final BroadcastReceiver addedBookReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(BookAdd.BOOK_ADDED))
                new AddedBookAsync().execute();
        }
    };


    private class AddedBookAsync extends AsyncTask<Void, Void, Void> {

        private ArrayList<BookDetail> books;

        @Override
        protected Void doInBackground(Void... params) {
            books = db.getAllBooks();
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            details.clear();
            details.addAll(books);
            adapt.notifyDataSetChanged();
        }
    }

    private class DeleteBookAsync extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            for (BookDetail b : deleteList) {
                db.deleteBook(b);
            }
            return null;
        }
    }

    @Override
    public void onDestroy() {
        bcm.unregisterReceiver(addedBookReceiver);
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        //do nothing to stay in activity
    }


    @Override
    public void onResume() {
        super.onResume();
        bcm.registerReceiver(addedBookReceiver, new IntentFilter(BookAdd.BOOK_ADDED));

        if (details.size() == 0) {
            String text = getString(R.string.media_view_how_to);
            Toast toast = Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL, 0, 0);
            toast.show();
        }

        SharedPreferences settings = getSharedPreferences(SHARED_PREFS, 0);
        int position = settings.getInt(SHARED_PREFS_CURRENT, -1);

        final BookDetail b = db.getBook(position);

        if (b != null) {
            Intent i = new Intent(getApplicationContext(), AudioPlayerService.class);
            i.putExtra(AudioPlayerService.BOOK_ID, b.getId());
            startService(i);

            //setting cover
            //set tree observer to measure height pre-drawn
            currentCover.getViewTreeObserver().addOnPreDrawListener(
                    new ViewTreeObserver.OnPreDrawListener() {
                        public boolean onPreDraw() {
                            int height = currentCover.getMeasuredHeight();
                            int width = currentCover.getMeasuredWidth();
                            String thumbPath = b.getThumb();
                            Bitmap thumb;
                            if (thumbPath.equals("") || new File(thumbPath).isDirectory()) {
                                thumb = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
                                Canvas c = new Canvas(thumb);
                                Paint textPaint = new Paint();
                                textPaint.setTextSize(2 * width / 3);
                                textPaint.setColor(getResources().getColor(android.R.color.white));
                                textPaint.setAntiAlias(true);
                                textPaint.setTextAlign(Paint.Align.CENTER);
                                Paint backgroundPaint = new Paint();
                                backgroundPaint.setColor(getResources().getColor(R.color.file_chooser_audio));
                                c.drawRect(0, 0, width, height, backgroundPaint);
                                int y = (int) ((c.getHeight() / 2) - ((textPaint.descent() + textPaint.ascent()) / 2));
                                c.drawText(b.getName().substring(0, 1).toUpperCase(), width / 2, y, textPaint);
                                currentCover.setImageBitmap(thumb);
                            } else if (new File(thumbPath).isFile()) {
                                thumb = BitmapFactory.decodeFile(thumbPath);
                                thumb = Bitmap.createScaledBitmap(thumb, width, height, false);
                                currentCover.setImageBitmap(thumb);
                            }
                            return true;
                        }
                    }
            );


            //setting play-state
            setPlayIcon(StateManager.getState());
            StateManager.setStateChangeListener(new OnStateChangedListener() {
                @Override
                public void onStateChanged(PlayerStates state) {
                    setPlayIcon(state);
                }
            });

            //setting text
            currentText.setText(b.getName());

            currentText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(getApplicationContext(), MediaPlay.class);
                    intent.putExtra(PLAY_BOOK, b.getId());
                    startActivity(intent);
                }
            });

            currentPlaying.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    bcm.sendBroadcast(new Intent(AudioPlayerService.CONTROL_PLAY_PAUSE));
                }
            });

            current.setVisibility(View.VISIBLE);

        } else {
            current.setVisibility(View.GONE);
        }

        //checking if external storage is available
        new CommonTasks().checkExternalStorage(this);
    }

    private void setPlayIcon(PlayerStates state) {
        if (state == PlayerStates.STARTED) {
            currentPlaying.setImageResource(R.drawable.av_pause);
        } else {
            currentPlaying.setImageResource(R.drawable.av_play);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        bcm.unregisterReceiver(addedBookReceiver);
    }
}
