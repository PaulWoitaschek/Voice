package de.ph1b.audiobook.interfaces

/**
 * A basic interface that informs if the current layout mode has multiple panels.

 * @author Paul Woitaschek
 */
interface MultiPaneInformer {

    /**
     * true if the layout has multiple panels.
     */
    fun isMultiPanel(): Boolean
}
