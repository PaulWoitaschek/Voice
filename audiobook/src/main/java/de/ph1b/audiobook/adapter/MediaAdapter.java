package de.ph1b.audiobook.adapter;

import android.app.ActivityManager;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.support.v4.util.LruCache;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.ph1b.audiobook.BuildConfig;
import de.ph1b.audiobook.R;
import de.ph1b.audiobook.content.BookDetail;
import de.ph1b.audiobook.content.DataBaseHelper;
import de.ph1b.audiobook.interfaces.OnItemClickListener;
import de.ph1b.audiobook.utils.CoverDownloader;
import de.ph1b.audiobook.utils.ImageHelper;


public class MediaAdapter extends RecyclerView.Adapter<MediaAdapter.ViewHolder> implements ComponentCallbacks2 {

    private final ArrayList<BookDetail> data;
    private final DataBaseHelper db;
    private final Context c;
    private final OnItemClickListener onItemClickListener;
    private final ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();
    private final ImageCache imageCache;
    private final OnCoverChangedListener onCoverChangedListener;


    public MediaAdapter(ArrayList<BookDetail> data, Context c, OnItemClickListener onItemClickListener, OnCoverChangedListener onCoverChangedListener) {
        this.data = data;
        this.c = c;
        this.onItemClickListener = onItemClickListener;
        this.onCoverChangedListener = onCoverChangedListener;

        ActivityManager am = (ActivityManager) c.getSystemService(Context.ACTIVITY_SERVICE);
        int memoryClassBytes = am.getMemoryClass() * 1024 * 1024;
        imageCache = new ImageCache(memoryClassBytes / 8);

        db = DataBaseHelper.getInstance(c);
    }

