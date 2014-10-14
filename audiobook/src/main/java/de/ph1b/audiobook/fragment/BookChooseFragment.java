package de.ph1b.audiobook.fragment;


import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.mobeta.android.dslv.DragSortListView;

import java.io.File;
import java.util.ArrayList;

import de.ph1b.audiobook.R;
import de.ph1b.audiobook.activity.BookChoose;
import de.ph1b.audiobook.activity.BookPlay;
import de.ph1b.audiobook.activity.FilesChoose;
import de.ph1b.audiobook.activity.Settings;
import de.ph1b.audiobook.adapter.MediaAdapter;
import de.ph1b.audiobook.dialog.EditBook;
import de.ph1b.audiobook.interfaces.OnStateChangedListener;
import de.ph1b.audiobook.service.AudioPlayerService;
import de.ph1b.audiobook.service.PlayerStates;
import de.ph1b.audiobook.utils.BookDetail;
import de.ph1b.audiobook.utils.CommonTasks;
import de.ph1b.audiobook.utils.DataBaseHelper;


public class BookChooseFragment extends Fragment implements View.OnClickListener, EditBook.OnEditBookFinished {


    private static final String TAG = "de.ph1b.audiobook.fragment.BookChooseFragment";

    private ArrayList<BookDetail> details;
    private DataBaseHelper db;
    private MediaAdapter adapt;

    private ImageView currentCover;
    private TextView currentText;
    private ImageButton currentPlaying;
    private ViewGroup current;

