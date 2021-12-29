package de.ph1b.audiobook.misc

import com.bluelinelabs.conductor.Router

/**
 * Implementing classes can provide a router
 */
interface RouterProvider {

  fun provideRouter(): Router
}
