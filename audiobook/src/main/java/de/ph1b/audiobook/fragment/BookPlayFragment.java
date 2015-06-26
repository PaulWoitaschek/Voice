package de.ph1b.audiobook.fragment;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import de.ph1b.audiobook.R;
import de.ph1b.audiobook.activity.SettingsActivity;
import de.ph1b.audiobook.dialog.BookmarkDialogFragment;
import de.ph1b.audiobook.dialog.JumpToPositionDialogFragment;
import de.ph1b.audiobook.dialog.PlaybackSpeedDialogFragment;
import de.ph1b.audiobook.mediaplayer.MediaPlayerController;
import de.ph1b.audiobook.model.Book;
import de.ph1b.audiobook.model.Chapter;
import de.ph1b.audiobook.model.DataBaseHelper;
import de.ph1b.audiobook.service.ServiceController;
import de.ph1b.audiobook.uitools.CoverReplacement;
import de.ph1b.audiobook.uitools.PlayPauseDrawable;
import de.ph1b.audiobook.uitools.ThemeUtil;
import de.ph1b.audiobook.utils.Communication;
import de.ph1b.audiobook.utils.L;
import de.ph1b.audiobook.utils.PrefsManager;


public class BookPlayFragment extends Fragment implements View.OnClickListener, Communication.OnSleepStateChangedListener, Communication.OnBookContentChangedListener, Communication.OnPlayStateChangedListener {

    public static final String TAG = BookPlayFragment.class.getSimpleName();
    private static final String BOOK_ID = "bookId";
    private final PlayPauseDrawable playPauseDrawable = new PlayPauseDrawable();
    private final Communication communication = Communication.getInstance();
    @Nullable
    private Snackbar snackbar;
    private TextView playedTimeView;
    private SeekBar seekBar;
    private volatile Spinner bookSpinner;
    private TextView maxTimeView;
    private PrefsManager prefs;
    private ServiceController controller;
    private long bookId;
    private DataBaseHelper db;
    private CoordinatorLayout coordinatorLayout;

    public static BookPlayFragment newInstance(long bookId) {
        Bundle args = new Bundle();
        args.putLong(BOOK_ID, bookId);

        BookPlayFragment bookPlayFragment = new BookPlayFragment();
        bookPlayFragment.setArguments(args);
        return bookPlayFragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_book_play, container, false);

