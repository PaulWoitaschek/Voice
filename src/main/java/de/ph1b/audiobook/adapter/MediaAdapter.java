package de.ph1b.audiobook.adapter;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;

import de.ph1b.audiobook.R;
import de.ph1b.audiobook.helper.BookDetail;
import de.ph1b.audiobook.helper.DataBaseHelper;
import de.ph1b.audiobook.helper.MediaDetail;


public class MediaAdapter extends BaseAdapter {

    private final ArrayList<BookDetail> data;
    private final Context c;
    private final DataBaseHelper db;


    public MediaAdapter(ArrayList<BookDetail> data, Context c) {
        this.data = data;
        this.c = c;
        db = DataBaseHelper.getInstance(c);
    }

    public int getCount() {
        return data != null ? data.size() : 0;
    }

    public Object getItem(int position) {
        return data.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewHolder viewHolder;
        if (convertView == null) {
            LayoutInflater vi = (LayoutInflater) c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = vi.inflate(R.layout.media_chooser_listview_layout, parent, false);

            viewHolder = new ViewHolder();
            viewHolder.iconImageView = (ImageView) convertView.findViewById(R.id.icon);
            viewHolder.textView = (TextView) convertView.findViewById(R.id.name);
            viewHolder.progressBar = (ProgressBar) convertView.findViewById(R.id.current_progress);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        final BookDetail b = data.get(position);

        //setting text
        String name = b.getName();
        viewHolder.textView.setText(name);


        viewHolder.iconImageView.getViewTreeObserver().addOnPreDrawListener(
                new ViewTreeObserver.OnPreDrawListener() {
                    public boolean onPreDraw() {
                        int height = viewHolder.iconImageView.getMeasuredHeight();
                        int width = viewHolder.iconImageView.getMeasuredWidth();
                        String thumbPath = b.getThumb();
                        Bitmap thumb;
                        if (thumbPath.equals("") || new File(thumbPath).isDirectory()) {
                            thumb = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
                            Canvas c = new Canvas(thumb);
                            Paint textPaint = new Paint();
                            textPaint.setTextSize(2 * width / 3);
                            Resources r = MediaAdapter.this.c.getResources();
                            textPaint.setColor(r.getColor(android.R.color.white));
                            textPaint.setAntiAlias(true);
                            textPaint.setTextAlign(Paint.Align.CENTER);
                            Paint backgroundPaint = new Paint();
                            backgroundPaint.setColor(r.getColor(R.color.file_chooser_audio));
                            c.drawRect(0, 0, width, height, backgroundPaint);
                            int y = (int) ((c.getHeight() / 2) - ((textPaint.descent() + textPaint.ascent()) / 2));
                            c.drawText(b.getName().substring(0, 1).toUpperCase(), width / 2, y, textPaint);
                            viewHolder.iconImageView.setImageBitmap(thumb);
                        } else {
                            viewHolder.iconImageView.setImageURI(Uri.parse(thumbPath));
                            //thumb = BitmapFactory.decodeFile(thumbPath);
                            //thumb = Bitmap.createScaledBitmap(thumb, width, height, false);
                        }
                        return true;
                    }
                }
        );

        //setting bar
        MediaDetail m = db.getMedia(b.getPosition());
        int[] mediaIds = b.getMediaIds();
        int bookDuration = mediaIds.length;
        int bookPosition = 1;
        for (int i = 0; i < mediaIds.length; i++)
            if (mediaIds[i] == b.getPosition())
                bookPosition = i + 1;
        int progressMax = bookDuration * 100;
        viewHolder.progressBar.setMax(progressMax);
        long mediaDuration = m.getDuration();
        long mediaPosition = m.getPosition();
        int progressNow = ((bookPosition - 1) * 100) + (int) (mediaPosition * 100 / mediaDuration);
        viewHolder.progressBar.setProgress(progressNow);

        return convertView;
    }

    static class ViewHolder {
        ImageView iconImageView;
        TextView textView;
        ProgressBar progressBar;
    }
}