    private AudioPlayerService mService;
    private boolean mBound = false;
    private ActionMode actionMode;
    private BookDetail currentBook;
    private BookDetail bookToEdit;
    private DragSortListView mediaListView;

    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            AudioPlayerService.LocalBinder binder = (AudioPlayerService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            if (mService.stateManager.getState() == PlayerStates.STARTED) {
                currentPlaying.setImageResource(R.drawable.av_pause);
            } else {
                currentPlaying.setImageResource(R.drawable.av_play);
            }
            mService.stateManager.addStateChangeListener(onStateChangedListener);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

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
            mService.stateManager.removeStateChangeListener(onStateChangedListener);
            getActivity().unbindService(mConnection);
            mBound = false;
        }
    }

    private final OnStateChangedListener onStateChangedListener = new OnStateChangedListener() {
        @Override
        public void onStateChanged(final PlayerStates state) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (state == PlayerStates.STARTED) {
                        currentPlaying.setImageResource(R.drawable.av_pause);
                    } else {
                        currentPlaying.setImageResource(R.drawable.av_play);
                    }
                }
            });
        }
    };


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = DataBaseHelper.getInstance(getActivity());
        details = db.getAllBooks();

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
        mediaListView = (DragSortListView) v.findViewById(R.id.listMediaView);

        adapt = new MediaAdapter(details, this);

        mediaListView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
        mediaListView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
            @Override
            public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
                adapt.setBookChecked(details.get(position).getId(), checked);
                // invalidates to invoke onPrepareActionMode again
                mode.invalidate();
            }


            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                adapt.toggleDrag(false);
                actionMode = mode;
                MenuInflater inflater = mode.getMenuInflater();
                inflater.inflate(R.menu.action_mode_mediaview, menu);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                MenuItem actionEdit = menu.findItem(R.id.action_edit);
                if (mediaListView.getCheckedItemCount() > 1)
                    actionEdit.setVisible(false);
                if (mediaListView.getCheckedItemCount() == 1)
                    actionEdit.setVisible(true);
                return true;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_delete:
                        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
                        int position = settings.getInt(BookChoose.SHARED_PREFS_CURRENT, -1);
                        ArrayList<BookDetail> books = adapt.getCheckedBooks();
                        for (BookDetail b : books) {
                            //setting visibility of play list at bottom to gone if book is gone
                            if (b.getId() == position) {
                                current.setVisibility(View.GONE);
                            }
                            details.remove(b);
                        }

                        adapt.notifyDataSetChanged();

                        db.deleteBooksAsync(books);
                        mode.finish();
                        return true;
                    case R.id.action_edit:
                        EditBook editBook = new EditBook();
                        Bundle bundle = new Bundle();
                        bookToEdit = adapt.getCheckedBooks().get(0);

                        ArrayList<Bitmap> bitmap = new ArrayList<Bitmap>();
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
                    default:
                        return false;
                }
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                adapt.toggleDrag(true);
                adapt.unCheckAll();
            }
        });

        mediaListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView a, View v, int position, long id) {
                BookDetail book = details.get(position);
                int bookId = book.getId();

                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
                SharedPreferences.Editor editor = settings.edit();
                editor.putInt(BookChoose.SHARED_PREFS_CURRENT, bookId);
                editor.apply();

                Intent i = new Intent(getActivity(), BookPlay.class);
                i.putExtra(AudioPlayerService.GUI_BOOK, book);
                startActivity(i);
            }

        });

        mediaListView.setAdapter(adapt);

        mediaListView.setDragSortListener(new DragSortListView.DragSortListener() {
            @Override
            public void drag(int from, int to) {
            }

            @Override
            public synchronized void drop(int from, int to) {
                if (from != to) {
                    if (from > to) {
                        while (from > to) {
                            swapBooks(from, from - 1);
                            from--;
                        }
                    } else {
                        while (from < to) {
                            swapBooks(from, from + 1);
                            from++;
                        }
                    }
                    db.updateBooksAsync(details);
                }
            }

            @Override
            public void remove(int which) {

            }
        });
        return v;
    }

    /*
    swaps the elements in details list.
    also swaps their sort id.
     */
    private void swapBooks(int oldPosition, int newPosition) {
        BookDetail oldBook = details.get(oldPosition);
        BookDetail newBook = details.get(newPosition);
        int oldSortId = oldBook.getSortId();
        int newSortId = newBook.getSortId();
        oldBook.setSortId(newSortId);
        newBook.setSortId(oldSortId);
        details.set(oldPosition, newBook);
        details.set(newPosition, oldBook);
        adapt.notifyDataSetChanged();
    }


    private void refreshBookList() {
        //updates list each time!
        ArrayList<BookDetail> tempDetails = db.getAllBooks();
        details.clear();
        details.addAll(tempDetails);
        adapt.notifyDataSetChanged();

        if (details.size() == 0) {
            String text = getString(R.string.media_view_how_to);
            Toast toast = Toast.makeText(getActivity(), text, Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL, 0, 0);
            toast.show();
        }
    }

    public void initPlayerWidget() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
        int currentBookPosition = settings.getInt(BookChoose.SHARED_PREFS_CURRENT, -1);

        currentBook = db.getBook(currentBookPosition);

        if (currentBook != null) {
            Intent i = new Intent(getActivity(), AudioPlayerService.class);
            i.putExtra(AudioPlayerService.GUI_BOOK, currentBook);
            getActivity().startService(i);

            //setting cover
            String thumbPath = currentBook.getThumb();
            if (thumbPath == null || thumbPath.equals("") || !new File(thumbPath).exists() || new File(thumbPath).isDirectory()) {
                String bookName = currentBook.getName();
                int px = CommonTasks.getThumbDimensions(getResources());
                Bitmap thumb = CommonTasks.genCapital(bookName, px, getResources());
                currentCover.setImageBitmap(thumb);
            } else if (new File(thumbPath).isFile()) {
                currentCover.setImageURI(Uri.parse(thumbPath));
            }

            //setting text
            currentText.setText(currentBook.getName());
            current.setOnClickListener(this);
            currentPlaying.setOnClickListener(this);
            current.setVisibility(View.VISIBLE);
        } else {
            current.setVisibility(View.GONE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        refreshBookList();
        initPlayerWidget();
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.action_media_view, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add:
                Intent i = new Intent(getActivity(), FilesChoose.class);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
                return true;
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
            case R.id.current:
                Intent i = new Intent(getActivity(), BookPlay.class);
                i.putExtra(AudioPlayerService.GUI_BOOK, currentBook);
                startActivity(i);
                break;
            case R.id.current_playing:
                Intent serviceIntent = new Intent(getActivity(), AudioPlayerService.class);
                serviceIntent.putExtra(AudioPlayerService.GUI_BOOK, currentBook);
                serviceIntent.setAction(AudioPlayerService.CONTROL_PLAY_PAUSE);
                getActivity().startService(serviceIntent);
                break;
            default:
                break;
        }
    }

    @Override
    public void onEditBookFinished(String bookName, Bitmap cover, Boolean success) {
        if (success) {
            bookToEdit.setName(bookName);
            String[] coverPaths = CommonTasks.saveCovers(cover, getActivity());
            bookToEdit.setCover(coverPaths[0]);
            bookToEdit.setThumb(coverPaths[1]);
            db.updateBook(bookToEdit);
            refreshBookList();
            if (actionMode != null)
                actionMode.finish();
            initPlayerWidget();
        }
    }
}
