package de.ph1b.audiobook.fragment;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
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

import de.ph1b.audiobook.R;
import de.ph1b.audiobook.activity.SettingsActivity;
import de.ph1b.audiobook.dialog.BookmarkDialogFragment;
import de.ph1b.audiobook.dialog.JumpToPositionDialogFragment;
import de.ph1b.audiobook.dialog.PlaybackSpeedDialogFragment;
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
import de.ph1b.audiobook.utils.Communication;
import de.ph1b.audiobook.utils.L;

/**
 * Base class for book playing interaction.
 *
 * @author Paul Woitaschek
 */
public class BookPlayFragment extends Fragment implements View.OnClickListener {

    public static final String TAG = BookPlayFragment.class.getSimpleName();
    private static final Communication COMMUNICATION = Communication.getInstance();
    private static final String NI_BOOK_ID = "niBookId";
    private final PlayPauseDrawable playPauseDrawable = new PlayPauseDrawable();
    private TextView playedTimeView;
    private SeekBar seekBar;
    private Spinner bookSpinner;
    private TextView maxTimeView;
    private PrefsManager prefs;
    private ServiceController controller;
    private DataBaseHelper db;
    private TextView timerCountdownView;
    private CountDownTimer countDownTimer;
    private boolean isMultiPanel = false;
    private long bookId;
    private final Communication.SimpleBookCommunication listener = new Communication.SimpleBookCommunication() {

        @Override
        public void onSleepStateChanged() {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    getActivity().invalidateOptionsMenu();
                    initializeTimerCountdown();
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
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setPlayState(true);
                }
            });
        }
    };

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

        L.i(TAG, "onCreateView");

        final Book book = db.getBook(bookId);
        isMultiPanel = ((MultiPaneInformer) getActivity()).isMultiPanel();

        //init views
        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        assert actionBar != null;
        actionBar.setDisplayHomeAsUpEnabled(!isMultiPanel);
        seekBar = (SeekBar) view.findViewById(R.id.seekBar);
        View previous_button = view.findViewById(R.id.previous);
        View rewindButton = view.findViewById(R.id.rewind);
        final FloatingActionButton playButton = (FloatingActionButton) view.findViewById(R.id.play);
        View fastForwardButton = view.findViewById(R.id.fastForward);
        View next_button = view.findViewById(R.id.next);
        playedTimeView = (TextView) view.findViewById(R.id.played);
        final ImageView coverView = (ImageView) view.findViewById(R.id.book_cover);
        maxTimeView = (TextView) view.findViewById(R.id.maxTime);
        bookSpinner = (Spinner) view.findViewById(R.id.book_spinner);
        timerCountdownView = (TextView) view.findViewById(R.id.timerView);
        View coverFrame = view.findViewById(R.id.cover_frame);

        //setup buttons
        playButton.setIconDrawable(playPauseDrawable);
        playButton.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                //transitionPostponeHelper.elementDone();
                playButton.getViewTreeObserver().removeOnPreDrawListener(this);
                return true;
            }
        });
        //noinspection deprecation
        MDTintHelper.setTint(seekBar, getResources().getColor(R.color.accent));
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
        coverFrame.setOnClickListener(this);
        previous_button.setOnClickListener(this);
        rewindButton.setOnClickListener(this);
        playButton.setOnClickListener(this);
        fastForwardButton.setOnClickListener(this);
        next_button.setOnClickListener(this);

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

            ArrayAdapter adapter = new ArrayAdapter<String>(getActivity(),
                    R.layout.fragment_book_play_spinner, R.id.spinnerTextItem, chaptersAsStrings) {
                @Override
                public View getDropDownView(int position, View convertView, ViewGroup parent) {
                    View view = super.getDropDownView(position, convertView, parent);

                    TextView textView = (TextView) view.findViewById(R.id.spinnerTextItem);

                    // highlights the selected item and un-highlights an item if it is not selected.
                    // default implementation uses a ViewHolder, so this is necessary.
                    if (position == bookSpinner.getSelectedItemPosition()) {
                        textView.setBackgroundResource(R.drawable.spinner_selected_background);
                        //noinspection deprecation
                        textView.setTextColor(getResources().getColor(R.color.abc_primary_text_material_dark));
                    } else {
                        textView.setBackgroundResource(ThemeUtil.getResourceId(getActivity(),
                                R.attr.selectableItemBackground));
                        //noinspection deprecation
                        textView.setTextColor(getResources().getColor(ThemeUtil.getResourceId(
                                getActivity(), android.R.attr.textColorPrimary)));
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
        final Drawable coverReplacement = new CoverReplacement(book == null ? "M" : book.getName(), getActivity());
        if (book != null && !book.isUseCoverReplacement() && book.getCoverFile().canRead()) {
            Picasso.with(getActivity()).load(book.getCoverFile()).placeholder(coverReplacement).into(coverView);
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

        ViewCompat.setTransitionName(playButton, getString(R.string.fab_transition));

        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        bookId = getArguments().getLong(NI_BOOK_ID);

        setRetainInstance(true);
        setHasOptionsMenu(true);

        prefs = PrefsManager.getInstance(getActivity());
        db = DataBaseHelper.getInstance(getActivity());
        controller = new ServiceController(getActivity());
    }

    @Override
    public void onClick(View v) {
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
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.book_play, menu);

        // sets playback speed icon enabled / disabled depending on device functionallity
        MenuItem timeLapseItem = menu.findItem(R.id.action_time_lapse);
        timeLapseItem.setVisible(MediaPlayerController.canSetSpeed());

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
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            case R.id.action_time_change:
                launchJumpToPositionDialog();
                return true;
            case R.id.action_sleep:
                controller.toggleSleepSand();
                if (prefs.setBookmarkOnSleepTimer() && !MediaPlayerController.isSleepTimerActive()) {
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
            listener.onBookContentChanged(book);
        }

        getActivity().invalidateOptionsMenu();

        COMMUNICATION.addBookCommunicationListener(listener);

        // Sleep timer countdown view
        initializeTimerCountdown();
    }

    @Override
    public void onStop() {
        super.onStop();

        COMMUNICATION.removeBookCommunicationListener(listener);

        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}
