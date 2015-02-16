package de.ph1b.audiobook.fragment;


import android.app.Fragment;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
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

import com.afollestad.materialdialogs.AlertDialogWrapper;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.ph1b.audiobook.R;
import de.ph1b.audiobook.activity.BookPlayActivity;
import de.ph1b.audiobook.activity.FilesChooseActivity;
import de.ph1b.audiobook.activity.SettingsActivity;
import de.ph1b.audiobook.adapter.MediaAdapter;
import de.ph1b.audiobook.content.Book;
import de.ph1b.audiobook.content.DataBaseHelper;
import de.ph1b.audiobook.dialog.EditBookDialog;
import de.ph1b.audiobook.interfaces.OnItemClickListener;
import de.ph1b.audiobook.interfaces.OnItemLongClickListener;
import de.ph1b.audiobook.service.AudioPlayerService;
import de.ph1b.audiobook.service.GlobalState;
import de.ph1b.audiobook.service.PlayerStates;
import de.ph1b.audiobook.service.ServiceController;
import de.ph1b.audiobook.utils.CustomOnSimpleGestureListener;
import de.ph1b.audiobook.utils.ImageHelper;
import de.ph1b.audiobook.utils.L;
import de.ph1b.audiobook.utils.PrefsManager;


public class BookShelfFragment extends Fragment implements View.OnClickListener, EditBookDialog.OnEditBookFinished, RecyclerView.OnItemTouchListener, GlobalState.ChangeListener {


    private static final String TAG = BookShelfFragment.class.getSimpleName();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final GlobalState globalState = GlobalState.INSTANCE;
    private DataBaseHelper db;
    private MediaAdapter adapter;
    private ImageView currentCover;
    private TextView currentText;
    private ImageButton currentPlaying;
    private ViewGroup current;
    private PrefsManager prefs;
    private GestureDetectorCompat detector;
    private Book bookToEdit;
    private float scrollBy = 0;
    private ServiceController controller;
    private ArrayList<Book> books;

    /**
     * Returns the amount of columns the main-grid will need
     *
     * @return The amount of columns, but at least 2.
     */
    private int getAmountOfColumns() {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        float dpWidth = displayMetrics.widthPixels / displayMetrics.density;
        int columns = Math.round(dpWidth / getResources().getDimension(R.dimen.desired_medium_cover));
        return columns > 2 ? columns : 2;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        globalState.init(getActivity());
        controller = new ServiceController(getActivity());

        Intent serviceIntent = new Intent(getActivity(), AudioPlayerService.class);
        getActivity().startService(serviceIntent);

        db = DataBaseHelper.getInstance(getActivity());
        prefs = new PrefsManager(getActivity());
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
                Book book = adapter.getItem(position);
                prefs.setCurrentBookId(book.getId());

                globalState.setBook(book);
                Intent i = new Intent(getActivity(), BookPlayActivity.class);
                startActivity(i);
            }

            @Override
            public void onPopupMenuClicked(View v, final int position) {
                L.d(TAG, "popup" + String.valueOf(position));
                PopupMenu popup = new PopupMenu(getActivity(), v);
                MenuInflater inflater = popup.getMenuInflater();
                inflater.inflate(R.menu.popup_menu, popup.getMenu());

                bookToEdit = adapter.getItem(position);

                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.edit_book:
                                EditBookDialog editBookDialog = new EditBookDialog();
                                Bundle bundle = new Bundle();

                                ArrayList<Bitmap> bitmap = new ArrayList<>();
                                Bitmap defaultCover = BitmapFactory.decodeFile(bookToEdit.getCover());
                                if (defaultCover != null)
                                    bitmap.add(defaultCover);

                                bundle.putParcelableArrayList(EditBookDialog.BOOK_COVER, bitmap);
                                bundle.putString(EditBookDialog.DIALOG_TITLE, getString(R.string.edit_book_title));
                                bundle.putString(EditBookDialog.BOOK_NAME, bookToEdit.getName());

                                editBookDialog.setArguments(bundle);
                                editBookDialog.setTargetFragment(BookShelfFragment.this, 0);
                                editBookDialog.show(getFragmentManager(), TAG);
                                return true;
                            case R.id.delete_book:
                                Book deleteBook = adapter.getItem(position);
                                AlertDialogWrapper.Builder builder = new AlertDialogWrapper.Builder(getActivity());
                                builder.setTitle(R.string.delete_book_title);
                                builder.setMessage(deleteBook.getName());
                                builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        //setting visibility of start widget at bottom to gone if book is gone
                                        long currentBookId = prefs.getCurrentBookId();
                                        if (adapter.getItem(position).getId() == currentBookId)
                                            current.setVisibility(View.GONE);

                                        adapter.removeItem(position);
                                    }
                                });
                                builder.setNegativeButton(R.string.delete_book_keep, null);
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

        books = db.getAllBooks();
        adapter = new MediaAdapter(books, getActivity(), onClickListener);
        recyclerView.setHasFixedSize(true);

        recyclerView.setLayoutManager(new GridLayoutManager(getActivity(), getAmountOfColumns()));
        recyclerView.setAdapter(adapter);
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
                        L.d(TAG, endX + "/" + endY);
                        View endChild = recyclerView.findChildViewUnder(endX, endY);
                        int to = recyclerView.getChildPosition(endChild);

                        if (from != -1 && to != -1) {
                            adapter.swapItems(from, to);
                            return true;
                        } else if (from != -1) {
                            to = adapter.getItemCount() - 1;
                            adapter.swapItems(from, to);
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
        long currentBookPosition = prefs.getCurrentBookId();

        boolean widgetInitialized = false;
        for (final Book b : adapter.getData()) {
            if (b.getId() == currentBookPosition) {
                //setting cover
                Picasso.with(getActivity()).load(new File(b.getCover())).into(currentCover);

                //setting text
                currentText.setText(b.getName());
                current.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent i = new Intent(getActivity(), BookPlayActivity.class);
                        startActivity(i);
                    }
                });
                currentPlaying.setOnClickListener(this);
                current.setVisibility(View.VISIBLE);
                widgetInitialized = true;
                break;
            }
        }
        if (!widgetInitialized) {
            current.setVisibility(View.GONE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        globalState.addChangeListener(this);
        onStateChanged(globalState.getState());

        executor.execute(new Runnable() {
            @Override
            public void run() {
                db.fillMissingCovers();
                ArrayList<Book> updatedBooks = db.getAllBooks();
                books.clear();
                books.addAll(updatedBooks);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        adapter.notifyDataSetChanged();
                    }
                });
            }
        });

        initPlayerWidget();
    }

    @Override
    public void onPause() {
        super.onPause();

        globalState.removeChangeListener(this);
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
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.current_playing:
                controller.playPause();
                break;
            case R.id.fab:
                Intent i = new Intent(getActivity(), FilesChooseActivity.class);
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
            adapter.updateItem(bookToEdit);
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

    @Override
    public void onTimeChanged(int time) {

    }

    @Override
    public void onStateChanged(final PlayerStates state) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (state == PlayerStates.PLAYING) {
                    currentPlaying.setImageResource(R.drawable.ic_pause_grey600_48dp);
                } else {
                    currentPlaying.setImageResource(R.drawable.ic_play_arrow_grey600_48dp);
                }
            }
        });
    }

    @Override
    public void onSleepTimerSet(boolean sleepTimerActive) {

    }

    @Override
    public void onPositionChanged(int position) {

    }
}
