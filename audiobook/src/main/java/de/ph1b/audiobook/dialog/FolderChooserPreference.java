package de.ph1b.audiobook.dialog;

import android.content.Context;
import android.content.Intent;
import android.preference.DialogPreference;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.melnykov.fab.FloatingActionButton;

import java.util.ArrayList;

import de.ph1b.audiobook.R;
import de.ph1b.audiobook.activity.FolderChooserActivity;
import de.ph1b.audiobook.utils.Prefs;


public class FolderChooserPreference extends DialogPreference implements View.OnClickListener {


    public FolderChooserPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDialogLayoutResource(R.layout.dialog_folder_overview);
    }

    @Override
    protected void onBindDialogView(@NonNull View view) {
        super.onBindDialogView(view);

        ListView listView = (ListView) view.findViewById(R.id.list1);
        FloatingActionButton fab = (FloatingActionButton) view.findViewById(R.id.fab);

        ArrayList<String> folders = Prefs.getAudiobookDirs(getContext());

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, folders);

        listView.setAdapter(adapter);

        fab.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.fab:
                Intent i = new Intent(getContext(), FolderChooserActivity.class);
                getContext().startActivity(i);
                break;
            default:
                break;
        }
    }
}
