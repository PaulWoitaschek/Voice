package de.ph1b.audiobook.misc


import kotlinx.coroutines.experimental.asCoroutineDispatcher
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

val IO = ThreadPoolExecutor(0, Integer.MAX_VALUE, 1L, TimeUnit.SECONDS, SynchronousQueue())
    .asCoroutineDispatcher()
