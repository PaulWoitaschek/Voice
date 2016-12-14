package de.ph1b.audiobook.misc

import com.bluelinelabs.conductor.Router

/**
 * Implementing classes can provide a router
 *
 * @author Paul Woitaschek
 */
interface RouterProvider {
  fun provideRouter(): Router
}