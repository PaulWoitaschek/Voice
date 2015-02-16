package de.ph1b.audiobook.dialog;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.AlertDialogWrapper;

import java.util.ArrayList;

import de.ph1b.audiobook.R;
import de.ph1b.audiobook.adapter.BookmarkAdapter;
import de.ph1b.audiobook.content.Book;
import de.ph1b.audiobook.content.Bookmark;
import de.ph1b.audiobook.content.DataBaseHelper;
import de.ph1b.audiobook.service.GlobalState;
import de.ph1b.audiobook.service.ServiceController;
import de.ph1b.audiobook.utils.DividerItemDecoration;
import de.ph1b.audiobook.utils.PrefsManager;

/**
 * @author <a href="mailto:woitaschek@posteo.de">Paul Woitaschek</a>
 * @link {http://www.paul-woitaschek.de}
 * @see <a href="http://www.paul-woitaschek.de">http://www.paul-woitaschek.de</a>
 */
public class BookmarkDialog extends DialogFragment {

    private BookmarkAdapter adapter;
    private AlertDialog dialog;

    public Dialog onCreateDialog(Bundle savedInstanceState) {

        LayoutInflater inflater = getActivity().getLayoutInflater();

        //passing null is fine because of fragment
        @SuppressLint("InflateParams") View v = inflater.inflate(R.layout.dialog_bookmark, null);

        final DataBaseHelper db = DataBaseHelper.getInstance(getActivity());
        final PrefsManager prefs = new PrefsManager(getActivity());
        GlobalState.INSTANCE.init(getActivity());
        final Book book = GlobalState.INSTANCE.getBook();
        final ArrayList<Bookmark> allBookmarks = db.getAllBookmarks(book.getId());
        final GlobalState globalState = GlobalState.INSTANCE;
        globalState.init(getActivity());

        BookmarkAdapter.OnOptionsMenuClickedListener listener = new BookmarkAdapter.OnOptionsMenuClickedListener() {
            @Override
            public void onOptionsMenuClicked(final int position, View v) {
                PopupMenu popup = new PopupMenu(getActivity(), v);
                MenuInflater inflater = popup.getMenuInflater();
                inflater.inflate(R.menu.popup_menu, popup.getMenu());
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        AlertDialogWrapper.Builder builder = new AlertDialogWrapper.Builder(getActivity());
                        switch (item.getItemId()) {
                            case R.id.edit_book:
                                final Bookmark editBookmark = adapter.getItem(position);
                                builder.setTitle(R.string.bookmark_edit_title);
                                final EditText editText = new EditText(getActivity());
                                editText.setInputType(InputType.TYPE_CLASS_TEXT);
                                int padding = getResources().getDimensionPixelSize(R.dimen.horizontal_margin);
                                editText.setPadding(padding, padding, padding, padding);
                                builder.setView(editText);
                                editText.setText(editBookmark.getTitle());
                                builder.setPositiveButton(R.string.dialog_confirm, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        editBookmark.setTitle(editText.getText().toString());
                                        db.updateBookmark(editBookmark);
                                        adapter.notifyItemChanged(position);
                                    }
                                });
                                builder.setNegativeButton(R.string.abort, null);
                                AlertDialog dialog = builder.show();
                                final Button positive = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                                positive.setEnabled(editText.getText().toString().length() > 0);
                                editText.addTextChangedListener(new TextWatcher() {
                                    @Override
                                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                                    }

                                    @Override
                                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                                        positive.setEnabled(s.length() > 0);
                                    }

                                    @Override
                                    public void afterTextChanged(Editable s) {

                                    }
                                });
                                return true;
                            case R.id.delete_book:
                                final Bookmark deleteBookmark = adapter.getItem(position);
                                builder.setTitle(R.string.bookmark_delete_title);
                                builder.setMessage(deleteBookmark.getTitle());
                                builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        db.deleteBookmark(deleteBookmark);
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

            @Override
            public void onBookmarkClicked(int position) {
                Bookmark bookmark = adapter.getItem(position);
                ServiceController controls = new ServiceController(getActivity());
                controls.changeBookPosition(bookmark.getPosition(), bookmark.getTime());
                dialog.cancel();
            }
        };

        final RecyclerView recyclerView = (RecyclerView) v.findViewById(R.id.recycler);
        adapter = new BookmarkAdapter(allBookmarks, listener, book);
        recyclerView.setAdapter(adapter);
        final LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.addItemDecoration(new DividerItemDecoration(getActivity()));
        recyclerView.setLayoutManager(layoutManager);

        final ImageView addButton = (ImageView) v.findViewById(R.id.add);
        final EditText bookmarkTitle = (EditText) v.findViewById(R.id.edit1);
        bookmarkTitle.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    addButton.performClick(); //same as clicking on the +
                    return true;
                }
                return false;
            }
        });

        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                long bookId = book.getId();
                int time = globalState.getTime();
                int position = globalState.getPosition();
                String mediaName = book.getContainingMedia().get(position).getName();

                String title = bookmarkTitle.getText().toString();
                if (title == null || title.equals("")) {
                    title = mediaName;
                }

                Bookmark bookmark = new Bookmark(bookId, position, time);
                bookmark.setTitle(title);

                long id = db.addBookmark(bookmark);
                bookmark.setId(id);
                int index = adapter.addItem(bookmark);
                recyclerView.smoothScrollToPosition(index);
                bookmarkTitle.setText("");
                Toast.makeText(getActivity(), R.string.bookmark_added, Toast.LENGTH_SHORT).show();
                dismiss();
            }
        });

        AlertDialogWrapper.Builder builder = new AlertDialogWrapper.Builder(getActivity());
        builder.setView(v);
        builder.setTitle(R.string.bookmark);
        builder.setNegativeButton(R.string.dialog_cancel, null);
        dialog = builder.create();
        return dialog;
    }
}