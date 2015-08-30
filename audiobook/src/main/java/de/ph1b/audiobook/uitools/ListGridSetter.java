package de.ph1b.audiobook.uitools;

import android.content.res.Resources;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;

import de.ph1b.audiobook.R;

/**
 * Helper class that manages the different layout modes and sets the layout managers accordingly.
 *
 * @author Paul Woitaschek
 */
public class ListGridSetter {

    private final RecyclerView.ItemDecoration listDecoration;
    private final RecyclerView recyclerView;
    private final RecyclerView.LayoutManager gridLayoutManager;
    private final RecyclerView.LayoutManager linearLayoutManager;
    @Nullable
    private DisplayMode displayMode;

    public ListGridSetter(RecyclerView recyclerView) {
        this.recyclerView = recyclerView;
        listDecoration = new DividerItemDecoration(recyclerView.getContext());
        gridLayoutManager = new GridLayoutManager(recyclerView.getContext(), getAmountOfColumns());
        linearLayoutManager = new LinearLayoutManager(recyclerView.getContext());
    }

    @DrawableRes
    public int getMenuIcon() {
        return displayMode == DisplayMode.GRID ? R.drawable.ic_view_list_white_24dp :
                R.drawable.ic_view_grid_white_24dp;
    }

    @Nullable
    public DisplayMode getDisplayMode() {
        return displayMode;
    }

    public void setDisplayMode(@NonNull DisplayMode displayMode) {
        if (this.displayMode != displayMode) {
            this.displayMode = displayMode;

            recyclerView.invalidateItemDecorations();
            if (displayMode == DisplayMode.GRID) {
                recyclerView.setLayoutManager(gridLayoutManager);
            } else {
                recyclerView.setLayoutManager(linearLayoutManager);
                recyclerView.addItemDecoration(listDecoration);
            }
        }
    }

    /**
     * Returns the amount of columns the main-grid will need.
     *
     * @return The amount of columns, but at least 2.
     */
    private int getAmountOfColumns() {
        Resources r = recyclerView.getResources();
        DisplayMetrics displayMetrics = r.getDisplayMetrics();
        float widthPx = displayMetrics.widthPixels;
        float desiredPx = r.getDimensionPixelSize(R.dimen.desired_medium_cover);
        int columns = Math.round(widthPx / desiredPx);
        return Math.max(columns, 2);
    }

    public enum DisplayMode {
        GRID,
        LIST
    }
}
