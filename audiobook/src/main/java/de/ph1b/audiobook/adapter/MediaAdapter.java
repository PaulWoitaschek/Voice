package de.ph1b.audiobook.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

import de.ph1b.audiobook.R;
import de.ph1b.audiobook.fragment.BookChooseFragment;
import de.ph1b.audiobook.utils.BookDetail;
import de.ph1b.audiobook.utils.CommonTasks;
import de.ph1b.audiobook.utils.DataBaseHelper;


public class MediaAdapter extends BaseAdapter {

    private final ArrayList<BookDetail> data;
    private final DataBaseHelper db;
    private final ArrayList<Integer> checkedBookIds = new ArrayList<Integer>();
    private final BookChooseFragment fragment;


    public MediaAdapter(ArrayList<BookDetail> data, BookChooseFragment a) {
        this.data = data;
        this.fragment = a;
        db = DataBaseHelper.getInstance(fragment.getActivity());
    }

    public int getCount() {
        return data != null ? data.size() : 0;
    }

    public BookDetail getItem(int position) {
        return data.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    public void setBookChecked(int bookId, Boolean checked) {
        if (checked)
            checkedBookIds.add(bookId);
        else
            checkedBookIds.remove(Integer.valueOf(bookId)); //integer value of to prevent accessing by position instead of object
    }

    public ArrayList<BookDetail> getCheckedBooks() {
        ArrayList<BookDetail> books = new ArrayList<BookDetail>();
        for (BookDetail b : data)
            for (Integer i : checkedBookIds)
                if (b.getId() == i)
                    books.add(b);
        if (books.size() > 0)
            return books;
        return null;
    }

    private void updateBookInData(BookDetail book) {
        for (int i = 0; i < data.size(); i++) {
            if (data.get(i).getId() == book.getId()) {
                data.set(i, book);
            }
        }
    }


    public void unCheckAll() {
        checkedBookIds.clear();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewHolder viewHolder;
        if (convertView == null) {
            LayoutInflater vi = (LayoutInflater) fragment.getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = vi.inflate(R.layout.media_chooser_listview_layout, parent, false);

            viewHolder = new ViewHolder();
            viewHolder.iconImageView = (ImageView) convertView.findViewById(R.id.icon);
            viewHolder.textView = (TextView) convertView.findViewById(R.id.name);
            viewHolder.progressBar = (ProgressBar) convertView.findViewById(R.id.current_progress);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        BookDetail b = data.get(position);

        //setting text
        String name = b.getName();
        viewHolder.textView.setText(name);

        String thumbPath = b.getThumb();
        if (thumbPath == null || thumbPath.equals("") || new File(thumbPath).isDirectory() || !(new File(thumbPath).exists())) {
            int px = CommonTasks.getThumbDimensions(fragment.getResources());
            viewHolder.iconImageView.setImageBitmap(CommonTasks.genCapital(b.getName(), px, fragment.getResources()));
            WeakReference<ImageView> weakReference = new WeakReference<ImageView>(viewHolder.iconImageView);

            //if device is online try to load image in the background!
            if (CommonTasks.isOnline(fragment.getActivity()))
                new AddCover(weakReference, b).execute();
        } else {
            viewHolder.iconImageView.setImageURI(Uri.parse(thumbPath));
        }

        //setting bar
        viewHolder.progressBar.setMax(1000);
        viewHolder.progressBar.setProgress(db.getGlobalProgress(b));

        return convertView;
    }

    private class AddCover extends AsyncTask<Void, Void, String> {

        private final WeakReference<ImageView> weakReference;
        private final BookDetail book;

        public AddCover(WeakReference<ImageView> weakReference, BookDetail book) {
            this.weakReference = weakReference;
            this.book = book;
        }


        @Override
        protected String doInBackground(Void... voids) {
            Bitmap bitmap = CommonTasks.genCoverFromInternet(book.getName(), 0, fragment.getActivity());
            String[] coverPaths = CommonTasks.saveCovers(bitmap, fragment.getActivity());
            if (coverPaths != null) {
                book.setCover(coverPaths[0]);
                book.setThumb(coverPaths[1]);
                db.updateBook(book);
                updateBookInData(book);
                return coverPaths[1];
            }
            return null;
        }


        @Override
        protected void onPostExecute(String thumbPath) {
            if (thumbPath != null && weakReference != null) {
                ImageView imageView = weakReference.get();
                if (imageView != null) {
                    imageView.setImageBitmap(BitmapFactory.decodeFile(thumbPath));
                }
            }
            //re-inits player widget to show new cover
            fragment.initPlayerWidget();
        }
    }


    static class ViewHolder {
        ImageView iconImageView;
        TextView textView;
        ProgressBar progressBar;
    }
}