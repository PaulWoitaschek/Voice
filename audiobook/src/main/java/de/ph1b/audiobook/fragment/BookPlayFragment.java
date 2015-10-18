package de.ph1b.audiobook.fragment;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.afollestad.materialdialogs.internal.MDTintHelper;
import com.getbase.floatingactionbutton.FloatingActionButton;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.ph1b.audiobook.R;
import de.ph1b.audiobook.activity.SettingsActivity;
import de.ph1b.audiobook.dialog.BookmarkDialogFragment;
import de.ph1b.audiobook.dialog.JumpToPositionDialogFragment;
import de.ph1b.audiobook.dialog.prefs.PlaybackSpeedDialogFragment;
import de.ph1b.audiobook.interfaces.MultiPaneInformer;
import de.ph1b.audiobook.mediaplayer.MediaPlayerController;
import de.ph1b.audiobook.model.Book;
import de.ph1b.audiobook.model.Chapter;
import de.ph1b.audiobook.persistence.DataBaseHelper;
import de.ph1b.audiobook.persistence.PrefsManager;
import de.ph1b.audiobook.service.ServiceController;
import de.ph1b.audiobook.uitools.CoverReplacement;
import de.ph1b.audiobook.uitools.PlayPauseDrawable;
import de.ph1b.audiobook.uitools.ThemeUtil;
import de.ph1b.audiobook.utils.App;
import de.ph1b.audiobook.utils.BaseModule;
import de.ph1b.audiobook.utils.Communication;
import de.ph1b.audiobook.utils.L;

/**
 * Base class for book playing interaction.
 *
 * @author Paul Woitaschek
 */
public class BookPlayFragment extends Fragment {

