package de.ph1b.audiobook.features

import android.support.annotation.StringRes
import android.support.v4.app.FragmentManager
import android.support.v7.app.AppCompatActivity
import com.bluelinelabs.conductor.rxlifecycle.RxController
import rx.Observable


abstract class BaseController : RxController() {
    fun <T> Observable<T>.bindToLifeCycle(): Observable<T> = compose(bindToLifecycle<T>())
    val fragmentManager: FragmentManager
        get() = (activity as AppCompatActivity).supportFragmentManager

    fun getString(@StringRes resId: Int): String = activity.getString(resId)
}