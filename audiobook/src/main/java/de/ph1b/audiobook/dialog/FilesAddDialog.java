package de.ph1b.audiobook.dialog;


import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import de.ph1b.audiobook.R;

public class FilesAddDialog extends DialogFragment {


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        LayoutInflater inflater = getActivity().getLayoutInflater();
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        //passing null is fine because of fragment
        @SuppressLint("InflateParams") View v = inflater.inflate(R.layout.dialog_files_add, null);

        EditText bookTitle = (EditText) v.findViewById(R.id.book_name);

        builder.setView(v);

        builder.setNegativeButton(R.string.choose_time_cancel, null);

        return builder.create();
    }

}
