package de.ph1b.audiobook.fragment;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;

import de.ph1b.audiobook.BuildConfig;
import de.ph1b.audiobook.R;
import de.ph1b.audiobook.activity.BookChoose;
import de.ph1b.audiobook.activity.BookPlay;
import de.ph1b.audiobook.activity.FilesChoose;
import de.ph1b.audiobook.activity.Settings;
import de.ph1b.audiobook.adapter.MediaAdapter;
import de.ph1b.audiobook.adapter.MediaAdapterChooser;
import de.ph1b.audiobook.content.BookDetail;
import de.ph1b.audiobook.content.DataBaseHelper;
import de.ph1b.audiobook.dialog.EditBook;
import de.ph1b.audiobook.interfaces.OnItemClickListener;
import de.ph1b.audiobook.interfaces.OnItemLongClickListener;
import de.ph1b.audiobook.interfaces.OnStateChangedListener;
import de.ph1b.audiobook.service.AudioPlayerService;
import de.ph1b.audiobook.service.PlayerStates;
import de.ph1b.audiobook.service.StateManager;
import de.ph1b.audiobook.utils.CustomOnSimpleGestureListener;
import de.ph1b.audiobook.utils.ImageHelper;


public class BookChooseFragment extends Fragment implements View.OnClickListener, EditBook.OnEditBookFinished, RecyclerView.OnItemTouchListener {


