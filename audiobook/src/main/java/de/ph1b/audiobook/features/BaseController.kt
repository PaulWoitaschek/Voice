package de.ph1b.audiobook.features

import android.os.Bundle
import android.support.annotation.StringRes
import android.support.v4.app.FragmentManager
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import com.bluelinelabs.conductor.rxlifecycle.RxController
import rx.Observable


abstract class BaseController : RxController {

    constructor(args: Bundle) : super(args)
    constructor() : super()

    fun <T> Observable<T>.bindToLifeCycle(): Observable<T> = compose(bindToLifecycle<T>())
    val fragmentManager: FragmentManager
        get() = activity.supportFragmentManager

    fun getString(@StringRes resId: Int): String = activity.getString(resId)

    val activity: AppCompatActivity
        get() = getActivity() as AppCompatActivity

    fun layoutInflater(): LayoutInflater = activity.layoutInflater
}