    public static final String TAG = BookPlayFragment.class.getSimpleName();
    private static final String NI_BOOK_ID = "niBookId";
    private final PlayPauseDrawable playPauseDrawable = new PlayPauseDrawable();
    @Inject Communication communication;
    @Bind(R.id.previous) View previous_button;
    @Bind(R.id.rewind) View rewindButton;
    @Bind(R.id.play) FloatingActionButton playButton;
    @Bind(R.id.fastForward) View fastForwardButton;
    @Bind(R.id.next) View next_button;
    @Bind(R.id.book_cover) ImageView coverView;
    @Bind(R.id.cover_frame) View coverFrame;
    @Bind(R.id.played) TextView playedTimeView;
    @Bind(R.id.seekBar) SeekBar seekBar;
    @Bind(R.id.book_spinner) Spinner bookSpinner;
    @Bind(R.id.maxTime) TextView maxTimeView;
    @Bind(R.id.timerView) TextView timerCountdownView;
    @Inject PrefsManager prefs;
    @Inject DataBaseHelper db;
    private ServiceController controller;
    private CountDownTimer countDownTimer;
    private boolean isMultiPanel = false;
    private long bookId;
    private AppCompatActivity hostingActivity;
    private final Communication.SimpleBookCommunication listener = new Communication.SimpleBookCommunication() {

        @Override
        public void onSleepStateChanged() {
            hostingActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    hostingActivity.invalidateOptionsMenu();
                    initializeTimerCountdown();
                }
            });
        }

        @Override
        public void onBookContentChanged(@NonNull final Book book) {
            hostingActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    L.d(TAG, "onBookContentChangedReciever called with bookId=" + book.getId());
                    if (book.getId() == bookId) {
                        List<Chapter> chapters = book.getChapters();
                        Chapter chapter = book.getCurrentChapter();

                        int position = chapters.indexOf(chapter);
                        /*
                          Setting position as a tag, so we can make sure onItemSelected is only fired when
                          the user changes the position himself.
                         */
                        bookSpinner.setTag(position);
                        bookSpinner.setSelection(position, true);
                        int duration = chapter.getDuration();
                        seekBar.setMax(duration);
                        maxTimeView.setText(formatTime(duration, duration));

                        // Setting seekBar and played time view
                        int progress = book.getTime();
                        if (!seekBar.isPressed()) {
                            seekBar.setProgress(progress);
                            playedTimeView.setText(formatTime(progress, duration));
                        }
                    }
                }
            });
        }

        @Override
        public void onPlayStateChanged() {
            hostingActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setPlayState(true);
                }
            });
        }
    };
    private MultiPaneInformer multiPaneInformer;

    private static String formatTime(int ms, int duration) {
        String h = String.valueOf(TimeUnit.MILLISECONDS.toHours(ms));
        String m = String.format("%02d", (TimeUnit.MILLISECONDS.toMinutes(ms) % 60));
        String s = String.format("%02d", (TimeUnit.MILLISECONDS.toSeconds(ms) % 60));

        if (TimeUnit.MILLISECONDS.toHours(duration) == 0) {
            return m + ":" + s;
        } else {
            return h + ":" + m + ":" + s;
        }
    }

    /**
     * Method to create a new instance of this fragment. Do not create a new instance yourself.
     *
     * @param bookId the id to use
     * @return The new instance
     */
    public static BookPlayFragment newInstance(long bookId) {
        BookPlayFragment bookPlayFragment = new BookPlayFragment();

        Bundle args = new Bundle();
        args.putLong(NI_BOOK_ID, bookId);
        bookPlayFragment.setArguments(args);

        return bookPlayFragment;
    }

    /**
     * @return the book id this fragment was instantiated with.
     */
    public long getBookId() {
        return getArguments().getLong(NI_BOOK_ID);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_book_play, container, false);
        ButterKnife.bind(this, view);

        final Book book = db.getBook(bookId);
        isMultiPanel = multiPaneInformer.isMultiPanel();

        //init views
        ActionBar actionBar = hostingActivity.getSupportActionBar();
        assert actionBar != null;
        actionBar.setDisplayHomeAsUpEnabled(!isMultiPanel);

        //setup buttons
        playButton.setIconDrawable(playPauseDrawable);
        playButton.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                playButton.getViewTreeObserver().removeOnPreDrawListener(this);
                return true;
            }
        });
        MDTintHelper.setTint(seekBar, ContextCompat.getColor(getContext(), R.color.accent));
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
                            .getFile());
                    playedTimeView.setText(formatTime(progress, seekBar.getMax()));
                }
            }
        });

        if (book != null) {
            actionBar.setTitle(book.getName());

            // adapter
            List<Chapter> chapters = book.getChapters();
            final List<String> chaptersAsStrings = new ArrayList<>(chapters.size());
            for (int i = 0; i < chapters.size(); i++) {
                String chapterName = chapters.get(i).getName();

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

            ArrayAdapter adapter = new ArrayAdapter<String>(getContext(),
                    R.layout.fragment_book_play_spinner, R.id.spinnerTextItem, chaptersAsStrings) {
                @Override
                public View getDropDownView(int position, View convertView, ViewGroup parent) {
                    View view = super.getDropDownView(position, convertView, parent);

                    TextView textView = ButterKnife.findById(view, R.id.spinnerTextItem);

                    // highlights the selected item and un-highlights an item if it is not selected.
                    // default implementation uses a ViewHolder, so this is necessary.
                    if (position == bookSpinner.getSelectedItemPosition()) {
                        textView.setBackgroundResource(R.drawable.spinner_selected_background);
                        textView.setTextColor(ContextCompat.getColor(getContext(), R.color.copy_abc_primary_text_material_dark));
                    } else {
                        textView.setBackgroundResource(ThemeUtil.getResourceId(getContext(),
                                R.attr.selectableItemBackground));
                        textView.setTextColor(ContextCompat.getColor(getContext(), ThemeUtil.getResourceId(
                                getContext(), android.R.attr.textColorPrimary)));
                    }

                    return view;
                }
            };
            bookSpinner.setAdapter(adapter);

            bookSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (parent.getTag() != null && ((int) parent.getTag()) != position) {
                        L.i(TAG, "spinner, onItemSelected, firing:" + position);
                        controller.changeTime(0, book.getChapters().get(
                                position).getFile());
                        parent.setTag(position);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });

            // Next/Prev/spinner/book progress views hiding
            if (book.getChapters().size() == 1) {
                next_button.setVisibility(View.GONE);
                previous_button.setVisibility(View.GONE);
                bookSpinner.setVisibility(View.GONE);
            } else {
                next_button.setVisibility(View.VISIBLE);
                previous_button.setVisibility(View.VISIBLE);
                bookSpinner.setVisibility(View.VISIBLE);
            }

            ViewCompat.setTransitionName(coverView, book.getCoverTransitionName());
        }

        // (Cover)
        final Drawable coverReplacement = new CoverReplacement(book == null ? "M" : book.getName(), getContext());
        if (book != null && !book.isUseCoverReplacement() && book.getCoverFile().canRead()) {
            Picasso.with(getContext()).load(book.getCoverFile()).placeholder(coverReplacement).into(coverView);
        } else {
            // we have to set the cover in onPreDraw. Else the transition will fail.
            coverView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    coverView.getViewTreeObserver().removeOnPreDrawListener(this);
                    coverView.setImageDrawable(coverReplacement);
                    return true;
                }
            });
        }

        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        App.getComponent().inject(this);

        bookId = getArguments().getLong(NI_BOOK_ID);

        setRetainInstance(true);
        setHasOptionsMenu(true);

        controller = new ServiceController(getContext());
    }

    @OnClick({R.id.play, R.id.cover_frame, R.id.rewind, R.id.fastForward, R.id.next, R.id.previous, R.id.played})
    void playbackControlClicked(View v) {
        switch (v.getId()) {
            case R.id.play:
            case R.id.cover_frame:
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
                if (db.getBook(bookId) != null) {
                    launchJumpToPositionDialog();
                }
                break;
            default:
                break;
        }
    }

    private void launchJumpToPositionDialog() {
        new JumpToPositionDialogFragment().show(getFragmentManager(), JumpToPositionDialogFragment.TAG);
    }

    private void initializeTimerCountdown() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        if (MediaPlayerController.isSleepTimerActive()) {
            timerCountdownView.setVisibility(View.VISIBLE);
            countDownTimer = new CountDownTimer(MediaPlayerController.getLeftSleepTimerTime(), 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    timerCountdownView.setText(formatTime((int) millisUntilFinished, (int) millisUntilFinished));
                }

                @Override
                public void onFinish() {
                    timerCountdownView.setVisibility(View.GONE);
                    L.i(TAG, "Countdown timer finished");
                }
            }.start();
        } else {
            timerCountdownView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        hostingActivity = (AppCompatActivity) context;
        multiPaneInformer = (MultiPaneInformer) context;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.book_play, menu);

        // sets playback speed icon enabled / disabled depending on device functionallity
        MenuItem timeLapseItem = menu.findItem(R.id.action_time_lapse);
        timeLapseItem.setVisible(BaseModule.canSetSpeed());

        // sets the correct sleep timer icon
        MenuItem sleepTimerItem = menu.findItem(R.id.action_sleep);
        if (MediaPlayerController.isSleepTimerActive()) {
            sleepTimerItem.setIcon(R.drawable.ic_alarm_on_white_24dp);
        } else {
            sleepTimerItem.setIcon(R.drawable.ic_snooze_white_24dp);
        }

        // hide bookmark and time change item if there is no valid book
        boolean currentBookExists = db.getBook(bookId) != null;
        MenuItem bookmarkItem = menu.findItem(R.id.action_bookmark);
        MenuItem timeChangeItem = menu.findItem(R.id.action_time_change);
        bookmarkItem.setVisible(currentBookExists);
        timeChangeItem.setVisible(currentBookExists);

        // if we are in multipane layout, we don't show the settings menu here. It will be handled
        // by the other fragment.
        menu.findItem(R.id.action_settings).setVisible(!isMultiPanel);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(getContext(), SettingsActivity.class));
                return true;
            case R.id.action_time_change:
                launchJumpToPositionDialog();
                return true;
            case R.id.action_sleep:
                controller.toggleSleepSand();
                if (prefs.setBookmarkOnSleepTimer() && !MediaPlayerController.isSleepTimerActive()) {
                    String date = DateUtils.formatDateTime(getContext(), System.currentTimeMillis(), DateUtils.FORMAT_SHOW_DATE |
                            DateUtils.FORMAT_SHOW_TIME |
                            DateUtils.FORMAT_NUMERIC_DATE);
                    BookmarkDialogFragment.addBookmark(bookId, date + ": " +
                            getString(R.string.action_sleep), db);
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
                hostingActivity.onBackPressed();
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
            listener.onBookContentChanged(book);
        }

        hostingActivity.invalidateOptionsMenu();

        communication.addBookCommunicationListener(listener);

        // Sleep timer countdown view
        initializeTimerCountdown();
    }

    @Override
    public void onStop() {
        super.onStop();

        communication.removeBookCommunicationListener(listener);

        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}
