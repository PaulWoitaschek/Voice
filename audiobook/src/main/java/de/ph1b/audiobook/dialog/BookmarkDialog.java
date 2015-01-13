package de.ph1b.audiobook.dialog;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ListView;

import com.melnykov.fab.FloatingActionButton;

import java.util.ArrayList;

import de.ph1b.audiobook.R;
import de.ph1b.audiobook.adapter.BookmarkAdapter;
import de.ph1b.audiobook.content.Bookmark;
import de.ph1b.audiobook.utils.MaterialCompatThemer;

/**
 * @author <a href="mailto:woitaschek@posteo.de">Paul Woitaschek</a>
 * @link {http://www.paul-woitaschek.de}
 * @see <a href="http://www.paul-woitaschek.de">http://www.paul-woitaschek.de</a>
 */
public class BookmarkDialog extends DialogFragment {

    public Dialog onCreateDialog(Bundle savedInstanceState) {

        LayoutInflater inflater = getActivity().getLayoutInflater();

        //passing null is fine because of fragment
        @SuppressLint("InflateParams") View v = inflater.inflate(R.layout.dialog_bookmark, null);

        FloatingActionButton fab = (FloatingActionButton) v.findViewById(R.id.fab);
        ListView list = (ListView) v.findViewById(R.id.list1);
        final BookmarkAdapter adapter = new BookmarkAdapter(getActivity(), new ArrayList<Bookmark>());




        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bookmark fakeBookmark = new Bookmark();
                fakeBookmark.setTitle("test title");
                adapter.addItem(fakeBookmark);
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(v);
        builder.setTitle(R.string.bookmark);
        builder.setNegativeButton(R.string.abort, null);
        return builder.create();
    }

    @Override
    public void onStart() {
        super.onStart();
        MaterialCompatThemer.theme(getDialog());
    }

}