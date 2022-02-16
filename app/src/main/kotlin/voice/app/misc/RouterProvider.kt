package voice.app.misc

import com.bluelinelabs.conductor.Router

/**
 * Implementing classes can provide a router
 */
interface RouterProvider {

  fun provideRouter(): Router
}
