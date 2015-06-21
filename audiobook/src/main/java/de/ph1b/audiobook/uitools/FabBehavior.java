package de.ph1b.audiobook.uitools;

import android.content.Context;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.util.AttributeSet;
import android.view.View;

import com.getbase.floatingactionbutton.FloatingActionButton;

import java.util.List;

/**
 * Created by Paul Woitaschek (http://www.paul-woitaschek.de, woitaschek@posteo.de)
 * Defines the behavior for the floating action button. If the dependency is a Snackbar, move the
 * fab up.
 */
public class FabBehavior extends CoordinatorLayout.Behavior<FloatingActionButton> {

    private float mTranslationY;

    @SuppressWarnings("unused")
    public FabBehavior() {
        super();
    }

    @SuppressWarnings("unused")
    public FabBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean layoutDependsOn(CoordinatorLayout parent, FloatingActionButton child, View dependency) {
        return dependency instanceof Snackbar.SnackbarLayout;
    }

    @Override
    public boolean onDependentViewChanged(CoordinatorLayout parent, FloatingActionButton child, View dependency) {
        if (dependency instanceof Snackbar.SnackbarLayout) {
            this.updateFabTranslationForSnackbar(parent, child, dependency);
        }

        return false;
    }

    private void updateFabTranslationForSnackbar(CoordinatorLayout parent, FloatingActionButton fab, View snackbar) {
        float translationY = this.getFabTranslationYForSnackbar(parent, fab);
        if (translationY != this.mTranslationY) {
            ViewCompat.animate(fab).cancel();
            if (Math.abs(translationY - this.mTranslationY) == (float) snackbar.getHeight()) {
                ViewCompat.animate(fab).translationY(translationY).setInterpolator(new FastOutSlowInInterpolator()).setListener(null);
            } else {
                ViewCompat.setTranslationY(fab, translationY);
            }

            this.mTranslationY = translationY;
        }

    }

    private float getFabTranslationYForSnackbar(CoordinatorLayout parent, FloatingActionButton fab) {
        float minOffset = 0.0F;
        List dependencies = parent.getDependencies(fab);
        int i = 0;

        for (int z = dependencies.size(); i < z; ++i) {
            View view = (View) dependencies.get(i);
            if (view instanceof Snackbar.SnackbarLayout && parent.doViewsOverlap(fab, view)) {
                minOffset = Math.min(minOffset, ViewCompat.getTranslationY(view) - (float) view.getHeight());
            }
        }

        return minOffset;
    }
}