    private static final String TAG = "de.ph1b.audiobook.fragment.BookChooseFragment";
    private final OnStateChangedListener onStateChangedListener = new OnStateChangedListener() {
        @Override
        public void onStateChanged(final PlayerStates state) {
            Activity a = getActivity();
            if (a != null)
                a.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (state == PlayerStates.STARTED) {
                            currentPlaying.setImageResource(R.drawable.ic_pause_grey600_36dp);
                        } else {
                            currentPlaying.setImageResource(R.drawable.ic_play_arrow_grey600_36dp);
                        }
                    }
                });
        }
    };
    //private ArrayList<BookDetail> details;
    private DataBaseHelper db;
    private MediaAdapter adapt;
    private ImageView currentCover;
    private TextView currentText;
    private ImageButton currentPlaying;
    private ViewGroup current;
    private AudioPlayerService mService;
    private boolean mBound = false;
    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            AudioPlayerService.LocalBinder binder = (AudioPlayerService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            if (stateManager.getState() == PlayerStates.STARTED) {
                currentPlaying.setImageResource(R.drawable.ic_pause_grey600_36dp);
            } else {
                currentPlaying.setImageResource(R.drawable.ic_play_arrow_grey600_36dp);
            }
            stateManager.addStateChangeListener(onStateChangedListener);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };
    private GestureDetectorCompat detector;
    private BookDetail bookToEdit;
    private float scrollBy = 0;

    private StateManager stateManager;

    /**
     * Returns the amount of columns the main-grid will need
     *
     * @param c Application Context
     * @return The amount of columns, but at least 2.
     */
    public static int getAmountOfColumns(Context c) {
        Resources r = c.getResources();
        DisplayMetrics displayMetrics = r.getDisplayMetrics();
        float dpWidth = displayMetrics.widthPixels / displayMetrics.density;
        int columns = Math.round(dpWidth / r.getDimension(R.dimen.desired_medium_cover));
        return columns > 2 ? columns : 2;
    }

    @Override
    public void onStart() {
        super.onStart();
        Intent intent = new Intent(getActivity(), AudioPlayerService.class);
        getActivity().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onStop() {
        super.onStop();
        // Unbind from the service
        if (mBound) {
            stateManager.removeStateChangeListener(onStateChangedListener);
            getActivity().unbindService(mConnection);
            mBound = false;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        stateManager = StateManager.getInstance(getActivity());

        db = DataBaseHelper.getInstance(getActivity());
        PreferenceManager.setDefaultValues(getActivity(), R.xml.preferences, false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_book_choose, container, false);

        setHasOptionsMenu(true);
        ActionBar actionBar = ((ActionBarActivity) getActivity()).getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setTitle(getActivity().getString(R.string.app_name));

        current = (ViewGroup) v.findViewById(R.id.current);
        currentCover = (ImageView) v.findViewById(R.id.current_cover);
        currentText = (TextView) v.findViewById(R.id.current_text);
        currentPlaying = (ImageButton) v.findViewById(R.id.current_playing);
        final RecyclerView recyclerView = (RecyclerView) v.findViewById(R.id.listMediaView);
        v.findViewById(R.id.fab).setOnClickListener(this);

        OnItemClickListener onClickListener = new OnItemClickListener() {
            @Override
            public void onCoverClicked(int position) {
                BookDetail book = adapt.getItem(position);
                int bookId = book.getId();

                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
                SharedPreferences.Editor editor = settings.edit();
                editor.putInt(BookChoose.SHARED_PREFS_CURRENT, bookId);
                editor.apply();

                Intent i = new Intent(getActivity(), BookPlay.class);
                if (BuildConfig.DEBUG)
                    Log.d(TAG, "Starting book with id: " + bookId);
                i.putExtra(AudioPlayerService.GUI_BOOK_ID, bookId);
                startActivity(i);
            }

            @Override
            public void onPopupMenuClicked(View v, final int position) {
                Log.d(TAG, "popup" + String.valueOf(position));
                PopupMenu popup = new PopupMenu(getActivity(), v);
                MenuInflater inflater = popup.getMenuInflater();
                inflater.inflate(R.menu.popup_menu, popup.getMenu());

                bookToEdit = adapt.getItem(position);

                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.edit_book:
                                EditBook editBook = new EditBook();
                                Bundle bundle = new Bundle();

                                ArrayList<Bitmap> bitmap = new ArrayList<>();
                                Bitmap defaultCover = BitmapFactory.decodeFile(bookToEdit.getCover());
                                if (defaultCover != null)
                                    bitmap.add(defaultCover);

                                bundle.putParcelableArrayList(EditBook.BOOK_COVER, bitmap);
                                bundle.putString(EditBook.DIALOG_TITLE, getString(R.string.edit_book_title));
                                bundle.putString(EditBook.BOOK_NAME, bookToEdit.getName());

                                editBook.setArguments(bundle);
                                editBook.setTargetFragment(BookChooseFragment.this, 0);
                                editBook.show(getFragmentManager(), TAG);
                                return true;
                            case R.id.delete_book:
                                BookDetail deleteBook = adapt.getItem(position);
                                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                                builder.setTitle(R.string.delete_book_title);
                                builder.setMessage(deleteBook.getName());
                                builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        //setting visibility of start widget at bottom to gone if book is gone
                                        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
                                        int currentBookId = settings.getInt(BookChoose.SHARED_PREFS_CURRENT, -1);
                                        if (adapt.getItem(position).getId() == currentBookId)
                                            current.setVisibility(View.GONE);

                                        adapt.removeItem(position);
                                    }
                                });
                                builder.setNegativeButton(R.string.delete_book_keep, null);
                                builder.show();
                                return true;
                            default:
                                return false;
                        }
                    }
                });
                popup.show();
            }
        };

        OnItemLongClickListener onLongClickListener = new OnItemLongClickListener() {
            @Override
            public void onItemLongClicked(int position, View view) {
                ClipData cData = ClipData.newPlainText("position", String.valueOf(position));
                View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(view);
                view.startDrag(cData, shadowBuilder, view, 0);
            }
        };

        MediaAdapter.OnCoverChangedListener onCoverChangedListener = new MediaAdapter.OnCoverChangedListener() {
            @Override
            public void onCoverChanged() {
                if (getActivity() != null)
                    initPlayerWidget();
            }
        };

        ArrayList<BookDetail> books = db.getAllBooks();
        adapt = MediaAdapterChooser.getAdapter(books, getActivity(), onClickListener, onCoverChangedListener);
        recyclerView.setHasFixedSize(true);

        recyclerView.setLayoutManager(new GridLayoutManager(getActivity(), getAmountOfColumns(getActivity().getApplicationContext())));
        recyclerView.setAdapter(adapt);
        recyclerView.addOnItemTouchListener(this);
        final Handler handler = new Handler();

        final Runnable smoothScroll = new Runnable() {
            @Override
            public void run() {
                recyclerView.smoothScrollBy(0, Math.round(scrollBy));
                handler.postDelayed(this, 50);
            }
        };

        View.OnDragListener onDragListener = new View.OnDragListener() {
            @Override
            public boolean onDrag(View v, DragEvent event) {


                int action = event.getAction();
                switch (action) {
                    case DragEvent.ACTION_DRAG_STARTED:
                        return event.getClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN);
                    case DragEvent.ACTION_DRAG_ENTERED:
                        return true;
                    case DragEvent.ACTION_DRAG_LOCATION:

                        int height = recyclerView.getMeasuredHeight();

                        float scrollArea = height / 4;
                        float maxScrollStrength = scrollArea / 3;

                        float y = event.getY();

                        scrollBy = 0;
                        if (y <= scrollArea && y > 0) {
                            scrollBy = maxScrollStrength * (y / scrollArea - 1);
                        } else if (y >= (height - scrollArea) && y <= height) {
                            scrollBy = maxScrollStrength * (1 - y / height);

                            float factor = 1 + (y * y - height * height) /
                                    (2 * scrollArea * height - scrollArea * scrollArea);

                            scrollBy = maxScrollStrength * factor;
                        }
                        handler.removeCallbacks(smoothScroll);
                        if (scrollBy != 0) {
                            handler.removeCallbacks(smoothScroll);
                            handler.post(smoothScroll);
                        }

                        return true;
                    case DragEvent.ACTION_DRAG_EXITED:
                        return true;
                    case DragEvent.ACTION_DROP:
                        ClipData.Item item = event.getClipData().getItemAt(0);
                        String dragData = String.valueOf(item.getText());
                        int from = Integer.valueOf(dragData);

                        float endX = event.getX();
                        float endY = event.getY();
                        Log.d(TAG, endX + "/" + endY);
                        View endChild = recyclerView.findChildViewUnder(endX, endY);
                        int to = recyclerView.getChildPosition(endChild);

                        if (from != -1 && to != -1) {
                            adapt.swapItems(from, to);
                            return true;
                        } else if (from != -1) {
                            to = adapt.getItemCount() - 1;
                            adapt.swapItems(from, to);
                            return true;
                        }
                        return false;
                    case DragEvent.ACTION_DRAG_ENDED:
                        handler.removeCallbacks(smoothScroll);
                        scrollBy = 0;
                        return true;
                }
                return false;
            }
        };
        recyclerView.setOnDragListener(onDragListener);
        detector = new GestureDetectorCompat(getActivity(),
                new CustomOnSimpleGestureListener(recyclerView, onLongClickListener));
        return v;
    }

    private void initPlayerWidget() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
        int currentBookPosition = settings.getInt(BookChoose.SHARED_PREFS_CURRENT, -1);

        boolean widgetInitialized = false;
        for (final BookDetail b : adapt.getData()) {
            if (b.getId() == currentBookPosition) {
                //setting cover
                String coverPath = b.getCover();
                if (coverPath == null || coverPath.equals("") || !new File(coverPath).exists() || new File(coverPath).isDirectory()) {
                    String bookName = b.getName();
                    Bitmap thumb = ImageHelper.genCapital(bookName, getActivity(), ImageHelper.TYPE_THUMB);
                    currentCover.setImageBitmap(thumb);
                } else if (new File(coverPath).isFile()) {
                    Bitmap bitmap = ImageHelper.genBitmapFromFile(coverPath, getActivity(), ImageHelper.TYPE_THUMB);
                    currentCover.setImageBitmap(bitmap);
                }

                //setting text
                currentText.setText(b.getName());
                current.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent i = new Intent(getActivity(), BookPlay.class);
                        i.putExtra(AudioPlayerService.GUI_BOOK_ID, b.getId());
                        startActivity(i);
                    }
                });
                currentPlaying.setOnClickListener(this);
                current.setVisibility(View.VISIBLE);
                widgetInitialized = true;
                break;
            }
        }
        if (!widgetInitialized)
            current.setVisibility(View.GONE);
    }

    @Override
    public void onResume() {
        super.onResume();

        startAudioPlayerService();

        initPlayerWidget();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.action_only_settings, menu);
        super.onCreateOptionsMenu(menu, inflater);
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
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.current_playing:
                if (mBound) {
                    if (stateManager.getState() == PlayerStates.STARTED) {
                        mService.pause(true);
                    } else {
                        mService.play();
                    }
                }
                break;
            case R.id.fab:
                Intent i = new Intent(getActivity(), FilesChoose.class);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
                break;
            default:
                break;
        }
    }

    @Override
    public void onEditBookFinished(String bookName, Bitmap cover, Boolean success) {
        if (success) {
            bookToEdit.setName(bookName);
            if (cover != null) {
                String coverPath = ImageHelper.saveCover(cover, getActivity());
                bookToEdit.setCover(coverPath);
            }
            adapt.updateItem(bookToEdit);
            initPlayerWidget();
        }
    }

    @Override
    public boolean onInterceptTouchEvent(RecyclerView recyclerView, MotionEvent motionEvent) {
        detector.onTouchEvent(motionEvent);
        return false;
    }

    @Override
    public void onTouchEvent(RecyclerView recyclerView, MotionEvent motionEvent) {

    }

    /**
     * Starts the service (async, on background thread)
     */
    private void startAudioPlayerService() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
                int currentBookId = settings.getInt(BookChoose.SHARED_PREFS_CURRENT, -1);
                for (BookDetail b : adapt.getData()) {
                    if (b.getId() == currentBookId) {
                        Intent serviceIntent = new Intent(getActivity(), AudioPlayerService.class);
                        serviceIntent.putExtra(AudioPlayerService.GUI_BOOK_ID, b.getId());
                        getActivity().startService(serviceIntent);

                        Intent intent = new Intent(getActivity(), AudioPlayerService.class);
                        getActivity().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
                    }
                }
            }
        }).start();
    }
}
