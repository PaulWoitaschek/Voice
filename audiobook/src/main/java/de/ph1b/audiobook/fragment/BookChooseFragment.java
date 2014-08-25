package de.ph1b.audiobook.fragment;


import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
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

import de.ph1b.audiobook.BuildConfig;
import de.ph1b.audiobook.R;
import de.ph1b.audiobook.activity.BookChoose;
import de.ph1b.audiobook.activity.BookPlay;
import de.ph1b.audiobook.activity.FilesChoose;
import de.ph1b.audiobook.activity.Settings;
import de.ph1b.audiobook.adapter.MediaAdapter;
import de.ph1b.audiobook.interfaces.OnStateChangedListener;
import de.ph1b.audiobook.service.AudioPlayerService;
import de.ph1b.audiobook.service.PlayerStates;
import de.ph1b.audiobook.service.StateManager;
import de.ph1b.audiobook.utils.BookDetail;
import de.ph1b.audiobook.utils.DataBaseHelper;


public class BookChooseFragment extends Fragment {

    private static final String TAG = "de.ph1b.audiobook.fragment.BookChoose";

    private ArrayList<BookDetail> details;
    private ArrayList<BookDetail> deleteList;
    private DataBaseHelper db;
    private MediaAdapter adapt;

    private LocalBroadcastManager bcm;

    private ImageView currentCover;
    private TextView currentText;
    private ImageButton currentPlaying;
    private LinearLayout current;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (BuildConfig.DEBUG)
            Log.d(TAG, "onCreate was called");


        db = DataBaseHelper.getInstance(getActivity());
        bcm = LocalBroadcastManager.getInstance(getActivity());


        details = db.getAllBooks();
        deleteList = new ArrayList<BookDetail>();

        PreferenceManager.setDefaultValues(getActivity(), R.xml.preferences, false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_book_choose, container, false);

        setHasOptionsMenu(true);
        ActionBar actionBar = ((ActionBarActivity) getActivity()).getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setTitle(getActivity().getString(R.string.app_name));

        current = (LinearLayout) v.findViewById(R.id.current);
        currentCover = (ImageView) v.findViewById(R.id.current_cover);
        currentText = (TextView) v.findViewById(R.id.current_text);
        currentPlaying = (ImageButton) v.findViewById(R.id.current_playing);
        ListView mediaListView = (ListView) v.findViewById(R.id.listMediaView);

        adapt = new MediaAdapter(details, getActivity());
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
                        SharedPreferences settings = getActivity().getSharedPreferences(BookChoose.SHARED_PREFS, 0);
                        int position = settings.getInt(BookChoose.SHARED_PREFS_CURRENT, -1);
                        for (BookDetail b : deleteList) {
                            //setting visibility of play list at bottom to gone if book is gone
                            if (b.getId() == position) {
                                if (BuildConfig.DEBUG)
                                    Log.d(TAG, "Deleted Book that is marked as current with ID: " + position);
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
                    SharedPreferences settings = BookChooseFragment.this.getActivity().getSharedPreferences(BookChoose.SHARED_PREFS, 0);
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putInt(BookChoose.SHARED_PREFS_CURRENT, bookId);
                    editor.apply();

                    Intent i = new Intent(getActivity(), BookPlay.class);
                    Bundle bundle = new Bundle();
                    bundle.putInt(BookChoose.PLAY_BOOK, bookId);
                    i.putExtras(bundle);
                    startActivity(new Intent(getActivity(), BookPlay.class));
                }
            }
        });
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (details.size() == 0) {
            String text = getString(R.string.media_view_how_to);
            Toast toast = Toast.makeText(getActivity(), text, Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL, 0, 0);
            toast.show();
        }

        SharedPreferences settings = getActivity().getSharedPreferences(BookChoose.SHARED_PREFS, 0);
        int position = settings.getInt(BookChoose.SHARED_PREFS_CURRENT, -1);

        final BookDetail b = db.getBook(position);

        if (b != null) {
            Intent i = new Intent(getActivity(), AudioPlayerService.class);
            i.putExtra(AudioPlayerService.BOOK_ID, b.getId());
            getActivity().startService(i);

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
                    Intent i = new Intent(getActivity(), BookPlay.class);
                    Bundle bundle = new Bundle();
                    bundle.putInt(BookChoose.PLAY_BOOK, b.getId());
                    i.putExtras(bundle);
                    startActivity(i);
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
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (BuildConfig.DEBUG)
            Log.d(TAG, "onCreateOptionsMenu was called!");
        inflater.inflate(R.menu.action_media_view, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add:
                startActivity(new Intent(getActivity(), FilesChoose.class));
                return true;
            case R.id.action_settings:
                startActivity(new Intent(getActivity(), Settings.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void setPlayIcon(PlayerStates state) {
        if (state == PlayerStates.STARTED) {
            currentPlaying.setImageResource(R.drawable.av_pause);
        } else {
            currentPlaying.setImageResource(R.drawable.av_play);
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
}
