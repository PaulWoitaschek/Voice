package de.ph1b.audiobook.dialog;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import de.ph1b.audiobook.R;
import de.ph1b.audiobook.content.MediaDetail;


public class FileAddingErrorDialog extends DialogFragment {


    public static final String TAG = "FileAddingErrorDialog";
    public static final String ARG_ERROR_FILES = TAG + ".ARG_ERROR_FILES";
    public static final String ARG_INTACT_FILES = TAG + ".ARG_INTACT_FILES";
    public static final String ARG_BOOK_ID = TAG + ".ARG_BOOK_ID";

    public interface ConfirmationListener {
        public void onButtonClicked(boolean keep, ArrayList<MediaDetail> intactFiles, int bookId);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        LayoutInflater inflater = getActivity().getLayoutInflater();
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.error_in_file_title));

        ArrayList<String> errorFiles = getArguments().getStringArrayList(ARG_ERROR_FILES);
        final ArrayList<MediaDetail> intactFiles = getArguments().getParcelableArrayList(ARG_INTACT_FILES);
        final int bookId = getArguments().getInt(ARG_BOOK_ID);

        //passing null is fine because of fragment
        @SuppressLint("InflateParams") View v = inflater.inflate(R.layout.dialog_file_adding_error, null);
        ListView listView = (ListView) v.findViewById(R.id.listView);
        TextView textView = (TextView) v.findViewById(R.id.textView);
        textView.setText(getString(R.string.error_in_file_description));
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_list_item_1, errorFiles);
        listView.setAdapter(adapter);

        builder.setView(v);
        builder.setPositiveButton(getResources().getText(R.string.dialog_confirm), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ConfirmationListener fragment = (ConfirmationListener) getTargetFragment();
                if (fragment != null) {
                    fragment.onButtonClicked(true, intactFiles, bookId);
                }
            }
        });
        builder.setNegativeButton(getResources().getText(R.string.dialog_cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ConfirmationListener fragment = (ConfirmationListener) getTargetFragment();
                if (fragment != null) {
                    fragment.onButtonClicked(false, intactFiles, bookId);
                }
            }
        });

        return builder.create();
    }
}