        bookId = getArguments().getLong(BOOK_ID);
        final Book book = db.getBook(bookId);
        if (book == null) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.content, new BookShelfFragment(), BookShelfFragment.TAG)
                    .commit();
            return null;
        }

        //setup actionbar
        Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar);
        AppCompatActivity appCompatActivity = (AppCompatActivity) getActivity();
        appCompatActivity.setSupportActionBar(toolbar);
        ActionBar actionBar = appCompatActivity.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(book.getName());
        }

        setHasOptionsMenu(true);

        //init buttons
        seekBar = (SeekBar) view.findViewById(R.id.seekBar);
        FloatingActionButton playButton = (FloatingActionButton) view.findViewById(R.id.play);
        ImageButton previous_button = (ImageButton) view.findViewById(R.id.previous);
        ImageButton next_button = (ImageButton) view.findViewById(R.id.next);
        playedTimeView = (TextView) view.findViewById(R.id.played);
        ImageView coverView = (ImageView) view.findViewById(R.id.book_cover);
        maxTimeView = (TextView) view.findViewById(R.id.maxTime);
        bookSpinner = (Spinner) view.findViewById(R.id.book_spinner);
        coordinatorLayout = (CoordinatorLayout) view.findViewById(R.id.bottom_layout);

        //setup buttons
        view.findViewById(R.id.fastForward).setOnClickListener(this);
        view.findViewById(R.id.rewind).setOnClickListener(this);
        previous_button.setOnClickListener(this);
        next_button.setOnClickListener(this);
        playButton.setOnClickListener(this);
        playButton.setIconDrawable(playPauseDrawable);
        playedTimeView.setOnClickListener(this);
        ThemeUtil.theme(seekBar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //sets text to adjust while using seekBar
                playedTimeView.setText(formatTime(progress, seekBar.getMax()));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int progress = seekBar.getProgress();
                Book currentBook = db.getBook(bookId);
                if (currentBook != null) {
                    controller.changeTime(progress, currentBook.getCurrentChapter()
                            .getPath());
                    playedTimeView.setText(formatTime(progress, seekBar.getMax()));
                }
            }
        });

        // adapter
        final List<String> chaptersAsStrings = new ArrayList<>();
        for (int i = 0; i < book.getChapters().size(); i++) {
            String chapterName = book.getChapters().get(i).getName();

            // cutting leading zeros
            chapterName = chapterName.replaceFirst("^0", "");
            String number = String.valueOf(i + 1);

            // desired format is "1 - Title"
            if (!chapterName.startsWith(number + " - ")) { // if title does not match desired format
                if (chapterName.startsWith(number)) {
                    // if it starts with a number, a " - " should follow
                    chapterName = number + " - " + chapterName.substring(chapterName.indexOf(number)
                            + number.length());
                } else {
                    // if the name does not match at all, set the correct format
                    chapterName = number + " - " + chapterName;
                }
            }

            chaptersAsStrings.add(chapterName);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
                R.layout.fragment_book_play_spinner, chaptersAsStrings) {
            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                final TextView textView = (TextView) super.getDropDownView(position, convertView,
                        parent);

                // this is necessary due to a bug in android causing the layout to be ignored.
                // see: http://stackoverflow.com/questions/14139106/spinner-does-not-wrap-text-is-th
                // is-an-android-bug
                textView.post(new Runnable() {
                    @Override
                    public void run() {
                        textView.setSingleLine(false);
                    }
                });

                // highlights the selected item and un-highlights an item if it is not selected.
                // default implementation uses a ViewHolder, so this is necessary.
                if (position == bookSpinner.getSelectedItemPosition()) {
                    textView.setBackgroundColor(getResources().getColor(ThemeUtil.getResourceId(
                            getActivity(), R.attr.colorAccent)));
                    textView.setTextColor(getResources().getColor(R.color.dark_text_primary));
                } else {
                    textView.setBackgroundColor(getResources().getColor(ThemeUtil.getResourceId(
                            getActivity(), android.R.attr.windowBackground)));
                    textView.setTextColor(getResources().getColor(ThemeUtil.getResourceId(
                            getActivity(), R.attr.text_primary)));
                }

                return textView;
            }
        };
        bookSpinner.setAdapter(adapter);

        bookSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int newPosition, long id) {
                if (parent.getTag() != null && ((int) parent.getTag()) != newPosition) {
                    L.i(TAG, "spinner, onItemSelected, firing:" + newPosition);
                    controller.changeTime(0, book.getChapters().get(
                            newPosition).getPath());
                    parent.setTag(newPosition);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // (Cover)
        File coverFile = book.getCoverFile();
        Drawable coverReplacement = new CoverReplacement(book.getName(),
                getActivity());
        if (!book.isUseCoverReplacement() && coverFile.exists() && coverFile.canRead()) {
            Picasso.with(getActivity()).load(coverFile).placeholder(coverReplacement).into(
                    coverView);
        } else {
            coverView.setImageDrawable(coverReplacement);
        }
        coverView.setOnClickListener(this);

        // Next/Prev/spinner hiding
        if (book.getChapters().size() == 1) {
            next_button.setVisibility(View.GONE);
            previous_button.setVisibility(View.GONE);
            bookSpinner.setVisibility(View.GONE);
        } else {
            next_button.setVisibility(View.VISIBLE);
            previous_button.setVisibility(View.VISIBLE);
            bookSpinner.setVisibility(View.VISIBLE);
        }

        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = PrefsManager.getInstance(getActivity());
        db = DataBaseHelper.getInstance(getActivity());
        controller = new ServiceController(getActivity());
    }

    private String formatTime(int ms, int duration) {
        String h = String.valueOf(TimeUnit.MILLISECONDS.toHours(ms));
        String m = String.format("%02d", (TimeUnit.MILLISECONDS.toMinutes(ms) % 60));
        String s = String.format("%02d", (TimeUnit.MILLISECONDS.toSeconds(ms) % 60));

        if (TimeUnit.MILLISECONDS.toHours(duration) == 0) {
            return m + ":" + s;
        } else {
            return h + ":" + m + ":" + s;
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.play:
            case R.id.book_cover:
                controller.playPause();
                break;
            case R.id.rewind:
                controller.rewind();
                break;
            case R.id.fastForward:
                controller.fastForward();
                break;
            case R.id.next:
                controller.next();
                break;
            case R.id.previous:
                controller.previous();
                break;
            case R.id.played:
                launchJumpToPositionDialog();
                break;
            default:
                break;
        }
    }

    private void launchJumpToPositionDialog() {
        new JumpToPositionDialogFragment().show(getFragmentManager(), JumpToPositionDialogFragment.TAG);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.book_play, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        MenuItem timeLapseItem = menu.findItem(R.id.action_time_lapse);
        timeLapseItem.setVisible(MediaPlayerController.canSetSpeed());
        MenuItem sleepTimerItem = menu.findItem(R.id.action_sleep);
        if (MediaPlayerController.sleepTimerActive) {
            sleepTimerItem.setIcon(R.drawable.ic_alarm_on_white_24dp);
        } else {
            sleepTimerItem.setIcon(R.drawable.ic_snooze_white_24dp);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            case R.id.action_time_change:
                launchJumpToPositionDialog();
                return true;
            case R.id.action_sleep:
                controller.toggleSleepSand();
                if (MediaPlayerController.sleepTimerActive && snackbar != null) {
                    snackbar.dismiss();
                }
                if (prefs.setBookmarkOnSleepTimer() && !MediaPlayerController.sleepTimerActive) {
                    String date = DateUtils.formatDateTime(getActivity(), System.currentTimeMillis(), DateUtils.FORMAT_SHOW_DATE |
                            DateUtils.FORMAT_SHOW_TIME |
                            DateUtils.FORMAT_NUMERIC_DATE);
                    BookmarkDialogFragment.addBookmark(bookId, date + ": " +
                            getString(R.string.action_sleep), getActivity());
                }
                return true;
            case R.id.action_time_lapse:
                new PlaybackSpeedDialogFragment().show(getFragmentManager(),
                        PlaybackSpeedDialogFragment.TAG);
                return true;
            case R.id.action_bookmark:
                BookmarkDialogFragment.newInstance(bookId).show(getFragmentManager(),
                        BookmarkDialogFragment.TAG);
                return true;
            case android.R.id.home:
            case R.id.home:
                getActivity().onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void setPlayState(boolean animated) {
        if (MediaPlayerController.getPlayState() == MediaPlayerController.PlayState.PLAYING) {
            playPauseDrawable.transformToPause(animated);
        } else {
            playPauseDrawable.transformToPlay(animated);
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        setPlayState(false);

        Book book = db.getBook(bookId);
        if (book != null) {
            onBookContentChanged(book);
        }

        getActivity().invalidateOptionsMenu();

        communication.addOnBookContentChangedListener(this);
        communication.addOnPlayStateChangedListener(this);
        communication.addOnSleepStateChangedListener(this);
    }

    @Override
    public void onStop() {
        super.onStop();

        communication.removeOnBookContentChangedListener(this);
        communication.removeOnPlayStateChangedListener(this);
        communication.removeOnSleepStateChangedListener(this);
    }

    @Override
    public void onSleepStateChanged() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                getActivity().invalidateOptionsMenu();
                if (MediaPlayerController.sleepTimerActive) {
                    int minutes = prefs.getSleepTime();
                    String message = getString(R.string.sleep_timer_started) + " " + minutes + " " +
                            getString(R.string.minutes);
                    snackbar = Snackbar.make(coordinatorLayout, message, Snackbar.LENGTH_LONG).setAction(
                            R.string.stop, new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    controller.toggleSleepSand();
                                }
                            });
                    Context context = getActivity().getApplicationContext();
                    ThemeUtil.theme(snackbar, context);
                    snackbar.show();
                }
            }
        });
    }

    @Override
    public void onBookContentChanged(@NonNull final Book book) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                L.d(TAG, "onBookContentChangedReciever called with bookId=" + book.getId());
                if (book.getId() == bookId) {

                    List<Chapter> chapters = book.getChapters();
                    Chapter chapter = book.getCurrentChapter();

                    int position = chapters.indexOf(chapter);
                    /**
                     * Setting position as a tag, so we can make sure onItemSelected is only fired when
                     * the user changes the position himself.
                     */
                    bookSpinner.setTag(position);
                    bookSpinner.setSelection(position, true);
                    int duration = chapter.getDuration();
                    seekBar.setMax(duration);
                    maxTimeView.setText(formatTime(duration, duration));

                    // Setting seekBar and played time view
                    if (!seekBar.isPressed()) {
                        int progress = book.getTime();
                        seekBar.setProgress(progress);
                        playedTimeView.setText(formatTime(progress, duration));
                    }
                }
            }
        });
    }

    @Override
    public void onPlayStateChanged() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setPlayState(true);
            }
        });
    }
}
