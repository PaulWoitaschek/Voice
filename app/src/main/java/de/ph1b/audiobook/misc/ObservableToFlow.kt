package de.ph1b.audiobook.misc

import io.reactivex.BackpressureStrategy
import io.reactivex.Observable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.flow.asFlow

fun <T : Any> Observable<T>.latestAsFlow(): Flow<T> = toFlowable(BackpressureStrategy.LATEST).asFlow()
