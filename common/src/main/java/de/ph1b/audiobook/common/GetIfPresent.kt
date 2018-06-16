package de.ph1b.audiobook.common

import io.reactivex.Observable

fun <T : Any> Observable<Optional<T>>.getIfPresent(): Observable<T> {
  return filter { it is Optional.Present }
    .map { (it as Optional.Present).value }
}
