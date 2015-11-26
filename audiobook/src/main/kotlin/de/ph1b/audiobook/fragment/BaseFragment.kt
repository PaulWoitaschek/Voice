package de.ph1b.audiobook.fragment

import android.support.v4.app.Fragment
import de.ph1b.audiobook.injection.App

/**
 * Base fragment all fragments should inherit from. Handles memory leak detection.
 *
 * @author Paul Woitaschek
 */
open class BaseFragment : Fragment() {

    override fun onDestroy() {
        super.onDestroy()

        App.leakWatch(this)
    }
}