    public void updateItem(final BookDetail book) {
        for (int position = 0; position < data.size(); position++) {
            if (data.get(position).getId() == book.getId()) {
                data.set(position, book);
                imageCache.remove(book.getId());
                notifyItemChanged(position);
                singleThreadExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        db.updateBook(book);
                    }
                });
            }
        }
    }

    public BookDetail getItem(int position) {
        return data.get(position);
    }

    public ArrayList<BookDetail> getData() {
        return data;
    }

    /**
     * Removes an item from the grid, deletes it from database and notifys the adapter about the item removed
     *
     * @param position The position of the item to be removed
     */
    public void removeItem(int position) {
        final BookDetail bookToRemove = getItem(position);
        data.remove(position);
        notifyItemRemoved(position);
        singleThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                db.deleteBook(bookToRemove);
            }
        });
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int i) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.media_chooser_recycler_grid_item, parent, false);
        return new ViewHolder(v, onItemClickListener);
    }

    /**
     * Swaps elements in the detailList and saves them to the database.
     *
     * @param from The first book to swap
     * @param to   The second book to swap
     */
    public void swapItems(int from, int to) {
        if (BuildConfig.DEBUG)
            Log.d("madapt", "swap items from to" + String.valueOf(from) + "/" + String.valueOf(to));
        final int finalFrom = from;
        if (from != to) {
            if (from > to) {
                while (from > to) {
                    swapItemsInData(from, --from);
                }
            } else {
                while (from < to) {
                    swapItemsInData(from, ++from);
                }
            }
        }
        if (BuildConfig.DEBUG)
            Log.d("madapt", "notifyItemMoved" + finalFrom + "/" + to);
        notifyItemMoved(finalFrom, to);
    }

    private void swapItemsInData(int from, int to) {
        if (BuildConfig.DEBUG)
            Log.d("madapt", "swapInData:" + from + "/" + to);
        final BookDetail oldBook = data.get(from);
        final BookDetail newBook = data.get(to);
        int oldSortId = oldBook.getSortId();
        int newSortId = newBook.getSortId();
        oldBook.setSortId(newSortId);
        newBook.setSortId(oldSortId);
        data.set(from, newBook);
        data.set(to, oldBook);
        singleThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                db.updateBook(newBook);
                db.updateBook(oldBook);
            }
        });
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {
        BookDetail b = data.get(position);

        //setting text
        String name = b.getName();
        viewHolder.titleView.setText(name);
        viewHolder.titleView.setActivated(true);

        Bitmap cached = imageCache.get(b.getId());
        if (cached != null) {
            if (BuildConfig.DEBUG) Log.d("madapt", "cached bitmap! " + b.getId());
            viewHolder.coverView.setImageBitmap(cached);
        } else {
            viewHolder.coverView.setImageBitmap(null);
            viewHolder.coverView.setTag(b.getId());
            String coverPath = b.getCover();
            boolean coverNotExists = (coverPath == null || coverPath.equals("") || new File(coverPath).isDirectory() || !(new File(coverPath).exists()));
            LoadCoverAsync loadCoverAsync = new LoadCoverAsync(position, viewHolder.coverView);
            if (coverNotExists)
                loadCoverAsync.execute();
            else
                loadCoverAsync.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    @Override
    public void onTrimMemory(int level) {
        if (level >= TRIM_MEMORY_MODERATE) {
            imageCache.evictAll();
        } else if (level >= TRIM_MEMORY_BACKGROUND) {
            imageCache.trimToSize(imageCache.size() / 2);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {

    }

    @Override
    public void onLowMemory() {

    }

    public interface OnCoverChangedListener {
        public void onCoverChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView coverView;
        final TextView titleView;
        final ImageButton editBook;

        public ViewHolder(View itemView, final OnItemClickListener onItemClickListener) {
            super(itemView);
            coverView = (ImageView) itemView.findViewById(R.id.cover);
            titleView = (TextView) itemView.findViewById(R.id.title);
            editBook = (ImageButton) itemView.findViewById(R.id.editBook);

            coverView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onItemClickListener.onCoverClicked(getPosition());
                }
            });
            editBook.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onItemClickListener.onPopupMenuClicked(v, getPosition());
                }
            });
        }
    }

    /**
     * Loads cover Async into the ViewHolder. If there is no cover specified and the devices is allowed
     * to go online, it will download an image from the internet and save it to the database.
     * If there is no cover and the device is not allowed to go online, a default capital-letter will
     * be generated and NOT be written to the database.
     * In any case the bitmap will be put to the lru cache.
     *
     * @see de.ph1b.audiobook.adapter.MediaAdapter.ImageCache
     * @see de.ph1b.audiobook.utils.CoverDownloader
     */
    private class LoadCoverAsync extends AsyncTask<Void, Void, Bitmap> {

        private final WeakReference<ImageView> weakReference;
        private final int position;
        private final BookDetail book;

        public LoadCoverAsync(int position, ImageView imageView) {
            this.position = position;
            weakReference = new WeakReference<>(imageView);
            book = data.get(position);
        }

        @Override
        protected Bitmap doInBackground(Void... params) {

            String coverPath = book.getCover();
            boolean coverNotExists = (coverPath == null || coverPath.equals("") || new File(coverPath).isDirectory() || !(new File(coverPath).exists()));
            Bitmap bitmap = null;

            //try to load a cover 3 times
            if (coverNotExists) {
                if (ImageHelper.isOnline(c))
                    for (int i = 0; i < 3; i++) {
                        bitmap = CoverDownloader.getCover(book.getName(), c, i);
                        if (bitmap != null) {
                            String savedCoverPath = ImageHelper.saveCover(bitmap, c);
                            book.setCover(savedCoverPath);
                            data.set(position, book);
                            db.updateBook(book);
                            break;
                        }
                    }
            } else {
                bitmap = ImageHelper.genBitmapFromFile(coverPath, c, ImageHelper.TYPE_MEDIUM);
            }

            if (bitmap == null)
                bitmap = ImageHelper.genCapital(book.getName(), c, ImageHelper.TYPE_MEDIUM);

            //save bitmap to lru cache
            imageCache.put(book.getId(), bitmap);

            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap != null) {
                ImageView imageView = weakReference.get();
                if (imageView != null && (Integer) imageView.getTag() == book.getId()) {
                    imageView.setImageBitmap(bitmap);
                    onCoverChangedListener.onCoverChanged();
                }
            }
        }
    }

    private class ImageCache extends LruCache<Integer, Bitmap> {

        public ImageCache(int maxSizeBytes) {
            super(maxSizeBytes);
        }

        @Override
        protected int sizeOf(Integer key, Bitmap value) {
            return value.getByteCount();
        }
    }
}