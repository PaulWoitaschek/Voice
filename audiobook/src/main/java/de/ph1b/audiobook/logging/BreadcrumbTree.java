package de.ph1b.audiobook.logging;

import android.support.annotation.NonNull;

import org.acra.ACRA;

/**
 * Curtom tree that adds regular logs as custom data to acra.
 */
public class BreadcrumbTree extends FormattedTree {

    private static final int CRUMBS_AMOUNT = 200;
    private int crumbCount = 0;

    public BreadcrumbTree() {
        ACRA.getErrorReporter().clearCustomData();
    }

    @Override
    public void onLogGathered(@NonNull String message) {
        ACRA.getErrorReporter().putCustomData(String.valueOf(getNextCrumbNumber()), message);
    }

    /**
     * Returns the number of the next breadcrumb.
     *
     * @return the next crumb number.
     */
    private int getNextCrumbNumber() {
        // returns current value and increases the next one by 1. When the limit is reached it will
        // reset the crumb.
        int nextCrumb = crumbCount;
        crumbCount++;
        if (crumbCount >= CRUMBS_AMOUNT) {
            crumbCount = 0;
        }
        return nextCrumb;
    }
}
